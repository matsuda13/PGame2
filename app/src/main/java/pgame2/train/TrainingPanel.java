package pgame2.train;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.Hashtable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** 能力カードと時間スライダーで鍛錬内容を選ぶ画面。 */
public final class TrainingPanel extends JPanel {
    /** ゲーム内の紙・インク・差し色。 */
    private static final Color PAPER = new Color(248, 242, 225), INK = new Color(41, 53, 65),
            BLUE = new Color(69, 126, 163), MUTED = new Color(112, 111, 99), GREEN = new Color(60, 126, 91);
    /** 選択中の能力。 */
    private TrainingAbility selected = TrainingAbility.WORK_EFFICIENCY;
    /** 1時間刻みの時間入力。 */
    private final JSlider hours;
    /** 能力ごとの予想値、増加量、選択カード。 */
    private final EnumMap<TrainingAbility, JLabel> values = new EnumMap<>(TrainingAbility.class),
            gains = new EnumMap<>(TrainingAbility.class);
    private final EnumMap<TrainingAbility, Card> cards = new EnumMap<>(TrainingAbility.class);
    /** 時間と実行内容の表示。 */
    private final JLabel durationLabel = text("", 22, true), selectedLabel = text("", 15, true),
            afterLabel = text("", 15, true), timeLabel = text("", 15, true), hpLabel = text("", 15, true),
            error = text("", 13, false);
    /** 実行ボタン。 */
    private final JButton execute = new JButton("鍛錬する");
    /** 現在値と予測、可否判定を呼び出し元へ委譲する。 */
    private final ToIntFunction<TrainingAbility> current;
    private final BiFunction<TrainingAbility, Integer, Integer> gain;
    private final java.util.function.IntUnaryOperator hpCost;
    private final BiFunction<TrainingAbility, Integer, String> reason;

