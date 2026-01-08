package com.stephanmeijer.minecraft.ae2.autorequester;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AE2Autorequester.MODID);

    // Autorequester Block Item
    public static final DeferredItem<BlockItem> AUTOREQUESTER = ITEMS.register(
            "autorequester",
            () -> new BlockItem(ModBlocks.AUTOREQUESTER.get(), new Item.Properties())
    );
}
