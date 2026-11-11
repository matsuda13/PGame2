package pgame2.item;

import java.util.EnumMap;
import java.util.Set;
import pgame2.common.ModifierContext;
import pgame2.effect.EffectTarget;
import pgame2.effect.GameEffect;
import pgame2.effect.GameEffect.Recovery;
import pgame2.effect.GameEffect.State;

/** セッションごとの所持情報を保持し、対応するタイミングの効果だけを呼ぶ。 */
public final class ItemEffectManager {
    /** 購入済み商品と、そのゲーム専用の効果インスタンス。 */
    private final EnumMap<Item, ItemEffect> owned = new EnumMap<>(Item.class);

    /** 支払い完了後に一度だけ登録する。 */
    public void acquire(Item item) {
        if (owned.containsKey(item)) throw new IllegalStateException("購入済みです");
        owned.put(item, ItemEffects.create(item));
    }
    public boolean owns(Item item) { return owned.containsKey(item); }
    public Set<Item> ownedItems() { return Set.copyOf(owned.keySet()); }

    /** 状態を変えずに補正値だけを収集する。 */
    public void collect(EffectTarget target, State state, ModifierContext context) {
        for (ItemEffect effect : owned.values()) {
            if (effect instanceof GameEffect.Numeric numeric) numeric.collect(target, state, context);
        }
    }
    /** 実際に反映した鍛錬を通知する。 */
    public void trainingCompleted(EffectTarget target, int day) {
        for (ItemEffect effect : owned.values()) {
            if (effect instanceof GameEffect.TrainingCompleted completed) completed.onTrainingCompleted(target, day);
        }
    }
    /** 成功した労働を通知する。 */
    public void workCompleted() {
        for (ItemEffect effect : owned.values()) {
            if (effect instanceof GameEffect.WorkCompleted completed) completed.onWorkCompleted();
        }
    }
    /** 休息完了時に反映する成長量を収集する。 */
    public Recovery restCompleted() {
        Recovery result = Recovery.NONE;
        for (ItemEffect effect : owned.values()) {
            if (effect instanceof GameEffect.RestCompleted completed) result = result.plus(completed.onRestCompleted());
        }
        return result;
    }
    /** 同一スナップショットで全条件を判定し、合計結果だけを返す。 */
    public Recovery dayEnd(State snapshot) {
        Recovery result = Recovery.NONE;
        for (ItemEffect effect : owned.values()) {
            if (effect instanceof GameEffect.DayEnd end) result = result.plus(end.onDayEnd(snapshot));
        }
        return result;
    }
    /** 実際の資源差分に基づく支出を通知する。 */
    public void plunSpent(long amount) {
        for (ItemEffect effect : owned.values()) {
            if (effect instanceof GameEffect.PlunSpent spent) spent.onPlunSpent(amount);
        }
    }
    /** 時間不足を許可する所持効果があるか調べる。 */
    public boolean allowsOvertime() {
        return owned.values().stream().anyMatch(effect -> effect instanceof GameEffect.ActionAvailability availability
                && availability.allowsOvertime());
    }
}
