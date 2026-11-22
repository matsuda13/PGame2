package pgame2.menu;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MainMenuTest {
    /** 労働画面を開くだけでは消費せず、鍛錬後の補正を詳細と実行に反映する。 */
    @Test
    void trainingUpdatesWorkRewardPreviewAndIncome() throws Exception {
        var game = fixedGame();
        TestMenu[] menu = new TestMenu[1];
        SwingUtilities.invokeAndWait(() -> {
            menu[0] = new TestMenu(game);
            var actions = collect(menu[0], JButton.class).stream().filter(b -> b.getText().equals("実行する")).toList();
            actions.get(3).doClick();
            assertEquals(1, game.getWorkEfficiency());
            actions.get(0).doClick();
            assertEquals(100, game.getPlun());
            collect(menu[0].workPanel, JToggleButton.class).getFirst().doClick();
            assertTrue(collect(menu[0].workPanel, JLabel.class).stream().anyMatch(l -> l.getText().contains("報酬 33")));
            start(menu[0].workPanel).doClick();
        });
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(133, game.getPlun());
            assertTrue(collect(menu[0], JTextArea.class).getFirst().getText().contains("プルン +33"));
            menu[0].removeNotify();
        });
    }

    @Test
    void fractionalEffectsAreAppliedBeforeRoundingAndLoggedAsActualChanges() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            pgame2.gameflow.GameSession game = new pgame2.gameflow.GameSession(new java.util.Random(0), new java.util.Random() { @Override public int nextInt(int bound) { return 0; } }) {
                @Override public Effects effects(Action action) {
                    if (action != Action.REST) return super.effects(action);
                    return new Effects(new java.math.BigDecimal("3.5"), new java.math.BigDecimal("-19.5"),
                            new java.math.BigDecimal("2.5"), new java.math.BigDecimal("1.5"));
                }
            };
            MainMenu menu = new MainMenu(game);
            assertTrue(collect(menu, JLabel.class).stream().anyMatch(label -> label.getText().contains("HP -19.5")));
            collect(menu, JButton.class).stream().filter(button -> button.getText().equals("実行する"))
                    .toList().get(1).doClick();
            assertEquals(80, game.getHp());
            assertEquals(8, game.getHours());
            assertEquals(102, game.getPlun());
            assertEquals(1, game.getWorkEfficiency());
            assertTrue(collect(menu, JTextArea.class).getFirst().getText()
                    .contains("HP -20 / プルン +2 / 労働効率 +1"));
        });
    }

    /** 選択中の予測は資源を変更せず、開始時に消え、完了後に次回分へ更新する。 */
    @Test void workPreviewFollowsSelectionExecutionAndClose() throws Exception {
        var game = fixedGame();
        TestMenu[] menu = new TestMenu[1];
        SwingUtilities.invokeAndWait(() -> {
            menu[0] = new TestMenu(game);
            menu[0].openWork();
            var gauges = collect(menu[0], PreviewGauge.class);
            collect(menu[0].workPanel, JToggleButton.class).getFirst().doClick();
            assertEquals("100 → 75 / 100", gauges.get(0).getString());
            assertEquals("12 → 8 / 12", gauges.get(1).getString());
            assertEquals(100, game.getHp());
            assertEquals(12, game.getHours());
            collect(menu[0].workPanel, JToggleButton.class).get(1).doClick();
            assertEquals("100 → 60 / 100", gauges.get(0).getString());
            collect(menu[0].workPanel, JToggleButton.class).getFirst().doClick();
            start(menu[0].workPanel).doClick();
            assertEquals("100 / 100", gauges.get(0).getString());
        });
        SwingUtilities.invokeAndWait(() -> {
            var gauges = collect(menu[0], PreviewGauge.class);
            assertEquals("75 → 50 / 100", gauges.get(0).getString());
            assertEquals("8 → 4 / 12", gauges.get(1).getString());
            menu[0].workPanel.requestClose();
            assertEquals("75 / 100", gauges.get(0).getString());
            assertEquals("8 / 12", gauges.get(1).getString());
            menu[0].removeNotify();
        });
    }

    /** 不足時は警告を表示し、日付変更と死亡で予測を解除する。 */
    @Test void workPreviewWarnsAndClearsOnDayChangeOrDeath() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var game = fixedGame();
            game.changeHp(java.math.BigDecimal.valueOf(-90));
            game.changeHours(java.math.BigDecimal.valueOf(-11));
            TestMenu menu = new TestMenu(game);
            menu.openWork();
            collect(menu.workPanel, JToggleButton.class).getFirst().doClick();
            var gauges = collect(menu, PreviewGauge.class);
            assertEquals("10 → 0 / 100", gauges.get(0).getString());
            assertEquals("1 → 0 / 12", gauges.get(1).getString());
            assertEquals("時間が足りません", gauges.get(0).getToolTipText());
            game.nextDay();
            menu.workPanel.refresh();
            assertFalse(gauges.get(0).getString().contains("→"));
            collect(menu.workPanel, JToggleButton.class).getFirst().doClick();
            game.changeHp(java.math.BigDecimal.valueOf(-10));
            menu.workPanel.refresh();
            assertFalse(gauges.get(0).getString().contains("→"));
            menu.removeNotify();
        });
    }

    /** 労働の結果から回復魔法の可否、資源、ログを更新する。 */
    @Test
    void actionButtonsUpdateStatusAndLog() throws Exception {
        var game = fixedGame();
        game.changePlun(java.math.BigDecimal.valueOf(-100));
        TestMenu[] menu = new TestMenu[1];
        SwingUtilities.invokeAndWait(() -> {
            menu[0] = new TestMenu(game);
            var actions = collect(menu[0], JButton.class).stream().filter(b -> b.getText().equals("実行する")).toList();
            assertEquals(6, actions.size());
            assertFalse(actions.get(2).isEnabled());
            actions.get(0).doClick();
            collect(menu[0].workPanel, JToggleButton.class).getFirst().doClick();
            start(menu[0].workPanel).doClick();
        });
        SwingUtilities.invokeAndWait(() -> {
            var actions = collect(menu[0], JButton.class).stream().filter(b -> b.getText().equals("実行する")).toList();
            assertTrue(actions.get(2).isEnabled());
            assertEquals(30, game.getPlun());
            actions.get(2).doClick();
            String log = collect(menu[0], JTextArea.class).getFirst().getText();
            assertTrue(log.contains("08:00  労働"));
            assertTrue(log.contains("12:00  回復魔法"));
            assertTrue(log.contains("HP +25"));
            assertEquals(7, game.getHours());
            menu[0].removeNotify();
        });
    }

    /** 襲撃なし・荷役候補で画面連携を検証する。 */
    private static pgame2.gameflow.GameSession fixedGame() {
        return new pgame2.gameflow.GameSession(new java.util.Random() {
            @Override public int nextInt(int bound) { return bound - 1; }
        }, new java.util.Random() { @Override public int nextInt(int bound) { return 0; } });
    }

    /** ネイティブダイアログの代わりに入力画面を保持する。 */
    private static class TestMenu extends MainMenu {
        /** 開いた労働画面。 */
        private pgame2.work.WorkPanel workPanel;
        TestMenu(pgame2.gameflow.GameSession game) { super(game); }
        @Override protected void openWork() { workPanel = createWorkPanel(() -> { }); }
        @Override protected void openTraining() {
            var panel = createTrainingPanel(() -> { });
            collect(panel, JButton.class).stream().filter(b -> b.getText().equals("鍛錬する")).findFirst().orElseThrow().doClick();
        }
    }

    /** 労働開始ボタンを取得する。 */
    private static JButton start(Container panel) {
        return collect(panel, JButton.class).stream().filter(b -> b.getText().equals("労働開始")).findFirst().orElseThrow();
    }

    /** 日付変更後の更新でも、初日と10の倍数の日だけショップの隣に行商人を表示する。 */
    @Test
    void travelerAppearsOnlyOnFirstDayAndMultiplesOfTen() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var game = new pgame2.gameflow.GameSession(new java.util.Random(0), new java.util.Random() { @Override public int nextInt(int bound) { return 0; } });
            game.changePlun(java.math.BigDecimal.valueOf(10000));
            MainMenu menu = new MainMenu(game);
            Container traveler = collect(menu, JLabel.class).stream()
                    .filter(label -> label.getText().equals("行商人")).findFirst().orElseThrow().getParent();
            Container shop = collect(menu, JLabel.class).stream()
                    .filter(label -> label.getText().equals("ショップ")).findFirst().orElseThrow().getParent();
            Component[] cards = shop.getParent().getComponents();
            int shopIndex = java.util.Arrays.asList(cards).indexOf(shop);
            assertSame(traveler, cards[shopIndex + 2]);
            assertTrue(traveler.isVisible());
            JButton rest = collect(menu, JButton.class).stream()
                    .filter(button -> button.getText().equals("実行する")).toList().get(1);
            for (int day = 2; day <= 21; day++) {
                game.nextDay();
                // 既存の行動による画面更新経路を通し、同じカードの表示切り替えを検証する。
                rest.doClick();
                assertEquals(day % 10 == 0, traveler.isVisible(), "Day " + day);
                assertEquals(traveler.isVisible(), cards[shopIndex + 1].isVisible());
                assertTrue(shop.isVisible());
            }
        });
    }

    /** 画面内の指定された種類の部品を取得する。 */
    private static <T extends Component> List<T> collect(Container container, Class<T> type) {
        List<T> results = new ArrayList<>();
        for (Component component : container.getComponents()) {
            if (type.isInstance(component)) results.add(type.cast(component));
            if (component instanceof Container child) results.addAll(collect(child, type));
        }
        return results;
    }
}
