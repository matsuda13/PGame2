package pgame2.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/** 数値補正の種類と値。計算途中の端数を保持する。 */
public record Modifier(Type type, BigDecimal value) {
    /** 計算順序と表示単位を区別する種類。 */
    public enum Type { PERCENT, FIXED, POINTS, MULTIPLY }

    /** 不完全な補正定義を拒否する。 */
    public Modifier {
        Objects.requireNonNull(type);
        Objects.requireNonNull(value);
    }

    /** 基本値に対する割合補正を定義する。 */
    public static Modifier percent(int value) { return new Modifier(Type.PERCENT, BigDecimal.valueOf(value)); }
    /** 固定値の加減算を定義する。 */
    public static Modifier fixed(int value) { return new Modifier(Type.FIXED, BigDecimal.valueOf(value)); }
    /** 確率のポイント加減算を定義する。 */
    public static Modifier points(int value) { return new Modifier(Type.POINTS, BigDecimal.valueOf(value)); }
    /** 明示的な倍率を十進文字列で定義する。 */
    public static Modifier multiply(String value) { return new Modifier(Type.MULTIPLY, new BigDecimal(value)); }
    /** 整数倍率を定義する。 */
    public static Modifier multiply(int value) { return multiply(Integer.toString(value)); }

    /** 割合合算、固定値合算、倍率の順に適用し、丸めずに返す。 */
    public static BigDecimal apply(BigDecimal base, List<Modifier> modifiers) {
        BigDecimal percent = BigDecimal.ZERO;
        BigDecimal fixed = BigDecimal.ZERO;
        BigDecimal multiplier = BigDecimal.ONE;
        for (Modifier modifier : modifiers) {
            switch (modifier.type) {
                case PERCENT -> percent = percent.add(modifier.value);
                case FIXED, POINTS -> fixed = fixed.add(modifier.value);
                case MULTIPLY -> multiplier = multiplier.multiply(modifier.value);
            }
        }
        return base.multiply(BigDecimal.ONE.add(percent.movePointLeft(2))).add(fixed).multiply(multiplier);
    }

    /** 最終値だけを負数も含めて小数点以下切り捨てする。 */
    public static int floor(BigDecimal value) { return value.setScale(0, RoundingMode.FLOOR).intValueExact(); }

    /** 確率の最終値を切り捨て、0〜100に制限する。 */
    public static int probability(BigDecimal value) {
        return floor(value.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100)));
    }

    /** 計算に使う値から補正表記を生成する。 */
    public String describe() {
        String number = value.stripTrailingZeros().toPlainString();
        if (type == Type.MULTIPLY) return "×" + number;
        return (value.signum() >= 0 ? "+" : "") + number
                + switch (type) { case PERCENT -> "%"; case POINTS -> "pt"; default -> ""; };
    }
}
