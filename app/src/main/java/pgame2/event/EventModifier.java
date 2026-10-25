package pgame2.event;

import pgame2.common.Modifier;

/** イベントの補正対象と数値定義を結び付ける。 */
public record EventModifier(Target target, Modifier modifier) {
    /** ポイント補正を確率以外へ適用する定義を拒否する。 */
    public EventModifier {
        java.util.Objects.requireNonNull(target);
        java.util.Objects.requireNonNull(modifier);
        if (modifier.type() == Modifier.Type.POINTS && target != Target.BATTLE_CHANCE) {
            throw new IllegalArgumentException("pt補正は確率にのみ使用できます");
        }
    }
    /** 現在のイベントで使用する補正対象。 */
    public enum Target {
        WORK_HP_COST("労働のHP消費"), WORK_PLUN_REWARD("労働の報酬プルン"),
        TRAIN_HP_COST("鍛錬のHP消費"), TRAIN_ABILITY_GAIN("鍛錬の獲得能力値"),
        REST_HOURS_COST("休息の消費時間"), DAILY_HOURS("1日の使用可能時間"),
        BATTLE_CHANCE("戦闘発生確率");

        /** 説明文で使用する対象名。 */
        private final String label;
        Target(String label) { this.label = label; }
    }

    /** 型のある補正定義を作成する。 */
    public static EventModifier modifier(Target target, Modifier modifier) {
        return new EventModifier(target, modifier);
    }

    /** 対象名と数値から説明を生成する。 */
    public String describe() { return target.label + " " + modifier.describe(); }
}
