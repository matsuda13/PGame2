package pgame2.effect;

import pgame2.common.ModifierContext;

/** イベントとアイテムに共通する効果の親型。 */
public interface GameEffect {
    /** 条件判定中に変更されないゲーム状態。 */
    record State(int hp, int maxHp, int plun, int day) { }
    /** 能力反映後に鍛錬対象と日付を通知する。 */
    interface TrainingCompleted extends GameEffect { void onTrainingCompleted(EffectTarget target, int day); }

    /** 同一状態で条件判定した後にまとめて反映する回復・成長量。 */
    record Recovery(int hp, int maxHp) {
        /** 回復・成長が発生しない結果。 */
        public static final Recovery NONE = new Recovery(0, 0);
        /** 複数効果の結果を合算する。 */
        public Recovery plus(Recovery other) { return new Recovery(hp + other.hp, maxHp + other.maxHp); }
    }

    /** 表示と実行で共用する、副作用のない数値補正収集。 */
    interface Numeric extends GameEffect {
        void collect(EffectTarget target, State state, ModifierContext context);
    }
    /** 朝の回復条件を返す。 */
    interface DayStart extends GameEffect { boolean restoresHp(); }
    /** 労働完了後の成長通知。 */
    interface WorkCompleted extends GameEffect { void onWorkCompleted(); }
    /** 休息完了後の回復・成長を返す。 */
    interface RestCompleted extends GameEffect { Recovery onRestCompleted(); }
    /** 実際に失ったプルンを通知する。 */
    interface PlunSpent extends GameEffect { void onPlunSpent(long amount); }
    /** 日終了時の同一スナップショットから回復・成長を計画する。 */
    interface DayEnd extends GameEffect { Recovery onDayEnd(State state); }
    /** 残り時間が正なら時間不足の行動を許可する。 */
    interface ActionAvailability extends GameEffect { boolean allowsOvertime(); }
}
