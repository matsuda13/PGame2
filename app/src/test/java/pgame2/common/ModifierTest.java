package pgame2.common;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static pgame2.common.Modifier.*;

class ModifierTest {
    @Test
    void percentagesAreAddedAndExplicitMultipliersApplyLast() {
        assertEquals(23, floor(apply(BigDecimal.TEN, List.of(percent(100), percent(30)))));
        assertEquals(26, floor(apply(BigDecimal.TEN, List.of(percent(30), multiply(2)))));
        assertEquals(26, floor(apply(BigDecimal.TEN,
                List.of(multiply(2), fixed(2), percent(-20), percent(30)))));
    }

    @Test
    void fractionsSurviveEveryIntermediateStep() {
        BigDecimal result = apply(BigDecimal.valueOf(15), List.of(percent(30), fixed(1), multiply(2)));
        assertEquals(0, new BigDecimal("41").compareTo(result));
        assertEquals(80, floor(BigDecimal.valueOf(100).subtract(
                apply(BigDecimal.valueOf(15), List.of(percent(30))))));
        assertEquals(33, floor(apply(BigDecimal.valueOf(30), List.of(percent(10)))));
        assertEquals(-2, floor(new BigDecimal("-1.1")));
    }

    @Test
    void probabilityUsesPointsAndFinalBounds() {
        assertEquals(60, probability(apply(BigDecimal.valueOf(20), List.of(percent(50), points(30)))));
        assertEquals(0, probability(new BigDecimal("-0.1")));
        assertEquals(100, probability(new BigDecimal("150.8")));
        assertEquals(39, probability(new BigDecimal("39.9")));
    }

    @Test
    void descriptionsRetainTheMeaningOfEachOperation() {
        assertEquals("+30%", percent(30).describe());
        assertEquals("-20%", percent(-20).describe());
        assertEquals("+30pt", points(30).describe());
        assertEquals("+2", fixed(2).describe());
        assertEquals("×1.5", multiply("1.50").describe());
    }
}
