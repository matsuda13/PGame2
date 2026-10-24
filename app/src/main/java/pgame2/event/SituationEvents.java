package pgame2.event;

import java.util.Arrays;
import java.util.random.RandomGenerator;

/** 予告済みイベントと開催期間を管理する。 */
public class SituationEvents {
    /** 初回開始日の調整前設定。 */
    public static final int FIRST_START = 9;
    /** 開始間隔。 */
    public static final int PERIOD = 15;
    /** 開催日数。 */
    public static final int DURATION = 10;
    /** 予告から開始までの日数。 */
    public static final int NOTICE_DAYS = 8;
    /** 状況変化の種類。 */
    public enum Kind {
        HEAT("酷暑", "労働の体力消費 +30%"),
        BEASTS("魔獣活発化", "戦闘発生確率 +30pt"),
        BOOM("好景気", "労働賃金 +20%"),
        PROTEIN("プロテインの雨", "鍛錬の能力上昇・体力消費 +100%"),
        SLEEP("春眠暁を覚えず", "使用可能時間 -1時間・休息 +2時間・毎朝体力全快");
        /** 表示名。 */
        public final String title;
        /** 効果説明。 */
        public final String effect;
        Kind(String title, String effect) { this.title = title; this.effect = effect; }
    }
    /** 抽選元。 */
    private final RandomGenerator random;
    /** 開催中の種類。 */
    private Kind active;
    /** 次回予告済みの種類。 */
    private Kind upcoming;
    /** 直前に選出した種類。 */
    private Kind previous;
    /** 開催中イベントの開始日。 */
    private int activeStart;
    /** 次回の開始日。 */
    private int nextStart = FIRST_START;

    public SituationEvents(RandomGenerator random) { this.random = random; }

    /** 終了、開始、予告の順に日付を反映する。 */
    public String startDay(int day) {
        StringBuilder notice = new StringBuilder();
        if (active != null && day >= activeStart + DURATION) {
            notice.append(active.title).append("が終了しました。\n");
            active = null;
        }
        if (day == nextStart) {
            active = upcoming;
            upcoming = null;
            activeStart = day;
            nextStart += PERIOD;
            notice.append(active.title).append("が始まりました。\n");
        }
        if (day == nextStart - NOTICE_DAYS) {
            Kind[] candidates = Arrays.stream(Kind.values()).filter(k -> k != previous).toArray(Kind[]::new);
            upcoming = candidates[random.nextInt(candidates.length)];
            previous = upcoming;
            notice.append("イベント予告：").append(upcoming.title).append("（Day ").append(nextStart).append("開始）\n");
        }
        return notice.toString();
    }

    public Kind getActive() { return active; }
    public Kind getUpcoming() { return upcoming; }

    /** 開催と予告を同時に表示する。 */
    public String describe() {
        return "開催中：" + (active == null ? "なし" : active.title + "（Day " + (activeStart + DURATION - 1) + "まで）\n" + active.effect)
                + "\n次回：" + (upcoming == null ? "未予告" : upcoming.title + "（Day " + nextStart + "開始）\n" + upcoming.effect);
    }
}
