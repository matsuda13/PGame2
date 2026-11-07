package pgame2.event;

import pgame2.common.Modifier;
import pgame2.effect.EffectTarget;

/** イベントの補正対象と数値定義を結び付ける。 */
public record EventModifier(EffectTarget target, Modifier modifier) {
    /** ポイント補正を確率以外へ適用する定義を拒否する。 */
    public EventModifier {
        java.util.Objects.requireNonNull(target);
        java.util.Objects.requireNonNull(modifier);
        if (modifier.type() == Modifier.Type.POINTS && target != EffectTarget.BATTLE_CHANCE) {
            throw new IllegalArgumentException("pt補正は確率にのみ使用できます");
        }
    }

    /** 型のある補正定義を作成する。 */
    public static EventModifier modifier(EffectTarget target, Modifier modifier) {
        return new EventModifier(target, modifier);
    }

    /** 対象名と数値から説明を生成する。 */
    public String describe() { return target.label() + " " + modifier.describe(); }
}
