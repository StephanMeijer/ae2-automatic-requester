package com.stephanmeijer.minecraft.ae2.autorequester;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = AE2Autorequester.MODID, bus = EventBusSubscriber.Bus.MOD)
public class AutorequesterConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue MAX_RULES_PER_BLOCK = BUILDER
            .comment("Maximum rules per autorequester block")
            .defineInRange("maxRulesPerBlock", 16, 1, 64);

    private static final ModConfigSpec.IntValue MAX_CONDITIONS_PER_RULE = BUILDER
            .comment("Maximum conditions per rule")
            .defineInRange("maxConditionsPerRule", 8, 1, 32);

    private static final ModConfigSpec.IntValue CHECK_INTERVAL = BUILDER
            .comment("Tick interval for checking conditions (20 = 1 second)")
            .defineInRange("checkInterval", 20, 1, 1200);

    private static final ModConfigSpec.IntValue MAX_BATCH_SIZE = BUILDER
            .comment("Maximum batch size allowed")
            .defineInRange("maxBatchSize", 10000, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.BooleanValue REQUIRES_CHANNEL = BUILDER
            .comment("Whether the autorequester requires a channel to operate")
            .define("requiresChannel", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    // Cached config values
    public static int maxRulesPerBlock;
    public static int maxConditionsPerRule;
    public static int checkInterval;
    public static int maxBatchSize;
    public static boolean requiresChannel;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        maxRulesPerBlock = MAX_RULES_PER_BLOCK.get();
        maxConditionsPerRule = MAX_CONDITIONS_PER_RULE.get();
        checkInterval = CHECK_INTERVAL.get();
        maxBatchSize = MAX_BATCH_SIZE.get();
        requiresChannel = REQUIRES_CHANNEL.get();
    }
}
