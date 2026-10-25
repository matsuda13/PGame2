package pgame2.gameflow;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static pgame2.gameflow.GameSession.Action.*;

class GameSessionTest {
    @Test
    void actionsConsumeResourcesAndHealingRespectsCap() {
        GameSession game = new GameSession(new java.util.Random() { @Override public int nextInt(int bound) { return bound - 1; } });
        assertThrows(IllegalStateException.class, () -> game.execute(HEAL));
        assertEquals(12, game.getHours());
        game.execute(WORK);
        assertEquals(80, game.getHp());
        assertEquals(30, game.getPlun());
        game.execute(HEAL);
        assertEquals(100, game.getHp());
        assertEquals(15, game.getPlun());
        game.execute(TRAIN);
        assertEquals(85, game.getHp());
        assertEquals(1, game.getWorkEfficiency());
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
        assertEquals(4, game.getWorkEfficiency());
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

