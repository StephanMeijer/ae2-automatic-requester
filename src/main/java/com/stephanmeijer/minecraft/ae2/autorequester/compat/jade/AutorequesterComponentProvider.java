package com.stephanmeijer.minecraft.ae2.autorequester.compat.jade;

import com.stephanmeijer.minecraft.ae2.autorequester.block.AutorequesterBlockEntity;
import com.stephanmeijer.minecraft.ae2.autorequester.data.RuleStatus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Provides Jade tooltip information for the Autorequester block.
 * Shows the number of configured rules and connection status.
 *
 * Uses server-side data provider to ensure accurate connection status.
 */
public enum AutorequesterComponentProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final String TAG_RULE_COUNT = "ruleCount";
    private static final String TAG_CONNECTED = "connected";
    private static final String TAG_HAS_MISSING_PATTERNS = "hasMissingPatterns";

    // ==================== Client-side tooltip rendering ====================

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();

        // Show connection status first (like AE2 devices)
        boolean connected = serverData.getBoolean(TAG_CONNECTED);
        if (connected) {
            tooltip.add(Component.translatable("ae2_autorequester.jade.online"));
        } else {
            tooltip.add(Component.translatable("ae2_autorequester.jade.offline")
                    .withStyle(style -> style.withColor(0xFF5555)));
        }

        // Show rule count in white for visibility
        int ruleCount = serverData.getInt(TAG_RULE_COUNT);
        tooltip.add(Component.translatable(
                "ae2_autorequester.jade.rules",
                ruleCount
        ).withStyle(style -> style.withColor(0xFFFFFF)));

        // Show missing patterns warning (yellow)
        if (serverData.getBoolean(TAG_HAS_MISSING_PATTERNS)) {
            tooltip.add(Component.translatable("ae2_autorequester.jade.missing_patterns")
                    .withStyle(style -> style.withColor(0xFFFF55)));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return JadePlugin.AUTOREQUESTER_INFO;
    }

    // ==================== Server-side data provider ====================

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof AutorequesterBlockEntity blockEntity) {
            data.putInt(TAG_RULE_COUNT, blockEntity.getRules().size());
            data.putBoolean(TAG_CONNECTED, blockEntity.isNetworkConnected());

            // Check if any enabled rules have missing patterns
            boolean hasMissingPatterns = blockEntity.getRules().stream()
                    .anyMatch(rule -> rule.isEnabled() && rule.getStatus() == RuleStatus.MISSING_PATTERN);
            data.putBoolean(TAG_HAS_MISSING_PATTERNS, hasMissingPatterns);
        }
    }
}
