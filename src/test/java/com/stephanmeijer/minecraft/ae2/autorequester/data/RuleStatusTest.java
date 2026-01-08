package com.stephanmeijer.minecraft.ae2.autorequester.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RuleStatus")
class RuleStatusTest {

    @Nested
    @DisplayName("isError")
    class IsErrorTests {
        @Test
        @DisplayName("MISSING_PATTERN, NO_CPU, and ERROR are errors")
        void errorStatuses() {
            assertTrue(RuleStatus.MISSING_PATTERN.isError());
            assertTrue(RuleStatus.NO_CPU.isError());
            assertTrue(RuleStatus.ERROR.isError());
        }

        @Test
        @DisplayName("other statuses are not errors")
        void nonErrorStatuses() {
            assertFalse(RuleStatus.IDLE.isError());
            assertFalse(RuleStatus.READY.isError());
            assertFalse(RuleStatus.CRAFTING.isError());
            assertFalse(RuleStatus.CONDITIONS_NOT_MET.isError());
        }
    }

    @Nested
    @DisplayName("isWarning")
    class IsWarningTests {
        @Test
        @DisplayName("only MISSING_PATTERN is a warning")
        void warningStatuses() {
            assertTrue(RuleStatus.MISSING_PATTERN.isWarning());
        }

        @Test
        @DisplayName("other statuses are not warnings")
        void nonWarningStatuses() {
            assertFalse(RuleStatus.IDLE.isWarning());
            assertFalse(RuleStatus.READY.isWarning());
            assertFalse(RuleStatus.CRAFTING.isWarning());
            assertFalse(RuleStatus.CONDITIONS_NOT_MET.isWarning());
            assertFalse(RuleStatus.NO_CPU.isWarning());
            assertFalse(RuleStatus.ERROR.isWarning());
        }
    }

    @Nested
    @DisplayName("isActive")
    class IsActiveTests {
        @Test
        @DisplayName("READY and CRAFTING are active")
        void activeStatuses() {
            assertTrue(RuleStatus.READY.isActive());
            assertTrue(RuleStatus.CRAFTING.isActive());
        }

        @Test
        @DisplayName("other statuses are not active")
        void inactiveStatuses() {
            assertFalse(RuleStatus.IDLE.isActive());
            assertFalse(RuleStatus.CONDITIONS_NOT_MET.isActive());
            assertFalse(RuleStatus.MISSING_PATTERN.isActive());
            assertFalse(RuleStatus.NO_CPU.isActive());
            assertFalse(RuleStatus.ERROR.isActive());
        }
    }

    @Nested
    @DisplayName("fromName")
    class FromNameTests {
        @ParameterizedTest
        @EnumSource(RuleStatus.class)
        @DisplayName("returns correct status for valid names")
        void validNames(RuleStatus status) {
            assertEquals(status, RuleStatus.fromName(status.name()));
        }

        @Test
        @DisplayName("returns IDLE for invalid names")
        void invalidNames() {
            assertEquals(RuleStatus.IDLE, RuleStatus.fromName("INVALID"));
            assertEquals(RuleStatus.IDLE, RuleStatus.fromName(""));
            assertEquals(RuleStatus.IDLE, RuleStatus.fromName(null));
        }
    }

    @Nested
    @DisplayName("getColor")
    class GetColorTests {
        @Test
        @DisplayName("active statuses are green")
        void activeGreen() {
            assertEquals(0x00FF00, RuleStatus.READY.getColor());
            assertEquals(0x00FF00, RuleStatus.CRAFTING.getColor());
        }

        @Test
        @DisplayName("warning status is yellow")
        void warningYellow() {
            assertEquals(0xFFFF00, RuleStatus.MISSING_PATTERN.getColor());
        }

        @Test
        @DisplayName("error statuses are red")
        void errorRed() {
            assertEquals(0xFF0000, RuleStatus.NO_CPU.getColor());
            assertEquals(0xFF0000, RuleStatus.ERROR.getColor());
        }

        @Test
        @DisplayName("idle statuses are gray")
        void idleGray() {
            assertEquals(0x808080, RuleStatus.IDLE.getColor());
            assertEquals(0x808080, RuleStatus.CONDITIONS_NOT_MET.getColor());
        }
    }

    @Nested
    @DisplayName("Status light colors match REQUIREMENTS.md")
    class RequirementsComplianceTests {
        @Test
        @DisplayName("Green: Active (conditions met, crafting or ready)")
        void greenForActive() {
            // From REQUIREMENTS.md: "Green: Active (conditions met, crafting or ready)"
            assertTrue(RuleStatus.READY.isActive());
            assertTrue(RuleStatus.CRAFTING.isActive());
            assertEquals(0x00FF00, RuleStatus.READY.getColor());
            assertEquals(0x00FF00, RuleStatus.CRAFTING.getColor());
        }

        @Test
        @DisplayName("Yellow: Warning (missing pattern, persistent failure)")
        void yellowForWarning() {
            // From REQUIREMENTS.md: "Yellow: Warning (missing pattern, persistent failure)"
            assertTrue(RuleStatus.MISSING_PATTERN.isWarning());
            assertEquals(0xFFFF00, RuleStatus.MISSING_PATTERN.getColor());
        }

        @Test
        @DisplayName("Red: Error (no CPU available)")
        void redForError() {
            // From REQUIREMENTS.md: "Red: Error (no CPU available)"
            assertTrue(RuleStatus.NO_CPU.isError());
            assertEquals(0xFF0000, RuleStatus.NO_CPU.getColor());
        }

        @Test
        @DisplayName("Off/Gray: Idle (no rules enabled or conditions not met)")
        void grayForIdle() {
            // From REQUIREMENTS.md: "Off: Idle (no rules enabled or conditions not met)"
            assertFalse(RuleStatus.IDLE.isActive());
            assertFalse(RuleStatus.CONDITIONS_NOT_MET.isActive());
            assertEquals(0x808080, RuleStatus.IDLE.getColor());
            assertEquals(0x808080, RuleStatus.CONDITIONS_NOT_MET.getColor());
        }
    }
}
