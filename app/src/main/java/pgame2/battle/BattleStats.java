package pgame2.battle;

/** 通常HPとは独立した戦闘能力。素早さは毎秒のゲージ増加量。 */
public record BattleStats(int attack, int defense, int toughness, int speed) {
    /** 主人公と固定敵に使用する初期能力。 */
    public static final BattleStats INITIAL = new BattleStats(10, 10, 100, 50);

    /** 停滞する戦闘や不正な軽減計算を防ぐ。 */
    public BattleStats {
        if (attack < 0 || defense < 0 || toughness <= 0 || speed <= 0)
            throw new IllegalArgumentException("攻撃・防御は0以上、タフさ・素早さは1以上が必要です");
    }

    /** 適用直前に切り捨て、最低1ダメージを保証する。 */
    public int damageAgainst(BattleStats target) {
        return (int) Math.max(1, (long) attack * 100 / (100L + target.defense));
    }
}
