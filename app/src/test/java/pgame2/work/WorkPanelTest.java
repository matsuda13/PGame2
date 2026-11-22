package pgame2.work;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkPanelTest {
    /** 選択だけでは実行せず、処理中の連打と閉じる操作を拒否する。 */
    @Test void selectionExecutionAndResultPersistence() throws Exception {
        AtomicInteger calls = new AtomicInteger(), closed = new AtomicInteger(), day = new AtomicInteger(1);
        WorkPanel[] panel = new WorkPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new WorkPanel(() -> new WorkPanel.State(day.get(), 12, 100, 100, 100, Work.CATALOG.subList(0, 2)),
                    work -> new WorkPanel.Preview(work.hours(), work.hpCost(), work.plunReward(), work.battleChance()),
                    work -> work == null ? "労働を選択してください" : "",
                    work -> { calls.incrementAndGet(); return "<html>荷役 完了<br>獲得 30 PLN / 消費 4時間 / HP損失 25<br>戦闘なし</html>"; },
                    closed::incrementAndGet);
            assertFalse(start(panel[0]).isEnabled());
            collect(panel[0], JToggleButton.class).getFirst().doClick();
            assertEquals(0, calls.get());
            start(panel[0]).doClick();
            start(panel[0]).doClick();
            panel[0].requestClose();
            assertEquals(0, closed.get());
            assertTrue(collect(panel[0], JToggleButton.class).stream().noneMatch(Component::isEnabled));
        });
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(1, calls.get());
            assertTrue(collect(panel[0], JToggleButton.class).getFirst().isSelected());
            collect(panel[0], JToggleButton.class).get(1).doClick();
            assertTrue(collect(panel[0], JLabel.class).stream().anyMatch(l -> l.getText().contains("荷役 完了")));
            day.incrementAndGet();
            panel[0].refresh();
            assertFalse(start(panel[0]).isEnabled());
            assertTrue(collect(panel[0], JToggleButton.class).stream().noneMatch(AbstractButton::isSelected));
            assertTrue(collect(panel[0], JLabel.class).stream().anyMatch(l -> l.getText().contains("荷役 完了")));
            panel[0].requestClose();
            assertEquals(1, closed.get());
        });
    }

    /** 実行不可でも詳細を表示し、予約後に資源が変わった場合も実行しない。 */
    @Test void unavailableDetailsAndRecheckBeforeExecution() throws Exception {
        AtomicInteger blocked = new AtomicInteger(), calls = new AtomicInteger();
        WorkPanel[] panel = new WorkPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            panel[0] = new WorkPanel(() -> new WorkPanel.State(1, 1, 10, 100, 100, Work.CATALOG.subList(0, 2)),
                    work -> new WorkPanel.Preview(work.hours(), work.hpCost(), work.plunReward(), work.battleChance()),
                    work -> work == null ? "選択してください" : blocked.get() == 1 ? "時間が足りません" : "",
                    work -> { calls.incrementAndGet(); return "完了"; }, () -> { });
            collect(panel[0], JToggleButton.class).getFirst().doClick();
            start(panel[0]).doClick();
            blocked.set(1);
        });
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(0, calls.get());
            assertFalse(start(panel[0]).isEnabled());
            assertTrue(collect(panel[0], JToggleButton.class).getFirst().isEnabled());
            assertTrue(collect(panel[0], JLabel.class).stream().anyMatch(l -> l.getText().contains("荷役 —")));
        });
    }

    /** 固定寸法で描画し、見た目の確認用画像も出力する。 */
    @Test void rendersSelectedWorkAtFixedSize() throws Exception {
        BufferedImage[] image = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            WorkPanel panel = new WorkPanel(() -> new WorkPanel.State(9, 7, 62, 100, 285, Work.CATALOG.subList(2, 4)),
                    work -> new WorkPanel.Preview(work.hours(), work.hpCost(), work.plunReward(), work.battleChance() + 30),
                    work -> work == null ? "労働を選択してください" : "",
                    work -> "完了", () -> { });
            panel.setSize(panel.getPreferredSize());
            layout(panel);
            Dimension before = panel.getSize();
            collect(panel, JToggleButton.class).getFirst().doClick();
            layout(panel);
            assertEquals(before, panel.getSize());
            assertEquals(2, collect(panel, JToggleButton.class).size());
            image[0] = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image[0].createGraphics();
            try { panel.printAll(graphics); } finally { graphics.dispose(); }
        });
        var output = java.nio.file.Path.of("build", "work-panel-preview.png");
        java.nio.file.Files.createDirectories(output.getParent());
        ImageIO.write(image[0], "png", output.toFile());
    }

    /** 労働開始ボタンを取得する。 */
    private static JButton start(Container panel) {
        return collect(panel, JButton.class).stream().filter(b -> b.getText().equals("労働開始")).findFirst().orElseThrow();
    }

    /** ネイティブウィンドウなしで子部品を配置する。 */
    private static void layout(Container parent) {
        parent.doLayout();
        for (Component child : parent.getComponents()) if (child instanceof Container nested) layout(nested);
    }

    /** 表示ツリーから指定種類の部品を集める。 */
    private static <T extends Component> List<T> collect(Container parent, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) result.add(type.cast(child));
            if (child instanceof Container nested) result.addAll(collect(nested, type));
        }
        return result;
    }
}
