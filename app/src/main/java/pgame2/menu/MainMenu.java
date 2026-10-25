package pgame2.menu;

import java.math.BigDecimal;
import pgame2.common.Modifier;

import java.awt.*;
import java.util.EnumMap;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import pgame2.gameflow.GameSession;
import pgame2.gameflow.GameSession.Action;

/** ステータス・行動選択・行動ログをまとめたメニュー。 */
public class MainMenu extends JPanel {
    /** カードの紙色。 */
    private static final Color PAPER = new Color(248, 242, 225);
    /** 本文の色。 */
    private static final Color INK = new Color(41, 53, 65);
    /** 操作ボタンの色。 */
    private static final Color BLUE = new Color(69, 126, 163);
    /** 現在のゲーム状態。 */
    private GameSession session;
    /** 日付表示。 */
    private final JLabel day = label("", 27);
    /** 所持プルン表示。 */
    private final JLabel plun = label("", 21);
    /** 基礎能力表示。 */
    private final JLabel ability = label("", 21);
    /** 体力表示。 */
    private final JProgressBar hp = meter(GameSession.MAX_HP, new Color(224, 113, 126));
    /** 残り時間表示。 */
    private final JProgressBar hours = meter(GameSession.DAILY_HOURS, new Color(89, 177, 209));
    /** 行動結果と進行状況の表示。 */
    private final JLabel message = label("今日はどうしようかな…", 18);
    /** 時系列の行動ログ。 */
    private final JTextArea log = new JTextArea();
    /** 実行条件に合わせて更新するボタン。 */
    private final EnumMap<Action, JButton> buttons = new EnumMap<>(Action.class);
    /** 補正済み行動の説明欄。 */
    private final EnumMap<Action, JLabel> effectLabels = new EnumMap<>(Action.class);
    /** 小目標とイベントの状態欄。 */
    private final JLabel progress = label("", 14);
    /** 任意の日終了ボタン。 */
    private final JButton endDay = button("一日を終了する");
    /** ゲームオーバー時の再開始ボタン。 */
    private final JButton restart = button("最初からやり直す");
    /** 一日開始時の体力。 */
    private int startHp;
    /** 一日開始時の所持金。 */
    private int startPlun;
    /** 一日開始時の能力。 */
    private int startAbility;

    /** サンプルの三列構成をリサイズ可能なレイアウトで組み立てる。 */
    public MainMenu() { this(new GameSession()); }

