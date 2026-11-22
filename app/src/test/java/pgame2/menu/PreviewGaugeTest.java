package pgame2.menu;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import pgame2.animation.GaugeAnimationManager;
import static org.junit.jupiter.api.Assertions.*;

class PreviewGaugeTest {
    /** アニメーションの途中値と文字更新が予測表示に混ざらず、解除後は最新状態へ戻る。 */
    @Test void previewDoesNotChangeAnimationModel() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PreviewGauge gauge = new PreviewGauge(100);
            GaugeAnimationManager animation = new GaugeAnimationManager();
            animation.show(gauge, 100, 100, 20);
            animation.show(gauge, 35, 100, 20);
            gauge.showPreview(35, 10, "");
            assertEquals(100, gauge.getValue());
            assertEquals("35 → 10 / 100", gauge.getString());
            gauge.setValue(60);
            animation.show(gauge, 35, 100, 20);
            assertEquals("35 → 10 / 100", gauge.getString());
            gauge.clearPreview();
            assertEquals(60, gauge.getValue());
            assertEquals("35 / 100", gauge.getString());
            animation.cancel();
            assertEquals(35, gauge.getValue());
        });
    }

    /** 通常・予測・不足を固定寸法で描画して確認できる画像を作る。 */
    @Test void rendersPreviewAndShortageWithinBounds() throws Exception {
        BufferedImage[] result = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            PreviewGauge hp = new PreviewGauge(100), hours = new PreviewGauge(12), blocked = new PreviewGauge(100);
            JPanel surface = new JPanel(null) {
                @Override protected void paintChildren(Graphics graphics) {
                    super.paintChildren(graphics);
                    hp.paintDelta(graphics, this);
                    hours.paintDelta(graphics, this);
                    blocked.paintDelta(graphics, this);
                }
            };
            surface.setBackground(new Color(248, 242, 225));
            surface.setSize(250, 270);
            PreviewGauge[] gauges = {hp, hours, blocked};
            for (int i = 0; i < gauges.length; i++) {
                PreviewGauge gauge = gauges[i];
                gauge.setBounds(25, 40 + i * 80, 180, 26);
                gauge.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
                gauge.setForeground(i == 1 ? new Color(89, 177, 209) : new Color(224, 113, 126));
                gauge.setBackground(new Color(235, 238, 241));
                surface.add(gauge);
            }
            hp.setValue(35);
            hours.setValue(8);
            blocked.setValue(10);
            hp.showPreview(35, 10, "");
            hours.showPreview(8, 4, "");
            blocked.showPreview(10, -15, "体力が足りません");
            assertEquals("10 → 0 / 100", blocked.getString());
            result[0] = new BufferedImage(250, 270, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = result[0].createGraphics();
            try { surface.printAll(g); } finally { g.dispose(); }
        });
        var output = java.nio.file.Path.of("build", "gauge-preview.png");
        java.nio.file.Files.createDirectories(output.getParent());
        ImageIO.write(result[0], "png", output.toFile());
    }
}