    /** 選択操作では状態を変えず、実行時だけ処理を呼び出す。 */
    public TrainingPanel(int remainingHours, ToIntFunction<TrainingAbility> current,
            BiFunction<TrainingAbility, Integer, Integer> gain, java.util.function.IntUnaryOperator hpCost,
            BiFunction<TrainingAbility, Integer, String> reason, BiConsumer<TrainingAbility, Integer> run, Runnable cancel) {
        this.current = current; this.gain = gain; this.hpCost = hpCost; this.reason = reason;
        setLayout(new BorderLayout(0, 14));
        setBorder(new EmptyBorder(18, 22, 18, 22));
        setBackground(PAPER);
        JPanel header = clear(new BorderLayout());
        JPanel heading = clear(new GridLayout(0, 1, 0, 4));
        heading.add(text("鍛錬", 24, true));
        JLabel instruction = text("鍛える能力を選択してください。", 13, false);
        instruction.setForeground(MUTED); heading.add(instruction);
        header.add(heading, BorderLayout.CENTER);
        JButton close = new JButton("×");
        close.setFont(new Font(Font.DIALOG, Font.PLAIN, 22));
        close.setForeground(MUTED); close.setContentAreaFilled(false); close.setBorderPainted(false);
        close.setToolTipText("キャンセル"); close.getAccessibleContext().setAccessibleName("キャンセル");
        close.addActionListener(event -> cancel.run());
        header.add(close, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel rows = clear(new GridLayout(5, 1, 0, 8));
        ButtonGroup group = new ButtonGroup();
        for (TrainingAbility ability : TrainingAbility.values()) {
            Card card = new Card(); cards.put(ability, card);
            card.setLayout(new BorderLayout(8, 0));
            card.setBorder(new EmptyBorder(8, 12, 8, 14));
            JRadioButton choice = new JRadioButton();
            choice.setActionCommand(ability.name());
            choice.getAccessibleContext().setAccessibleName(ability.title);
            choice.setToolTipText(ability.title);
            choice.setSelected(ability == selected); choice.setOpaque(false);
            group.add(choice); card.add(choice, BorderLayout.WEST);
            JPanel description = clear(new GridLayout(2, 1, 0, 3));
            description.add(text(ability.title, 15, true));
            JLabel detail = text(ability.description, 12, false); detail.setForeground(MUTED);
            description.add(detail); card.add(description, BorderLayout.CENTER);
            JPanel numbers = clear(new GridLayout(2, 1, 0, 3));
            JLabel value = text("", 15, true), growth = text("", 12, true);
            value.setHorizontalAlignment(SwingConstants.RIGHT); growth.setHorizontalAlignment(SwingConstants.RIGHT);
            growth.setForeground(GREEN); values.put(ability, value); gains.put(ability, growth);
            numbers.add(value); numbers.add(growth); card.add(numbers, BorderLayout.EAST);
            choice.addActionListener(event -> { selected = ability; refresh(); });
            makeSelectable(card, choice);
            rows.add(card);
        }
        add(rows, BorderLayout.CENTER);

        JPanel bottom = clear(new BorderLayout(0, 10));
        JPanel time = clear(new BorderLayout(0, 4));
        time.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
                new Color(220, 211, 192)), new EmptyBorder(12, 0, 0, 0)));
        JPanel timeHeader = clear(new BorderLayout());
        timeHeader.add(text("鍛錬時間", 14, true), BorderLayout.WEST);
        timeHeader.add(durationLabel, BorderLayout.EAST); time.add(timeHeader, BorderLayout.NORTH);
        int maximum = Math.max(1, remainingHours);
        hours = new JSlider(1, maximum, 1);
        hours.setUI(new javax.swing.plaf.metal.MetalSliderUI() {
            /** トラックを押した位置へ移動してから、標準のドラッグ処理を開始する。 */
            @Override protected TrackListener createTrackListener(JSlider slider) {
                return new TrackListener() {
                    @Override public void mousePressed(MouseEvent event) {
                        calculateGeometry();
                        if (slider.isEnabled() && SwingUtilities.isLeftMouseButton(event)
                                && !thumbRect.contains(event.getPoint())) {
                            slider.setValue(valueForXPosition(event.getX()));
                            // 移動後のつまみ中央からドラッグを始め、標準の段階移動を防ぐ。
                            event = new MouseEvent(slider, event.getID(), event.getWhen(), event.getModifiersEx(),
                                    thumbRect.x + thumbRect.width / 2, thumbRect.y + thumbRect.height / 2,
                                    event.getClickCount(), event.isPopupTrigger(), event.getButton());
                        }
                        super.mousePressed(event);
                    }
                };
            }
        });
        hours.setOpaque(false); hours.setForeground(BLUE);
        hours.setMinorTickSpacing(1); hours.setSnapToTicks(true); hours.setPaintTicks(true);
        hours.setEnabled(remainingHours > 0);
        hours.getAccessibleContext().setAccessibleName("鍛錬時間");
        Hashtable<Integer, JLabel> ticks = new Hashtable<>();
        for (int value = 1; value <= maximum; value++) {
            if (maximum <= 6 || value == 1 || value == maximum || value % 3 == 0)
                ticks.put(value, text(value + "h", 11, false));
        }
        hours.setLabelTable(ticks); hours.setPaintLabels(true);
        hours.addChangeListener(event -> refresh()); time.add(hours, BorderLayout.CENTER);
        JLabel remaining = text("残り時間 " + remainingHours + "時間", 12, false);
        remaining.setForeground(MUTED); remaining.setHorizontalAlignment(SwingConstants.RIGHT);
        time.add(remaining, BorderLayout.SOUTH); bottom.add(time, BorderLayout.NORTH);

        Card summary = new Card(); summary.setBackground(new Color(239, 231, 211));
        summary.setLayout(new GridLayout(4, 1, 0, 5)); summary.setBorder(new EmptyBorder(12, 14, 12, 14));
        summary.add(summaryRow("選択中", selectedLabel)); summary.add(summaryRow("鍛錬後", afterLabel));
        summary.add(summaryRow("消費時間", timeLabel)); summary.add(summaryRow("消費HP", hpLabel));
        bottom.add(summary, BorderLayout.CENTER);
        JPanel actions = clear(new BorderLayout(0, 4));
        error.setForeground(new Color(164, 65, 55)); error.setVisible(false);
        actions.add(error, BorderLayout.NORTH);
        execute.setFont(new Font(Font.DIALOG, Font.BOLD, 16)); execute.setBackground(BLUE); execute.setForeground(Color.WHITE);
        execute.setFocusPainted(false); execute.setBorder(new EmptyBorder(11, 12, 11, 12));
        execute.addActionListener(event -> {
            int duration = hours.getValue();
            if (!reason.apply(selected, duration).isEmpty()) { refresh(); return; }
            run.accept(selected, duration);
        });
        actions.add(execute, BorderLayout.CENTER); bottom.add(actions, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
        refresh();
    }

    /** カードの文字や余白もクリックで選べるようにする。 */
    private static void makeSelectable(Component component, JRadioButton choice) {
        if (component == choice) return;
        component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        component.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) { choice.doClick(); }
        });
        if (component instanceof Container container)
            for (Component child : container.getComponents()) makeSelectable(child, choice);
    }
    /** 各能力を選んだ場合の予測と、実行する一能力の詳細を更新する。 */
    private void refresh() {
        int duration = hours.getValue();
        values.forEach((ability, label) -> {
            int growth = gain.apply(ability, duration);
            label.setText(current.applyAsInt(ability) + " → " + (current.applyAsInt(ability) + growth));
            gains.get(ability).setText("+" + growth);
            cards.get(ability).selected = ability == selected;
            cards.get(ability).repaint();
        });
        durationLabel.setText(duration + "時間");
        selectedLabel.setText(selected.title);
        afterLabel.setText(values.get(selected).getText());
        timeLabel.setText(duration + "時間"); hpLabel.setText(String.valueOf(hpCost.applyAsInt(duration)));
        String unavailable = reason.apply(selected, duration);
        error.setText(unavailable); error.setVisible(!unavailable.isEmpty());
        execute.setEnabled(unavailable.isEmpty());
    }
    /** 内容に余白を持たせつつ、参照画像に近い横幅を保つ。 */
    @Override public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(Math.max(620, size.width), size.height);
    }
    /** 確認欄の名称と値を左右へ配置する。 */
    private static JPanel summaryRow(String name, JLabel value) {
        JPanel row = clear(new BorderLayout());
        JLabel label = text(name, 13, false); label.setForeground(MUTED);
        row.add(label, BorderLayout.WEST); row.add(value, BorderLayout.EAST); return row;
    }
    /** 紙の背景を透過する配置パネル。 */
    private static JPanel clear(LayoutManager layout) {
        JPanel panel = new JPanel(layout); panel.setOpaque(false); return panel;
    }
    /** 日本語の文字サイズとインク色を統一する。 */
    private static JLabel text(String value, int size, boolean bold) {
        JLabel label = new JLabel(value); label.setForeground(INK);
        label.setFont(new Font(Font.DIALOG, bold ? Font.BOLD : Font.PLAIN, size)); return label;
    }
    /** 選択状態を淡い青と輪郭で示す角丸カード。 */
    private static final class Card extends JPanel {
        /** 選択中かどうか。 */
        private boolean selected;
        Card() { setOpaque(false); setBackground(new Color(252, 248, 238)); }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(selected ? new Color(229, 240, 240) : getBackground());
                g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 18, 18);
                g.setColor(selected ? BLUE : new Color(224, 216, 199));
                g.setStroke(new BasicStroke(selected ? 2f : 1f));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 18, 18);
            } finally { g.dispose(); }
        }
    }
}
