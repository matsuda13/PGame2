package pgame2.animation;

import javax.swing.JComponent;

/** 種類ごとの演出の一時停止とキャンセルを統括する。 */
public final class AnimationEffectManager {
    /** ゲージ専用の再生管理。 */
    private final GaugeAnimationManager gauges = new GaugeAnimationManager();
    /** 文字専用の再生管理。 */
    private final FloatingTextAnimationManager texts;

    /** 文字の描画先を指定する。 */
    public AnimationEffectManager(JComponent surface) { texts = new FloatingTextAnimationManager(surface); }

    public GaugeAnimationManager gauges() { return gauges; }
    public FloatingTextAnimationManager texts() { return texts; }

    /** 全種類を同時に一時停止・再開する。 */
    public void setPaused(boolean paused) {
        gauges.setPaused(paused);
        texts.setPaused(paused);
    }

    /** 最新状態へ揃えて全演出を取り消す。 */
    public void cancel() {
        gauges.cancel();
        texts.cancel();
    }
}
