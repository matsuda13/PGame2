package pgame2.common;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModifierContextTest {
    /** 収集順に依存せず、割合・固定値は加算し、倍率は乗算する。 */
    @Test
    void aggregatesUntilFinalCalculationAndNewContextStartsEmpty() {
        ModifierContext context = new ModifierContext();
        context.add(Modifier.multiply(2));
        context.add(Modifier.fixed(3));
        context.add(Modifier.percent(30));
        context.add(Modifier.fixed(-1));
        context.add(Modifier.percent(-20));
        context.add(Modifier.multiply("1.5"));
        assertEquals(0, new BigDecimal("39").compareTo(context.calculate(BigDecimal.TEN)));
        assertEquals(0, BigDecimal.TEN.compareTo(new ModifierContext().calculate(BigDecimal.TEN)));
        assertEquals(0, new BigDecimal("39").compareTo(context.calculate(BigDecimal.TEN)));
    }

    /** 2/3のHP損失も途中で丸めず、整数に戻る最終報酬を正確に保つ。 */
    @Test
    void fractionalPercentDoesNotLoseAnIntegerAtRoundingBoundary() {
        ModifierContext context = new ModifierContext();
        context.addPercentRatio(BigDecimal.valueOf(200), BigDecimal.valueOf(3));
        assertEquals(50, Modifier.floor(context.calculate(BigDecimal.valueOf(30))));
        context.add(Modifier.percent(10));
        assertEquals(53, Modifier.floor(context.calculate(BigDecimal.valueOf(30))));
    }
}
