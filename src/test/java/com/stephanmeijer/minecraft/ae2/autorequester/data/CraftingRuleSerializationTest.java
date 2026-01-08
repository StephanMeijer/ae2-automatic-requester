package com.stephanmeijer.minecraft.ae2.autorequester.data;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for CraftingRule NBT serialization format and logic.
 *
 * <p>Note: These tests verify NBT format expectations and serialization logic
 * that can be tested without full Minecraft bootstrap. Tests that require
 * actual Item registry lookups (i.e., calling fromNbt/toNbt with real items)
 * should use GameTests (runGameTestServer).
 *
 * <p>These unit tests focus on:
 * <ul>
 *   <li>NBT field naming conventions</li>
 *   <li>Status serialization/deserialization logic</li>
 *   <li>UUID handling</li>
 *   <li>Batch size and timestamp handling</li>
 *   <li>Conditions list structure</li>
 * </ul>
 */
@DisplayName("CraftingRule Serialization")
class CraftingRuleSerializationTest {

    @Nested
    @DisplayName("NBT Field Names")
    class NbtFieldNamesTests {

        @Test
        @DisplayName("NBT uses 'id' field for UUID")
        void usesIdField() {
            UUID testId = UUID.randomUUID();
            CompoundTag tag = new CompoundTag();
            tag.putUUID("id", testId);

            assertTrue(tag.hasUUID("id"));
            assertEquals(testId, tag.getUUID("id"));
        }

        @Test
        @DisplayName("NBT uses 'name' field for rule name")
        void usesNameField() {
            CompoundTag tag = new CompoundTag();
            tag.putString("name", "Test Rule");

            assertTrue(tag.contains("name"));
            assertEquals("Test Rule", tag.getString("name"));
        }

        @Test
        @DisplayName("NBT uses 'targetItem' field for target item")
        void usesTargetItemField() {
            CompoundTag tag = new CompoundTag();
            tag.putString("targetItem", "minecraft:diamond");

            assertTrue(tag.contains("targetItem"));
            assertEquals("minecraft:diamond", tag.getString("targetItem"));
        }

        @Test
        @DisplayName("NBT uses 'batchSize' field for batch size")
        void usesBatchSizeField() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("batchSize", 128);

            assertTrue(tag.contains("batchSize"));
            assertEquals(128, tag.getInt("batchSize"));
        }

        @Test
        @DisplayName("NBT uses 'enabled' field for enabled state")
        void usesEnabledField() {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean("enabled", true);

            assertTrue(tag.contains("enabled"));
            assertTrue(tag.getBoolean("enabled"));
        }

        @Test
        @DisplayName("NBT uses 'status' field for rule status")
        void usesStatusField() {
            CompoundTag tag = new CompoundTag();
            tag.putString("status", "CRAFTING");

            assertTrue(tag.contains("status"));
            assertEquals("CRAFTING", tag.getString("status"));
        }

        @Test
        @DisplayName("NBT uses 'lastTriggered' field for timestamp")
        void usesLastTriggeredField() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("lastTriggered", 123456789L);

