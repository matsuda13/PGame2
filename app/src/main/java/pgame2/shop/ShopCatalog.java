package pgame2.shop;

import java.util.List;
import java.util.random.RandomGenerator;

/** 画面確認用の仮商品と、店舗・共通セリフの表示マスタ。 */
public final class ShopCatalog {
    /** 表示内容だけを切り替える店舗定義。行商人の出現判定は含まない。 */
    public enum Merchant {
        PERMANENT(1, "常設ショップ", "/pgame2/shop/permanent.png"),
        TRAVELER(2, "行商人", "/pgame2/shop/traveler.png");

        /** セリフ候補の絞り込みに使う商人ID。 */
        public final int id;
        /** ヘッダーに表示する店舗名。 */
        public final String title;
        /** クラスパス上の商人画像。 */
        public final String imagePath;

        Merchant(int id, String title, String imagePath) {
            this.id = id;
            this.title = title;
            this.imagePath = imagePath;
        }
    }

    /** 商品表示データ。効果は説明のみで、ゲーム状態には適用しない。 */
    public record Product(String name, String effect, String description, int price) { }

    /** 商人IDが0のセリフは全店舗で使用できる。 */
    private record Dialogue(int merchantId, String text) { }

    /** 画面ラフを参考にした仮商品。順序をそのまま商品一覧へ反映する。 */
    public static final List<Product> PREVIEW_PRODUCTS = List.of(
            new Product("職人の道具箱", "効果：未定", "使い込まれた道具が整然と収まった箱。毎日の仕事のおともに。", 500),
            new Product("丈夫な作業着", "効果：未定", "丈夫な生地で仕立てられた作業着。長時間の仕事にも寄り添ってくれそう。", 500),
            new Product("ふかふか布団", "効果：未定", "やわらかな綿を詰めた布団。くるまれば、穏やかな夢が見られそう。", 500),
            new Product("成長する杖", "効果：未定", "小さな芽をつけた不思議な杖。持ち主とともに育つと言われている。", 500));

    /** 初期実装では開店時に一度だけ抽選する共通セリフ。 */
    private static final List<Dialogue> DIALOGUES = List.of(
            new Dialogue(0, "いらっしゃい！ ゆっくり見ていってね。"),
            new Dialogue(0, "毎日の暮らしに寄り添う品を、集めているんだ。"),
            new Dialogue(0, "気になる品があったら、選んで眺めてみてね。"));

    private ShopCatalog() { }

    /** 共通と対象商人のセリフから一件選ぶ。 */
    public static String greeting(Merchant merchant, RandomGenerator random) {
        List<Dialogue> candidates = DIALOGUES.stream()
                .filter(line -> line.merchantId() == 0 || line.merchantId() == merchant.id).toList();
        return candidates.get(random.nextInt(candidates.size())).text();
    }
}