    /** 指定セッションの状態から画面を作る。 */
    public MainMenu(GameSession session) {
        this.session = session;
        startHp = session.getHp();
        startPlun = session.getPlun();
        startAbility = session.getAbility();
        setLayout(new BorderLayout(18, 18));
        setBackground(new Color(73, 63, 54));
        setBorder(new EmptyBorder(22, 22, 22, 22));
        add(createStatus(), BorderLayout.WEST);
        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);
        JPanel scene = new JPanel(new BorderLayout());
        scene.setBackground(new Color(224, 231, 212));
        message.setBorder(new EmptyBorder(16, 18, 12, 18));
        scene.add(message, BorderLayout.NORTH);
        scene.add(new PlunPicture(), BorderLayout.CENTER);
        progress.setBorder(new EmptyBorder(8, 18, 8, 18));
        scene.add(progress, BorderLayout.SOUTH);
        center.add(scene, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, 4, 10, 0));
        actions.setOpaque(false);
        for (Action action : Action.values()) actions.add(createAction(action));
        center.add(actions, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
        add(createLog(), BorderLayout.EAST);
        showNotices();
        refresh();
    }

    /** 日付と資源を左側へ配置する。 */
    private JPanel createStatus() {
        JPanel panel = paper();
        panel.setPreferredSize(new Dimension(205, 0));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        for (JComponent item : new JComponent[] {day, label("晴れ（試作表示）", 15),
                label("HP", 18), hp, label("残り時間", 18), hours, plun, ability}) {
            item.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(item);
            panel.add(Box.createVerticalStrut(22));
        }
        panel.add(Box.createVerticalGlue());
        panel.add(label("ぷるぷるサバイバル", 15));
        return panel;
    }

    /** 行動の効果と実行ボタンを同じカードへ配置する。 */
    private JPanel createAction(Action action) {
        JPanel panel = paper();
        panel.setLayout(new BorderLayout(0, 12));
        JLabel title = label(action.title, 22);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(title, BorderLayout.NORTH);
        JLabel effects = label("", 15);
        effectLabels.put(action, effects);
        panel.add(effects, BorderLayout.CENTER);
        JButton execute = button("実行する");
        execute.addActionListener(event -> execute(action));
        buttons.put(action, execute);
        panel.add(execute, BorderLayout.SOUTH);
        panel.setPreferredSize(new Dimension(165, 265));
        return panel;
    }

    /** スクロール可能なログと進行操作を右側へ配置する。 */
    private JPanel createLog() {
        JPanel panel = paper();
        panel.setPreferredSize(new Dimension(270, 0));
        panel.setLayout(new BorderLayout(0, 16));
        panel.add(label("行動ログ", 23), BorderLayout.NORTH);
        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setFont(new Font(Font.DIALOG, Font.PLAIN, 15));
        log.setBackground(INK);
        log.setForeground(PAPER);
        log.setMargin(new Insets(12, 12, 12, 12));
        log.setText("Day 1 — 一日の始まり\n");
        panel.add(new JScrollPane(log), BorderLayout.CENTER);
        JPanel controls = new JPanel(new GridLayout(0, 1, 0, 10));
        controls.setOpaque(false);
        endDay.addActionListener(event -> finishDay());
        controls.add(endDay);
        restart.addActionListener(event -> restart());
        controls.add(restart);
        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

    /** 実際の増減を記録し、死亡・日終了を順に判定する。 */
    private void execute(Action action) {
        int oldHp = session.getHp();
        int time = 8 + session.getDailyHours() - session.getHours();
        GameSession.Effects effect = session.effects(action);
        int plunChange = Modifier.floor(BigDecimal.valueOf(session.getPlun()).add(effect.plun())) - session.getPlun();
        int abilityChange = Modifier.floor(BigDecimal.valueOf(session.getAbility()).add(effect.ability())) - session.getAbility();
        session.execute(action);
        append(String.format("%02d:00  %s\nHP %s / プルン %s / 基礎能力 %s\n",
                time, action.title, signed(session.getHp() - oldHp), signed(plunChange), signed(abilityChange)));
        if (!session.getBattleMessage().isEmpty()) append(session.getBattleMessage());
        message.setText(action.title + "が終わった！");
        showNotices();
        refresh();
        if (session.isGameOver()) {
            message.setText("HPが0になりました。ゲームオーバー。");
            append("GAME OVER — 最初からやり直せます。\n");
        } else if (session.shouldEndDay()) {
            finishDay();
        }
    }

    /** 一日の収支を表示してから資源を引き継ぎ、翌日を始める。 */
    private void finishDay() {
        String summary = "Day " + session.getDay() + " が終わりました\n\n"
                + "HP " + startHp + " → " + session.getHp() + "\n"
                + "プルン " + startPlun + " → " + session.getPlun() + "\n"
                + "基礎能力 " + startAbility + " → " + session.getAbility();
        JOptionPane.showOptionDialog(this, summary, "一日の終了", JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, new String[] {"翌日へ"}, "翌日へ");
        append(summary + "\n");
        session.nextDay();
        showNotices();
        startHp = session.getHp();
        startPlun = session.getPlun();
        startAbility = session.getAbility();
        append("Day " + session.getDay() + " — 一日の始まり\n");
        message.setText("新しい一日が始まりました。");
        refresh();
    }

    /** ゲームオーバー後に初期状態へ戻す。 */
    private void restart() {
        session = new GameSession();
        startHp = session.getHp();
        startPlun = session.getPlun();
        startAbility = session.getAbility();
        log.setText("Day 1 — 一日の始まり\n");
        message.setText("今日はどうしようかな…");
        showNotices();
        refresh();
    }

    /** 最新の状態と実行条件を表示へ反映する。 */
    private void refresh() {
        day.setText("Day " + session.getDay());
        hp.setValue(session.getHp());
        hp.setString(session.getHp() + " / " + GameSession.MAX_HP);
        hours.setMaximum(session.getDailyHours());
        hours.setValue(session.getHours());
        hours.setString(session.getHours() + " / " + session.getDailyHours());
        plun.setText("プルン   " + session.getPlun());
        ability.setText("基礎能力   " + session.getAbility());
        progress.setText("<html>" + (session.getObjectiveDescription() + "\n\n" + session.getEventDescription()).replace("\n", "<br>") + "</html>");
        buttons.forEach((action, button) -> {
            GameSession.Effects effect = session.effects(action);
            effectLabels.get(action).setText("<html><body style='width:100px'>" + action.description
                    + "<br><br>時間 −" + effect.hours().stripTrailingZeros().toPlainString() + " 時間<br>HP " + signed(effect.hp())
                    + (effect.plun().signum() == 0 ? "" : "<br>プルン " + signed(effect.plun()))
                    + (effect.ability().signum() == 0 ? "" : "<br>基礎能力 " + signed(effect.ability())) + "</body></html>");
            String reason = session.unavailableReason(action);
            button.setEnabled(reason.isEmpty());
            button.setToolTipText(reason.isEmpty() ? action.description : reason);
        });
        endDay.setEnabled(!session.isGameOver());
        restart.setVisible(session.isGameOver());
    }

    /** 次の操作を受け付ける前に達成や予告を通知する。 */
    private void showNotices() {
        for (String notice : session.drainNotices()) {
            append(notice);
            if (isShowing()) JOptionPane.showMessageDialog(this, notice, "お知らせ", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** 直近のログが見えるよう追記する。 */
    private void append(String text) {
        log.append("\n" + text);
        log.setCaretPosition(log.getDocument().getLength());
    }

    /** 増減値に符号を付ける。 */
    private static String signed(int value) { return value > 0 ? "+" + value : String.valueOf(value); }

    /** 補正後の小数を丸めずに表示する。 */
    private static String signed(BigDecimal value) {
        return (value.signum() > 0 ? "+" : "") + value.stripTrailingZeros().toPlainString();
    }

    /** 共通の文字スタイルを適用する。 */
    private static JLabel label(String text, int size) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.DIALOG, Font.PLAIN, size));
        label.setForeground(INK);
        return label;
    }

    /** 紙色のカードを作る。 */
    private static JPanel paper() {
        JPanel panel = new JPanel();
        panel.setBackground(PAPER);
        panel.setBorder(new EmptyBorder(16, 14, 16, 14));
        return panel;
    }

    /** 資源の割合と値を表示する。 */
    private static JProgressBar meter(int max, Color color) {
        JProgressBar meter = new JProgressBar(0, max);
        meter.setForeground(color);
        meter.setStringPainted(true);
        meter.setMaximumSize(new Dimension(220, 26));
        meter.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
        return meter;
    }

    /** 操作ボタンの外観をそろえる。 */
    private static JButton button(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        button.setBackground(BLUE);
        button.setForeground(Color.WHITE);
        button.setMargin(new Insets(10, 8, 10, 8));
        return button;
    }

    /** メインウィンドウを開く。 */
    public static void open() {
        JFrame frame = new JFrame("ぷるぷるサバイバル");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new MainMenu());
        frame.setSize(1360, 780);
        frame.setMinimumSize(new Dimension(1280, 700));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /** 外部画像に依存しない、窓辺のプルンの試作イラスト。 */
    private static class PlunPicture extends JPanel {
        PlunPicture() { setOpaque(false); }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.scale(getWidth() / 640.0, getHeight() / 330.0);
                g.setColor(new Color(151, 199, 214));
                g.fillRoundRect(150, 5, 340, 240, 70, 70);
                g.setColor(new Color(246, 222, 149));
                g.fillOval(375, 28, 50, 50);
                g.setColor(new Color(115, 157, 127));
                g.fillOval(130, 155, 380, 140);
                g.setColor(new Color(112, 83, 58));
                g.setStroke(new BasicStroke(12));
                g.drawRoundRect(150, 5, 340, 240, 70, 70);
                g.drawLine(320, 7, 320, 240);
                g.setColor(new Color(177, 137, 95));
                g.fillRect(0, 258, 640, 72);
                g.setColor(new Color(139, 111, 80));
                g.fillOval(208, 278, 225, 28);
                g.setPaint(new GradientPaint(0, 150, new Color(92, 195, 236), 0, 290, new Color(198, 240, 238)));
                g.fillRoundRect(220, 145, 200, 150, 135, 135);
                g.setColor(new Color(230, 252, 255));
                g.setStroke(new BasicStroke(4));
                g.drawRoundRect(220, 145, 200, 150, 135, 135);
                g.fillOval(247, 168, 38, 20);
                g.setColor(INK);
                g.fillRoundRect(285, 220, 8, 23, 8, 8);
                g.fillRoundRect(348, 220, 8, 23, 8, 8);
                g.drawArc(310, 235, 20, 13, 185, 170);
            } finally {
                g.dispose();
            }
        }
    }
}
