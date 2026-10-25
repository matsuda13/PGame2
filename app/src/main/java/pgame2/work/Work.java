package pgame2.work;

/** 一種類の労働の基本設定。イベント補正前の値を保持する。 */
public final class Work {
    /** 現在使用する労働。バランス調整はここで行う。 */
    public static final Work DEFAULT = new Work(4, 20, 30, 0);

    /** 消費時間。 */
    private final int hours;
    /** 消費HP（正の値）。 */
    private final int hpCost;
    /** 報酬プルン。 */
    private final int plunReward;
    /** 獲得する基礎能力値。 */
    private final int abilityGain;

    /** 労働一種類分の消費と報酬を定義する。 */
    public Work(int hours, int hpCost, int plunReward, int abilityGain) {
        this.hours = hours;
        this.hpCost = hpCost;
        this.plunReward = plunReward;
        this.abilityGain = abilityGain;
    }

    public int getHours() { return hours; }
    public int getHpCost() { return hpCost; }
    public int getPlunReward() { return plunReward; }
    public int getAbilityGain() { return abilityGain; }
}
