package pgame2.gameflow;

import java.util.ArrayList;
import java.math.BigDecimal;
import pgame2.common.Modifier;
import static pgame2.effect.EffectTarget.*;
import java.util.List;
import java.util.random.RandomGenerator;
import pgame2.battle.SimpleBattle;
import pgame2.battle.AutoBattle;
import pgame2.battle.BattleStats;
import pgame2.event.SituationEvents;
import pgame2.common.ModifierContext;
import pgame2.effect.EffectTarget;
import pgame2.effect.GameEffect;
import pgame2.item.Item;
import pgame2.item.ItemEffectManager;
import pgame2.objective.SmallObjectives;
import pgame2.train.Train;
import pgame2.train.TrainingAbility;
import pgame2.work.Work;

/** 数値はメニュー画像を参考にした調整前の試作用設定。 */
public class GameSession {
    /** 試作の体力上限。 */
    public static final int MAX_HP = 100;
    /** 一日に使用できる時間。 */
    public static final int DAILY_HOURS = 12;
    /** 生活費 */
    public static final int LIVING_COST = 50;

    /** 現在の日数。 */
    private int day = 1;
    /** 残り時間。 */
    private int hours = DAILY_HOURS;
    /** 持ち越す体力。 */
    private int hp = MAX_HP;
    /** アイテムで成長する、このゲームのHP上限。 */
    private int maxHp = MAX_HP;
    /** 所持プルン。 */
    private int plun;
    /** 蓄積した労働効率。 */
    private int workEfficiency;
    /** 購入情報と購入後の累積効果。 */
    private final ItemEffectManager items = new ItemEffectManager();
    /** 当日の行商人の商品。購入しても同じ来訪中は一覧に残す。 */
    private List<Item> travelerStock = List.of();
    /** 商品抽選済みの日。 */
    private int travelerStockDay = -1;
    /** 直近の日終了時の実際の生活費支払い額。 */
    private int lastLivingCostPaid;
    /** 直近の日終了時に生活費不足があったか。 */
    private boolean lastLivingCostShortage;

    /** 襲撃の抽選元。 */
    private final RandomGenerator random;
    /** 簡易戦闘の処理。 */
    private final SimpleBattle battle = new SimpleBattle();
    /** 主人公の戦闘能力。鍛錬時に更新する。 */
    private BattleStats battleStats = BattleStats.INITIAL;
    /** 結果確認を待っている戦闘。 */
    private AutoBattle pendingBattle;
    /** 状況変化の管理。 */
    private final SituationEvents events;
    /** 小目標の管理。 */
    private final SmallObjectives objectives;
    /** 次の操作前に通知する内容。 */
    private final List<String> notices = new ArrayList<>();
    /** 行動結果に追記する襲撃結果。 */
    private String battleMessage = "";

    public GameSession() { this(RandomGenerator.getDefault()); }

    /** 抽選元を注入して再現可能な進行を作る。 */
    public GameSession(RandomGenerator random) {
        this.random = random;
        plun = 100;
        events = new SituationEvents(random);
        objectives = new SmallObjectives(random);
        beginDay();
    }

    /** 補正済みのコマンド効果。 */
    public record Effects(BigDecimal hours, BigDecimal hp, BigDecimal plun, BigDecimal workEfficiency) { }

    /** 基本値に労働効率とイベントの補正を合算し、端数を保持する。 */
    public Effects effects(Action action) {
        BigDecimal hpChange = BigDecimal.valueOf(action.hp);
        BigDecimal income = BigDecimal.valueOf(action.plun);
        BigDecimal growth = BigDecimal.valueOf(action.workEfficiency);
        BigDecimal duration = BigDecimal.valueOf(action.hours);
        if (action == Action.WORK) {
            hpChange = calculate(WORK_HP_COST, -action.hp).negate();
            income = calculate(WORK_PLUN_REWARD, action.plun,
                    new Modifier(Modifier.Type.PERCENT, BigDecimal.valueOf(workEfficiency)
                            .multiply(BigDecimal.valueOf(Work.EFFICIENCY_REWARD_PERCENT))));
        }
        if (action == Action.TRAIN) {
            hpChange = calculate(TRAIN_HP_COST, -action.hp).negate();
            growth = calculate(TRAIN_WORK_EFFICIENCY_GAIN, action.workEfficiency);
        }
        if (action == Action.REST) {
            duration = calculate(REST_HOURS_COST, action.hours);
            hpChange = calculate(REST_HP_RECOVERY, action.hp);
        }
        return new Effects(duration, hpChange, income, growth);
    }

    /** 計算ごとに空のContextへ全補正を収集し、最後に基本値へ適用する。 */
    private BigDecimal calculate(EffectTarget target, int base, Modifier... intrinsic) {
        ModifierContext context = new ModifierContext();
        for (Modifier modifier : intrinsic) context.add(modifier);
        GameEffect.State state = effectState();
        events.collect(target, state, context);
        items.collect(target, state, context);
        return context.calculate(BigDecimal.valueOf(base));
    }

