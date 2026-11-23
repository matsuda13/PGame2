package pgame2.gameflow;

import java.math.BigDecimal;
import java.util.Random;
import org.junit.jupiter.api.Test;
import pgame2.work.Work;
import pgame2.item.Item;
import static org.junit.jupiter.api.Assertions.*;

class WorkTest {
    /** 抽選値を固定し、候補とイベントを独立して再現する。 */
    private Random fixed(int value) {
        return new Random() { @Override public int nextInt(int bound) { return Math.min(value, bound - 1); } };
    }

    /** 全ての順序付き組み合わせが等確率の抽選経路を一つずつ持つ。 */
    @Test void drawsTwoDistinctCandidatesAndKeepsThemUntilNextMorning() {
        var pairs = new java.util.HashSet<java.util.List<Work>>();
        for (int first = 0; first < 5; first++) for (int second = 0; second < 4; second++) {
            int[] draws = {first, second};
            Random choice = new Random() {
                /** 候補の抽選回数。 */
                private int count;
                @Override public int nextInt(int bound) { return draws[count++ % 2]; }
            };
            GameSession game = new GameSession(fixed(99), choice);
            var candidates = game.getDailyWork();
            assertEquals(2, candidates.stream().distinct().count());
            pairs.add(candidates);
            game.previewWork(candidates.getFirst());
            game.workUnavailableReason(candidates.getFirst());
            game.work(candidates.getFirst());
            assertSame(candidates, game.getDailyWork());
            assertThrows(UnsupportedOperationException.class, () -> candidates.clear());
            game.nextDay();
            assertNotSame(candidates, game.getDailyWork());
        }
        assertEquals(20, pairs.size());
    }

    /** コストと同じHPでは拒否し、端数処理後に1残る境界では許可する。 */
    @Test void hpBoundaryUsesFinalRoundingAndRejectedWorkDoesNotMutate() {
        GameSession game = new GameSession(fixed(99), fixed(0));
        Work work = game.getDailyWork().getFirst();
        game.changePlun(BigDecimal.valueOf(1000));
        game.purchase(Item.WORKWEAR);
        game.changeHp(BigDecimal.valueOf(-77));
        assertEquals(23, game.previewWork(work).hpCost());
        assertFalse(game.workUnavailableReason(work).isEmpty());
        int money = game.getPlun();
        assertThrows(IllegalStateException.class, () -> game.work(work));
        assertEquals(money, game.getPlun());
        assertEquals(12, game.getHours());
        assertEquals(23, game.getHp());
        game.changeHp(BigDecimal.ONE);
        var result = game.work(work);
        assertEquals(23, result.hpCost());
        assertEquals(1, game.getHp());
        assertEquals(30, result.reward());
        assertEquals(4, result.hours());
    }

    /** 補正によって確率が正になっても危険度0なら戦闘を生成しない。 */
    @Test void zeroDangerOverridesBoostedBattleChance() {
        GameSession game = new GameSession(fixed(1), fixed(2));
        game.changePlun(BigDecimal.valueOf(1000));
        while (game.getDay() < 9) game.nextDay();
        Work sewing = game.getDailyWork().getFirst();
        assertEquals("sewing", sewing.id());
        assertEquals(30, game.previewWork(sewing).battleChance());
        var result = game.work(sewing);
        assertFalse(result.battleOccurred());
        assertNull(game.getPendingBattle());
        assertEquals(90, game.getHp());
    }

    /** 労働ごとの発生率の直前・境界と、戦闘待ちの操作禁止を確認する。 */
    @Test void battleChanceUsesSelectedWorkAndPreservesNormalHp() {
        for (int index : new int[] {0, 1, 3, 4}) {
            int chance = Work.CATALOG.get(index).battleChance();
            for (int roll : new int[] {chance - 1, chance}) {
                GameSession game = new GameSession(fixed(roll), fixed(index));
                Work work = game.getDailyWork().getFirst();
                var result = game.work(work);
                assertEquals(roll < chance, result.battleOccurred());
                if (result.battleOccurred()) {
                    int hp = game.getHp();
                    assertThrows(IllegalStateException.class, () -> game.work(work));
                    assertThrows(IllegalStateException.class, game::nextDay);
                    game.getPendingBattle().advanceTo(100);
                    game.confirmBattleResult();
                    assertEquals(hp, game.getHp());
                }
            }
        }
    }

    /** 候補外と旧APIからの選択省略は拒否する。 */
    @Test void cannotBypassSelectionOrDailyCandidates() {
        GameSession game = new GameSession(fixed(99), fixed(0));
        assertThrows(IllegalStateException.class, () -> game.execute(GameSession.Action.WORK));
        assertThrows(IllegalStateException.class, () -> game.work(Work.CATALOG.get(4)));
        assertEquals(100, game.getHp());
        assertEquals(100, game.getPlun());
        assertEquals(12, game.getHours());
    }

    /** 同じ候補を反復でき、翌朝の再抽選で外れた候補は実行できない。 */
    @Test void repeatedWorkAndStaleCandidateRejection() {
        Random choice = new Random() {
            /** 朝ごとの抽選を切り替える。 */
            private int calls;
            @Override public int nextInt(int bound) { return calls++ < 2 ? 0 : bound - 1; }
        };
        GameSession game = new GameSession(fixed(99), choice);
        Work original = game.getDailyWork().getFirst();
        game.work(original);
        game.work(original);
        assertEquals(50, game.getHp());
        assertEquals(160, game.getPlun());
        assertEquals(4, game.getHours());
        game.nextDay();
        assertFalse(game.getDailyWork().contains(original));
        assertThrows(IllegalStateException.class, () -> game.work(original));
    }

    /** 新しい労働でも小目標の実行回数を一回ずつ記録する。 */
    @Test void completedWorkCountsTowardObjectiveOnlyOnce() {
        GameSession game = new GameSession(fixed(2), fixed(2));
        game.changePlun(BigDecimal.valueOf(2000));
        while (game.getDay() < 5) game.nextDay();
        Work sewing = game.getDailyWork().getFirst();
        for (int i = 0; i < 8; i++) {
            game.previewWork(sewing);
            game.workUnavailableReason(sewing);
            if (game.getHours() < sewing.hours()) game.nextDay();
            game.work(sewing);
        }
        assertEquals(1, game.drainNotices().stream().filter(n -> n.contains("小目標達成")).count());
    }

    /** 労働報酬には小目標の達成報酬を混ぜない。 */
    @Test void resultExcludesObjectiveReward() {
        GameSession game = new GameSession(fixed(0), fixed(0));
        game.changePlun(BigDecimal.valueOf(1000));
        while (game.getDay() < 5) game.nextDay();
        int before = game.getPlun() + 140;
        game.changePlun(BigDecimal.valueOf(140));
        var result = game.work(game.getDailyWork().getFirst());
        assertEquals(30, result.reward());
        assertEquals(before + 90, game.getPlun());
    }
}
