package pgame2.menu;

import java.awt.*;
import javax.swing.*;

/** 確定値のアニメーションとは別に、行動後の予測を重ねるゲージ。 */
public final class PreviewGauge extends JProgressBar {
    /** 予測元の確定値。未選択時はnull。 */
    private Integer current;
    /** 上下限に収める前の予測値。 */
    private int after;
    /** 実行不可の理由。 */
    private String unavailable = "";

    /** 通常描画とアニメーションは既存のProgressBarに任せる。 */
    public PreviewGauge(int maximum) { super(0, maximum); }

    /** ゲージモデルを変更せず、確定状態に基づく予測を設定する。 */
    public void showPreview(int current, int after, String unavailable) {
        this.current = current;
        this.after = after;
        this.unavailable = unavailable;
        setToolTipText(unavailable.isEmpty() ? "労働直後の予測（日終了処理は含みません）" : unavailable);
        repaint();
        if (getParent() != null) getParent().repaint();
    }

    /** 保存していた通常表示へ戻す。演出の途中値は変更しない。 */
    public void clearPreview() {
        current = null;
        unavailable = "";
        setToolTipText(null);
        repaint();
        if (getParent() != null) getParent().repaint();
    }

    @Override public String getString() {
        return current == null ? super.getString() : current + " → " + Math.max(0, after) + " / " + getMaximum();
    }

    @Override protected void paintComponent(Graphics graphics) {
        // 消費アニメーション中は通常の塗りを維持し、確定値へ到達してから予測範囲を重ねる。
        if (current == null || getValue() != current) { super.paintComponent(graphics); return; }
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            Insets insets = getInsets();
            int x = insets.left, y = insets.top;
            int width = Math.max(0, getWidth() - x - insets.right);
            int height = Math.max(0, getHeight() - y - insets.bottom);
            int beforeX = pixels(current, width), afterX = pixels(after, width);
            g.setColor(getBackground());
            g.fillRect(x, y, width, height);
            g.setColor(getForeground());
            g.fillRect(x, y, beforeX, height);
            g.setColor(unavailable.isEmpty() ? new Color(255, 255, 255, 155) : new Color(220, 65, 65, 170));
            int left = x + Math.min(beforeX, afterX), span = Math.abs(beforeX - afterX);
            g.fillRect(left, y, span, height);
            Shape clip = g.getClip();
            g.clipRect(left, y, span, height);
            g.setColor(new Color(255, 255, 255, 140));
            for (int line = left - height; line < left + span; line += 6) g.drawLine(line, y + height, line + height, y);
            g.setClip(clip);
            g.setFont(getFont().deriveFont(Font.BOLD, 13f));
            g.setColor(new Color(35, 45, 55));
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(getString(), (getWidth() - metrics.stringWidth(getString())) / 2,
                    (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
        } finally { g.dispose(); }
    }

    /** 消費予定量を親画面の余白に描き、ゲージ内の値と重ねない。 */
    public void paintDelta(Graphics graphics, JComponent surface) {
        if (current == null) return;
        Point point = SwingUtilities.convertPoint(this, 0, 0, surface);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setColor(unavailable.isEmpty() ? getForeground().darker() : new Color(180, 40, 40));
            g.setFont(getFont().deriveFont(Font.BOLD, 14f));
            int delta = after - current;
            String text = (delta > 0 ? "+" : "") + delta + (unavailable.isEmpty() ? "" : "（実行不可）");
            g.drawString(text, point.x, point.y - 5);
        } finally { g.dispose(); }
    }

    /** 不足時でも描画をゲージの範囲内に収める。 */
    private int pixels(int value, int width) {
        return (int) Math.round(width * Math.max(0, Math.min(getMaximum(), value)) / (double) Math.max(1, getMaximum()));
    }
}
