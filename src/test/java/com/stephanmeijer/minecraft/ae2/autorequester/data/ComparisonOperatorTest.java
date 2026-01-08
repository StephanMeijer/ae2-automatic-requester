package com.stephanmeijer.minecraft.ae2.autorequester.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ComparisonOperator")
class ComparisonOperatorTest {

    @Nested
    @DisplayName("LESS_THAN (<)")
    class LessThanTests {
        @ParameterizedTest(name = "{0} < {1} = {2}")
        @CsvSource({
            "0, 100, true",
            "99, 100, true",
            "100, 100, false",
            "101, 100, false",
            "0, 0, false",
            "-1, 0, true"
        })
        void evaluate(long itemCount, long threshold, boolean expected) {
            assertEquals(expected, ComparisonOperator.LESS_THAN.evaluate(itemCount, threshold));
        }
    }

    @Nested
    @DisplayName("LESS_THAN_OR_EQUAL (<=)")
    class LessThanOrEqualTests {
        @ParameterizedTest(name = "{0} <= {1} = {2}")
        @CsvSource({
            "0, 100, true",
            "99, 100, true",
            "100, 100, true",
            "101, 100, false",
            "0, 0, true"
        })
        void evaluate(long itemCount, long threshold, boolean expected) {
            assertEquals(expected, ComparisonOperator.LESS_THAN_OR_EQUAL.evaluate(itemCount, threshold));
        }
    }

    @Nested
    @DisplayName("GREATER_THAN (>)")
    class GreaterThanTests {
        @ParameterizedTest(name = "{0} > {1} = {2}")
        @CsvSource({
            "0, 100, false",
            "99, 100, false",
            "100, 100, false",
            "101, 100, true",
            "1000, 100, true",
            "0, 0, false"
        })
        void evaluate(long itemCount, long threshold, boolean expected) {
            assertEquals(expected, ComparisonOperator.GREATER_THAN.evaluate(itemCount, threshold));
        }
    }

    @Nested
    @DisplayName("GREATER_THAN_OR_EQUAL (>=)")
    class GreaterThanOrEqualTests {
        @ParameterizedTest(name = "{0} >= {1} = {2}")
        @CsvSource({
            "0, 100, false",
            "99, 100, false",
            "100, 100, true",
            "101, 100, true",
            "0, 0, true"
        })
        void evaluate(long itemCount, long threshold, boolean expected) {
            assertEquals(expected, ComparisonOperator.GREATER_THAN_OR_EQUAL.evaluate(itemCount, threshold));
        }
    }

    @Nested
    @DisplayName("EQUAL (=)")
    class EqualTests {
        @ParameterizedTest(name = "{0} = {1} = {2}")
        @CsvSource({
            "0, 100, false",
            "99, 100, false",
            "100, 100, true",
            "101, 100, false",
            "0, 0, true"
        })
        void evaluate(long itemCount, long threshold, boolean expected) {
            assertEquals(expected, ComparisonOperator.EQUAL.evaluate(itemCount, threshold));
        }
    }

    @Nested
    @DisplayName("NOT_EQUAL (!=)")
    class NotEqualTests {
        @ParameterizedTest(name = "{0} != {1} = {2}")
        @CsvSource({
            "0, 100, true",
            "99, 100, true",
            "100, 100, false",
            "101, 100, true",
            "0, 0, false"
        })
        void evaluate(long itemCount, long threshold, boolean expected) {
            assertEquals(expected, ComparisonOperator.NOT_EQUAL.evaluate(itemCount, threshold));
        }
    }

    @Nested
    @DisplayName("fromOrdinal")
    class FromOrdinalTests {
        @Test
        @DisplayName("returns correct operator for valid ordinals")
        void validOrdinals() {
            assertEquals(ComparisonOperator.LESS_THAN, ComparisonOperator.fromOrdinal(0));
            assertEquals(ComparisonOperator.LESS_THAN_OR_EQUAL, ComparisonOperator.fromOrdinal(1));
            assertEquals(ComparisonOperator.GREATER_THAN, ComparisonOperator.fromOrdinal(2));
            assertEquals(ComparisonOperator.GREATER_THAN_OR_EQUAL, ComparisonOperator.fromOrdinal(3));
            assertEquals(ComparisonOperator.EQUAL, ComparisonOperator.fromOrdinal(4));
            assertEquals(ComparisonOperator.NOT_EQUAL, ComparisonOperator.fromOrdinal(5));
        }

        @Test
        @DisplayName("returns LESS_THAN for invalid ordinals")
        void invalidOrdinals() {
            assertEquals(ComparisonOperator.LESS_THAN, ComparisonOperator.fromOrdinal(-1));
            assertEquals(ComparisonOperator.LESS_THAN, ComparisonOperator.fromOrdinal(100));
        }
    }

    @Nested
    @DisplayName("getSymbol")
    class GetSymbolTests {
        @Test
        @DisplayName("returns correct symbols")
        void symbols() {
            assertEquals("<", ComparisonOperator.LESS_THAN.getSymbol());
            assertEquals("<=", ComparisonOperator.LESS_THAN_OR_EQUAL.getSymbol());
            assertEquals(">", ComparisonOperator.GREATER_THAN.getSymbol());
            assertEquals(">=", ComparisonOperator.GREATER_THAN_OR_EQUAL.getSymbol());
            assertEquals("=", ComparisonOperator.EQUAL.getSymbol());
            assertEquals("!=", ComparisonOperator.NOT_EQUAL.getSymbol());
        }
    }

    @Nested
    @DisplayName("Edge cases from REQUIREMENTS.md examples")
    class RequirementsExamplesTests {
        @Test
        @DisplayName("Example 1: Iron Ingot < 1000 triggers when stock is low")
        void maintainMinimumStock() {
            // Iron Ingot < 1000 should trigger when we have less than 1000
            assertTrue(ComparisonOperator.LESS_THAN.evaluate(500, 1000));
            assertTrue(ComparisonOperator.LESS_THAN.evaluate(999, 1000));
            assertFalse(ComparisonOperator.LESS_THAN.evaluate(1000, 1000));
            assertFalse(ComparisonOperator.LESS_THAN.evaluate(2000, 1000));
        }

        @Test
        @DisplayName("Example 2: Glass < 3000 AND Sand >= 1000")
        void craftWithResourceLimit() {
            // Glass < 3000: should craft when glass is low
            assertTrue(ComparisonOperator.LESS_THAN.evaluate(2999, 3000));
            assertFalse(ComparisonOperator.LESS_THAN.evaluate(3000, 3000));

            // Sand >= 1000: should only craft when sand is abundant
            assertTrue(ComparisonOperator.GREATER_THAN_OR_EQUAL.evaluate(1000, 1000));
            assertTrue(ComparisonOperator.GREATER_THAN_OR_EQUAL.evaluate(5000, 1000));
            assertFalse(ComparisonOperator.GREATER_THAN_OR_EQUAL.evaluate(999, 1000));
        }

        @Test
        @DisplayName("Example 4: Cobblestone > 10000 for overflow crafting")
        void overflowCrafting() {
            // Cobblestone > 10000: convert excess
            assertFalse(ComparisonOperator.GREATER_THAN.evaluate(10000, 10000));
            assertTrue(ComparisonOperator.GREATER_THAN.evaluate(10001, 10000));
            assertTrue(ComparisonOperator.GREATER_THAN.evaluate(50000, 10000));
        }
    }
}