    /** 効果に渡す読み取り専用の状態を確定する。 */
    private GameEffect.State effectState() { return new GameEffect.State(hp, maxHp, plun, day); }

    /** コマンドを識別し、各機能の基本設定を共通の増減値として公開する。 */
    public enum Action {
        WORK("労働", "体を使ってプルンを稼ぐ", Work.DEFAULT.getHours(),
                -Work.DEFAULT.getHpCost(), Work.DEFAULT.getPlunReward(), Work.DEFAULT.getWorkEfficiencyGain()),
        REST("休息", "ゆっくり休んで体力を回復", 3, 25, 0, 0),
        HEAL("回復魔法", "プルンを使って体力を大きく回復", 1, 40, -15, 0),
        TRAIN("鍛錬", "時間と育てる能力を選ぶ", Train.DEFAULT.getHours(),
                -Train.DEFAULT.getHpCost(), Train.DEFAULT.getPlunReward(), Train.DEFAULT.getWorkEfficiencyGain());
        /** 表示する行動名。 */
        public final String title;
        /** 行動の目的。 */
        public final String description;
        /** 消費時間。 */
        public final int hours;
        /** 体力の増減。 */
        public final int hp;
        /** プルンの増減。 */
        public final int plun;
        /** 労働効率の増減。 */
        public final int workEfficiency;

        Action(String title, String description, int hours, int hp, int plun, int workEfficiency) {
            this.title = title;
            this.description = description;
            this.hours = hours;
            this.hp = hp;
            this.plun = plun;
            this.workEfficiency = workEfficiency;
        }
    }

    /** 実行不可の理由。HPを消費し尽くす行動は死亡判定の対象として許可する。 */
    public String unavailableReason(Action action) {
        if (action == Action.TRAIN) return trainingUnavailableReason(TrainingAbility.WORK_EFFICIENCY, action.hours);
        if (isGameOver()) return "ゲームオーバーです";
        if (pendingBattle != null) return "戦闘結果を確認してください";
        Effects effect = effects(action);
        if (BigDecimal.valueOf(hours).compareTo(effect.hours()) < 0
                && !(hours > 0 && items.allowsOvertime())) return "時間が足りません";
        if (BigDecimal.valueOf(plun).add(effect.plun()).signum() < 0) return "プルンが足りません";
        return "";
    }

    /** 実行条件を確認して資源へ反映する。 */
    public void execute(Action action) {
        if (action == Action.TRAIN) { train(TrainingAbility.WORK_EFFICIENCY, action.hours); return; }
        String reason = unavailableReason(action);
        if (!reason.isEmpty()) throw new IllegalStateException(reason);
        Effects effect = effects(action);
        BigDecimal timeCost = items.allowsOvertime() ? effect.hours().min(BigDecimal.valueOf(hours)) : effect.hours();
        changeHours(timeCost.negate());
        changeHp(effect.hp());
        changePlun(effect.plun());
        changeWorkEfficiency(effect.workEfficiency());
        battleMessage = "";
        if (isGameOver()) return;
        if (action == Action.WORK && battle.occurs(random, calculate(BATTLE_CHANCE, SimpleBattle.BASE_CHANCE))) {
            pendingBattle = new AutoBattle(battleStats, BattleStats.INITIAL);
            battleMessage = "帰り道でトカゲ戦士に遭遇した！";
        }
        if (action == Action.WORK) items.workCompleted();
        if (action == Action.REST) applyRecovery(items.restCompleted());
        objectives.recordAction(action == Action.WORK);
        evaluateObjective(false);
    }

