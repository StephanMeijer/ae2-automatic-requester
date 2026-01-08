package com.stephanmeijer.minecraft.ae2.autorequester.block;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.stephanmeijer.minecraft.ae2.autorequester.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

public class AutorequesterBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<BlockStatus> STATUS = EnumProperty.create("status", BlockStatus.class);
    public static final MapCodec<AutorequesterBlock> CODEC = simpleCodec(p -> new AutorequesterBlock());

    // Block properties matching AE2 machine blocks (easy to break)
    private static final float HARDNESS = 2.2F;
    private static final float RESISTANCE = 11.0F;

    public AutorequesterBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(HARDNESS, RESISTANCE));
        // No requiresCorrectToolForDrops - can be broken with any tool like AE2 machines
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(STATUS, BlockStatus.OFF));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STATUS);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(STATUS, BlockStatus.OFF);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AutorequesterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlocks.AUTOREQUESTER_BLOCK_ENTITY.get(),
                (lvl, pos, st, be) -> be.serverTick());
    }

    /**
     * Handle item interactions - specifically wrench to pick up the block.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hitResult) {
        // Check if player is using a wrench (shift + wrench = dismantle)
        if (player.isShiftKeyDown() && isWrench(stack)) {
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
                // Drop the block with NBT preserved
                ItemStack drop = new ItemStack(this);
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof AutorequesterBlockEntity autorequester) {
                    saveBlockEntityToItem(autorequester, drop);
                    autorequester.onRemoved();
                }

                // Remove the block
                level.removeBlock(pos, false);

                // Drop the item
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);

                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AutorequesterBlockEntity autorequester) {
                autorequester.openMenu(serverPlayer);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AutorequesterBlockEntity autorequester) {
                autorequester.onRemoved();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // ==================== Wrench Support ====================

    // Common wrench tag - mods should tag their wrenches with c:tools/wrench
    private static final TagKey<Item> WRENCH_TAG = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

    /**
     * Check if the item is a wrench by checking for the c:tools/wrench tag.
     * AE2's wrenches are tagged with this common tag.
     */
    private boolean isWrench(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.is(WRENCH_TAG);
    }

    // ==================== NBT Preservation ====================

    /**
     * Override getDrops to include block entity NBT data in the dropped item.
     * This ensures rules are preserved when the block is broken.
     */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof AutorequesterBlockEntity autorequester) {
            ItemStack stack = new ItemStack(this);
            saveBlockEntityToItem(autorequester, stack);
            return List.of(stack);
        }
        return super.getDrops(state, builder);
    }

    /**
     * Restore NBT data when the block is placed from an item with saved data.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof AutorequesterBlockEntity autorequester) {
                loadBlockEntityFromItem(autorequester, stack);
            }
        }
    }

    /**
     * Returns the item to pick when middle-clicking in creative mode.
     * Includes block entity NBT data.
     */
    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        ItemStack stack = new ItemStack(this);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AutorequesterBlockEntity autorequester) {
            saveBlockEntityToItem(autorequester, stack);
        }
        return stack;
    }

    /**
     * Save block entity data to an ItemStack using the BLOCK_ENTITY_DATA component.
     */
    private void saveBlockEntityToItem(AutorequesterBlockEntity autorequester, ItemStack stack) {
        CompoundTag tag = autorequester.saveWithoutMetadata(autorequester.getLevel().registryAccess());
        // Remove transient data that shouldn't be saved to item
        tag.remove("gridReady");
        // Only save if there's meaningful data (rules)
        if (tag.contains("rules")) {
            stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        }
    }

    /**
     * Load block entity data from an ItemStack's BLOCK_ENTITY_DATA component.
     */
    private void loadBlockEntityFromItem(AutorequesterBlockEntity autorequester, ItemStack stack) {
        CustomData customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            autorequester.loadAdditional(tag, autorequester.getLevel().registryAccess());
        }
    }
}
