package pgame2.gameflow;

import java.util.ArrayList;
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
    /** 現在の日数。 */
    private int day = 1;
    /** 残り時間。 */
    private int hours = DAILY_HOURS;
    /** 持ち越す体力。 */
    private int hp = MAX_HP;
    /** 所持プルン。 */
    private int plun;
    /** 蓄積した基礎能力。 */
    private int ability;

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
        events = new SituationEvents(random);
        objectives = new SmallObjectives(random);
        beginDay();
    }

    /** 補正済みのコマンド効果。 */
    public record Effects(int hours, int hp, int plun, int ability) { }

    /** 基本値からイベント効果を計算し、終了後に補正を残さない。 */
    public Effects effects(Action action) {
        Kind event = events.getActive();
        int hpChange = action.hp;
        int income = action.plun;
        int growth = action.ability;
        int duration = action.hours;
        if (action == Action.WORK && event == Kind.HEAT) hpChange = -(int) Math.floor(-action.hp * 1.3);
        if (action == Action.WORK && event == Kind.BOOM) income = (int) Math.floor(action.plun * 1.2);
        if (action == Action.TRAIN && event == Kind.PROTEIN) { hpChange *= 2; growth *= 2; }
        if (action == Action.REST && event == Kind.SLEEP) duration += 2;
        return new Effects(duration, hpChange, income, growth);
    }

    /** コマンドを識別し、各機能の基本設定を共通の増減値として公開する。 */
    public enum Action {
        WORK("労働", "体を使ってプルンを稼ぐ", Work.DEFAULT.getHours(),
                -Work.DEFAULT.getHpCost(), Work.DEFAULT.getPlunReward(), Work.DEFAULT.getAbilityGain()),
        REST("休息", "ゆっくり休んで体力を回復", 3, 25, 0, 0),
        HEAL("回復魔法", "プルンを使って体力を大きく回復", 1, 40, -15, 0),
        TRAIN("鍛錬", "体に負荷をかけて基礎能力を高める", Train.DEFAULT.getHours(),
                -Train.DEFAULT.getHpCost(), Train.DEFAULT.getPlunReward(), Train.DEFAULT.getAbilityGain());
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
        /** 能力の増減。 */
        public final int ability;

        Action(String title, String description, int hours, int hp, int plun, int ability) {
            this.title = title;
            this.description = description;
            this.hours = hours;
            this.hp = hp;
            this.plun = plun;
            this.ability = ability;
        }
    }

    /** 実行不可の理由。HPを消費し尽くす行動は死亡判定の対象として許可する。 */
    public String unavailableReason(Action action) {
        if (isGameOver()) return "ゲームオーバーです";
        if (hours < effects(action).hours()) return "時間が足りません";
        if (plun + effects(action).plun() < 0) return "プルンが足りません";
        return "";
    }

    /** 実行条件を確認して資源へ反映する。 */
    public void execute(Action action) {
        String reason = unavailableReason(action);
        if (!reason.isEmpty()) throw new IllegalStateException(reason);
        Effects effect = effects(action);
        hours -= effect.hours();
        hp = Math.max(0, Math.min(MAX_HP, hp + effect.hp()));
        plun += effect.plun();
        ability += effect.ability();
        battleMessage = "";
        if (isGameOver()) return;
        if (action == Action.WORK && battle.occurs(random, events.getActive() == Kind.BEASTS ? 30 : 0)) {
            int before = hp;
            hp = battle.remainingHp(hp);
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
        evaluateObjective(true);
        day++;
        beginDay();
    }

    /** 開催状態を切り替えてから朝の効果と予告を適用する。 */
    private void beginDay() {
        String notice = events.startDay(day);
        if (!notice.isEmpty()) notices.add(notice.strip());
        hours = getDailyHours();
        if (events.getActive() == Kind.SLEEP) hp = MAX_HP;
        if (objectives.startDay(day, plun)) notices.add("小目標予告\n" + objectives.describe(plun));
    }

    /** 達成報酬を一度だけ付与し、操作前の通知へ送る。 */
    private void evaluateObjective(boolean endOfDay) {
        SmallObjectives.Result result = objectives.evaluate(day, plun, endOfDay);
        if (result == SmallObjectives.Result.NONE) return;
        if (result == SmallObjectives.Result.SUCCESS) plun += SmallObjectives.REWARD;
        notices.add(objectives.describe(plun));
    }

    /** 通知を取り出して重複表示を防ぐ。 */
    public List<String> drainNotices() {
        List<String> result = List.copyOf(notices);
        notices.clear();
        return result;
    }

    public int getDailyHours() { return DAILY_HOURS - (events.getActive() == Kind.SLEEP ? 1 : 0); }
    public String getEventDescription() { return events.describe(); }
    public String getObjectiveDescription() { return objectives.describe(plun); }
    public String getBattleMessage() { return battleMessage; }

    public int getDay() { return day; }
    public int getHours() { return hours; }
    public int getHp() { return hp; }
    public int getPlun() { return plun; }
    public int getAbility() { return ability; }
    public boolean isGameOver() { return hp <= 0; }
}
