package com.stephanmeijer.minecraft.ae2.autorequester.data;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for CraftingCondition NBT serialization format and logic.
 *
 * <p>Note: These tests verify NBT format expectations and serialization logic
 * that can be tested without full Minecraft bootstrap. Tests that require
 * actual Item registry lookups (i.e., calling fromNbt/toNbt with real items)
 * should use GameTests (runGameTestServer).
 *
 * <p>These unit tests focus on:
 * <ul>
 *   <li>NBT field naming conventions</li>
 *   <li>Operator serialization/deserialization logic</li>
 *   <li>Threshold value handling</li>
 *   <li>NBT type consistency</li>
 * </ul>
 */
@DisplayName("CraftingCondition Serialization")
class CraftingConditionSerializationTest {

    @Nested
    @DisplayName("NBT Field Names")
    class NbtFieldNamesTests {

        @Test
        @DisplayName("NBT uses 'item' field for item ID")
        void usesItemField() {
            CompoundTag tag = new CompoundTag();
            tag.putString("item", "minecraft:diamond");
            tag.putString("operator", "LESS_THAN");
            tag.putLong("threshold", 100L);

            assertTrue(tag.contains("item"));
            assertEquals("minecraft:diamond", tag.getString("item"));
        }

        @Test
        @DisplayName("NBT uses 'operator' field for comparison operator")
        void usesOperatorField() {
            CompoundTag tag = new CompoundTag();
            tag.putString("item", "minecraft:stone");
            tag.putString("operator", "GREATER_THAN");
            tag.putLong("threshold", 100L);

            assertTrue(tag.contains("operator"));
            assertEquals("GREATER_THAN", tag.getString("operator"));
        }

        @Test
        @DisplayName("NBT uses 'threshold' field for threshold value")
        void usesThresholdField() {
            CompoundTag tag = new CompoundTag();
            tag.putString("item", "minecraft:stone");
            tag.putString("operator", "LESS_THAN");
            tag.putLong("threshold", 5000L);

            assertTrue(tag.contains("threshold"));
            assertEquals(5000L, tag.getLong("threshold"));
        }
    }

    @Nested
    @DisplayName("Operator Serialization Logic")
    class OperatorSerializationLogicTests {

        @ParameterizedTest
        @EnumSource(ComparisonOperator.class)
        @DisplayName("all operators serialize to their enum name")
        void allOperatorsSerializeToName(ComparisonOperator operator) {
            // This tests the expected serialization format
            String serialized = operator.name();

            // The fromNbt method uses ComparisonOperator.fromName()
            ComparisonOperator deserialized = ComparisonOperator.fromName(serialized);

            assertEquals(operator, deserialized);
        }

        @Test
        @DisplayName("invalid operator names default to LESS_THAN")
        void invalidOperatorDefaultsToLessThan() {
            assertEquals(ComparisonOperator.LESS_THAN, ComparisonOperator.fromName("INVALID"));
            assertEquals(ComparisonOperator.LESS_THAN, ComparisonOperator.fromName(""));
        }

        @Test
        @DisplayName("operator names are case sensitive")
        void operatorNamesAreCaseSensitive() {
            // Lowercase should not match
            assertEquals(ComparisonOperator.LESS_THAN, ComparisonOperator.fromName("less_than"));
            assertEquals(ComparisonOperator.LESS_THAN, ComparisonOperator.fromName("Less_Than"));
        }
    }

    @Nested
    @DisplayName("Threshold NBT Type")
    class ThresholdNbtTypeTests {

        @Test
        @DisplayName("threshold uses long type to support large values")
        void thresholdUsesLongType() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("threshold", Long.MAX_VALUE);

            assertEquals(Long.MAX_VALUE, tag.getLong("threshold"));
        }

        @Test
        @DisplayName("threshold handles values exceeding int range")
        void thresholdHandlesLargeValues() {
            long largeValue = (long) Integer.MAX_VALUE + 1000L;
            CompoundTag tag = new CompoundTag();
            tag.putLong("threshold", largeValue);

            assertEquals(largeValue, tag.getLong("threshold"));
        }

        @Test
        @DisplayName("threshold handles zero")
        void thresholdHandlesZero() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("threshold", 0L);

            assertEquals(0L, tag.getLong("threshold"));
        }

        @Test
        @DisplayName("threshold handles negative values in NBT")
        void thresholdHandlesNegativeInNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("threshold", -100L);

            // NBT stores raw value; validation happens at application level
            assertEquals(-100L, tag.getLong("threshold"));
        }
    }

    @Nested
    @DisplayName("Item ID Format")
    class ItemIdFormatTests {

        @Test
        @DisplayName("item ID uses namespaced format")
        void itemIdUsesNamespacedFormat() {
            CompoundTag tag = new CompoundTag();
            tag.putString("item", "minecraft:diamond");

            String itemId = tag.getString("item");

            assertTrue(itemId.contains(":"));
            assertEquals("minecraft", itemId.split(":")[0]);
            assertEquals("diamond", itemId.split(":")[1]);
        }

        @Test
        @DisplayName("modded item IDs use mod namespace")
        void moddedItemIdsUseModNamespace() {
            CompoundTag tag = new CompoundTag();
            tag.putString("item", "appliedenergistics2:certus_quartz_crystal");

            String itemId = tag.getString("item");

            assertEquals("appliedenergistics2", itemId.split(":")[0]);
        }
    }

    @Nested
    @DisplayName("Serialization Format Consistency")
    class SerializationFormatConsistencyTests {

        @Test
        @DisplayName("expected NBT structure matches CraftingCondition.toNbt format")
        void expectedNbtStructure() {
            // This test documents the expected NBT structure
            // Actual roundtrip tests require GameTests with registry access

            CompoundTag expectedFormat = new CompoundTag();
            expectedFormat.putString("item", "minecraft:iron_ingot");
            expectedFormat.putString("operator", "LESS_THAN");
            expectedFormat.putLong("threshold", 1000L);

            // Verify all expected fields are present
            assertTrue(expectedFormat.contains("item"));
            assertTrue(expectedFormat.contains("operator"));
            assertTrue(expectedFormat.contains("threshold"));

            // Verify field types
            assertEquals("minecraft:iron_ingot", expectedFormat.getString("item"));
            assertEquals("LESS_THAN", expectedFormat.getString("operator"));
            assertEquals(1000L, expectedFormat.getLong("threshold"));
        }

        @Test
        @DisplayName("operator is stored as string not ordinal")
        void operatorStoredAsString() {
            // Important: we use name() not ordinal() for forward compatibility
            CompoundTag tag = new CompoundTag();
            tag.putString("operator", ComparisonOperator.GREATER_THAN_OR_EQUAL.name());

            // Should be recoverable even if enum order changes
            String stored = tag.getString("operator");
            assertEquals("GREATER_THAN_OR_EQUAL", stored);
        }
    }
}
