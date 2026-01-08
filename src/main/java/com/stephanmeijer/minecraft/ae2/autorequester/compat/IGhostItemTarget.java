package com.stephanmeijer.minecraft.ae2.autorequester.compat;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

/**
 * Interface for screens that support ghost item drag-drop from recipe viewers (JEI/EMI).
 * Implementing this interface allows both JEI and EMI to provide drop targets
 * for their ghost ingredient/drag-drop handlers.
 */
public interface IGhostItemTarget {

    /**
     * Get the bounds of the ghost item slot in screen coordinates.
     *
     * @return The bounds of the drop target, or null if no target is available
     */
    Rect2i getGhostItemSlotBounds();

    /**
     * Accept a ghost item dropped from a recipe viewer.
     *
     * @param stack The item stack to accept (will be a copy, can be modified)
     */
    void acceptGhostItem(ItemStack stack);
}
