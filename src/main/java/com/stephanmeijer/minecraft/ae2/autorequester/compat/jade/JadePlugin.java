package com.stephanmeijer.minecraft.ae2.autorequester.compat.jade;

import com.stephanmeijer.minecraft.ae2.autorequester.AE2Autorequester;
import com.stephanmeijer.minecraft.ae2.autorequester.block.AutorequesterBlock;
import com.stephanmeijer.minecraft.ae2.autorequester.block.AutorequesterBlockEntity;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade/WAILA plugin for showing Autorequester block information.
 * Shows rule count and connection status when hovering over the block.
 */
@WailaPlugin
public class JadePlugin implements IWailaPlugin {

    public static final ResourceLocation AUTOREQUESTER_INFO =
            ResourceLocation.fromNamespaceAndPath(AE2Autorequester.MODID, "autorequester_info");

    @Override
    public void register(IWailaCommonRegistration registration) {
        // Register server data provider for accurate connection status
        registration.registerBlockDataProvider(AutorequesterComponentProvider.INSTANCE, AutorequesterBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // Register client-side tooltip component provider
        registration.registerBlockComponent(AutorequesterComponentProvider.INSTANCE, AutorequesterBlock.class);
    }
}
