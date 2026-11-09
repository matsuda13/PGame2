package pgame2.battle;

import java.util.random.RandomGenerator;
import java.math.BigDecimal;
import pgame2.common.Modifier;
import java.util.List;

/** 労働後の戦闘発生判定。 */
public class SimpleBattle {
    /** 調整用の基本発生率。 */
    public static final int BASE_CHANCE = 10;

    /** ポイント補正を加え、確率を有効範囲に収めて抽選する。 */
    public boolean occurs(RandomGenerator random, int bonusPoints) {
        return occurs(random, Modifier.apply(BigDecimal.valueOf(BASE_CHANCE), List.of(Modifier.points(bonusPoints))));
    }

    /** 補正済み確率の端数と上下限を処理して抽選する。 */
    public boolean occurs(RandomGenerator random, BigDecimal chance) {
        return random.nextInt(100) < Modifier.probability(chance);
    }

}
