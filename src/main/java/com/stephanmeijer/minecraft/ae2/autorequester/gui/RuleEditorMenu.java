package com.stephanmeijer.minecraft.ae2.autorequester.gui;

import com.stephanmeijer.minecraft.ae2.autorequester.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Simple menu for the Rule Editor screen.
 * Contains a single hidden slot to satisfy EMI's screen detection requirements.
 * EMI only shows its panel for screens with non-empty slot lists.
 */
public final class RuleEditorMenu extends AbstractContainerMenu {

    // Dummy container for the hidden slot
    private final SimpleContainer dummyContainer = new SimpleContainer(1);

    // Client constructor (from network) - not actually used for network opening
    public RuleEditorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory);
    }

    // Direct constructor for client-side opening
    public RuleEditorMenu(int containerId, Inventory playerInventory) {
        super(ModMenus.RULE_EDITOR_MENU.get(), containerId);
        // Add a hidden slot off-screen so EMI recognizes this as a valid screen
        addSlot(new HiddenSlot(dummyContainer, 0, -9999, -9999));
    }

    /**
     * A slot that is always hidden and non-interactive.
     */
    private static class HiddenSlot extends Slot {
        public HiddenSlot(SimpleContainer container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
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
