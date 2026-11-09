package pgame2.battle;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** 戦闘を観戦し、決着後にのみ閉じられる専用画面。 */
public final class BattleDialog extends JDialog {
    /** 紙とインクを基調にした表示色。 */
    private static final Color PAPER = new Color(250, 246, 230), INK = new Color(76, 66, 50);
    /** 観戦対象の戦闘。 */
    private final AutoBattle battle;
    /** 固定領域内に表示する戦闘ログ。 */
    private final JTextArea log = new JTextArea();
    /** 主人公と敵の現在値。 */
    private final JLabel playerStatus = new JLabel(), enemyStatus = new JLabel();
    /** 決着と経過時刻の表示。 */
    private final JLabel result = new JLabel("魔獣に遭遇！", SwingConstants.CENTER);
    /** 戦闘終了まで無効にする確認ボタン。 */
    private final JButton close = new JButton("閉じる");
    /** 描画用の風景とキャラクター。 */
    private final Scene scene;
    /** 描画更新の起点。 */
    private long started;
    /** 戦闘計算と再描画を約60fpsで駆動する。 */
    private final Timer timer = new Timer(1000 / 60, event -> tick());

    /** 主人公情報、戦闘風景、ログ、敵情報の4領域を組み立てる。 */
    public BattleDialog(Window owner, AutoBattle battle) {
        super(owner, "オートバトル", ModalityType.APPLICATION_MODAL);
        this.battle = battle;
        BufferedImage playerImage = load("player.png");
        BufferedImage enemyImage = load("enemy.png");
        scene = new Scene(playerImage, enemyImage, load("background.png"));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBackground(PAPER);
        content.setBorder(new EmptyBorder(16, 16, 16, 16));
        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);
        JPanel status = section("主人公", playerStatus);
        status.setPreferredSize(new Dimension(200, 360));
        top.add(status, BorderLayout.WEST);
        top.add(scene, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new GridLayout(1, 2, 12, 0));
        bottom.setOpaque(false);
        bottom.setPreferredSize(new Dimension(0, 210));
        log.setEditable(false);
        log.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
        log.setBackground(PAPER);
        log.setForeground(INK);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setMargin(new Insets(8, 8, 8, 8));
        bottom.add(section("戦闘ログ", new JScrollPane(log)));
        JPanel enemyInfo = new JPanel(new BorderLayout(12, 0));
        enemyInfo.setOpaque(false);
        JLabel portrait = new JLabel(new ImageIcon(enemyImage.getScaledInstance(140, 140, Image.SCALE_SMOOTH)));
        enemyInfo.add(portrait, BorderLayout.WEST);
        enemyInfo.add(enemyStatus, BorderLayout.CENTER);
        bottom.add(section("トカゲ戦士", enemyInfo));
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(top, BorderLayout.CENTER);
        body.add(bottom, BorderLayout.SOUTH);
        content.add(body, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        result.setFont(new Font(Font.DIALOG, Font.BOLD, 18));
        footer.add(result, BorderLayout.CENTER);
        close.setEnabled(false);
        close.addActionListener(event -> dispose());
        footer.add(close, BorderLayout.EAST);
        content.add(footer, BorderLayout.SOUTH);
        setContentPane(content);
        setSize(1080, 720);
        setMinimumSize(new Dimension(960, 650));
        setLocationRelativeTo(owner);
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent event) {
                started = System.nanoTime();
                timer.start();
            }
            @Override public void windowClosing(WindowEvent event) {
                if (battle.isFinished()) dispose();
            }
        });
        updateStatus();
    }

    /** 必須素材の欠落・破損を明示して読み込む。 */
    private static BufferedImage load(String name) {
        var url = BattleDialog.class.getResource("/pgame2/battle/" + name);
        if (url == null) throw new IllegalStateException("戦闘素材がありません: " + name);
        try {
            BufferedImage image = ImageIO.read(url);
            if (image == null) throw new IllegalStateException("戦闘素材を読み込めません: " + name);
            return image;
        } catch (IOException exception) {
            throw new IllegalStateException("戦闘素材の読み込みに失敗: " + name, exception);
        }
    }

    /** 枠内の部品が増えても他領域の寸法を変えない。 */
    private static JPanel section(String title, JComponent body) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(PAPER);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(187, 177, 153)), new EmptyBorder(12, 12, 12, 12)));
        JLabel heading = new JLabel(title);
        heading.setFont(new Font(Font.DIALOG, Font.BOLD, 19));
        heading.setForeground(INK);
        panel.add(heading, BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    /** 描画の遅れがあっても、経過時刻までの攻撃をすべて順序通り処理する。 */
    private void tick() {
        for (AutoBattle.Attack attack : battle.advanceTo((System.nanoTime() - started) / 1_000_000_000.0)) {
            log.append(String.format(Locale.ROOT, "%.2fs %sの攻撃！ %dダメージ%s！%n",
                    attack.seconds(), attack.playerAttacks() ? "主人公" : "トカゲ戦士", attack.damage(),
                    attack.playerAttacks() ? "を与えた" : "を受けた"));
        }
        log.setCaretPosition(log.getDocument().getLength());
        updateStatus();
        scene.repaint();
        if (battle.isFinished()) {
            timer.stop();
            String outcome = battle.playerWon() ? "勝利！" : "敗北…";
            result.setText(String.format(Locale.ROOT, "%s　戦闘終了 %.2fs", outcome, battle.elapsed()));
            log.append(outcome + " 戦闘が終了しました。\n");
            log.setCaretPosition(log.getDocument().getLength());
            close.setEnabled(true);
            getRootPane().setDefaultButton(close);
            close.requestFocusInWindow();
        } else {
            result.setText(String.format(Locale.ROOT, "戦闘中　%.2fs", battle.elapsed()));
        }
    }

    /** 能力と戦闘専用HPを更新する。 */
    private void updateStatus() {
        playerStatus.setText(statusText(battle.player(), battle.playerHp(), ""));
        enemyStatus.setText(statusText(battle.enemy(), battle.enemyHp(), "<br>草原で出会った、<br>槍を持つ小さな戦士。"));
    }

    /** 素早さは毎秒の攻撃回数に換算する。 */
    private static String statusText(BattleStats stats, int hp, String description) {
        return String.format(Locale.ROOT,
                "<html>戦闘HP　%d / %d<br><br>攻撃力　%d<br>防御力　%d<br>攻撃速度　%.2f atk/s<br>%s</html>",
                hp, stats.toughness(), stats.attack(), stats.defense(), stats.speed() / 100.0, description);
    }

    /** ウィンドウ破棄時にタイマーも停止する。 */
    @Override public void dispose() {
        timer.stop();
        super.dispose();
    }

    /** 提供素材と固定配置のHP・行動ゲージを描画する。 */
    private final class Scene extends JPanel {
        /** 元画像を保ち、表示時にだけ拡縮する。 */
        private final BufferedImage playerImage, enemyImage, background;

        /** 観戦風景に使用する3枚の画像を受け取る。 */
        Scene(BufferedImage playerImage, BufferedImage enemyImage, BufferedImage background) {
            this.playerImage = playerImage;
            this.enemyImage = enemyImage;
            this.background = background;
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(background, 0, 0, getWidth(), getHeight(), null);
                drawFighter(g, true, getWidth() / 4);
                drawFighter(g, false, getWidth() * 3 / 4);
            } finally { g.dispose(); }
        }

        /** 数値の桁数に影響されない座標へキャラクターとバーを描く。 */
        private void drawFighter(Graphics2D g, boolean player, int center) {
            int width = Math.min(210, getWidth() / 3);
            int hp = player ? battle.playerHp() : battle.enemyHp();
            int max = player ? battle.player().toughness() : battle.enemy().toughness();
            g.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
            g.setColor(INK);
            String name = player ? "主人公" : "トカゲ戦士";
            g.drawString(name, center - g.getFontMetrics().stringWidth(name) / 2, 25);
            bar(g, center - width / 2, 37, width, hp / (double) max, new Color(78, 159, 91));
            g.setColor(INK);
            String hpText = hp + " / " + max;
            g.drawString(hpText, center - g.getFontMetrics().stringWidth(hpText) / 2, 72);
            BufferedImage picture = player ? playerImage : enemyImage;
            double scale = Math.min(width / (double) picture.getWidth(), (getHeight() - 135.0) / picture.getHeight());
            int imageWidth = (int) (picture.getWidth() * scale), imageHeight = (int) (picture.getHeight() * scale);
            g.drawImage(picture, center - imageWidth / 2, getHeight() - 45 - imageHeight, imageWidth, imageHeight, null);
            bar(g, center - width / 2, getHeight() - 30, width, battle.gauge(player) / 100, new Color(227, 190, 66));
        }

        /** HPと行動ゲージを同じ寸法で描画する。 */
        private void bar(Graphics2D g, int x, int y, int width, double ratio, Color color) {
            g.setColor(new Color(247, 242, 221));
            g.fillRoundRect(x, y, width, 15, 8, 8);
            g.setColor(color);
            g.fillRoundRect(x, y, (int) Math.round(width * ratio), 15, 8, 8);
            g.setColor(new Color(133, 122, 93));
            g.drawRoundRect(x, y, width, 15, 8, 8);
        }
    }
}
