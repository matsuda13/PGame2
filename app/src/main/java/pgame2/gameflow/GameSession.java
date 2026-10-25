package pgame2.gameflow;

import java.util.ArrayList;
import java.math.BigDecimal;
import pgame2.common.Modifier;
import static pgame2.event.EventModifier.Target.*;
import java.util.List;
import java.util.random.RandomGenerator;
import pgame2.battle.SimpleBattle;
import pgame2.event.SituationEvents;
import pgame2.event.SituationEvents.Kind;
import pgame2.objective.SmallObjectives;
import pgame2.train.Train;
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
    /** 所持プルン。 */
    private int plun;
    /** 蓄積した労働効率。 */
    private int workEfficiency;

    /** 襲撃の抽選元。 */
    private final RandomGenerator random;
    /** 簡易戦闘の処理。 */
    private final SimpleBattle battle = new SimpleBattle();
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
            hpChange = events.apply(WORK_HP_COST, -action.hp).negate();
            income = events.apply(WORK_PLUN_REWARD, action.plun,
                    new Modifier(Modifier.Type.PERCENT, BigDecimal.valueOf(workEfficiency)
                            .multiply(BigDecimal.valueOf(Work.EFFICIENCY_REWARD_PERCENT))));
        }
        if (action == Action.TRAIN) {
            hpChange = events.apply(TRAIN_HP_COST, -action.hp).negate();
            growth = events.apply(TRAIN_WORK_EFFICIENCY_GAIN, action.workEfficiency);
        }
        if (action == Action.REST) duration = events.apply(REST_HOURS_COST, action.hours);
        return new Effects(duration, hpChange, income, growth);
    }

    /** コマンドを識別し、各機能の基本設定を共通の増減値として公開する。 */
    public enum Action {
        WORK("労働", "体を使ってプルンを稼ぐ", Work.DEFAULT.getHours(),
                -Work.DEFAULT.getHpCost(), Work.DEFAULT.getPlunReward(), Work.DEFAULT.getWorkEfficiencyGain()),
        REST("休息", "ゆっくり休んで体力を回復", 3, 25, 0, 0),
        HEAL("回復魔法", "プルンを使って体力を大きく回復", 1, 40, -15, 0),
        TRAIN("鍛錬", "体に負荷をかけて労働効率を高める", Train.DEFAULT.getHours(),
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
        if (isGameOver()) return "ゲームオーバーです";
        Effects effect = effects(action);
        if (BigDecimal.valueOf(hours).compareTo(effect.hours()) < 0) return "時間が足りません";
        if (BigDecimal.valueOf(plun).add(effect.plun()).signum() < 0) return "プルンが足りません";
        return "";
    }

    /** 実行条件を確認して資源へ反映する。 */
    public void execute(Action action) {
        String reason = unavailableReason(action);
        if (!reason.isEmpty()) throw new IllegalStateException(reason);
        Effects effect = effects(action);
        changeHours(effect.hours().negate());
        changeHp(effect.hp());
        changePlun(effect.plun());
        changeWorkEfficiency(effect.workEfficiency());
        battleMessage = "";
        if (isGameOver()) return;
        if (action == Action.WORK && battle.occurs(random, events.apply(BATTLE_CHANCE, SimpleBattle.BASE_CHANCE))) {
            int before = hp;
            changeHp(BigDecimal.valueOf(battle.remainingHp(hp) - hp));
            battleMessage = "帰り道で魔獣に襲われた！ なんとか逃げ切ったが、体力を" + (before - hp) + "消耗した。";
        }
        objectives.recordAction(action == Action.WORK, action == Action.TRAIN);
        evaluateObjective(false);
    }

    /** 死亡判定を優先し、実行可能な行動がなくなったら日を終了する。 */
    public boolean shouldEndDay() {
        if (isGameOver()) return false;
        for (Action action : Action.values()) {
            if (unavailableReason(action).isEmpty()) return false;
        }
        return true;
    }

    /** 当日の期限判定後、翌日の状況と日次効果を適用する。 */
    public void nextDay() {
        if (isGameOver()) throw new IllegalStateException("ゲームオーバー後は翌日に進めません");
        finishDay();
        day++;
        beginDay();
    }

    /** 開催状態を切り替えてから朝の効果と予告を適用する。 */
    private void beginDay() {
        String notice = events.startDay(day);
        if (!notice.isEmpty()) notices.add(notice.strip());
        resetDailyHours();
        if (events.getActive() == Kind.SLEEP) restoreHpToMax();
        if (objectives.startDay(day, plun)) notices.add("小目標予告\n" + objectives.describe(plun));
    }

    /** 一日の終了直前の処理がある場合ここで行う */
    private void finishDay(){
        evaluateObjective(true);
        payLivingCost();        
    }

    /** 生活費を支払う。払えない場合は残りの全plunを失い、hp-50（暫定） */
    private void payLivingCost(){
        if(plun - LIVING_COST >= 0){
            changePlun(BigDecimal.valueOf(-LIVING_COST));
        } else {
            changePlun(BigDecimal.valueOf(-plun));
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
        hp = Modifier.floor(BigDecimal.valueOf(hp).add(delta).max(BigDecimal.ZERO).min(BigDecimal.valueOf(MAX_HP)));
    }

    /** 所持プルンの最終値を切り捨てる。 */
    public void changePlun(BigDecimal delta) { plun = Modifier.floor(BigDecimal.valueOf(plun).add(delta)); }

    /** 労働効率の最終値を切り捨てる。 */
    public void changeWorkEfficiency(BigDecimal delta) { workEfficiency = Modifier.floor(BigDecimal.valueOf(workEfficiency).add(delta)); }

    /** 残り時間の最終値を切り捨てる。 */
    public void changeHours(BigDecimal delta) { hours = Modifier.floor(BigDecimal.valueOf(hours).add(delta)); }

    /** 朝の全回復を適用する。 */
    private void restoreHpToMax() { hp = MAX_HP; }

    /** 翌日の使用可能時間にリセットする。 */
    private void resetDailyHours() { hours = getDailyHours(); }

    public int getDailyHours() { return Modifier.floor(events.apply(pgame2.event.EventModifier.Target.DAILY_HOURS, DAILY_HOURS)); }
    public String getEventDescription() { return events.describe(); }
    public String getObjectiveDescription() { return objectives.describe(plun); }
    public String getBattleMessage() { return battleMessage; }

    public int getDay() { return day; }
    public int getHours() { return hours; }
    public int getHp() { return hp; }
    public int getPlun() { return plun; }
    public int getWorkEfficiency() { return workEfficiency; }
    public boolean isGameOver() { return hp <= 0; }
}
