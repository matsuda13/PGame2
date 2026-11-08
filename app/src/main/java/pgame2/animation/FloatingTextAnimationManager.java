package pgame2.animation;

import java.awt.*;
import javax.swing.*;

/** 増減値を1秒かけて30px上昇・透明化させる。 */
public final class FloatingTextAnimationManager extends AnimationQueue<FloatingTextAnimationManager.Change> {
    /** 文字を重ね描きする画面。 */
    private final JComponent surface;
    /** 表示中の文字。 */
    private Change visible;

    /** 対象項目に追従する増減表示。 */
    static final class Change {
        /** 位置の基準。 */
        final JLabel anchor;
        /** 符号付き増減。 */
        final String text;
        /** 増減に応じた文字色。 */
        final Color color;
        /** 経過秒数。 */
        double elapsed;

        /** 対象項目の増減表示を作る。 */
        Change(JLabel anchor, int delta) {
            this.anchor = anchor;
            text = (delta > 0 ? "+" : "") + delta;
            color = delta > 0 ? new Color(25, 125, 65) : new Color(180, 45, 45);
        }
    }

    /** 描画先を指定する。 */
    public FloatingTextAnimationManager(JComponent surface) { this.surface = surface; }

    /** 変化がある項目だけキューへ追加する。 */
    public void show(JLabel anchor, int delta) {
        if (delta != 0) enqueue(new Change(anchor, delta));
    }

    @Override
    protected boolean update(Change change, double seconds) {
        change.elapsed = Math.min(1, change.elapsed + seconds);
        visible = change.elapsed < 1 ? change : null;
        surface.repaint();
        return change.elapsed >= 1;
    }

    /** 入力を遮る部品を追加せず、子部品の上に描画する。 */
    public void paint(Graphics graphics) {
        if (visible == null) return;
        Change change = visible;
        Point point = SwingUtilities.convertPoint(change.anchor, 0, 0, surface);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setComposite(AlphaComposite.SrcOver.derive((float) (1 - change.elapsed)));
            g.setFont(change.anchor.getFont().deriveFont(Font.BOLD));
            g.setColor(change.color);
            g.drawString(change.text, point.x + change.anchor.getWidth() / 2,
                    point.y + change.anchor.getHeight() - (int) (30 * change.elapsed));
        } finally {
            g.dispose();
        }
    }

    /** 表示中と待機中の文字を取り消す。 */
    public void cancel() {
        clear();
        visible = null;
        surface.repaint();
    }
}
