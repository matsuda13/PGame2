package pgame2.train;

/** 一種類の鍛錬の基本設定。イベント補正前の値を保持する。 */
public final class Train {
    /** 1時間あたりの基本HP消費。 */
    public static final int HP_COST_PER_HOUR = 5;
    /** 既存APIで使う3時間の労働効率鍛錬。 */
    public static final Train DEFAULT = new Train(3, 3 * HP_COST_PER_HOUR, 0, 3 * TrainingAbility.WORK_EFFICIENCY.gainPerHour);

    /** 消費時間。 */
    private final int hours;
    /** 消費HP（正の値）。 */
    private final int hpCost;
    /** 報酬プルン。 */
    private final int plunReward;
    /** 獲得する労働効率値。 */
    private final int workEfficiencyGain;

    /** 鍛錬一種類分の消費と報酬を定義する。 */
    public Train(int hours, int hpCost, int plunReward, int workEfficiencyGain) {
        this.hours = hours;
        this.hpCost = hpCost;
        this.plunReward = plunReward;
        this.workEfficiencyGain = workEfficiencyGain;
    }

    public int getHours() { return hours; }
    public int getHpCost() { return hpCost; }
    public int getPlunReward() { return plunReward; }
    public int getWorkEfficiencyGain() { return workEfficiencyGain; }
}
