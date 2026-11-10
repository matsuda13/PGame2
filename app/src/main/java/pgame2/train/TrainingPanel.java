package pgame2.train;

import java.awt.*;
import java.util.EnumMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** 状態参照と実行を呼び出し元へ委譲する鍛錬の入力画面。 */
public final class TrainingPanel extends JPanel {
    /** 選択中の能力。 */
    private TrainingAbility selected = TrainingAbility.WORK_EFFICIENCY;
    /** 時間の入力欄。 */
    private final JSpinner hours;
    /** 能力ごとの現在値と予想値。 */
    private final EnumMap<TrainingAbility, JLabel> values = new EnumMap<>(TrainingAbility.class);
    /** 実行内容とエラーを表示する欄。 */
    private final JLabel summary = new JLabel();
    /** 実行ボタン。 */
    private final JButton execute = new JButton("実行");
    /** 能力の現在値を取得する。 */
    private final ToIntFunction<TrainingAbility> current;
    /** 成長量を予測する。 */
    private final BiFunction<TrainingAbility, Integer, Integer> gain;
    /** HPの実消費量を予測する。 */
    private final java.util.function.IntUnaryOperator hpCost;
    /** 実行不可理由を取得する。 */
    private final BiFunction<TrainingAbility, Integer, String> reason;

    /** 画面表示だけではゲーム状態を変更しない。 */
    public TrainingPanel(int remainingHours, ToIntFunction<TrainingAbility> current,
            BiFunction<TrainingAbility, Integer, Integer> gain, java.util.function.IntUnaryOperator hpCost,
            BiFunction<TrainingAbility, Integer, String> reason, BiConsumer<TrainingAbility, Integer> run, Runnable cancel) {
        this.current = current; this.gain = gain; this.hpCost = hpCost; this.reason = reason;
        setLayout(new BorderLayout(12, 16));
        setBorder(new EmptyBorder(20, 24, 20, 24));
        setBackground(new Color(248, 242, 225));
        JLabel title = new JLabel("鍛錬 — 育てる能力を選択");
        title.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
        add(title, BorderLayout.NORTH);
        JPanel rows = new JPanel(new GridLayout(0, 1, 0, 12));
        rows.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (TrainingAbility ability : TrainingAbility.values()) {
            JPanel row = new JPanel(new BorderLayout(12, 4));
            row.setOpaque(false);
            JRadioButton choice = new JRadioButton(ability.title, ability == selected);
            choice.setOpaque(false);
            choice.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
            group.add(choice);
            row.add(choice, BorderLayout.WEST);
            JLabel value = new JLabel(); values.put(ability, value);
            row.add(value, BorderLayout.EAST);
            row.add(new JLabel(ability.description), BorderLayout.SOUTH);
            choice.addActionListener(event -> { selected = ability; refresh(); });
            rows.add(row);
        }
        add(rows, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new GridLayout(0, 1, 0, 12)); bottom.setOpaque(false);
        JPanel time = new JPanel(new FlowLayout(FlowLayout.LEFT)); time.setOpaque(false);
        hours = new JSpinner(new SpinnerNumberModel(1, 1, Math.max(1, remainingHours), 1));
        ((JSpinner.DefaultEditor) hours.getEditor()).getTextField().setEditable(false);
        hours.setEnabled(remainingHours > 0);
        hours.addChangeListener(event -> refresh());
        time.add(new JLabel("鍛錬時間")); time.add(hours);
        time.add(new JLabel("時間 / 残り " + remainingHours + "時間"));
        bottom.add(time); bottom.add(summary);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT)); buttons.setOpaque(false);
        JButton back = new JButton("キャンセル"); back.addActionListener(event -> cancel.run());
        execute.addActionListener(event -> {
            int duration = (Integer) hours.getValue();
            if (!reason.apply(selected, duration).isEmpty()) { refresh(); return; }
            run.accept(selected, duration);
        });
        buttons.add(back); buttons.add(execute); bottom.add(buttons);
        add(bottom, BorderLayout.SOUTH);
        refresh();
    }
    /** 選択内容から予想値と実行可否を更新する。 */
    private void refresh() {
        int duration = (Integer) hours.getValue();
        values.forEach((ability, label) -> label.setText(current.applyAsInt(ability) + " → "
                + (current.applyAsInt(ability) + (ability == selected ? gain.apply(ability, duration) : 0))));
        String unavailable = reason.apply(selected, duration);
        summary.setText("<html>" + selected.title + " +" + gain.apply(selected, duration)
                + " / 時間 −" + duration + " / HP −" + hpCost.applyAsInt(duration)
                + (unavailable.isEmpty() ? "" : "<br>" + unavailable) + "</html>");
        execute.setEnabled(unavailable.isEmpty());
    }
}
