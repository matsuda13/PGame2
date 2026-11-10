package pgame2.battle;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AutoBattleTest {
    /** 同能力では同時刻に主人公が先行し、致死攻撃後の反撃はない。 */
    @Test void initialBattleFinishesAt24Seconds() {
        AutoBattle battle = new AutoBattle(BattleStats.INITIAL, BattleStats.INITIAL);
        assertTrue(battle.advanceTo(1.99).isEmpty());
        var attacks = battle.advanceTo(100);
        assertEquals(23, attacks.size());
        assertTrue(attacks.getFirst().playerAttacks());
        assertFalse(attacks.get(1).playerAttacks());
        assertEquals(2, attacks.getFirst().seconds());
        assertEquals(24, battle.elapsed());
        assertEquals(1, battle.playerHp());
        assertEquals(0, battle.enemyHp());
        assertTrue(battle.playerWon());
        assertTrue(battle.advanceTo(200).isEmpty());
    }

    /** 更新間隔が変わっても攻撃時刻・順番・勝敗は一致する。 */
    @Test void renderingFrequencyDoesNotChangeBattle() {
        var player = new BattleStats(10, 10, 100, 250);
        var enemy = new BattleStats(10, 10, 100, 150);
        AutoBattle slow = new AutoBattle(player, enemy);
        AutoBattle smooth = new AutoBattle(player, enemy);
        var expected = slow.advanceTo(100);
        var actual = new ArrayList<AutoBattle.Attack>();
        for (int frame = 0; frame <= 6000; frame++) actual.addAll(smooth.advanceTo(frame / 60.0));
        assertEquals(expected, actual);
        assertEquals(slow.playerHp(), smooth.playerHp());
        assertEquals(slow.elapsed(), smooth.elapsed());
        assertEquals(0.4, actual.get(0).seconds());
        assertEquals(2.0 / 3, actual.get(1).seconds());
        assertEquals(0.8, actual.get(2).seconds());
        assertEquals(1.2, actual.get(3).seconds());
    }

    /** 余剰ゲージを持ち越し、整数比較で同時刻を扱う。 */
    @Test void gaugeRemainderAndExactTie() {
        AutoBattle battle = new AutoBattle(new BattleStats(1, 0, 100, 250), new BattleStats(1, 0, 100, 150));
        battle.advanceTo(0.5);
        assertEquals(25, battle.gauge(true), 1e-9);
        assertEquals(75, battle.gauge(false), 1e-9);
        var attacks = battle.advanceTo(2);
        assertTrue(attacks.get(attacks.size() - 2).playerAttacks());
        assertFalse(attacks.getLast().playerAttacks());
        assertEquals(2, attacks.getLast().seconds());
        assertEquals(0, battle.gauge(true), 1e-9);
    }

    /** 敵が先に致死攻撃を行う場合は敗北し、主人公は攻撃しない。 */
    @Test void enemyCanWin() {
        AutoBattle battle = new AutoBattle(BattleStats.INITIAL, new BattleStats(1000, 0, 100, 100));
        assertEquals(1, battle.advanceTo(100).size());
        assertTrue(battle.isFinished());
        assertFalse(battle.playerWon());
        assertEquals(0, battle.playerHp());
        assertEquals(100, battle.enemyHp());
    }

    /** ダメージを最終段階で切り捨て、極端な防御でも停止しない。 */
    @Test void damageRoundsDownWithMinimumAndNoOverflow() {
        assertEquals(9, BattleStats.INITIAL.damageAgainst(BattleStats.INITIAL));
        assertEquals(66, new BattleStats(100, 0, 1, 1).damageAgainst(new BattleStats(1, 50, 1, 1)));
        assertEquals(1, new BattleStats(0, 0, 1, 1).damageAgainst(new BattleStats(1, Integer.MAX_VALUE, 1, 1)));
        assertEquals(Integer.MAX_VALUE, new BattleStats(Integer.MAX_VALUE, 0, 1, 1).damageAgainst(new BattleStats(1, 0, 1, 1)));
        assertThrows(IllegalArgumentException.class, () -> new BattleStats(1, 0, 1, 0));
        AutoBattle battle = new AutoBattle(BattleStats.INITIAL, BattleStats.INITIAL);
        assertThrows(IllegalArgumentException.class, () -> battle.advanceTo(Double.NaN));
        battle.advanceTo(1);
        assertThrows(IllegalArgumentException.class, () -> battle.advanceTo(0));
    }
}
