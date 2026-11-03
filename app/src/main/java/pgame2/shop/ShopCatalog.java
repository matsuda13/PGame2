package pgame2.shop;

import java.util.List;
import java.util.random.RandomGenerator;

/** 店舗・共通セリフの表示マスタ。 */
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

    /** 商人IDが0のセリフは全店舗で使用できる。 */
    private record Dialogue(int merchantId, String text) { }

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
