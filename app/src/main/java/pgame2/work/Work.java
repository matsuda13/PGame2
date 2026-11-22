package pgame2.work;

import java.util.List;

/** 労働マスタ。補正前の値と素材の対応を保持する。 */
public record Work(String id, String name, String description, int hours, int hpCost,
                   int plunReward, int battleChance, int danger, String unlockCondition,
                   String iconPath, String workingImagePath, String resultImagePath) {
    /** 労働効率1点あたりの報酬増加率。 */
    public static final int EFFICIENCY_REWARD_PERCENT = 10;
    /** 初期解放済みの労働。 */
    public static final List<Work> CATALOG = List.of(
            initial("hauling", "荷役", "町に届いた荷物を運ぶ。", 4, 25, 30, 15, 1),
            initial("bridge", "橋作り", "川に架かる橋を組み立てる。", 6, 40, 60, 15, 2),
            initial("sewing", "針仕事", "針と糸で布を丁寧に縫う。", 2, 10, 15, 0, 0),
            initial("mining", "採掘", "岩を砕いて鉱石を掘り出す。", 7, 50, 130, 25, 3),
            initial("gathering", "採取", "野山で暮らしに役立つ素材を集める。", 5, 40, 100, 25, 2));

    /** 初期労働の素材パスは未登録とし、画面では仮表示する。 */
    private static Work initial(String id, String name, String description, int hours,
                                int hpCost, int reward, int chance, int danger) {
        return new Work(id, name, description, hours, hpCost, reward, chance, danger,
                "なし", "", "", "");
    }
}