    /** 表示と実行で共有する鍛錬の最終結果。 */
    public record TrainingPreview(TrainingAbility ability, int hours, int hpAfter, int before, int after) {
        public int gain() { return after - before; }
    }
    /** 育成対象の現在値を返す。 */
    public int abilityValue(TrainingAbility ability) {
        return switch (ability) {
            case WORK_EFFICIENCY -> workEfficiency;
            case ATTACK -> battleStats.attack();
            case DEFENSE -> battleStats.defense();
            case TOUGHNESS -> battleStats.toughness();
            case SPEED -> battleStats.speed();
        };
    }
    /** 使用状態を変えずに、補正と端数処理後の結果を予測する。 */
    public TrainingPreview previewTraining(TrainingAbility ability, int duration) {
        java.util.Objects.requireNonNull(ability);
        if (duration < 1 || duration > getDailyHours()) throw new IllegalArgumentException("鍛錬時間が範囲外です");
        int before = abilityValue(ability);
        int after = Modifier.floor(BigDecimal.valueOf(before).add(calculate(ability.effectTarget(), Math.multiplyExact(duration, ability.gainPerHour))));
        int hpAfter = Modifier.floor(BigDecimal.valueOf(hp).subtract(calculate(TRAIN_HP_COST, Math.multiplyExact(duration, Train.HP_COST_PER_HOUR))).max(BigDecimal.ZERO));
        return new TrainingPreview(ability, duration, hpAfter, before, after);
    }
    /** 砂時計の有無に関係なく、指定時間を確保する。 */
    public String trainingUnavailableReason(TrainingAbility ability, int duration) {
        if (ability == null) return "能力を選択してください";
        if (isGameOver()) return "ゲームオーバーです";
        if (pendingBattle != null) return "戦闘結果を確認してください";
        if (duration < 1) return "1時間以上を指定してください";
        if (duration > hours || duration > getDailyHours()) return "時間が足りません";
        return "";
    }
    /** 能力と資源の反映後に使用日と小目標を更新する。 */
    public TrainingPreview train(TrainingAbility ability, int duration) {
        String reason = trainingUnavailableReason(ability, duration);
        if (!reason.isEmpty()) throw new IllegalStateException(reason);
        TrainingPreview result = previewTraining(ability, duration);
        BattleStats next = switch (ability) {
            case WORK_EFFICIENCY -> battleStats;
            case ATTACK -> new BattleStats(result.after(), battleStats.defense(), battleStats.toughness(), battleStats.speed());
            case DEFENSE -> new BattleStats(battleStats.attack(), result.after(), battleStats.toughness(), battleStats.speed());
            case TOUGHNESS -> new BattleStats(battleStats.attack(), battleStats.defense(), result.after(), battleStats.speed());
            case SPEED -> new BattleStats(battleStats.attack(), battleStats.defense(), battleStats.toughness(), result.after());
        };
        hours -= duration;
        hp = result.hpAfter();
        battleStats = next;
        if (ability == TrainingAbility.WORK_EFFICIENCY) workEfficiency = result.after();
        items.trainingCompleted(ability.effectTarget(), day);
        battleMessage = "";
        if (isGameOver()) return result;
        objectives.recordTraining(result.gain());
        evaluateObjective(false);
        return result;
    }

    /** 死亡判定を優先し、実行可能な行動がなくなったら日を終了する。 */
    public boolean shouldEndDay() {
        if (isGameOver() || pendingBattle != null) return false;
        if (trainingUnavailableReason(TrainingAbility.WORK_EFFICIENCY, 1).isEmpty()) return false;
        for (Action action : Action.values()) {
            if (unavailableReason(action).isEmpty()) return false;
        }
        return true;
    }

    /** 当日の期限判定後、翌日の状況と日次効果を適用する。 */
    public void nextDay() {
        if (isGameOver()) throw new IllegalStateException("ゲームオーバー後は翌日に進めません");
        if (pendingBattle != null) throw new IllegalStateException("戦闘結果を確認してください");
        finishDay();
        if (isGameOver()) return;
        day++;
        beginDay();
    }

    /** 開催状態を切り替えてから朝の効果と予告を適用する。 */
    private void beginDay() {
        String notice = events.startDay(day);
        if (!notice.isEmpty()) notices.add(notice.strip());
        resetDailyHours();
        if (events.restoresHpAtDayStart()) restoreHpToMax();
        if (objectives.startDay(day, plun)) notices.add("小目標予告\n" + objectives.describe(plun));
    }

    /** 一日の終了直前の処理がある場合ここで行う */
    private void finishDay(){
        evaluateObjective(true);
        applyRecovery(items.dayEnd(effectState()));
        payLivingCost();
    }

    /** 条件判定後の成長と回復を一括反映する。最大HP増加分は現在HPにも加える。 */
    private void applyRecovery(GameEffect.Recovery recovery) {
        maxHp = Math.addExact(maxHp, recovery.maxHp());
        changeHp(BigDecimal.valueOf((long) recovery.maxHp() + recovery.hp()));
    }

    /** 生活費を支払う。払えない場合は残りの全plunを失い、hp-50（暫定） */
    private void payLivingCost(){
        int cost = getLivingCost();
        lastLivingCostShortage = plun < cost;
        lastLivingCostPaid = Math.max(0, Math.min(plun, cost));
        if(!lastLivingCostShortage){
            changePlun(BigDecimal.valueOf(-cost));
        } else {
            changePlun(BigDecimal.valueOf(-lastLivingCostPaid));
            changeHp(BigDecimal.valueOf(-50));
        }
    }

    /** 達成報酬を一度だけ付与し、操作前の通知へ送る。 */
    private void evaluateObjective(boolean endOfDay) {
        SmallObjectives.Result result = objectives.evaluate(day, plun, endOfDay);
        if (result == SmallObjectives.Result.NONE) return;
        if (result == SmallObjectives.Result.SUCCESS) changePlun(BigDecimal.valueOf(SmallObjectives.REWARD));
        notices.add(objectives.describe(plun));
    }

