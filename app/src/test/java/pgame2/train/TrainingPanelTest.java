package pgame2.train;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import pgame2.gameflow.GameSession;
import static org.junit.jupiter.api.Assertions.*;

class TrainingPanelTest {
    /** 選択と時間変更は予測だけを更新し、キャンセルでは消費しない。 */
    @Test void selectingAndCancellingNeverChangesResources() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameSession game = new GameSession(new java.util.Random(0));
            AtomicInteger closed = new AtomicInteger();
            TrainingPanel panel = new TrainingPanel(game.getHours(), game::abilityValue,
                    (a, h) -> game.previewTraining(a, h).gain(), h -> 5 * h,
                    game::trainingUnavailableReason, (a, h) -> game.train(a, h), closed::incrementAndGet);
            collect(panel, JRadioButton.class).stream().filter(b -> b.getAccessibleContext().getAccessibleName().equals("素早さ")).findFirst().orElseThrow().doClick();
            collect(panel, JSlider.class).getFirst().setValue(3);
            assertTrue(collect(panel, JLabel.class).stream().anyMatch(l -> l.getText().equals("50 → 53")));
            collect(panel, JButton.class).stream().filter(b -> b.getText().equals("×")).findFirst().orElseThrow().doClick();
            assertEquals(1, closed.get());
            assertEquals(100, game.getHp());
            assertEquals(12, game.getHours());
            collect(panel, JButton.class).stream().filter(b -> b.getText().equals("鍛錬する")).findFirst().orElseThrow().doClick();
            assertEquals(53, game.getBattleStats().speed());
            assertEquals(85, game.getHp());
            assertEquals(9, game.getHours());
        });
    }
    /** 子部品を再帰的に集める。 */
    private static <T extends Component> List<T> collect(Container parent, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) result.add(type.cast(child));
            if (child instanceof Container container) result.addAll(collect(container, type));
        }
        return result;
    }
}
