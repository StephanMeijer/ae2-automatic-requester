package com.stephanmeijer.minecraft.ae2.autorequester;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration for the Autorequester mod.
 * Values are read directly from the spec to support runtime changes.
 * For limit configurations (maxBatchSize, maxRules, maxConditions),
 * a value of -1 means unlimited.
 */
public class AutorequesterConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue CHECK_INTERVAL = BUILDER
            .comment("Tick interval for checking conditions (20 = 1 second)")
            .defineInRange("checkInterval", 20, 1, 1200);

    private static final ModConfigSpec.IntValue MAX_BATCH_SIZE = BUILDER
            .comment("Maximum batch size allowed per crafting request (-1 = unlimited)")
            .defineInRange("maxBatchSize", -1, -1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue MAX_RULES = BUILDER
            .comment("Maximum number of rules per autorequester block (-1 = unlimited)")
            .defineInRange("maxRules", -1, -1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue MAX_CONDITIONS = BUILDER
            .comment("Maximum number of conditions per rule (-1 = unlimited)")
            .defineInRange("maxConditions", -1, -1, Integer.MAX_VALUE);

    private static final ModConfigSpec.BooleanValue REQUIRES_CHANNEL = BUILDER
            .comment("Whether the autorequester requires a channel to operate")
            .define("requiresChannel", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    // Getters that read directly from config (supports runtime changes)
    public static int getCheckInterval() {
        return CHECK_INTERVAL.get();
    }

    public static int getMaxBatchSize() {
        return MAX_BATCH_SIZE.get();
    }

    public static int getMaxRules() {
        return MAX_RULES.get();
    }

    public static int getMaxConditions() {
        return MAX_CONDITIONS.get();
    }

    public static boolean requiresChannel() {
        return REQUIRES_CHANNEL.get();
    }

    /**
     * Check if a batch size is within the configured limit.
     * @param batchSize the batch size to check
     * @return true if within limit or if limit is unlimited (-1)
     */
    public static boolean validBatchSize(int batchSize) {
        int max = getMaxBatchSize();
        return max == -1 || batchSize <= max;
    }

    /**
     * Check if adding another rule would exceed the configured limit.
     * @param currentRuleCount the current number of rules
     * @return true if another rule can be added
     */
    public static boolean validRuleCount(int currentRuleCount) {
        int max = getMaxRules();
        return max == -1 || currentRuleCount < max;
    }

    /**
     * Check if adding another condition would exceed the configured limit.
     * @param currentConditionCount the current number of conditions
     * @return true if another condition can be added
     */
    public static boolean validConditionCount(int currentConditionCount) {
        int max = getMaxConditions();
        return max == -1 || currentConditionCount < max;
    }

    // Legacy static fields for compatibility (updated on config load)
    public static int checkInterval = 20;
    public static int maxBatchSize = -1;
    public static int maxRules = -1;
    public static int maxConditions = -1;
    public static boolean requiresChannel = true;

    /**
     * Called to refresh cached values. Config changes via file edit
     * are picked up automatically through the getter methods.
     */
    public static void refresh() {
        checkInterval = CHECK_INTERVAL.get();
        maxBatchSize = MAX_BATCH_SIZE.get();
        maxRules = MAX_RULES.get();
        maxConditions = MAX_CONDITIONS.get();
        requiresChannel = REQUIRES_CHANNEL.get();
    }
}
