package pgame2.item;

/** 商品ID・価格・説明。列挙名を購入済み管理の安定した識別子とする。 */
public enum Item {
    TOOLBOX("職人の道具箱", "労働報酬 +10%", "毎日の仕事を助ける、使い込まれた道具の一式。", false),
    WORKWEAR("丈夫な作業着", "労働消費HP -10%", "丈夫な生地で仕立てられた、働く人の相棒。", false),
    BED("ふかふか布団", "休息回復HP +20%", "やわらかな綿に包まれて、ゆっくり休もう。", false),
    TEXTBOOK("熟練者の教本", "鍛錬の労働効率上昇量 +1", "先人の工夫が詰まった、鍛錬の手引き。", false),
    BOTTLE("ぷるぷる水筒", "日終了時にHP +5", "一日の終わりに、ほっとひと息。", false),
    PIGGY_BANK("貯金箱", "生活費 -10%", "小さな節約を毎日の習慣に。", false),
    PILLOW("成長する枕", "休息するたび最大HP・現在HP +1", "休むたびに少しずつ、丈夫な体に。", false),
    VETERAN_WORKWEAR("熟練の作業着", "労働するたび、以後の労働報酬 +1%", "積み重ねた仕事が、次の仕事の力になる。", true),
    GROWING_BANK("ぷるぷる貯金箱", "100プルン消費するたび、以後の労働報酬 +1%", "使ったプルンも、いつか暮らしの力に。", true),
    MEDAL("苦労人の勲章", "日終了時にHP半分以下なら最大HP・現在HP +1", "苦労を重ねた者に、小さな力を授ける。", true),
    CROWN("貯蓄の王冠", "所持プルン500以上で労働報酬 +20%", "蓄えのある暮らしが、自信につながる。", true),
    BRACELET("逆境の腕輪", "労働前に失っているHPの割合分、労働報酬増加", "苦しいときほど、もうひと踏ん張り。", true),
    HOURGLASS("金の砂時計", "残り時間が正なら時間不足でも行動可能", "最後のひと粒が落ちるまで、まだ働ける。", true);

    /** 商品名。 */
    private final String title;
    /** 効果説明。 */
    private final String effect;
    /** 世界観の補足説明。 */
    private final String description;
    /** 行商人限定商品か。 */
    private final boolean traveler;
    Item(String title, String effect, String description, boolean traveler) {
        this.title = title; this.effect = effect; this.description = description; this.traveler = traveler;
    }
    public String title() { return title; }
    public String effect() { return effect; }
    public String description() { return description; }
    public boolean traveler() { return traveler; }
    public int price() { return traveler ? 1000 : 500; }
}
