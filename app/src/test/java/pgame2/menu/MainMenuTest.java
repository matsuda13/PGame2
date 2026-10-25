package pgame2.menu;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MainMenuTest {
    /** 鍛錬後に労働報酬の表示と獲得額がともに更新される。 */
    @Test
    void trainingUpdatesWorkRewardPreviewAndIncome() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var game = new pgame2.gameflow.GameSession(new java.util.Random(0));
            MainMenu menu = new MainMenu(game);
            List<JButton> actions = collect(menu, JButton.class).stream()
                    .filter(button -> button.getText().equals("実行する")).toList();
            actions.get(3).doClick();
            assertEquals(1, game.getWorkEfficiency());
            assertTrue(collect(menu, JLabel.class).stream()
                    .anyMatch(label -> label.getText().contains("プルン +33")));
            actions.get(0).doClick();
            assertEquals(33, game.getPlun());
            assertTrue(collect(menu, JTextArea.class).getFirst().getText().contains("プルン +33"));
        });
    }

    @Test
    void fractionalEffectsAreAppliedBeforeRoundingAndLoggedAsActualChanges() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            pgame2.gameflow.GameSession game = new pgame2.gameflow.GameSession(new java.util.Random(0)) {
                @Override public Effects effects(Action action) {
                    if (action != Action.TRAIN) return super.effects(action);
                    return new Effects(new java.math.BigDecimal("3.5"), new java.math.BigDecimal("-19.5"),
                            new java.math.BigDecimal("2.5"), new java.math.BigDecimal("1.5"));
                }
            };
            MainMenu menu = new MainMenu(game);
            assertTrue(collect(menu, JLabel.class).stream().anyMatch(label -> label.getText().contains("HP -19.5")));
            collect(menu, JButton.class).stream().filter(button -> button.getText().equals("実行する"))
                    .toList().get(3).doClick();
            assertEquals(80, game.getHp());
            assertEquals(8, game.getHours());
            assertEquals(2, game.getPlun());
            assertEquals(1, game.getWorkEfficiency());
            assertTrue(collect(menu, JTextArea.class).getFirst().getText()
                    .contains("HP -20 / プルン +2 / 労働効率 +1"));
        });
    }

    /** ボタン操作によって資源表示・ログ・実行可否が連動することを確認する。 */
    @Test
    void actionButtonsUpdateStatusAndLog() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MainMenu menu = new MainMenu(new pgame2.gameflow.GameSession(new java.util.Random() { @Override public int nextInt(int bound) { return bound - 1; } }));
            List<JButton> actions = new ArrayList<>();
            collect(menu, JButton.class).stream()
                    .filter(button -> button.getText().equals("実行する")).forEach(actions::add);
            assertEquals(4, actions.size());
            assertFalse(actions.get(2).isEnabled());
            actions.get(0).doClick();
            assertTrue(actions.get(2).isEnabled());
            assertTrue(collect(menu, JLabel.class).stream()
                    .anyMatch(label -> label.getText().equals("プルン   30")));
            actions.get(2).doClick();
            JTextArea log = collect(menu, JTextArea.class).getFirst();
            assertTrue(log.getText().contains("08:00  労働"));
            assertTrue(log.getText().contains("12:00  回復魔法"));
            assertTrue(log.getText().contains("HP +20"));
            assertTrue(collect(menu, JProgressBar.class).stream()
                    .anyMatch(bar -> bar.getString().equals("7 / 12")));
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
