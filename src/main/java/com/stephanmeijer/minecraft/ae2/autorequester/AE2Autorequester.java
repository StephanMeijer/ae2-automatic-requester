package com.stephanmeijer.minecraft.ae2.autorequester;

import appeng.api.AECapabilities;
import com.mojang.logging.LogUtils;
import com.stephanmeijer.minecraft.ae2.autorequester.block.AutorequesterBlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.slf4j.Logger;

@Mod(AE2Autorequester.MODID)
public class AE2Autorequester {
    public static final String MODID = "ae2_autorequester";
    private static final Logger LOGGER = LogUtils.getLogger();

    public AE2Autorequester(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("AE2 Autorequester initializing...");

        // Register deferred registers
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.BLOCK_ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);

        // Register config
        modContainer.registerConfig(ModConfig.Type.SERVER, AutorequesterConfig.SPEC);

        // Register event handlers
        modEventBus.addListener(this::registerCapabilities);

        LOGGER.info("AE2 Autorequester initialized");
    }

    /**
     * Register AE2 capabilities for our block entities.
     */
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Register the IInWorldGridNodeHost capability for our block entity
        // This allows AE2 cables to connect to our block
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                ModBlocks.AUTOREQUESTER_BLOCK_ENTITY.get(),
                (blockEntity, direction) -> blockEntity
        );
    }
}