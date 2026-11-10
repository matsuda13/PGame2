package pgame2.battle;

import java.util.ArrayList;
import java.util.List;

/** 描画から独立した、到達時刻順のオートバトル。 */
public final class AutoBattle {
    /** 戦闘中に固定する両者の能力。 */
    private final BattleStats player, enemy;
    /** 戦闘ごとに全回復する専用HP。 */
    private int playerHp, enemyHp;
    /** 次の攻撃番号。累積加算による時刻誤差を避ける。 */
    private long playerTurn = 1, enemyTurn = 1;
    /** 最後に処理した経過秒数。終了時には決着時刻となる。 */
    private double elapsed;

    /** 一回の攻撃結果。damageは防御適用後の計算ダメージ。 */
    public record Attack(double seconds, boolean playerAttacks, int damage) { }

    /** 通常HPに依存せず戦闘HPを初期化する。 */
    public AutoBattle(BattleStats player, BattleStats enemy) {
        this.player = java.util.Objects.requireNonNull(player);
        this.enemy = java.util.Objects.requireNonNull(enemy);
        playerHp = player.toughness();
        enemyHp = enemy.toughness();
    }

    /** 戦闘開始から指定秒数まで、未処理の攻撃を時刻順に実行する。 */
    public List<Attack> advanceTo(double seconds) {
        if (!Double.isFinite(seconds) || seconds < elapsed)
            throw new IllegalArgumentException("経過時間は有限で逆行しない値が必要です");
        List<Attack> attacks = new ArrayList<>();
        if (isFinished()) return attacks;
        while (!isFinished()) {
            // 分数を整数で比較し、同時刻は主人公を優先する。
            boolean playerAttacks = playerTurn * enemy.speed() <= enemyTurn * player.speed();
            double at = playerAttacks ? playerTurn * 100.0 / player.speed()
                    : enemyTurn * 100.0 / enemy.speed();
            if (at > seconds) break;
            int damage = playerAttacks ? player.damageAgainst(enemy) : enemy.damageAgainst(player);
            if (playerAttacks) {
                enemyHp = Math.max(0, enemyHp - damage);
                playerTurn++;
            } else {
                playerHp = Math.max(0, playerHp - damage);
                enemyTurn++;
            }
            elapsed = at;
            attacks.add(new Attack(at, playerAttacks, damage));
        }
        if (!isFinished()) elapsed = seconds;
        return attacks;
    }

    /** 処理済み攻撃数を引いた、表示用の余剰ゲージ。 */
    public double gauge(boolean forPlayer) {
        BattleStats stats = forPlayer ? player : enemy;
        long turn = forPlayer ? playerTurn : enemyTurn;
        return Math.max(0, Math.min(100, elapsed * stats.speed() - (turn - 1) * 100.0));
    }

    public BattleStats player() { return player; }
    public BattleStats enemy() { return enemy; }
    public int playerHp() { return playerHp; }
    public int enemyHp() { return enemyHp; }
    public double elapsed() { return elapsed; }
    public boolean isFinished() { return playerHp == 0 || enemyHp == 0; }
    public boolean playerWon() { return isFinished() && enemyHp == 0; }
}
