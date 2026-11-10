package pgame2.gameflow;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import pgame2.item.Item;
import static org.junit.jupiter.api.Assertions.*;
import static pgame2.gameflow.GameSession.Action.*;
import static pgame2.item.Item.*;

class ItemIntegrationTest {
    /** 最初のイベントと行商人の商品を固定し、労働後の襲撃を避ける。 */
    private GameSession game(int event, Item... desiredStock) {
        List<Item> pool = new ArrayList<>(Arrays.stream(Item.values()).filter(Item::traveler).toList());
        Random random = new Random() {
            /** 初回イベントと初回商品抽選を識別する呼び出し回数。 */
            private int calls;
            @Override public int nextInt(int bound) {
                int call = calls++;
                if (call == 0) return event;
                if (call <= 3) {
                    int index = call <= desiredStock.length ? pool.indexOf(desiredStock[call - 1]) : 0;
                    pool.remove(index);
                    return index;
                }
                return bound - 1;
            }
        };
        GameSession game = new GameSession(random);
        game.shopItems(true);
        game.changePlun(BigDecimal.valueOf(10000));
        return game;
    }

    /** 期待する十進値を丸めずに比較する。 */
    private void decimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), actual.toPlainString());
    }

    /** 目標の日まで生活費を払いながら進める。 */
    private void advance(GameSession game, int day) {
        while (game.getDay() < day) game.nextDay();
    }

    /** 所持金を指定値に調整する。減額も支出として扱われる。 */
    private void balance(GameSession game, int value) {
        game.changePlun(BigDecimal.valueOf((long) value - game.getPlun()));
    }

    @Test
    void itemEfficiencyAndEventPercentagesAreSummedOnce() {
        GameSession game = game(2);
        game.purchase(TOOLBOX);
        game.changeWorkEfficiency(BigDecimal.ONE);
        advance(game, 9);
        for (int i = 0; i < 5; i++) decimal("42", game.effects(WORK).plun());
        int before = game.getPlun();
        game.execute(WORK);
        assertEquals(before + 42, game.getPlun());
    }

    @Test
    void workwearCombinesWithHeatAndTextbookAddsAfterProteinPercentage() {
        GameSession heat = game(0);
        heat.purchase(WORKWEAR);
        advance(heat, 9);
        decimal("-24", heat.effects(WORK).hp());
        heat.execute(WORK);
        assertEquals(76, heat.getHp());
        GameSession protein = game(3);
        protein.purchase(TEXTBOOK);
        advance(protein, 9);
        decimal("7", protein.effects(TRAIN).workEfficiency());
        protein.execute(TRAIN);
        assertEquals(7, protein.getWorkEfficiency());
    }

    @Test
    void restRecoveryAndPillowGrowCurrentAndMaximumHpOnlyAfterRest() {
        GameSession game = game(0);
        game.purchase(BED);
        game.purchase(PILLOW);
        game.changeHp(BigDecimal.valueOf(-50));
        for (int i = 0; i < 5; i++) decimal("30", game.effects(REST).hp());
        assertEquals(100, game.getMaxHp());
        game.execute(REST);
        assertEquals(101, game.getMaxHp());
        assertEquals(81, game.getHp());
        game.execute(HEAL);
        assertEquals(101, game.getHp());
        assertEquals(101, game.getMaxHp());
    }

    @Test
    void dayEndConditionsShareSnapshotRegardlessOfPurchaseOrder() {
        for (boolean bottleFirst : new boolean[] {true, false}) {
            GameSession game = game(0, MEDAL);
            game.purchase(bottleFirst ? BOTTLE : MEDAL);
            game.purchase(bottleFirst ? MEDAL : BOTTLE);
            game.changeHp(BigDecimal.valueOf(-52));
            game.nextDay();
            assertEquals(101, game.getMaxHp());
            assertEquals(54, game.getHp());
        }
        GameSession boundary = game(0, MEDAL);
        boundary.purchase(MEDAL);
        boundary.changeHp(BigDecimal.valueOf(-49));
        boundary.nextDay();
        assertEquals(100, boundary.getMaxHp());
        boundary.changeHp(BigDecimal.valueOf(-1));
        boundary.nextDay();
        assertEquals(101, boundary.getMaxHp());
        assertEquals(51, boundary.getHp());
    }

    @Test
    void recoveryPrecedesLivingCostAndDeathStopsMorningResurrection() {
        GameSession saved = game(0);
        saved.purchase(BOTTLE);
        balance(saved, 0);
        saved.changeHp(BigDecimal.valueOf(-52));
        saved.nextDay();
        assertEquals(3, saved.getHp());
        assertEquals(2, saved.getDay());

        GameSession dead = game(4);
        advance(dead, 8);
        balance(dead, 0);
        dead.changeHp(BigDecimal.valueOf(-50));
        dead.nextDay();
        assertTrue(dead.isGameOver());
        assertEquals(8, dead.getDay());
        assertEquals(0, dead.getHp());
        assertThrows(IllegalStateException.class, dead::nextDay);
    }

    @Test
    void morningRestoresTheGrownMaximum() {
        GameSession game = game(4);
        game.purchase(PILLOW);
        game.execute(REST);
        game.changeHp(BigDecimal.valueOf(-30));
        advance(game, 9);
        assertEquals(101, game.getMaxHp());
        assertEquals(101, game.getHp());
    }

    @Test
    void livingCostDiscountChangesAffordabilityAndActualPayment() {
        for (int money : new int[] {44, 45}) {
            GameSession game = game(0);
            game.purchase(PIGGY_BANK);
            balance(game, money);
            assertEquals(45, game.getLivingCost());
            game.nextDay();
            assertEquals(money, game.getLastLivingCostPaid());
            assertEquals(money == 44, game.hadLivingCostShortage());
            assertEquals(money == 44 ? 50 : 100, game.getHp());
            assertEquals(0, game.getPlun());
        }
    }

    @Test
    void workwearGrowthAffectsOnlySubsequentWorkAndQueriesDoNotGrowIt() {
        GameSession game = game(0, VETERAN_WORKWEAR);
        game.purchase(VETERAN_WORKWEAR);
        for (int i = 0; i < 10; i++) {
            decimal("30", game.effects(WORK).plun());
            game.unavailableReason(WORK);
            game.shouldEndDay();
        }
        int before = game.getPlun();
        game.execute(WORK);
        assertEquals(before + 30, game.getPlun());
        decimal("30.3", game.effects(WORK).plun());
        game.execute(REST);
        decimal("30.3", game.effects(WORK).plun());
        game.execute(WORK);
        decimal("30.6", game.effects(WORK).plun());
    }

    @Test
    void growingBankExcludesOwnPriceAndCountsOtherPurchasesHealingAndLivingCosts() {
        GameSession game = game(0, GROWING_BANK);
        game.purchase(GROWING_BANK);
        decimal("30", game.effects(WORK).plun());
        game.purchase(BED); // 他の商品購入500。
        decimal("31.5", game.effects(WORK).plun());
        for (int i = 0; i < 4; i++) game.execute(HEAL); // 60消費、余りを保持。
        decimal("31.5", game.effects(WORK).plun());
        game.nextDay(); // 50消費、累積610。
        decimal("31.8", game.effects(WORK).plun());
        game.changePlun(BigDecimal.valueOf(100)); // 収入は成長しない。
        decimal("31.8", game.effects(WORK).plun());
        game.changePlun(new BigDecimal("-89.1")); // 実際には90失い、累積700。
        decimal("32.1", game.effects(WORK).plun());
    }

    @Test
    void growingBankCountsConfiscatedRemainderButNotUnpaidCost() {
        GameSession game = game(0, GROWING_BANK);
        balance(game, 1040);
        game.purchase(GROWING_BANK);
        game.nextDay(); // 未払い50ではなく、残金40だけを消費。
        assertEquals(40, game.getLastLivingCostPaid());
        game.changePlun(BigDecimal.valueOf(100));
        game.changePlun(BigDecimal.valueOf(-59));
        decimal("30", game.effects(WORK).plun());
        game.changePlun(BigDecimal.valueOf(-1));
        decimal("30.3", game.effects(WORK).plun());
    }

    @Test
    void crownAndBraceletReadPreWorkResources() {
        GameSession game = game(0, CROWN, BRACELET);
        game.purchase(CROWN);
        game.purchase(BRACELET);
        balance(game, 499);
        game.changeHp(BigDecimal.valueOf(-50));
        decimal("45", game.effects(WORK).plun());
        game.execute(WORK);
        assertEquals(544, game.getPlun());
        assertEquals(30, game.getHp());
        decimal("57", game.effects(WORK).plun());
        balance(game, 500);
        decimal("57", game.effects(WORK).plun());
    }

    @Test
    void hourglassAllowsPositiveTimeOnlyAndNeverBypassesOtherRestrictions() {
        GameSession game = game(0, HOURGLASS);
        game.purchase(HOURGLASS);
        game.changeHours(BigDecimal.valueOf(-11));
        assertEquals("", game.unavailableReason(WORK));
        assertFalse(game.shouldEndDay());
        game.execute(WORK);
        assertEquals(0, game.getHours());
        assertTrue(game.shouldEndDay());
        assertThrows(IllegalStateException.class, () -> game.execute(REST));
        game.nextDay();
        balance(game, 0);
        assertEquals("プルンが足りません", game.unavailableReason(HEAL));
        game.changeHp(BigDecimal.valueOf(-1000));
        assertThrows(IllegalStateException.class, () -> game.execute(WORK));
    }

    @Test
    void purchasesValidateMoneyOwnershipAndAvailabilityWithoutTimeCost() {
        GameSession game = game(0);
        balance(game, 499);
        assertThrows(IllegalStateException.class, () -> game.purchase(TOOLBOX));
        assertEquals(499, game.getPlun());
        assertFalse(game.owns(TOOLBOX));
        game.changePlun(BigDecimal.ONE);
        game.purchase(TOOLBOX);
        assertEquals(0, game.getPlun());
        assertEquals(12, game.getHours());
        assertEquals(100, game.getHp());
        assertThrows(IllegalStateException.class, () -> game.purchase(TOOLBOX));
        assertEquals(0, game.getPlun());
        assertFalse(new GameSession(new Random(0)).owns(TOOLBOX));
        decimal("30", new GameSession(new Random(0)).effects(WORK).plun());
    }

    @Test
    void travelerStockIsStableWithinVisitAndExcludesOwnedItemsOnNextVisit() {
        GameSession game = game(0, HOURGLASS, GROWING_BANK, MEDAL);
        List<Item> stock = game.shopItems(true);
        assertEquals(3, stock.size());
        assertEquals(3, stock.stream().distinct().count());
        Item bought = stock.getFirst();
        game.purchase(bought);
        assertEquals(stock, game.shopItems(true));
        game.nextDay();
        assertTrue(game.shopItems(true).isEmpty());
        assertThrows(IllegalStateException.class, () -> game.purchase(GROWING_BANK));
        advance(game, 10);
        assertFalse(game.shopItems(true).contains(bought));
        assertEquals(3, game.shopItems(true).size());
    }

    /** 期限日の資産維持目標は生活費より先に評価し、報酬も先に付与する。 */
    @Test
    void objectiveRewardPrecedesDayEndPayment() {
        GameSession game = new GameSession(new Random() {
            @Override public int nextInt(int bound) { return Math.min(1, bound - 1); }
        });
        game.changePlun(BigDecimal.valueOf(2000));
        advance(game, 20);
        balance(game, 150);
        game.nextDay();
        assertEquals(160, game.getPlun()); // 目標報酬60を加えた後に生活費50を支払う。
        assertEquals(1, game.drainNotices().stream().filter(notice -> notice.contains("小目標達成")).count());
    }

    /** 購入済みが増えて候補が3件未満になっても、抽選は残りの種類だけで終了する。 */
    @Test
    void travelerEventuallyHasNoUnownedStock() {
        GameSession game = game(0);
        for (Item item : game.shopItems(true)) game.purchase(item);
        advance(game, 10);
        for (Item item : game.shopItems(true)) game.purchase(item);
        advance(game, 20);
        assertTrue(game.shopItems(true).isEmpty());
        assertTrue(game.shopItems(true).isEmpty());
    }
}
