package pgame2.train;

import pgame2.effect.EffectTarget;

/** 鍛錬対象別の表示と成長量。 */
public enum TrainingAbility {
    WORK_EFFICIENCY("労働効率", "労働の収入を増やす", 1),
    ATTACK("攻撃力", "戦闘で与えるダメージを増やす", 1),
    DEFENSE("防御力", "戦闘で受けるダメージを減らす", 1),
    TOUGHNESS("タフさ", "通常HPとは別の戦闘HPを増やす", 1),
    SPEED("素早さ", "行動ゲージを速くためる", 1);
    /** 表示名。 */
    public final String title;
    /** 能力の用途。 */
    public final String description;
    /** 時間あたりの基本成長量。 */
    public final int gainPerHour;
    TrainingAbility(String title, String description, int gainPerHour) {
        this.title = title; this.description = description; this.gainPerHour = gainPerHour;
    }
    /** 対応する補正対象を返す。 */
    public EffectTarget effectTarget() {
        return this == WORK_EFFICIENCY ? EffectTarget.TRAIN_WORK_EFFICIENCY_GAIN : EffectTarget.TRAIN_BATTLE_GAIN;
    }
}
