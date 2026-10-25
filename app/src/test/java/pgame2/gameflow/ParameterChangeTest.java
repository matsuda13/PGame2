package pgame2.gameflow;

import java.math.BigDecimal;
import java.util.Random;
import org.junit.jupiter.api.Test;
import pgame2.event.SituationEvents.Kind;
import static pgame2.event.EventModifier.Target.*;
import static org.junit.jupiter.api.Assertions.*;

class ParameterChangeTest {
    /** 労働効率の各点が報酬に反映され、労働以外のプルン増減は変わらない。 */
    @Test
    void workEfficiencyIncreasesOnlyWorkIncome() {
        for (int points : new int[] {0, 1, 5, 10}) {
            GameSession game = new GameSession(new Random(0));
            game.changeWorkEfficiency(BigDecimal.valueOf(points));
            int expected = 30 + 3 * points;
            assertEquals(0, BigDecimal.valueOf(expected).compareTo(game.effects(GameSession.Action.WORK).plun()));
            assertEquals(0, BigDecimal.ZERO.compareTo(game.effects(GameSession.Action.TRAIN).plun()));
            assertEquals(0, BigDecimal.valueOf(-15).compareTo(game.effects(GameSession.Action.HEAL).plun()));
            game.execute(GameSession.Action.WORK);
            assertEquals(expected, game.getPlun());
        }
    }

    /** イベントと能力の割合を合算し、端数は資源へ反映するまで残す。 */
    @Test
    void efficiencyAndEventPercentagesAreAddedBeforeRounding() {
        BigDecimal income = Kind.BOOM.apply(WORK_PLUN_REWARD, 7, pgame2.common.Modifier.percent(10));
        assertEquals(0, new BigDecimal("9.1").compareTo(income));
        GameSession game = new GameSession(new Random(0));
        game.changePlun(income);
        assertEquals(9, game.getPlun());
    }

    @Test
    void heatKeepsFractionsUntilHpIsChanged() {
        GameSession game = new GameSession(new Random(0));
        BigDecimal cost = Kind.HEAT.apply(WORK_HP_COST, 15);
        assertEquals(0, new BigDecimal("19.5").compareTo(cost));
        game.changeHp(cost.negate());
        assertEquals(80, game.getHp());
        assertTrue(Kind.HEAT.effect.contains("HP消費 +30%"));
        assertEquals(0, BigDecimal.valueOf(15).compareTo(Kind.HEAT.apply(TRAIN_HP_COST, 15)));
    }

    @Test
    void everyResourceRoundsItsFinalValueAndOnlyHpHasBounds() {
        GameSession game = new GameSession(new Random(0));
        game.changePlun(new BigDecimal("30.9"));
        game.changePlun(new BigDecimal("-15.5"));
        assertEquals(14, game.getPlun());
        game.changeWorkEfficiency(new BigDecimal("1.9"));
        assertEquals(1, game.getWorkEfficiency());
        game.changeHours(new BigDecimal("-3.5"));
        assertEquals(8, game.getHours());
        game.changeHp(new BigDecimal("1000.5"));
        assertEquals(100, game.getHp());
        game.changeHp(new BigDecimal("-1000.5"));
        assertEquals(0, game.getHp());
        game.changeWorkEfficiency(new BigDecimal("-2.1"));
        assertEquals(-2, game.getWorkEfficiency());
    }
}
