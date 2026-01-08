package com.stephanmeijer.minecraft.ae2.autorequester.block;

import net.minecraft.util.StringRepresentable;

/**
 * Visual status of the Autorequester block, displayed via the status light.
 * This is a simplified version of RuleStatus for block state rendering.
 */
public enum BlockStatus implements StringRepresentable {
    OFF("off"),       // Not connected to ME network
    IDLE("idle"),     // Connected, but no rules active or conditions not met
    ACTIVE("active"), // Actively crafting or ready to craft
    WARNING("warning"), // Missing pattern or similar issue
    ERROR("error");   // No CPU available or other error

    private final String name;

    BlockStatus(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static BlockStatus fromName(String name) {
        for (BlockStatus status : values()) {
            if (status.name.equals(name)) {
                return status;
            }
        }
        return OFF;
    }
}
