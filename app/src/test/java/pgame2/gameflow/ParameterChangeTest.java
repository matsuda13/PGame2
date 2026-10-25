package pgame2.gameflow;

import java.math.BigDecimal;
import java.util.Random;
import org.junit.jupiter.api.Test;
import pgame2.event.SituationEvents.Kind;
import static pgame2.event.EventModifier.Target.*;
import static org.junit.jupiter.api.Assertions.*;

class ParameterChangeTest {
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
        game.changeAbility(new BigDecimal("1.9"));
        assertEquals(1, game.getAbility());
        game.changeHours(new BigDecimal("-3.5"));
        assertEquals(8, game.getHours());
        game.changeHp(new BigDecimal("1000.5"));
        assertEquals(100, game.getHp());
        game.changeHp(new BigDecimal("-1000.5"));
        assertEquals(0, game.getHp());
        game.changeAbility(new BigDecimal("-2.1"));
        assertEquals(-2, game.getAbility());
    }
}
