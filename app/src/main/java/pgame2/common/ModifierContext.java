package pgame2.common;

import java.math.BigDecimal;
import java.math.MathContext;

/** 一回の計算に限って補正を蓄積し、収集完了後に基本値へ適用する。 */
public final class ModifierContext {
    /** 割合補正の合計（%）。 */
    private BigDecimal percent = BigDecimal.ZERO;
    /** HP割合のような循環小数を途中で丸めないための割合補正の分母。 */
    private BigDecimal percentDenominator = BigDecimal.ONE;
    /** 固定値・ポイント補正の合計。 */
    private BigDecimal fixed = BigDecimal.ZERO;
    /** 明示倍率の積。 */
    private BigDecimal multiplier = BigDecimal.ONE;

    /** 種類に応じて加算または乗算し、途中では丸めない。 */
    public void add(Modifier modifier) {
        switch (modifier.type()) {
            case PERCENT -> percent = percent.add(modifier.value().multiply(percentDenominator));
            case FIXED, POINTS -> fixed = fixed.add(modifier.value());
            case MULTIPLY -> multiplier = multiplier.multiply(modifier.value());
        }
    }

    /** 分数で表した割合（%）を、除算せずに既存の割合補正へ加算する。 */
    public void addPercentRatio(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.signum() <= 0) throw new IllegalArgumentException("割合の分母は正数が必要です");
        percent = percent.multiply(denominator).add(numerator.multiply(percentDenominator));
        percentDenominator = percentDenominator.multiply(denominator);
    }

    /** 割合、固定値、明示倍率の順で一度だけ計算する。 */
    public BigDecimal calculate(BigDecimal base) {
        BigDecimal denominator = percentDenominator.multiply(BigDecimal.valueOf(100));
        BigDecimal numerator = base.multiply(denominator.add(percent)).add(fixed.multiply(denominator)).multiply(multiplier);
        // 有限小数は厳密値を維持し、最終結果が循環小数の場合だけ十分な精度で返す。
        try {
            return numerator.divide(denominator);
        } catch (ArithmeticException nonTerminating) {
            return numerator.divide(denominator, MathContext.DECIMAL128);
        }
    }

    public BigDecimal getPercent() { return percent.divide(percentDenominator, MathContext.DECIMAL128); }
    public BigDecimal getFixed() { return fixed; }
    public BigDecimal getMultiplier() { return multiplier; }
}