    /** 通知を取り出して重複表示を防ぐ。 */
    public List<String> drainNotices() {
        List<String> result = List.copyOf(notices);
        notices.clear();
        return result;
    }

    /** HPの最終値を切り捨て、既存の上下限を適用する。 */
    public void changeHp(BigDecimal delta) {
        hp = Modifier.floor(BigDecimal.valueOf(hp).add(delta).max(BigDecimal.ZERO).min(BigDecimal.valueOf(maxHp)));
    }

    /** 所持プルンの最終値を切り捨てる。 */
    public void changePlun(BigDecimal delta) {
        int before = plun;
        plun = Modifier.floor(BigDecimal.valueOf(plun).add(delta));
        if (plun < before) items.plunSpent((long) before - plun);
    }

    /** 労働効率の最終値を切り捨てる。 */
    public void changeWorkEfficiency(BigDecimal delta) { workEfficiency = Modifier.floor(BigDecimal.valueOf(workEfficiency).add(delta)); }

    /** 残り時間の最終値を切り捨てる。 */
    public void changeHours(BigDecimal delta) { hours = Modifier.floor(BigDecimal.valueOf(hours).add(delta)); }

    /** 朝の全回復を適用する。 */
    private void restoreHpToMax() { hp = maxHp; }

    /** 翌日の使用可能時間にリセットする。 */
    private void resetDailyHours() { hours = getDailyHours(); }

    public int getDailyHours() { return Modifier.floor(calculate(EffectTarget.DAILY_HOURS, DAILY_HOURS)); }
    public int getLivingCost() { return Modifier.floor(calculate(EffectTarget.LIVING_COST, LIVING_COST)); }
    public int getLastLivingCostPaid() { return lastLivingCostPaid; }
    public boolean hadLivingCostShortage() { return lastLivingCostShortage; }
    public int getMaxHp() { return maxHp; }
    public boolean owns(Item item) { return items.owns(item); }
    public boolean isTravelerDay() { return day == 1 || day % 10 == 0; }

    /** 常設商品、または来訪日ごとに一度だけ抽選した最大3商品の一覧を返す。 */
    public List<Item> shopItems(boolean traveler) {
        if (!traveler) return java.util.Arrays.stream(Item.values()).filter(item -> !item.traveler()).toList();
        if (!isTravelerDay()) return List.of();
        if (travelerStockDay != day) {
            List<Item> pool = new ArrayList<>(java.util.Arrays.stream(Item.values())
                    .filter(Item::traveler).filter(item -> !owns(item)).toList());
            List<Item> selected = new ArrayList<>();
            while (selected.size() < 3 && !pool.isEmpty()) selected.add(pool.remove(random.nextInt(pool.size())));
            travelerStock = List.copyOf(selected);
            travelerStockDay = day;
        }
        return travelerStock;
    }

    /** 支払い直前にも購入可否を確認する。 */
    public String purchaseUnavailableReason(Item item) {
        if (isGameOver()) return "ゲームオーバーです";
        if (pendingBattle != null) return "戦闘結果を確認してください";
        if (owns(item)) return "購入済みです";
        if (!shopItems(item.traveler()).contains(item)) return "この商品は販売されていません";
        if (plun < item.price()) return "プルンが足りません";
        return "";
    }

    /** 代金を支払ってから所持品へ登録する。購入品自身には今回の支出を通知しない。 */
    public void purchase(Item item) {
        String reason = purchaseUnavailableReason(item);
        if (!reason.isEmpty()) throw new IllegalStateException(reason);
        changePlun(BigDecimal.valueOf(-item.price()));
        items.acquire(item);
    }
    public String getEventDescription() { return events.describe(); }
    public String getObjectiveDescription() { return objectives.describe(plun); }
    public BattleStats getBattleStats() { return battleStats; }
    public AutoBattle getPendingBattle() { return pendingBattle; }

    /** 勝敗の確認後に進行を解放する。通常HP・報酬には影響しない。 */
    public void confirmBattleResult() {
        if (pendingBattle == null || !pendingBattle.isFinished())
            throw new IllegalStateException("戦闘が終了していません");
        battleMessage = pendingBattle.playerWon() ? "トカゲ戦士との戦闘に勝利した！" : "トカゲ戦士との戦闘に敗北した。";
        pendingBattle = null;
    }

    public String getBattleMessage() { return battleMessage; }

    public int getDay() { return day; }
    public int getHours() { return hours; }
    public int getHp() { return hp; }
    public int getPlun() { return plun; }
    public int getWorkEfficiency() { return workEfficiency; }
    public boolean isGameOver() { return hp <= 0; }
}
