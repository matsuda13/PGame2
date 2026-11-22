package pgame2.work;

import java.awt.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** 日替わりの労働を選択し、結果を残したまま繰り返し実行する画面。 */
public final class WorkPanel extends JPanel {
    /** 画面表示に必要なゲーム状態。 */
    public record State(int day, int hours, int hp, int maxHp, int plun, List<Work> candidates) { }
    /** 補正後の実消費と報酬。 */
    public record Preview(int hours, int hpCost, int reward, int chance) { }
    /** 表示を更新するための読み取り口。 */
    private final Supplier<State> state;
    /** 選択した仕事の表示用計算。 */
    private final Function<Work, Preview> preview;
    /** 開始直前にも利用する実行条件。 */
    private final Function<Work, String> unavailable;
    /** 戦闘と日終了を含めた進行処理。 */
    private final Function<Work, String> perform;
    /** ウィンドウを閉じる処理。 */
    private final Runnable close;
    /** 当日内で維持する選択。 */
    private Work selected;
    /** 最後に表示した日。 */
    private int shownDay = -1;
    /** 再入と二重実行を防ぐ処理中フラグ。 */
    private boolean busy;
    /** 状態・選択詳細・結果を固定領域に表示する。 */
    private final JLabel status = new JLabel(), detail = new JLabel(), reason = new JLabel(), result = new JLabel("労働結果はここに表示されます。");
    /** 同寸法の候補一覧。 */
    private final JPanel choices = new JPanel(new GridLayout(2, 1, 0, 12));
    /** 素材未登録時にも同じサイズを確保する。 */
    private final JLabel illustration = new JLabel("仕事を選択してください", SwingConstants.CENTER);
    /** 実行と閉じる操作。 */
    private final JButton start = new JButton("労働開始"), closeButton = new JButton("閉じる");

    /** 状態と操作を受け取り、ゲーム進行との依存をコールバックに限定する。 */
    public WorkPanel(Supplier<State> state, Function<Work, Preview> preview,
                     Function<Work, String> unavailable, Function<Work, String> perform, Runnable close) {
        this.state = state;
        this.preview = preview;
        this.unavailable = unavailable;
        this.perform = perform;
        this.close = close;
        setLayout(new BorderLayout(16, 16));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(new Color(248, 242, 225));
        JPanel header = paper(new BorderLayout(12, 0));
        header.add(status, BorderLayout.CENTER);
        header.add(closeButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        choices.setOpaque(false);
        JPanel left = paper(new BorderLayout(0, 12));
        left.setPreferredSize(new Dimension(240, 320));
        left.add(new JLabel("本日の労働"), BorderLayout.NORTH);
        left.add(choices, BorderLayout.CENTER);
        JPanel right = paper(new BorderLayout(12, 12));
        detail.setVerticalAlignment(SwingConstants.TOP);
        detail.setPreferredSize(new Dimension(580, 140));
        right.add(detail, BorderLayout.NORTH);
        illustration.setOpaque(true);
        illustration.setBackground(new Color(227, 237, 221));
        right.add(illustration, BorderLayout.CENTER);
        JPanel controls = paper(new BorderLayout(4, 6));
        controls.add(reason, BorderLayout.NORTH);
        controls.add(start, BorderLayout.EAST);
        right.add(controls, BorderLayout.SOUTH);
        JPanel center = paper(new BorderLayout(16, 0));
        center.add(left, BorderLayout.WEST);
        center.add(right, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        JPanel footer = paper(new BorderLayout(16, 0));
        footer.setPreferredSize(new Dimension(800, 110));
        JLabel resultPicture = new JLabel("結果イラスト（仮）", SwingConstants.CENTER);
        resultPicture.setPreferredSize(new Dimension(160, 100));
        footer.add(resultPicture, BorderLayout.WEST);
        footer.add(result, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
        applyFont(this);
        reason.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
        start.addActionListener(event -> beginWork());
        closeButton.addActionListener(event -> requestClose());
        refresh();
    }

    /** 処理中はタイトルバーの閉じる操作も無効化する。 */
    public void requestClose() { if (!busy) close.run(); }

    /** 開始を予約して先に操作をロックし、連続したクリックを受け付けない。 */
    private void beginWork() {
        if (busy || selected == null) return;
        if (!unavailable.apply(selected).isEmpty()) { refresh(); return; }
        Work work = selected;
        busy = true;
        refresh();
        SwingUtilities.invokeLater(() -> {
            try {
                String blocked = unavailable.apply(work);
                if (blocked.isEmpty()) result.setText(perform.apply(work));
                else result.setText(blocked);
            } finally {
                busy = false;
                refresh();
            }
        });
    }

    /** 日付変更時だけ選択を解除し、結果は次の実行まで維持する。 */
    public void refresh() {
        State current = state.get();
        if (shownDay != current.day()) { selected = null; shownDay = current.day(); }
        status.setText("労働  Day " + current.day() + "   残り " + current.hours() + "時間   HP "
                + current.hp() + "/" + current.maxHp() + "   " + current.plun() + " PLN");
        choices.removeAll();
        for (Work work : current.candidates()) {
            Preview value = preview.apply(work);
            JToggleButton card = new JToggleButton("<html>◇ " + work.name() + "<br><br>報酬 " + value.reward()
                    + " PLN<br>時間 " + value.hours() + "時間 / HP " + value.hpCost() + "</html>");
            card.setFont(new Font(Font.DIALOG, Font.PLAIN, 17));
            card.setSelected(work.equals(selected));
            card.setEnabled(!busy);
            card.addActionListener(event -> { selected = work; refresh(); });
            choices.add(card);
        }
        String blocked = unavailable.apply(selected);
        start.setEnabled(!busy && blocked.isEmpty());
        closeButton.setEnabled(!busy);
        reason.setText(busy ? "労働中…" : blocked.isEmpty() ? " " : blocked);
        if (selected == null) {
            detail.setText("<html>本日の労働から仕事を選択してください。<br><br><br><br><br></html>");
            illustration.setText("仕事を選択してください");
        } else {
            Preview value = preview.apply(selected);
            detail.setText("<html>" + selected.name() + " — " + selected.description() + "<br><br>時間 "
                    + value.hours() + "時間 / HP " + value.hpCost() + " / 報酬 " + value.reward()
                    + " PLN<br>戦闘発生確率 " + value.chance() + "% / 戦闘危険度 " + selected.danger()
                    + "<br>" + (selected.danger() == 0 ? "危険度0：戦闘は発生しません" : "戦闘HPは通常HPとは独立しています") + "</html>");
            illustration.setText(selected.name() + "のイラスト（仮）");
        }
        revalidate();
        repaint();
    }

    @Override public Dimension getPreferredSize() { return new Dimension(900, 580); }

    /** 紙色を透かす固定領域を作る。 */
    private static JPanel paper(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    /** 子部品の日本語フォントを統一する。 */
    private static void applyFont(Container parent) {
        for (Component child : parent.getComponents()) {
            child.setFont(new Font(Font.DIALOG, Font.PLAIN, 17));
            if (child instanceof Container nested) applyFont(nested);
        }
    }
}
