package pgame2.shop;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.*;
import pgame2.gameflow.GameSession;
import pgame2.item.Item;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShopPanelTest {
    /** 両店舗で選択・購入・資金不足・購入済み表示・閉じる操作を確認する。 */
    @Test
    void merchantsShareSelectionPurchaseAndClose() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (var merchant : ShopCatalog.Merchant.values()) {
                assertNotNull(ShopPanel.class.getResource(merchant.imagePath));
                AtomicBoolean closed = new AtomicBoolean();
                GameSession game = new GameSession(new java.util.Random(0));
                game.changePlun(java.math.BigDecimal.valueOf(1150));
                var stock = game.shopItems(merchant == ShopCatalog.Merchant.TRAVELER);
                ShopPanel panel = new ShopPanel(merchant, stock, game,
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
                        .anyMatch(area -> area.getText().startsWith(stock.get(1).title())));
                assertTrue(purchase.isEnabled());
                purchase.doClick();
                assertFalse(closed.get());
                assertTrue(game.owns(stock.get(1)));
                assertEquals(1250 - stock.get(1).price(), game.getPlun());
                assertTrue(collect(panel, JLabel.class).stream().anyMatch(label -> label.getText().equals(
                        String.format(java.util.Locale.JAPAN, "所持プルン  %,d PLN", game.getPlun()))));
                assertEquals(12, game.getHours());
                assertFalse(purchase.isEnabled());
                assertEquals("購入済み", purchase.getText());
                list.setSelectedIndex(0);
                assertEquals(game.getPlun() >= stock.get(0).price(), purchase.isEnabled());
                collect(panel, JButton.class).stream().filter(button -> button.getText().equals("閉じる"))
                        .findFirst().orElseThrow().doClick();
                assertTrue(closed.get());
            }
        });
    }

    /** 表示後に資金が減っていても、購入押下時に再確認して支払いを拒否する。 */
    @Test
    void purchaseRechecksCurrentBalance() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameSession game = new GameSession(new java.util.Random(0));
            game.changePlun(java.math.BigDecimal.valueOf(400));
            ShopPanel panel = new ShopPanel(ShopCatalog.Merchant.PERMANENT, game.shopItems(false), game, () -> {});
            collect(panel, JList.class).getFirst().setSelectedIndex(0);
            JButton purchase = collect(panel, JButton.class).stream().filter(b -> b.getText().equals("購入"))
                    .findFirst().orElseThrow();
            assertTrue(purchase.isEnabled());
            game.changePlun(java.math.BigDecimal.valueOf(-1));
            purchase.doClick();
            assertEquals(499, game.getPlun());
            assertFalse(game.owns(Item.TOOLBOX));
            assertFalse(purchase.isEnabled());
        });
    }

    /** 商品数や説明の長さが増えても一覧と詳細の境界は動かない。 */
    @Test
    void longDescriptionsAndManyProductsStayInsideTheirRegions() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            List<Item> products = new ArrayList<>(List.of(Item.TOOLBOX, Item.WORKWEAR, Item.BED, Item.TEXTBOOK));
            for (int i = 0; i < 20; i++) {
                products.add(Item.TOOLBOX);
            }
            ShopPanel panel = new ShopPanel(ShopCatalog.Merchant.PERMANENT, products, new GameSession(new java.util.Random(0)), () -> {});
            panel.setSize(panel.getPreferredSize());
            layout(panel);
            JList<?> list = collect(panel, JList.class).getFirst();
            JScrollPane listScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, list);
            Rectangle bounds = listScroll.getBounds();
            assertTrue(listScroll.getVerticalScrollBar().isVisible());
            list.setSelectedIndex(products.size() - 1);
            collect(panel, JTextArea.class).stream().filter(area -> area.getText().startsWith(Item.TOOLBOX.title()))
                    .findFirst().orElseThrow().setText("長い説明文です。".repeat(300));
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
