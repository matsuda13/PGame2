package pgame2.gameflow;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static pgame2.gameflow.GameSession.Action.*;

class GameSessionTest {
    @Test
    void actionsConsumeResourcesAndHealingRespectsCap() {
        GameSession game = new GameSession(new java.util.Random() { @Override public int nextInt(int bound) { return bound - 1; } });
        assertEquals(100, game.getPlun());
        assertTrue(game.unavailableReason(HEAL).isEmpty());
        assertEquals(12, game.getHours());
        game.execute(WORK);
        assertEquals(80, game.getHp());
        assertEquals(130, game.getPlun());
        game.execute(HEAL);
        assertEquals(100, game.getHp());
        assertEquals(115, game.getPlun());
        game.execute(TRAIN);
        assertEquals(85, game.getHp());
        assertEquals(3, game.getWorkEfficiency());
        game.execute(REST);
        assertEquals(100, game.getHp());
        assertEquals(1, game.getHours());
        assertFalse(game.shouldEndDay());
        game.execute(HEAL);
        assertTrue(game.shouldEndDay());
    }

    @Test
    void noAffordableActionEndsDayAndResourcesCarryOver() {
        GameSession game = new GameSession(new java.util.Random() { @Override public int nextInt(int bound) { return bound - 1; } });
        game.execute(TRAIN);
        game.execute(TRAIN);
        game.execute(TRAIN);
        game.execute(TRAIN);
        assertTrue(game.shouldEndDay());
        assertThrows(IllegalStateException.class, () -> game.execute(WORK));
        game.nextDay();
        assertEquals(2, game.getDay());
        assertEquals(12, game.getHours());
        assertEquals(40, game.getHp());
        assertEquals(12, game.getWorkEfficiency());
        assertEquals(50, game.getPlun());
    }

    /** 回復魔法は15プルンちょうどで実行でき、不足時には資源を変更しない。 */
    @Test
    void healingRequiresEnoughPlun() {
        GameSession game = new GameSession(new java.util.Random(0));
        game.changePlun(java.math.BigDecimal.valueOf(-86));
        assertEquals("プルンが足りません", game.unavailableReason(HEAL));
        assertThrows(IllegalStateException.class, () -> game.execute(HEAL));
        assertEquals(14, game.getPlun());
        assertEquals(100, game.getHp());
        assertEquals(12, game.getHours());
        game.changePlun(java.math.BigDecimal.ONE);
        game.execute(HEAL);
        assertEquals(0, game.getPlun());
        assertEquals(11, game.getHours());
    }

    /** 生活費の境界では、支払える場合だけHPを維持し、不足時は全額とHP50を失う。 */
    @Test
    void livingCostIsChargedAtDayEndWithShortagePenalty() {
        for (int balance : new int[] {0, 49, 50, 51}) {
            GameSession game = new GameSession(new java.util.Random(0));
            game.changePlun(java.math.BigDecimal.valueOf(balance - game.getPlun()));
            game.nextDay();
            assertEquals(Math.max(0, balance - 50), game.getPlun(), "所持金 " + balance);
            assertEquals(balance < 50 ? 50 : 100, game.getHp(), "所持金 " + balance);
            assertEquals(2, game.getDay());
            assertEquals(12, game.getHours());
        }
    }

    /** 生活費不足でHPを使い切った場合も、その後の行動と翌日移行を禁止する。 */
    @Test
    void livingCostShortageCanEndGame() {
        GameSession game = new GameSession(new java.util.Random(0));
        game.changePlun(java.math.BigDecimal.valueOf(-100));
        game.changeHp(java.math.BigDecimal.valueOf(-50));
        game.nextDay();
        assertEquals(0, game.getHp());
        assertEquals(0, game.getPlun());
        assertTrue(game.isGameOver());
        assertThrows(IllegalStateException.class, () -> game.execute(REST));
        assertThrows(IllegalStateException.class, game::nextDay);
    }

    @Test
    void timeShortageBlocksLongActionsButAllowsMagic() {
        GameSession game = new GameSession(new java.util.Random() { @Override public int nextInt(int bound) { return bound - 1; } });
        game.execute(WORK);
        game.execute(WORK);
        game.execute(REST);
        assertEquals(1, game.getHours());
        assertFalse(game.unavailableReason(WORK).isEmpty());
        assertTrue(game.unavailableReason(HEAL).isEmpty());
        assertFalse(game.shouldEndDay());
    }

    @Test
    void deathTakesPriorityOverDayTransition() {
        GameSession game = new GameSession(new java.util.Random() { @Override public int nextInt(int bound) { return bound - 1; } });
        game.execute(WORK);
        game.execute(WORK);
        game.execute(WORK);
        game.nextDay();
        game.execute(WORK);
        game.execute(WORK);
        assertTrue(game.isGameOver());
        assertFalse(game.shouldEndDay());
        assertThrows(IllegalStateException.class, game::nextDay);
        assertThrows(IllegalStateException.class, () -> game.execute(REST));
    }
}

