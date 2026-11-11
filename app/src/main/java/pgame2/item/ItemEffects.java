package pgame2.item;

import java.math.BigDecimal;
import pgame2.common.Modifier;
import pgame2.common.ModifierContext;
import pgame2.effect.EffectTarget;
import pgame2.effect.GameEffect;
import static pgame2.effect.EffectTarget.*;

/** 単純補正は共通化し、条件付き・累積効果は専用処理へ分離する。 */
final class ItemEffects {
    private ItemEffects() { }

    /** 購入ごとに新しい効果を作り、ゲーム間で累積状態を共有しない。 */
    static ItemEffect create(Item item) {
        return switch (item) {
            case TOOLBOX -> new Bonus(WORK_PLUN_REWARD, Modifier.percent(10));
            case WORKWEAR -> new Bonus(WORK_HP_COST, Modifier.percent(-10));
            case BED -> new Bonus(REST_HP_RECOVERY, Modifier.percent(20));
            case TEXTBOOK -> new Textbook();
            case PIGGY_BANK -> new Bonus(LIVING_COST, Modifier.percent(-10));
            case BOTTLE -> new Bottle();
            case PILLOW -> new Pillow();
            case VETERAN_WORKWEAR -> new VeteranWorkwear();
            case GROWING_BANK -> new GrowingBank();
            case MEDAL -> new Medal();
            case CROWN -> new Crown();
            case BRACELET -> new Bracelet();
            case HOURGLASS -> new Hourglass();
        };
    }

    /** 当日最初の労働効率の鍛錬だけに加算する。 */
    private static final class Textbook implements ItemEffect, GameEffect.Numeric, GameEffect.TrainingCompleted {
        /** 最後に効果を適用した日。 */
        private int lastUsedDay = -1;
        public void collect(EffectTarget target, State state, ModifierContext context) {
            if (target == TRAIN_WORK_EFFICIENCY_GAIN && lastUsedDay != state.day()) context.add(Modifier.fixed(1));
        }
        public void onTrainingCompleted(EffectTarget target, int day) {
            if (target == TRAIN_WORK_EFFICIENCY_GAIN) lastUsedDay = day;
        }
    }
    /** 無条件の補正値を追加する共通効果。 */
    private record Bonus(EffectTarget target, Modifier modifier) implements ItemEffect, GameEffect.Numeric {
        public void collect(EffectTarget requested, State state, ModifierContext context) {
            if (requested == target) context.add(modifier);
        }
    }
    /** 一日の疲れを回復する。 */
    private static final class Bottle implements ItemEffect, GameEffect.DayEnd {
        public Recovery onDayEnd(State state) { return new Recovery(5, 0); }
    }
    /** 休息完了後に最大HPを成長させる。 */
    private static final class Pillow implements ItemEffect, GameEffect.RestCompleted {
        public Recovery onRestCompleted() { return new Recovery(0, 1); }
    }
    /** 低HPの判定には日終了時の共通スナップショットを使う。 */
    private static final class Medal implements ItemEffect, GameEffect.DayEnd {
        public Recovery onDayEnd(State state) {
            return (long) state.hp() * 2 <= state.maxHp() ? new Recovery(0, 1) : Recovery.NONE;
        }
    }
    /** 蓄えがある場合だけ報酬補正を加える。 */
    private static final class Crown implements ItemEffect, GameEffect.Numeric {
        public void collect(EffectTarget target, State state, ModifierContext context) {
            if (target == WORK_PLUN_REWARD && state.plun() >= 500) context.add(Modifier.percent(20));
        }
    }
    /** 労働前に失っているHPの割合を報酬へ加える。 */
    private static final class Bracelet implements ItemEffect, GameEffect.Numeric {
        public void collect(EffectTarget target, State state, ModifierContext context) {
            if (target == WORK_PLUN_REWARD) {
                context.addPercentRatio(BigDecimal.valueOf((long) (state.maxHp() - state.hp()) * 100),
                        BigDecimal.valueOf(state.maxHp()));
            }
        }
    }
    /** 完了した労働だけを数え、次回以降の報酬を増やす。 */
    private static final class VeteranWorkwear implements ItemEffect, GameEffect.Numeric, GameEffect.WorkCompleted {
        /** 購入後に完了した労働回数。 */
        private long count;
        public void onWorkCompleted() { count++; }
        public void collect(EffectTarget target, State state, ModifierContext context) {
            if (target == WORK_PLUN_REWARD) context.add(new Modifier(Modifier.Type.PERCENT, BigDecimal.valueOf(count)));
        }
    }
    /** 購入後の実支出を累積し、100未満の余りも保持する。 */
    private static final class GrowingBank implements ItemEffect, GameEffect.Numeric, GameEffect.PlunSpent {
        /** 購入後の累積支出額。 */
        private long spent;
        public void onPlunSpent(long amount) { spent = Math.addExact(spent, amount); }
        public void collect(EffectTarget target, State state, ModifierContext context) {
            if (target == WORK_PLUN_REWARD) context.add(new Modifier(Modifier.Type.PERCENT, BigDecimal.valueOf(spent / 100)));
        }
    }
    /** 時間不足のみを緩和し、死亡や資金不足は緩和しない。 */
    private static final class Hourglass implements ItemEffect, GameEffect.ActionAvailability {
        public boolean allowsOvertime() { return true; }
    }
}
