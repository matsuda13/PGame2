package pgame2.work;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.*;

/** 労働素材を透過と縦横比を保って固定領域へ描く。 */
final class WorkArtwork extends JComponent {
    /** 同じ素材の再読込を避ける。Swingイベントスレッドから利用する。 */
    private static final Map<String, BufferedImage> IMAGES = new HashMap<>();
    /** 表示中の画像。未選択時はnull。 */
    private BufferedImage image;
    /** 画像未登録時の案内。 */
    private String fallback = "";

    /** 表示領域の寸法を画像サイズから独立させる。 */
    WorkArtwork() { setPreferredSize(new Dimension(160, 100)); }

    /** 登録済み素材を切り替える。空パスだけは案内表示を許可する。 */
    void showImage(String path, String description) {
        image = path.isEmpty() ? null : load(path);
        fallback = description;
        setToolTipText(description.isEmpty() ? null : description);
        repaint();
    }

    /** 一覧カードのアイコンを作る。 */
    static Icon icon(String path) {
        if (path.isEmpty()) return null;
        BufferedImage image = load(path);
        return new Icon() {
            public int getIconWidth() { return 56; }
            public int getIconHeight() { return 56; }
            public void paintIcon(Component component, Graphics graphics, int x, int y) {
                draw(graphics, image, x, y, 56, 56);
            }
        };
    }

    /** 設定ミスや読込失敗を仮画像で隠さず通知する。 */
    private static BufferedImage load(String path) {
        return IMAGES.computeIfAbsent(path, resource -> {
            var url = WorkArtwork.class.getResource(resource);
            if (url == null) throw new IllegalStateException("労働素材が見つかりません: " + resource);
            try {
                BufferedImage result = ImageIO.read(url);
                if (result == null) throw new IllegalStateException("労働素材を画像として読み込めません: " + resource);
                return trimTransparentMargin(result);
            } catch (IOException exception) {
                throw new UncheckedIOException("労働素材の読込に失敗しました: " + resource, exception);
            }
        });
    }

    /** 原本を変更せず、完全に透明な余白だけを表示対象から除く。 */
    private static BufferedImage trimTransparentMargin(BufferedImage image) {
        int width = image.getWidth(), height = image.getHeight();
        int left = width, top = height, right = -1, bottom = -1;
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            if ((pixels[y * width + x] >>> 24) == 0) continue;
            left = Math.min(left, x); right = Math.max(right, x);
            top = Math.min(top, y); bottom = Math.max(bottom, y);
        }
        if (right < left) throw new IllegalStateException("労働素材に表示可能な画素がありません");
        return image.getSubimage(left, top, right - left + 1, bottom - top + 1);
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (image != null) draw(graphics, image, 0, 0, getWidth(), getHeight());
        else {
            graphics.setColor(getForeground());
            FontMetrics metrics = graphics.getFontMetrics();
            graphics.drawString(fallback, Math.max(0, (getWidth() - metrics.stringWidth(fallback)) / 2),
                    (getHeight() + metrics.getAscent()) / 2);
        }
    }

    /** 拡縮しても主役を切らず、アルファを保持して中央に収める。 */
    private static void draw(Graphics graphics, BufferedImage image, int x, int y, int width, int height) {
        double scale = Math.min(width / (double) image.getWidth(), height / (double) image.getHeight());
        int drawnWidth = (int) Math.round(image.getWidth() * scale);
        int drawnHeight = (int) Math.round(image.getHeight() * scale);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(image, x + (width - drawnWidth) / 2, y + (height - drawnHeight) / 2, drawnWidth, drawnHeight, null);
        } finally { g.dispose(); }
    }
}
