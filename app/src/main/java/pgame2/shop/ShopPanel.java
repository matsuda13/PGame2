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
import pgame2.shop.ShopCatalog.Product;

/** 商人画像・商品一覧・詳細欄を固定配置するショップの画面。 */
public final class ShopPanel extends JPanel {
    /** 紙をイメージした背景色。 */
    private static final Color PAPER = new Color(248, 242, 225);
    /** インクをイメージした文字色。 */
    private static final Color INK = new Color(65, 54, 45);
    /** 商品選択時の淡い背景色。 */
    private static final Color SELECTED = new Color(218, 230, 216);
    /** 単一選択の商品一覧。 */
    private final JList<Product> products;
    /** 固定領域内で折り返す商品詳細。 */
    private final JTextArea description = textArea("商品を選択してください", 16);
    /** 機能設計が確定するまで無効にしておく購入ボタン。 */
    private final JButton purchase = new JButton("購入");

    /** 店舗ごとの差し替えデータを受け取り、ゲーム状態を変更せず画面を構築する。 */
    public ShopPanel(Merchant merchant, List<Product> catalog, int plun, Runnable close) {
        setLayout(new BorderLayout(18, 18));
        setBackground(PAPER);
        setBorder(new EmptyBorder(20, 22, 22, 22));
        setPreferredSize(new Dimension(1060, 650));

        JPanel header = panel(new BorderLayout(16, 0));
        header.add(label(merchant.title, 26), BorderLayout.WEST);
        JPanel controls = panel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        controls.add(label(String.format(Locale.JAPAN, "所持プルン  %,d PLN", plun), 18));
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
        listing.add(label("商品一覧（仮）", 20), BorderLayout.NORTH);
        products = new JList<>(catalog.toArray(Product[]::new));
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
        footer.add(label("商品は仮表示です。購入は準備中です。", 13), BorderLayout.CENTER);
        purchase.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
        purchase.setEnabled(false);
        purchase.setToolTipText("購入機能は準備中です");
        footer.add(purchase, BorderLayout.EAST);
        details.add(footer, BorderLayout.SOUTH);
        goods.add(details, BorderLayout.SOUTH);
        body.add(goods, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    /** メイン画面を親とする固定サイズのモーダル画面を開く。 */
    public static void open(Component parent, Merchant merchant, int plun) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), merchant.title,
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(new ShopPanel(merchant, ShopCatalog.PREVIEW_PRODUCTS, plun, dialog::dispose));
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

    /** 選択だけを詳細に反映し、購入や資源変更は行わない。 */
    private void showSelection() {
        Product selected = products.getSelectedValue();
        description.setText(selected == null ? "商品を選択してください"
                : selected.name() + "\n\n" + selected.effect() + "\n\n" + selected.description());
        description.setCaretPosition(0);
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
    private static final class ProductRenderer implements ListCellRenderer<Product> {
        @Override
        public Component getListCellRendererComponent(JList<? extends Product> list, Product product,
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
            row.add(label(product.name(), 17), BorderLayout.CENTER);
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
