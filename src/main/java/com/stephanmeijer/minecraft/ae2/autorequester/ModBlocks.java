package com.stephanmeijer.minecraft.ae2.autorequester;

import com.stephanmeijer.minecraft.ae2.autorequester.block.AutorequesterBlock;
import com.stephanmeijer.minecraft.ae2.autorequester.block.AutorequesterBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AE2Autorequester.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AE2Autorequester.MODID);

    // Autorequester Block
    public static final DeferredBlock<AutorequesterBlock> AUTOREQUESTER = BLOCKS.register(
            "autorequester",
            AutorequesterBlock::new
    );

    // Autorequester Block Entity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutorequesterBlockEntity>> AUTOREQUESTER_BLOCK_ENTITY = BLOCK_ENTITIES.register(
            "autorequester",
            () -> BlockEntityType.Builder.of(AutorequesterBlockEntity::new, AUTOREQUESTER.get()).build(null)
    );
}