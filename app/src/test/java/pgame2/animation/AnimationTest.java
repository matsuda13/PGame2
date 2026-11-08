package pgame2.animation;

import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnimationTest {
    /** 連続操作でも途中の増減を省略せず、指定速度で順番に再生する。 */
    @Test
    void gaugesKeepOrderedChangesAndFixedSpeed() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var manager = new GaugeAnimationManager();
            var hp = new JProgressBar(0, 100);
            var hours = new JProgressBar(0, 12);
            manager.show(hp, 100, 100, 20);
            manager.show(hours, 12, 12, 4);
            manager.show(hp, 80, 100, 20);
            manager.show(hours, 8, 12, 4);
            manager.show(hp, 90, 100, 20);
            manager.advance(.5);
            assertEquals(90, hp.getValue());
            assertEquals(12, hours.getValue());
            manager.advance(.5);
            assertEquals(80, hp.getValue());
            manager.advance(.5);
            assertEquals(10, hours.getValue());
            manager.advance(.5);
            manager.advance(.5);
            assertEquals(90, hp.getValue());
            assertEquals(8, hours.getValue());
            assertTrue(manager.queue.isEmpty());
            manager.cancel();
        });
    }

    /** 最大値変更は古い到達点を破棄し、キャンセルは最新値へ揃える。 */
    @Test
    void maximumChangesAndCancellationDiscardStaleTargets() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var manager = new AnimationEffectManager(new JPanel());
            var hp = new JProgressBar(0, 100);
            manager.gauges().show(hp, 100, 100, 20);
            manager.gauges().show(hp, 60, 100, 20);
            manager.gauges().advance(.5);
            manager.setPaused(true);
            manager.gauges().advance(10);
            assertEquals(90, hp.getValue());
            manager.gauges().show(hp, 70, 120, 20);
            assertEquals(70, hp.getValue());
            assertEquals(120, hp.getMaximum());
            assertTrue(manager.gauges().queue.isEmpty());
            manager.gauges().show(hp, 50, 120, 20);
            manager.setPaused(false);
            manager.gauges().advance(.5);
            assertEquals(60, hp.getValue());
            manager.cancel();
            assertEquals(50, hp.getValue());
        });
    }

    /** 文字は1秒で消え、停止中の時間は再生時間に含めない。 */
    @Test
    void floatingTextsExpireInOrderAndRespectPauseAndCancel() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var manager = new AnimationEffectManager(new JPanel());
            var label = new JLabel();
            manager.texts().show(label, 10);
            manager.texts().show(label, -5);
            manager.texts().show(label, 0);
            manager.texts().advance(.5);
            assertEquals(.5, manager.texts().queue.getFirst().elapsed);
            manager.setPaused(true);
            manager.texts().advance(5);
            assertEquals(2, manager.texts().queue.size());
            manager.setPaused(false);
            manager.texts().advance(.5);
            assertEquals("-5", manager.texts().queue.getFirst().text);
            manager.texts().advance(1);
            assertTrue(manager.texts().queue.isEmpty());
            manager.texts().show(label, 20);
            manager.cancel();
            assertTrue(manager.texts().queue.isEmpty());
        });
    }
}
