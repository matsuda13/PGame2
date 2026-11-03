package pgame2.shop;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.random.RandomGenerator;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import pgame2.shop.ShopCatalog.Merchant;
import pgame2.item.Item;
import pgame2.gameflow.GameSession;

/** 商人画像・商品一覧・詳細欄を固定配置するショップの画面。 */
public final class ShopPanel extends JPanel {
    /** 紙をイメージした背景色。 */
    private static final Color PAPER = new Color(248, 242, 225);
    /** インクをイメージした文字色。 */
    private static final Color INK = new Color(65, 54, 45);
    /** 商品選択時の淡い背景色。 */
    private static final Color SELECTED = new Color(218, 230, 216);
    /** 単一選択の商品一覧。 */
    private final JList<Item> products;
    /** 固定領域内で折り返す商品詳細。 */
    private final JTextArea description = textArea("商品を選択してください", 16);
    /** 選択商品と資金に応じて更新する購入ボタン。 */
    private final JButton purchase = new JButton("購入");
    /** 購入処理を所有するゲーム状態。 */
    private final GameSession session;
    /** 購入直後にも更新する所持金表示。 */
    private final JLabel balance = label("", 18);
    /** 購入できない理由や購入結果。 */
    private final JLabel status = label("商品を選択してください", 13);

