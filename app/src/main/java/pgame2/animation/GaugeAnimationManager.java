package pgame2.animation;

import java.util.IdentityHashMap;
import java.util.Map;
import javax.swing.JProgressBar;

/** HPと時間の増減を固定速度で順番に再生する。 */
public final class GaugeAnimationManager extends AnimationQueue<GaugeAnimationManager.Change> {
    /** 各ゲージの最新の確定値。 */
    private final Map<JProgressBar, Integer> targets = new IdentityHashMap<>();

    /** 再生途中の値と到達点。 */
    static final class Change {
        /** 対象ゲージ。 */
        final JProgressBar bar;
        /** 到達値。 */
        final int target;
        /** 毎秒の変化量。 */
        final double speed;
        /** 小数を保持した表示値。 */
        double value;
        /** 表示開始済みか。 */
        boolean started;

        /** 開始時点の表示から動かす演出を作る。 */
        Change(JProgressBar bar, int target, double speed) {
            this.bar = bar;
            this.target = target;
            this.speed = speed;
        }
    }

    /** 最大値変更時は対象の待機列を取り消し、現在値を即時反映する。 */
    public void show(JProgressBar bar, int value, int maximum, double speed) {
        if (!Double.isFinite(speed) || speed <= 0) throw new IllegalArgumentException("速度は正の有限値が必要です");
        Integer old = targets.put(bar, value);
        if (old == null || bar.getMaximum() != maximum) {
            queue.removeIf(change -> change.bar == bar);
            bar.setMaximum(maximum);
            bar.setValue(value);
        } else if (old != value) {
            enqueue(new Change(bar, value, speed));
        }
        // 操作判断に使えるよう、数値は確定状態を表示する。
        bar.setString(value + " / " + maximum);
    }

    @Override
    protected boolean update(Change change, double seconds) {
        if (!change.started) {
            change.value = change.bar.getValue();
            change.started = true;
        }
        double distance = change.target - change.value;
        double step = change.speed * seconds;
        change.value = Math.abs(distance) <= step ? change.target
                : change.value + Math.copySign(step, distance);
        change.bar.setValue((int) Math.round(change.value));
        return change.value == change.target;
    }

    /** 最新の確定値へ揃えて全演出を取り消す。 */
    public void cancel() {
        clear();
        targets.forEach(JProgressBar::setValue);
    }
}
