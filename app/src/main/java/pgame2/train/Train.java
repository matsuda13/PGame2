package pgame2.train;

/** 一種類の鍛錬の基本設定。イベント補正前の値を保持する。 */
public final class Train {
    /** 現在使用する鍛錬。バランス調整はここで行う。 */
    public static final Train DEFAULT = new Train(3, 15, 0, 1);

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
