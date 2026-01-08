package com.stephanmeijer.minecraft.ae2.autorequester.gui;

import java.util.List;

import com.stephanmeijer.minecraft.ae2.autorequester.AutorequesterConfig;
import com.stephanmeijer.minecraft.ae2.autorequester.ModBlocks;
import com.stephanmeijer.minecraft.ae2.autorequester.ModMenus;
import com.stephanmeijer.minecraft.ae2.autorequester.block.AutorequesterBlockEntity;
import com.stephanmeijer.minecraft.ae2.autorequester.data.CraftingRule;
import com.stephanmeijer.minecraft.ae2.autorequester.network.SyncRulesPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutorequesterMenu extends AbstractContainerMenu {
    private static final Logger LOG = LoggerFactory.getLogger(AutorequesterMenu.class);

    private final AutorequesterBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final boolean isClientSide;

    // Client constructor (from network)
    public AutorequesterMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, extraData));
    }

    // Server constructor
    public AutorequesterMenu(int containerId, Inventory playerInventory, AutorequesterBlockEntity blockEntity) {
        super(ModMenus.AUTOREQUESTER_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.isClientSide = blockEntity.getLevel() != null && blockEntity.getLevel().isClientSide();

        // No inventory slots - this is a configuration GUI
        // All interaction happens through screen widgets
    }

    /**
     * Sends all rules to the server for persistence.
     * Should be called after any rule modification on the client.
     */
    public void syncRulesToServer() {
        if (isClientSide) {
            List<CraftingRule> rules = blockEntity.getRules();
            LOG.info("[Menu] syncRulesToServer - sending {} rules", rules.size());
            for (int i = 0; i < rules.size(); i++) {
                CraftingRule r = rules.get(i);
                LOG.info("[Menu]   Rule[{}]: id={}, name='{}', target={}, batchSize={}, conditions={}",
                        i, r.getId(), r.getName(),
                        r.getTargetItem() != null ? r.getTargetItem().toString() : "null",
                        r.getBatchSize(), r.getConditions().size());
            }
            PacketDistributor.sendToServer(new SyncRulesPacket(blockEntity.getBlockPos(), rules));
        }
    }

    private static AutorequesterBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        var pos = extraData.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof AutorequesterBlockEntity autorequester) {
            return autorequester;
        }
        throw new IllegalStateException("Block entity is not an AutorequesterBlockEntity at " + pos);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        // No slots to quick-move
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.AUTOREQUESTER.get());
    }

    public AutorequesterBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public List<CraftingRule> getRules() {
        return blockEntity.getRules();
    }

    public boolean isNetworkConnected() {
        return blockEntity.isNetworkConnected();
    }

    // Rule operations - these modify locally and sync to server
    public boolean canAddRule() {
        return AutorequesterConfig.validRuleCount(blockEntity.getRules().size());
    }

    public void addRule() {
        if (!canAddRule()) {
            return;
        }
        blockEntity.addRule(new CraftingRule());
        syncRulesToServer();
    }

    public void removeRule(int index) {
        blockEntity.removeRule(index);
        syncRulesToServer();
    }

    public void duplicateRule(int index) {
        blockEntity.duplicateRule(index);
        syncRulesToServer();
    }

    public void moveRuleUp(int index) {
        blockEntity.moveRuleUp(index);
        syncRulesToServer();
    }

    public void moveRuleDown(int index) {
        blockEntity.moveRuleDown(index);
        syncRulesToServer();
    }

    public void updateRule(CraftingRule rule) {
        blockEntity.updateRule(rule);
        syncRulesToServer();
    }
}
