package pgame2.battle;

import java.util.random.RandomGenerator;
import java.math.BigDecimal;
import pgame2.common.Modifier;
import java.util.List;

/** 労働後の襲撃判定と簡易結果。 */
public class SimpleBattle {
    /** 調整用の基本発生率。 */
    public static final int BASE_CHANCE = 10;
    /** 襲撃時の消耗量。 */
    public static final int DAMAGE = 10;

    /** ポイント補正を加え、確率を有効範囲に収めて抽選する。 */
    public boolean occurs(RandomGenerator random, int bonusPoints) {
        return occurs(random, Modifier.apply(BigDecimal.valueOf(BASE_CHANCE), List.of(Modifier.points(bonusPoints))));
    }

    /** 補正済み確率の端数と上下限を処理して抽選する。 */
    public boolean occurs(RandomGenerator random, BigDecimal chance) {
        return random.nextInt(100) < Modifier.probability(chance);
    }

    /** 生存者にのみ結果を適用し、通常労働での死亡を取り消さない。 */
    public int remainingHp(int hp) {
        return hp <= 0 ? 0 : Math.max(1, hp - DAMAGE);
    }
}
