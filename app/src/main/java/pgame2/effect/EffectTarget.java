package pgame2.effect;

/** アイテムとイベントで共通に使用する数値補正の対象。 */
public enum EffectTarget {
    WORK_HP_COST("労働のHP消費"), WORK_PLUN_REWARD("労働の報酬プルン"),
    TRAIN_HP_COST("鍛錬のHP消費"), TRAIN_WORK_EFFICIENCY_GAIN("鍛錬の獲得労働効率"),
    TRAIN_BATTLE_GAIN("鍛錬の獲得戦闘能力"),
    REST_HOURS_COST("休息の消費時間"), REST_HP_RECOVERY("休息のHP回復"),
    DAILY_HOURS("1日の使用可能時間"), BATTLE_CHANCE("戦闘発生確率"), LIVING_COST("生活費");

    /** 効果説明に使用する対象名。 */
    private final String label;
    EffectTarget(String label) { this.label = label; }
    public String label() { return label; }
}
