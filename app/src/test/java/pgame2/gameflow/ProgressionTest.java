package pgame2.gameflow;

import java.util.Random;
import org.junit.jupiter.api.Test;
import pgame2.battle.SimpleBattle;
import pgame2.event.SituationEvents;
import pgame2.objective.SmallObjectives;
import static org.junit.jupiter.api.Assertions.*;
import static pgame2.gameflow.GameSession.Action.*;
import static pgame2.objective.SmallObjectives.Result.*;

class ProgressionTest {
    /** 候補選択と確率判定を境界値で再現する。 */
    private Random fixed(int value) {
        return new Random() {
            @Override public int nextInt(int bound) { return Math.min(value, bound - 1); }
        };
    }

    @Test
    void battleChanceBoundariesAndSurvivalFloor() {
        SimpleBattle battle = new SimpleBattle();
        assertTrue(battle.occurs(fixed(9), 0));
        assertFalse(battle.occurs(fixed(10), 0));
        assertTrue(battle.occurs(fixed(39), 30));
        assertFalse(battle.occurs(fixed(40), 30));
        assertFalse(battle.occurs(fixed(0), -100));
        assertTrue(battle.occurs(fixed(99), 200));
        assertEquals(1, battle.remainingHp(5));
        assertEquals(1, battle.remainingHp(1));
        assertEquals(0, battle.remainingHp(0));
        GameSession game = new GameSession(fixed(0));
        game.execute(WORK);
        assertEquals(70, game.getHp());
        assertTrue(game.getBattleMessage().contains("10"));
        game.execute(WORK);
        game.execute(WORK);
        game.nextDay();
        game.execute(WORK);
        assertTrue(game.isGameOver());
        assertEquals("", game.getBattleMessage());
    }

    @Test
    void eventAnnouncementOverlapAndNoConsecutiveRepeat() {
        SituationEvents events = new SituationEvents(fixed(0));
        for (int day = 1; day <= 39; day++) {
            events.startDay(day);
            if (day == 1) {
                assertNull(events.getActive());
                assertEquals(SituationEvents.Kind.HEAT, events.getUpcoming());
            }
            if (day == 9 || day == 18) assertEquals(SituationEvents.Kind.HEAT, events.getActive());
            if (day == 16) {
                assertEquals(SituationEvents.Kind.HEAT, events.getActive());
                assertEquals(SituationEvents.Kind.BEASTS, events.getUpcoming());
            }
            if (day == 19 || day == 23) assertNull(events.getActive());
            if (day == 24) assertEquals(SituationEvents.Kind.BEASTS, events.getActive());
            if (day == 39) assertEquals(SituationEvents.Kind.HEAT, events.getActive());
        }
    }

    @Test
    void allEventModifiersAndRestAvailability() {
        for (int kind = 0; kind < 5; kind++) {
            GameSession game = new GameSession(fixed(kind));
            game.execute(TRAIN);
            while (game.getDay() < 9) game.nextDay();
            if (kind == 0) {
                assertEquals(0, java.math.BigDecimal.valueOf(-26).compareTo(game.effects(WORK).hp()));
                assertEquals(0, java.math.BigDecimal.valueOf(-15).compareTo(game.effects(TRAIN).hp()));
            }
            if (kind == 2) assertEquals(0, java.math.BigDecimal.valueOf(36).compareTo(game.effects(WORK).plun()));
            if (kind == 3) {
                assertEquals(0, java.math.BigDecimal.valueOf(-30).compareTo(game.effects(TRAIN).hp()));
                assertEquals(0, java.math.BigDecimal.valueOf(2).compareTo(game.effects(TRAIN).ability()));
            }
            if (kind == 4) {
                assertEquals(100, game.getHp());
                assertEquals(11, game.getHours());
                assertEquals(0, java.math.BigDecimal.valueOf(5).compareTo(game.effects(REST).hours()));
                game.execute(TRAIN);
                game.nextDay();
                assertEquals(100, game.getHp());
                game.execute(REST);
                game.execute(TRAIN);
                assertFalse(game.unavailableReason(REST).isEmpty());
            }
            while (game.getDay() < 19) game.nextDay();
            assertEquals(12, game.getDailyHours());
            assertEquals(0, java.math.BigDecimal.valueOf(3).compareTo(game.effects(REST).hours()));
            assertEquals(0, java.math.BigDecimal.valueOf(-20).compareTo(game.effects(WORK).hp()));
            assertEquals(0, java.math.BigDecimal.valueOf(30).compareTo(game.effects(WORK).plun()));
            assertEquals(0, java.math.BigDecimal.valueOf(1).compareTo(game.effects(TRAIN).ability()));
        }
    }

    @Test
    void allObjectiveConditionsDeadlinesAndNoRepeat() {
        for (int kind = 0; kind < 4; kind++) {
            SmallObjectives goals = new SmallObjectives(fixed(kind));
            goals.recordAction(true, true);
            assertFalse(goals.startDay(4, 100));
            assertTrue(goals.startDay(5, 100));
            assertEquals(20, goals.getDeadline());
            SmallObjectives.Kind original = goals.getActive();
            assertFalse(goals.startDay(15, 100));
            assertEquals(NONE, goals.evaluate(5, 100, false));
            if (kind == 0) {
                assertEquals(NONE, goals.evaluate(6, 249, false));
                assertEquals(SUCCESS, goals.evaluate(6, 250, false));
            } else if (kind == 1) {
                assertEquals(NONE, goals.evaluate(20, 150, false));
                assertEquals(SUCCESS, goals.evaluate(20, 150, true));
            } else {
                int target = kind == 2 ? 8 : 5;
                for (int i = 0; i < target - 1; i++) goals.recordAction(kind == 2, kind == 3);
                assertEquals(NONE, goals.evaluate(6, 0, false));
                goals.recordAction(kind == 2, kind == 3);
                assertEquals(SUCCESS, goals.evaluate(6, 0, false));
            }
            assertEquals(NONE, goals.evaluate(20, 999, true));
            assertTrue(goals.startDay(25, 100));
            assertNotEquals(original, goals.getActive());
            assertEquals(FAILURE, goals.evaluate(40, 0, true));
        }
        SmallObjectives hold = new SmallObjectives(fixed(1));
        hold.startDay(5, 0);
        assertEquals(NONE, hold.evaluate(19, 200, true));
        assertEquals(FAILURE, hold.evaluate(20, 149, true));
    }

    @Test
    void trainingCountsActionsAndRewardIsGrantedOnlyOnce() {
        GameSession game = new GameSession(fixed(3));
        while (game.getDay() < 9) game.nextDay();
        for (int i = 0; i < 5; i++) {
            game.execute(TRAIN);
            game.execute(REST);
            game.nextDay();
        }
        assertEquals(10, game.getAbility());
        assertEquals(60, game.getPlun());
        assertEquals(1, game.drainNotices().stream().filter(n -> n.contains("小目標達成")).count());
        assertTrue(game.drainNotices().isEmpty());
        game.nextDay();
        assertEquals(60, game.getPlun());
    }
}