    /** 店舗ごとの商品一覧と購入先のセッションを受け取る。 */
    public ShopPanel(Merchant merchant, List<Item> catalog, GameSession session, Runnable close) {
        this.session = session;
        setLayout(new BorderLayout(18, 18));
        setBackground(PAPER);
        setBorder(new EmptyBorder(20, 22, 22, 22));
        setPreferredSize(new Dimension(1060, 650));

        JPanel header = panel(new BorderLayout(16, 0));
        header.add(label(merchant.title, 26), BorderLayout.WEST);
        JPanel controls = panel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        controls.add(balance);
        JButton closeButton = new JButton("閉じる");
        closeButton.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
        closeButton.addActionListener(event -> close.run());
        controls.add(closeButton);
        header.add(controls, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel body = panel(new BorderLayout(18, 0));
        JPanel merchantArea = panel(new BorderLayout(0, 12));
        merchantArea.setPreferredSize(new Dimension(430, 0));
        merchantArea.add(new MerchantPicture(merchant.imagePath), BorderLayout.CENTER);
        JTextArea greeting = textArea("店主\n\n" + ShopCatalog.greeting(merchant, RandomGenerator.getDefault()), 18);
        greeting.setBackground(new Color(227, 209, 179));
        greeting.setBorder(new EmptyBorder(18, 18, 18, 18));
        JScrollPane counter = scroll(greeting);
        counter.setPreferredSize(new Dimension(430, 140));
        merchantArea.add(counter, BorderLayout.SOUTH);
        body.add(merchantArea, BorderLayout.WEST);

        JPanel goods = panel(new BorderLayout(0, 14));
        JPanel listing = panel(new BorderLayout(0, 10));
        listing.add(label("商品一覧", 20), BorderLayout.NORTH);
        products = new JList<>(catalog.toArray(Item[]::new));
        products.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        products.setFixedCellHeight(76);
        products.setBackground(PAPER);
        products.setCellRenderer(new ProductRenderer());
        products.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) showSelection();
        });
        listing.add(scroll(products), BorderLayout.CENTER);
        goods.add(listing, BorderLayout.CENTER);

        JPanel details = panel(new BorderLayout(12, 12));
        details.setPreferredSize(new Dimension(0, 210));
        details.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(185, 168, 142)), new EmptyBorder(14, 14, 14, 14)));
        details.add(scroll(description), BorderLayout.CENTER);
        JPanel footer = panel(new BorderLayout(12, 0));
        footer.add(status, BorderLayout.CENTER);
        purchase.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
        purchase.setEnabled(false);
        purchase.addActionListener(event -> purchaseSelected());
        footer.add(purchase, BorderLayout.EAST);
        details.add(footer, BorderLayout.SOUTH);
        goods.add(details, BorderLayout.SOUTH);
        body.add(goods, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
        refreshPurchase();
    }

    /** メイン画面を親とする固定サイズのモーダル画面を開く。 */
    public static void open(Component parent, Merchant merchant, GameSession session) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), merchant.title,
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(new ShopPanel(merchant, session.shopItems(merchant == Merchant.TRAVELER), session, dialog::dispose));
        dialog.setResizable(false);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "closeShop");
        dialog.getRootPane().getActionMap().put("closeShop", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { dialog.dispose(); }
        });
        dialog.setVisible(true);
    }

    /** 選択時は詳細と購入可否のみを更新する。 */
    private void showSelection() {
        Item selected = products.getSelectedValue();
        description.setText(selected == null ? "商品を選択してください"
                : selected.title() + "\n\n" + selected.effect() + "\n\n" + selected.description());
        description.setCaretPosition(0);
        refreshPurchase();
    }

    /** 最新の所持金と購入済み状態を表示へ反映する。 */
    private void refreshPurchase() {
        balance.setText(String.format(Locale.JAPAN, "所持プルン  %,d PLN", session.getPlun()));
        Item selected = products.getSelectedValue();
        String reason = selected == null
                ? (products.getModel().getSize() == 0 ? "今回の商品はありません" : "商品を選択してください")
                : session.purchaseUnavailableReason(selected);
        purchase.setText(selected != null && session.owns(selected) ? "購入済み" : "購入");
        purchase.setEnabled(reason.isEmpty());
        purchase.setToolTipText(reason.isEmpty() ? "この商品を購入します" : reason);
        status.setText(reason.isEmpty() ? "購入すると永続的に効果を発揮します" : reason);
        products.repaint();
    }

    /** 押下時に条件を再確認し、購入後も画面を開いたままにする。 */
    private void purchaseSelected() {
        Item selected = products.getSelectedValue();
        if (selected == null || !session.purchaseUnavailableReason(selected).isEmpty()) {
            refreshPurchase();
            return;
        }
        session.purchase(selected);
        refreshPurchase();
        status.setText(selected.title() + "を購入しました");
    }

    /** 透明な領域を作り、紙色を共用する。 */
    private static JPanel panel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    /** 日本語の文字サイズと色をそろえる。 */
    private static JLabel label(String text, int size) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.DIALOG, Font.PLAIN, size));
        label.setForeground(INK);
        return label;
    }

    /** 長文を固定領域内に折り返す読み取り専用欄を作る。 */
    private static JTextArea textArea(String text, int size) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(Font.DIALOG, Font.PLAIN, size));
        area.setForeground(INK);
        area.setBackground(PAPER);
        return area;
    }

    /** 領域の内側だけで縦方向にスクロールさせる。 */
    private static JScrollPane scroll(Component component) {
        JScrollPane scroll = new JScrollPane(component, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(PAPER);
        return scroll;
    }

    /** 固定寸法の仮アイコン、商品名、右揃え価格を一行で描画する。 */
    private final class ProductRenderer implements ListCellRenderer<Item> {
        @Override
        public Component getListCellRendererComponent(JList<? extends Item> list, Item product,
                int index, boolean selected, boolean focused) {
            JPanel row = new JPanel(new BorderLayout(12, 0));
            row.setBackground(selected ? SELECTED : PAPER);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(selected ? new Color(110, 138, 107) : new Color(223, 212, 192)),
                    new EmptyBorder(10, 12, 10, 12)));
            JLabel icon = label("◇", 26);
            icon.setHorizontalAlignment(SwingConstants.CENTER);
            icon.setPreferredSize(new Dimension(36, 36));
            row.add(icon, BorderLayout.WEST);
            JLabel title = label(product.title() + (session.owns(product) ? " （購入済み）" : ""), 17);
            if (session.owns(product)) title.setForeground(Color.GRAY);
            row.add(title, BorderLayout.CENTER);
            JLabel price = label(String.format(Locale.JAPAN, "%,d PLN", product.price()), 15);
            price.setHorizontalAlignment(SwingConstants.RIGHT);
            row.add(price, BorderLayout.EAST);
            return row;
        }
    }

    /** 画像の縦横比を保ち、未登録でも同じ領域を確保する。 */
    private static final class MerchantPicture extends JPanel {
        /** 読み込み済みの静止画。未登録の場合はnull。 */
        private final BufferedImage image;

        MerchantPicture(String path) {
            setBackground(PAPER);
            URL resource = path == null ? null : ShopPanel.class.getResource(path);
            try {
                image = resource == null ? null : ImageIO.read(resource);
                if (resource != null && image == null) throw new IOException("画像形式を読み込めません: " + path);
            } catch (IOException error) {
                throw new UncheckedIOException("商人画像を読み込めません: " + path, error);
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (image == null) return;
            double scale = Math.min((double) getWidth() / image.getWidth(), (double) getHeight() / image.getHeight());
            int width = (int) (image.getWidth() * scale);
            int height = (int) (image.getHeight() * scale);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.drawImage(image, (getWidth() - width) / 2, (getHeight() - height) / 2, width, height, null);
            } finally {
                g.dispose();
            }
        }
    }
}
