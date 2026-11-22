package pgame2.gameflow;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import pgame2.item.Item;
import static pgame2.gameflow.GameSession.Action.*;
import static org.junit.jupiter.api.Assertions.*;

class ActionPreviewTest {
    /** 上限到達後の枕の成長まで予測し、繰り返し参照しても資源を変えない。 */
    @Test void restPreviewIncludesRecoveryCapAndPillowGrowth() {
        GameSession game = new GameSession(new java.util.Random(0));
        game.changePlun(BigDecimal.valueOf(2000));
        game.purchase(Item.BED);
        game.purchase(Item.PILLOW);
        game.changeHp(BigDecimal.valueOf(-10));
        for (int i = 0; i < 5; i++) {
            var preview = game.previewAction(REST);
            assertEquals(101, preview.hpAfter());
            assertEquals(101, preview.maxHpAfter());
            assertEquals(9, preview.hoursAfter());
        }
        assertEquals(90, game.getHp());
        assertEquals(100, game.getMaxHp());
        assertEquals(12, game.getHours());
        game.execute(REST);
        assertEquals(101, game.getHp());
        assertEquals(101, game.getMaxHp());
    }

    /** 回復上限と各資源の最終切り捨てが実行結果と一致する。 */
    @Test void previewMatchesFractionalEffectsAndHealingCap() {
        GameSession game = new GameSession(new java.util.Random(0)) {
            @Override public Effects effects(Action action) {
                if (action != REST) return super.effects(action);
                return new Effects(new BigDecimal("3.5"), new BigDecimal("12.5"), new BigDecimal("2.5"), new BigDecimal("1.5"));
            }
        };
        game.changeHp(BigDecimal.valueOf(-40));
        var preview = game.previewAction(REST);
        assertEquals(102, preview.plunAfter());
        assertEquals(1, preview.workEfficiencyAfter());
        assertEquals(72, preview.hpAfter());
        assertEquals(8, preview.hoursAfter());
        game.execute(REST);
        assertEquals(preview.plunAfter(), game.getPlun());
        assertEquals(preview.workEfficiencyAfter(), game.getWorkEfficiency());
        assertEquals(preview.hpAfter(), game.getHp());
        assertEquals(preview.hoursAfter(), game.getHours());
        preview = game.previewAction(HEAL);
        assertEquals(100, preview.hpAfter());
        game.execute(HEAL);
        assertEquals(preview.plunAfter(), game.getPlun());
        assertEquals(preview.workEfficiencyAfter(), game.getWorkEfficiency());
        assertEquals(preview.hpAfter(), game.getHp());
        assertEquals(preview.hoursAfter(), game.getHours());
    }
}
