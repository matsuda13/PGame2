package pgame2.gameflow;

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

    /** 行動の試作設定を一か所で管理する。 */
    public enum Action {
        WORK("労働", "体を使ってプルンを稼ぐ", 4, -20, 30, 0),
        REST("休息", "ゆっくり休んで体力を回復", 3, 25, 0, 0),
        HEAL("回復魔法", "プルンを使って体力を大きく回復", 1, 40, -15, 0),
        TRAIN("鍛錬", "体に負荷をかけて基礎能力を高める", 3, -15, 0, 1);
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
        if (hours < action.hours) return "時間が足りません";
        if (plun + action.plun < 0) return "プルンが足りません";
        return "";
    }

    /** 実行条件を確認して資源へ反映する。 */
    public void execute(Action action) {
        String reason = unavailableReason(action);
        if (!reason.isEmpty()) throw new IllegalStateException(reason);
        hours -= action.hours;
        hp = Math.max(0, Math.min(MAX_HP, hp + action.hp));
        plun += action.plun;
        ability += action.ability;
    }

    /** 死亡判定を優先し、実行可能な行動がなくなったら日を終了する。 */
    public boolean shouldEndDay() {
        if (isGameOver()) return false;
        for (Action action : Action.values()) {
            if (unavailableReason(action).isEmpty()) return false;
        }
        return true;
    }

    /** 日次効果は未定のため、資源を持ち越して時間だけを戻す。 */
    public void nextDay() {
        if (isGameOver()) throw new IllegalStateException("ゲームオーバー後は翌日に進めません");
        day++;
        hours = DAILY_HOURS;
    }

    public int getDay() { return day; }
    public int getHours() { return hours; }
    public int getHp() { return hp; }
    public int getPlun() { return plun; }
    public int getAbility() { return ability; }
    public boolean isGameOver() { return hp <= 0; }
}
