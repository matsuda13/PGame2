package pgame2.animation;

import java.util.ArrayDeque;
import java.util.Deque;
import javax.swing.Timer;

/** 種類ごとの60fps再生キュー。Swingイベントスレッドで操作する。 */
abstract class AnimationQueue<T> {
    /** 未完了の演出。 */
    protected final Deque<T> queue = new ArrayDeque<>();
    /** 種類専用のタイマー。 */
    private final Timer timer = new Timer(1000 / 60, event -> tick());
    /** 前回の更新時刻。 */
    private long previous;
    /** 一時停止状態。 */
    private boolean paused;

    /** 演出を順番待ちへ追加する。 */
    protected void enqueue(T effect) {
        queue.addLast(effect);
        start();
    }

    /** 待機中の再生を開始する。 */
    private void start() {
        if (!paused && !queue.isEmpty() && !timer.isRunning()) {
            previous = System.nanoTime();
            timer.start();
        }
    }

    /** 描画間隔によらず実時間の速度を維持する。 */
    private void tick() {
        long now = System.nanoTime();
        advance((now - previous) / 1_000_000_000.0);
        previous = now;
    }

    /** 指定秒数だけ先頭を進める。 */
    final void advance(double seconds) {
        if (queue.isEmpty()) {
            timer.stop();
            return;
        }
        if (paused) return;
        updateFrame(seconds);
        if (queue.isEmpty()) timer.stop();
    }

    /** 通常は先頭だけを進め、並列再生する種類では更新単位を変更できる。 */
    protected void updateFrame(double seconds) {
        if (update(queue.getFirst(), seconds)) queue.removeFirst();
    }

    /** 演出を更新し、完了したか返す。 */
    protected abstract boolean update(T effect, double seconds);

    /** 停止時間を除外して一時停止・再開する。 */
    public final void setPaused(boolean paused) {
        this.paused = paused;
        if (paused) timer.stop(); else start();
    }

    /** タイマーと待機列を破棄する。 */
    protected void clear() {
        timer.stop();
        queue.clear();
    }
}