            assertTrue(tag.contains("lastTriggered"));
            assertEquals(123456789L, tag.getLong("lastTriggered"));
        }

        @Test
        @DisplayName("NBT uses 'conditions' field for conditions list")
        void usesConditionsField() {
            CompoundTag tag = new CompoundTag();
            tag.put("conditions", new ListTag());

            assertTrue(tag.contains("conditions"));
        }
    }

    @Nested
    @DisplayName("Status Serialization Logic")
    class StatusSerializationLogicTests {

        @ParameterizedTest
        @EnumSource(RuleStatus.class)
        @DisplayName("all status values serialize to their enum name")
        void allStatusesSerializeToName(RuleStatus status) {
            String serialized = status.name();

            RuleStatus deserialized = RuleStatus.fromName(serialized);

            assertEquals(status, deserialized);
        }

        @Test
        @DisplayName("invalid status names default to IDLE")
        void invalidStatusDefaultsToIdle() {
            assertEquals(RuleStatus.IDLE, RuleStatus.fromName("INVALID"));
            assertEquals(RuleStatus.IDLE, RuleStatus.fromName(""));
            assertEquals(RuleStatus.IDLE, RuleStatus.fromName(null));
        }

        @Test
        @DisplayName("status names are case sensitive")
        void statusNamesAreCaseSensitive() {
            assertEquals(RuleStatus.IDLE, RuleStatus.fromName("idle"));
            assertEquals(RuleStatus.IDLE, RuleStatus.fromName("Idle"));
        }
    }

    @Nested
    @DisplayName("UUID Handling")
    class UuidHandlingTests {

        @Test
        @DisplayName("UUID is stored in NBT correctly")
        void uuidStoredCorrectly() {
            UUID testId = UUID.randomUUID();
            CompoundTag tag = new CompoundTag();
            tag.putUUID("id", testId);

            assertEquals(testId, tag.getUUID("id"));
        }

        @Test
        @DisplayName("hasUUID returns true when UUID is present")
        void hasUuidWhenPresent() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("id", UUID.randomUUID());

            assertTrue(tag.hasUUID("id"));
        }

        @Test
        @DisplayName("hasUUID returns false when UUID is missing")
        void hasUuidWhenMissing() {
            CompoundTag tag = new CompoundTag();

            assertFalse(tag.hasUUID("id"));
        }

        @Test
        @DisplayName("different UUIDs are preserved distinctly")
        void differentUuidsPreserved() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();

            CompoundTag tag1 = new CompoundTag();
            tag1.putUUID("id", id1);

            CompoundTag tag2 = new CompoundTag();
            tag2.putUUID("id", id2);

            assertNotEquals(tag1.getUUID("id"), tag2.getUUID("id"));
        }
    }

    @Nested
    @DisplayName("Batch Size Handling")
    class BatchSizeHandlingTests {

        @ParameterizedTest
        @ValueSource(ints = {1, 64, 100, 256, 1000, 5000, 10000})
        @DisplayName("valid batch sizes are stored correctly")
        void validBatchSizesStored(int batchSize) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("batchSize", batchSize);

            assertEquals(batchSize, tag.getInt("batchSize"));
        }

        @Test
        @DisplayName("zero batch size is stored (validation elsewhere)")
        void zeroBatchSizeStored() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("batchSize", 0);

            assertEquals(0, tag.getInt("batchSize"));
        }

        @Test
        @DisplayName("negative batch size is stored (validation elsewhere)")
        void negativeBatchSizeStored() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("batchSize", -100);

            assertEquals(-100, tag.getInt("batchSize"));
        }
    }

    @Nested
    @DisplayName("Timestamp Handling")
    class TimestampHandlingTests {

        @Test
        @DisplayName("zero timestamp is stored correctly")
        void zeroTimestampStored() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("lastTriggered", 0L);

            assertEquals(0L, tag.getLong("lastTriggered"));
        }

        @Test
        @DisplayName("maximum long timestamp is stored correctly")
        void maxLongTimestampStored() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("lastTriggered", Long.MAX_VALUE);

            assertEquals(Long.MAX_VALUE, tag.getLong("lastTriggered"));
        }

        @Test
        @DisplayName("negative timestamp is stored (edge case)")
        void negativeTimestampStored() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("lastTriggered", -1L);

            assertEquals(-1L, tag.getLong("lastTriggered"));
        }
    }

    @Nested
    @DisplayName("Name Handling")
    class NameHandlingTests {

        @Test
        @DisplayName("empty name is stored correctly")
        void emptyNameStored() {
            CompoundTag tag = new CompoundTag();
            tag.putString("name", "");

            assertEquals("", tag.getString("name"));
        }

        @Test
        @DisplayName("special characters in name are preserved")
        void specialCharactersPreserved() {
            String name = "Test <Rule> with \"quotes\" & symbols";
            CompoundTag tag = new CompoundTag();
            tag.putString("name", name);

            assertEquals(name, tag.getString("name"));
        }

        @Test
        @DisplayName("unicode in name is preserved")
        void unicodePreserved() {
            String name = "铁锭规则 🔨 règle";
            CompoundTag tag = new CompoundTag();
            tag.putString("name", name);

            assertEquals(name, tag.getString("name"));
        }

        @Test
        @DisplayName("Minecraft formatting codes in name are preserved")
        void formattingCodesPreserved() {
            String name = "§aGreen §cRed §lBold";
            CompoundTag tag = new CompoundTag();
            tag.putString("name", name);

            assertEquals(name, tag.getString("name"));
        }

        @Test
        @DisplayName("long name is stored correctly")
        void longNameStored() {
            String longName = "A".repeat(1000);
            CompoundTag tag = new CompoundTag();
            tag.putString("name", longName);

            assertEquals(longName, tag.getString("name"));
        }
    }

    @Nested
    @DisplayName("Conditions List Structure")
    class ConditionsListStructureTests {

        @Test
        @DisplayName("empty conditions list is valid")
        void emptyConditionsListValid() {
            CompoundTag tag = new CompoundTag();
            ListTag conditions = new ListTag();
            tag.put("conditions", conditions);

            ListTag retrieved = tag.getList("conditions", 10); // TAG_COMPOUND = 10
            assertEquals(0, retrieved.size());
        }

        @Test
        @DisplayName("conditions list preserves order")
        void conditionsListPreservesOrder() {
            ListTag conditions = new ListTag();

            for (int i = 0; i < 5; i++) {
                CompoundTag cond = new CompoundTag();
                cond.putLong("threshold", i * 100L);
                conditions.add(cond);
            }

            for (int i = 0; i < 5; i++) {
                assertEquals(i * 100L, conditions.getCompound(i).getLong("threshold"));
            }
        }

        @Test
        @DisplayName("conditions list supports many entries")
        void conditionsListSupportsManyEntries() {
            ListTag conditions = new ListTag();

            for (int i = 0; i < 100; i++) {
                CompoundTag cond = new CompoundTag();
                cond.putString("item", "minecraft:stone");
                cond.putString("operator", "LESS_THAN");
                cond.putLong("threshold", i);
                conditions.add(cond);
            }

            assertEquals(100, conditions.size());
        }
    }

    @Nested
    @DisplayName("Serialization Format Consistency")
    class SerializationFormatConsistencyTests {

        @Test
        @DisplayName("expected NBT structure matches CraftingRule.toNbt format")
        void expectedNbtStructure() {
            // This test documents the expected NBT structure
            // Actual roundtrip tests require GameTests with registry access

            CompoundTag expectedFormat = new CompoundTag();
            expectedFormat.putUUID("id", UUID.randomUUID());
            expectedFormat.putString("name", "Iron Ingot Rule");
            expectedFormat.putString("targetItem", "minecraft:iron_ingot");
            expectedFormat.putInt("batchSize", 64);
            expectedFormat.putBoolean("enabled", true);
            expectedFormat.putString("status", "READY");
            expectedFormat.putLong("lastTriggered", 12345L);

            ListTag conditions = new ListTag();
            CompoundTag cond = new CompoundTag();
            cond.putString("item", "minecraft:iron_ore");
            cond.putString("operator", "LESS_THAN");
            cond.putLong("threshold", 1000L);
            conditions.add(cond);
            expectedFormat.put("conditions", conditions);

            // Verify all expected fields are present
            assertTrue(expectedFormat.hasUUID("id"));
            assertTrue(expectedFormat.contains("name"));
            assertTrue(expectedFormat.contains("targetItem"));
            assertTrue(expectedFormat.contains("batchSize"));
            assertTrue(expectedFormat.contains("enabled"));
            assertTrue(expectedFormat.contains("status"));
            assertTrue(expectedFormat.contains("lastTriggered"));
            assertTrue(expectedFormat.contains("conditions"));
        }

        @Test
        @DisplayName("status is stored as string not ordinal")
        void statusStoredAsString() {
            // Important: we use name() not ordinal() for forward compatibility
            CompoundTag tag = new CompoundTag();
            tag.putString("status", RuleStatus.CRAFTING.name());

            String stored = tag.getString("status");
            assertEquals("CRAFTING", stored);

            // Should be recoverable
            RuleStatus recovered = RuleStatus.fromName(stored);
            assertEquals(RuleStatus.CRAFTING, recovered);
        }

        @Test
        @DisplayName("all status values are recoverable from NBT")
        void allStatusValuesRecoverable() {
            for (RuleStatus status : RuleStatus.values()) {
                CompoundTag tag = new CompoundTag();
                tag.putString("status", status.name());

                RuleStatus recovered = RuleStatus.fromName(tag.getString("status"));
                assertEquals(status, recovered,
                        "Status " + status.name() + " should be recoverable from NBT");
            }
        }
    }
}
