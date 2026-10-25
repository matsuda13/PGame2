package pgame2.event;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.util.stream.Collectors;
import pgame2.common.Modifier;
import static pgame2.common.Modifier.*;
import static pgame2.event.EventModifier.modifier;
import static pgame2.event.EventModifier.Target.*;
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
        HEAT("酷暑", "", modifier(WORK_HP_COST, percent(30))),
        BEASTS("魔獣活発化", "", modifier(BATTLE_CHANCE, points(30))),
        BOOM("好景気", "", modifier(WORK_PLUN_REWARD, percent(20))),
        PROTEIN("プロテインの雨", "", modifier(TRAIN_HP_COST, percent(100)),
                modifier(TRAIN_WORK_EFFICIENCY_GAIN, percent(100))),
        SLEEP("春眠暁を覚えず", "毎朝体力全快", modifier(DAILY_HOURS, fixed(-1)),
                modifier(REST_HOURS_COST, fixed(2)));
        /** 表示名。 */
        public final String title;
        /** 効果説明。 */
        public final String effect;
        /** 計算と説明文の共通定義。 */
        private final List<EventModifier> modifiers;

        Kind(String title, String specialEffect, EventModifier... modifiers) {
            this.title = title;
            this.modifiers = List.of(modifiers);
            String numeric = this.modifiers.stream().map(EventModifier::describe).collect(Collectors.joining("・"));
            this.effect = numeric + (specialEffect.isEmpty() ? "" : "・" + specialEffect);
        }

        /** 指定対象の補正を合算し、端数を保持した結果を返す。 */
        public BigDecimal apply(EventModifier.Target target, int base, Modifier... additional) {
            List<Modifier> combined = new ArrayList<>(Arrays.asList(additional));
            modifiers.stream().filter(m -> m.target() == target)
                    .map(EventModifier::modifier).forEach(combined::add);
            return Modifier.apply(BigDecimal.valueOf(base), combined);
        }
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

    /** 開催中の補正と追加補正を合算し、計算途中の端数を保持する。 */
    public BigDecimal apply(EventModifier.Target target, int base, Modifier... additional) {
        return active == null ? Modifier.apply(BigDecimal.valueOf(base), Arrays.asList(additional))
                : active.apply(target, base, additional);
    }

    /** 開催と予告を同時に表示する。 */
    public String describe() {
        return "開催中：" + (active == null ? "なし" : active.title + "（Day " + (activeStart + DURATION - 1) + "まで）\n" + active.effect)
                + "\n次回：" + (upcoming == null ? "未予告" : upcoming.title + "（Day " + nextStart + "開始）\n" + upcoming.effect);
    }
}
