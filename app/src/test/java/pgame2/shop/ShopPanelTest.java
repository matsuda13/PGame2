package pgame2.shop;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShopPanelTest {
    /** 両店舗で画像を読め、選択や閉じる操作と購入不可の状態を維持する。 */
    @Test
    void merchantsShareSelectionAndDisabledPurchase() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (var merchant : ShopCatalog.Merchant.values()) {
                assertNotNull(ShopPanel.class.getResource(merchant.imagePath));
                AtomicBoolean closed = new AtomicBoolean();
                ShopPanel panel = new ShopPanel(merchant, ShopCatalog.PREVIEW_PRODUCTS, 1250,
                        () -> closed.set(true));
                panel.setSize(panel.getPreferredSize());
                layout(panel);
                assertTrue(collect(panel, JLabel.class).stream()
                        .anyMatch(label -> label.getText().equals("所持プルン  1,250 PLN")));
                JButton purchase = collect(panel, JButton.class).stream()
                        .filter(button -> button.getText().equals("購入")).findFirst().orElseThrow();
                assertFalse(purchase.isEnabled());
                JList<?> list = collect(panel, JList.class).getFirst();
                assertEquals(-1, list.getSelectedIndex());
                assertTrue(collect(panel, JTextArea.class).stream()
                        .anyMatch(area -> area.getText().equals("商品を選択してください")));
                list.setSelectedIndex(1);
                assertTrue(collect(panel, JTextArea.class).stream()
                        .anyMatch(area -> area.getText().startsWith("丈夫な作業着")));
                assertFalse(purchase.isEnabled());
                purchase.doClick();
                assertFalse(closed.get());
                collect(panel, JButton.class).stream().filter(button -> button.getText().equals("閉じる"))
                        .findFirst().orElseThrow().doClick();
                assertTrue(closed.get());
            }
        });
    }

    /** 商品数や説明の長さが増えても一覧と詳細の境界は動かない。 */
    @Test
    void longDescriptionsAndManyProductsStayInsideTheirRegions() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            List<ShopCatalog.Product> products = new ArrayList<>(ShopCatalog.PREVIEW_PRODUCTS);
            for (int i = 0; i < 20; i++) {
                products.add(new ShopCatalog.Product("長い名前の商品".repeat(8), "効果：未定",
                        "長い説明文です。".repeat(300), 500));
            }
            ShopPanel panel = new ShopPanel(ShopCatalog.Merchant.PERMANENT, products, 100, () -> {});
            panel.setSize(panel.getPreferredSize());
            layout(panel);
            JList<?> list = collect(panel, JList.class).getFirst();
            JScrollPane listScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, list);
            Rectangle bounds = listScroll.getBounds();
            assertTrue(listScroll.getVerticalScrollBar().isVisible());
            list.setSelectedIndex(products.size() - 1);
            layout(panel);
            assertEquals(bounds, listScroll.getBounds());
            assertEquals(76, list.getCellBounds(0, 0).height);
            assertEquals(76, list.getCellBounds(23, 23).height);
            assertFalse(listScroll.getHorizontalScrollBar().isVisible());
            assertTrue(collect(panel, JButton.class).stream().filter(button -> button.getText().equals("購入"))
                    .noneMatch(JButton::isEnabled));
        });
    }

    /** 非表示の画面でも子部品まで配置を確定する。 */
    private static void layout(Container container) {
        container.doLayout();
        for (Component component : container.getComponents()) {
            if (component instanceof Container child) layout(child);
        }
    }

    /** 画面内の指定した種類の部品を取得する。 */
    private static <T extends Component> List<T> collect(Container container, Class<T> type) {
        List<T> results = new ArrayList<>();
        for (Component component : container.getComponents()) {
            if (type.isInstance(component)) results.add(type.cast(component));
            if (component instanceof Container child) results.addAll(collect(child, type));
        }
        return results;
    }
}
