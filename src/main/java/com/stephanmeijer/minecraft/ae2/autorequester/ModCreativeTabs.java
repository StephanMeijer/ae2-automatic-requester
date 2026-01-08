package com.stephanmeijer.minecraft.ae2.autorequester;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AE2Autorequester.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AE2_AUTOREQUESTER_TAB = CREATIVE_MODE_TABS.register(
            "ae2_autorequester_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + AE2Autorequester.MODID))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.AUTOREQUESTER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.AUTOREQUESTER.get());
                    })
                    .build()
    );
}
