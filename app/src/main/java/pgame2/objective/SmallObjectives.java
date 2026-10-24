package pgame2.objective;

import java.util.Arrays;
import java.util.random.RandomGenerator;

/** 小目標の予告と進捗、達成・期限判定を扱う。 */
public class SmallObjectives {
    /** 調整前の期限日数。 */
    public static final int DAYS = 15;
    /** 調整前の資産目標額。 */
    public static final int ASSET_TARGET = 150;
    /** 調整前の労働回数。 */
    public static final int WORK_TARGET = 8;
    /** 調整前の鍛錬回数。 */
    public static final int TRAIN_TARGET = 5;
    /** 調整前の共通報酬。 */
    public static final int REWARD = 60;
    /** 先行実装する4種類。 */
    public enum Kind { REACH, HOLD, WORK, GROW }
    /** 判定結果。 */
    public enum Result { NONE, SUCCESS, FAILURE }
    /** 抽選元。 */
    private final RandomGenerator random;
    /** 現在の目標。 */
    private Kind active;
    /** 直前の目標。 */
    private Kind previous;
    /** 期限日。 */
    private int deadline;
    /** 必要量。 */
    private int target;
    /** 期間内の労働回数。 */
    private int workCount;
    /** 期間内の鍛錬回数。 */
    private int trainCount;
    /** 最後の結果表示。 */
    private String lastResult = "小目標はまだありません";

    public SmallObjectives(RandomGenerator random) { this.random = random; }

    /** 空いている予告日に、直前と異なる目標を提示する。 */
    public boolean startDay(int day, int plun) {
        if (active != null || day % 10 != 5) return false;
        Kind[] candidates = Arrays.stream(Kind.values()).filter(k -> k != previous).toArray(Kind[]::new);
        active = candidates[random.nextInt(candidates.length)];
        previous = active;
        deadline = day + DAYS;
        target = switch (active) {
            case REACH -> plun + ASSET_TARGET;
            case HOLD -> ASSET_TARGET;
            case WORK -> WORK_TARGET;
            case GROW -> TRAIN_TARGET;
        };
        workCount = 0;
        trainCount = 0;
        return true;
    }

    /** 成功したコマンドの回数のみを積算する。 */
    public void recordAction(boolean work, boolean train) {
        if (active == null) return;
        if (work) workCount++;
        if (train) trainCount++;
    }

    /** 維持型は期限日の終了時だけ判定し、他は即時達成を許す。 */
    public Result evaluate(int day, int plun, boolean endOfDay) {
        if (active == null) return Result.NONE;
        boolean due = endOfDay && day >= deadline;
        boolean success = switch (active) {
            case REACH -> plun >= target;
            case HOLD -> due && plun >= target;
            case WORK -> workCount >= target;
            case GROW -> trainCount >= target;
        };
        if (!success && !due) return Result.NONE;
        active = null;
        lastResult = success ? "小目標達成！ 報酬 " + REWARD + "プルン" : "小目標の期限が終了しました。ペナルティはありません。";
        return success ? Result.SUCCESS : Result.FAILURE;
    }

    /** 条件、現在の進捗、期限、報酬をまとめる。 */
    public String describe(int plun) {
        if (active == null) return lastResult;
        String condition = switch (active) {
            case REACH -> "資産到達：" + plun + " / " + target + "プルン";
            case HOLD -> "資産維持：" + plun + " / " + target + "プルン（期限終了時）";
            case WORK -> "労働：" + workCount + " / " + target + "回";
            case GROW -> "鍛錬：" + trainCount + " / " + target + "回";
        };
        return condition + "\n期限：Day " + deadline + " 終了時／報酬：" + REWARD + "プルン";
    }

    public Kind getActive() { return active; }
    public int getDeadline() { return deadline; }
}
