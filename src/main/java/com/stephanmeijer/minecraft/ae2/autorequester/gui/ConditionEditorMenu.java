package com.stephanmeijer.minecraft.ae2.autorequester.gui;

import com.stephanmeijer.minecraft.ae2.autorequester.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Simple menu for the Condition Editor screen.
 * This menu has no slots - it exists only to make this a ContainerScreen so JEI shows.
 */
public class ConditionEditorMenu extends AbstractContainerMenu {

    // Client constructor (from network) - not actually used for network opening
    public ConditionEditorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory);
    }

    // Direct constructor for client-side opening
    public ConditionEditorMenu(int containerId, Inventory playerInventory) {
        super(ModMenus.CONDITION_EDITOR_MENU.get(), containerId);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
