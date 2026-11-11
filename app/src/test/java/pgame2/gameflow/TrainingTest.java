package pgame2.gameflow;

import java.math.BigDecimal;
import java.util.Random;
import org.junit.jupiter.api.Test;
import pgame2.battle.BattleStats;
import pgame2.item.Item;
import pgame2.train.TrainingAbility;
import static pgame2.train.TrainingAbility.*;
import static org.junit.jupiter.api.Assertions.*;

class TrainingTest {
    /** 初回イベントを固定し、生活費を用意する。 */
    private GameSession game(int event) {
        GameSession game = new GameSession(new Random() {
            @Override public int nextInt(int bound) { return Math.min(event, bound - 1); }
        });
        game.changePlun(BigDecimal.valueOf(10000));
        return game;
    }
    /** 各能力だけが指定量成長し、予想と実行が一致する。 */
    @Test void allAbilitiesMatchPreview() {
        for (TrainingAbility ability : TrainingAbility.values()) {
            GameSession game = game(0);
            var expected = game.previewTraining(ability, 3);
            assertEquals(3, expected.gain());
            assertEquals(expected, game.train(ability, 3));
            assertEquals(expected.after(), game.abilityValue(ability));
            assertEquals(85, game.getHp());
            assertEquals(9, game.getHours());
            for (TrainingAbility other : TrainingAbility.values()) {
                if (other != ability) assertEquals(game(0).abilityValue(other), game.abilityValue(other));
            }
        }
    }
    /** 照会や別能力では消費せず、実行日のみを記録する。 */
    @Test void textbookIsUsedOnlyOncePerDayAfterActualEfficiencyTraining() {
        GameSession game = game(0);
        game.train(WORK_EFFICIENCY, 1);
        game.purchase(Item.TEXTBOOK);
        for (int i = 0; i < 5; i++) {
            assertEquals(4, game.previewTraining(WORK_EFFICIENCY, 3).gain());
            game.trainingUnavailableReason(WORK_EFFICIENCY, 3);
            game.shouldEndDay();
        }
        game.train(ATTACK, 1);
        assertThrows(IllegalStateException.class, () -> game.train(WORK_EFFICIENCY, 20));
        assertEquals(4, game.train(WORK_EFFICIENCY, 3).gain());
        assertEquals(1, game.train(WORK_EFFICIENCY, 1).gain());
        game.nextDay();
        assertEquals(2, game.train(WORK_EFFICIENCY, 1).gain());
        GameSession fresh = game(0);
        assertEquals(BattleStats.INITIAL, fresh.getBattleStats());
        assertEquals(0, fresh.getWorkEfficiency());
        fresh.purchase(Item.TEXTBOOK);
        assertEquals(2, fresh.previewTraining(WORK_EFFICIENCY, 1).gain());
    }
    /** イベントは全能力に適用し、教本の固定加算は割合補正の後。 */
    @Test void proteinAppliesToAllAbilitiesAndObjectiveCountsActualGains() {
        GameSession game = game(3);
        game.purchase(Item.TEXTBOOK);
        while (game.getDay() < 9) game.nextDay();
        for (TrainingAbility ability : TrainingAbility.values()) {
            var preview = game.previewTraining(ability, 3);
            assertEquals(ability == WORK_EFFICIENCY ? 7 : 6, preview.gain());
            assertEquals(70, preview.hpAfter());
        }
        game.train(WORK_EFFICIENCY, 3);
        game.train(ATTACK, 3);
        assertTrue(game.getObjectiveDescription().contains("13 / 15"));
        int plun = game.getPlun();
        game.train(SPEED, 1);
        assertEquals(plun + 60, game.getPlun());
        game.train(DEFENSE, 1);
        assertEquals(plun + 60, game.getPlun());
    }
    /** 時間不足時は無変更で拒否し、1時間の鍛錬は日終了を防ぐ。 */
    @Test void hourglassDoesNotAllowTrainingOvertime() {
        GameSession game = game(0);
        // 抽選を固定して砂時計を購入できる来訪日まで進める。
        while (!game.shopItems(true).contains(Item.HOURGLASS)) {
            for (Item item : game.shopItems(true)) game.purchase(item);
            do { game.nextDay(); } while (!game.isTravelerDay());
        }
        game.purchase(Item.HOURGLASS);
        game.changeHours(BigDecimal.valueOf(1 - game.getHours()));
        assertFalse(game.shouldEndDay());
        assertThrows(IllegalStateException.class, () -> game.train(ATTACK, 2));
        assertThrows(IllegalStateException.class, () -> game.train(ATTACK, 0));
        assertEquals(100, game.getHp());
        assertEquals(10, game.getBattleStats().attack());
        assertEquals("", game.unavailableReason(GameSession.Action.WORK));
        game.train(ATTACK, 1);
        assertEquals(0, game.getHours());
        assertThrows(IllegalStateException.class, () -> game.train(ATTACK, 1));
        assertTrue(game.shouldEndDay());
    }
    /** 育成後の値を次の戦闘へ渡し、敵の能力は変更しない。 */
    @Test void battleUsesTrainedStatsAndBlocksFurtherTraining() {
        GameSession game = game(0);
        for (TrainingAbility ability : TrainingAbility.values()) game.train(ability, 1);
        game.execute(GameSession.Action.WORK);
        var battle = game.getPendingBattle();
        assertNotNull(battle);
        assertEquals(new BattleStats(11, 11, 101, 51), battle.player());
        assertEquals(BattleStats.INITIAL, battle.enemy());
        assertEquals(101, battle.playerHp());
        assertThrows(IllegalStateException.class, () -> game.train(SPEED, 1));
    }
    /** 死亡する鍛錬でも適用済みの教本は使用済みにする。 */
    @Test void fatalTrainingRecordsTextbookUse() {
        GameSession game = game(0);
        game.purchase(Item.TEXTBOOK);
        game.changeHp(BigDecimal.valueOf(-95));
        game.train(WORK_EFFICIENCY, 1);
        assertTrue(game.isGameOver());
        assertEquals(2, game.getWorkEfficiency());
        assertEquals(1, game.previewTraining(WORK_EFFICIENCY, 1).gain());
        assertFalse(game.shouldEndDay());
    }
}
