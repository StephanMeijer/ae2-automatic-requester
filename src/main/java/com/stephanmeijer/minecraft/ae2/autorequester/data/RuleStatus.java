package com.stephanmeijer.minecraft.ae2.autorequester.data;

import net.minecraft.network.chat.Component;

/**
 * Status of a crafting rule.
 */
public enum RuleStatus {
    IDLE("idle", 0x808080),           // Gray - no rules enabled or conditions not met
    READY("ready", 0x00FF00),          // Green - conditions met, ready to craft
    CRAFTING("crafting", 0x00FF00),    // Green - crafting job in progress
    CONDITIONS_NOT_MET("conditions_not_met", 0x808080), // Gray - conditions not satisfied
    MISSING_PATTERN("missing_pattern", 0xFFFF00),  // Yellow - no pattern found
    NO_CPU("no_cpu", 0xFF0000),        // Red - no crafting CPU available
    ERROR("error", 0xFF0000);          // Red - general error

    private final String translationKey;
    private final int color;

    RuleStatus(String translationKey, int color) {
        this.translationKey = translationKey;
        this.color = color;
    }

    public Component getDisplayName() {
        return Component.translatable("ae2_autorequester.status." + translationKey);
    }

    public int getColor() {
        return color;
    }

    public boolean isError() {
        return this == MISSING_PATTERN || this == NO_CPU || this == ERROR;
    }

    public boolean isWarning() {
        return this == MISSING_PATTERN;
    }

    public boolean isActive() {
        return this == READY || this == CRAFTING;
    }

    /**
     * Deserialize from enum name string.
     * Safer than ordinal because it won't break if enum is reordered.
     */
    public static RuleStatus fromName(String name) {
        if (name == null) {
            return IDLE;
        }
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return IDLE;
        }
    }
}
