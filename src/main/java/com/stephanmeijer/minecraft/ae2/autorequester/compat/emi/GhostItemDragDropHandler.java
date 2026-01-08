package com.stephanmeijer.minecraft.ae2.autorequester.compat.emi;

import com.stephanmeijer.minecraft.ae2.autorequester.compat.IGhostItemTarget;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

/**
 * EMI drag-drop handler for screens that implement IGhostItemTarget.
 * This allows dragging items from EMI's item list onto ghost item slots.
 */
class GhostItemDragDropHandler implements EmiDragDropHandler<Screen> {

    @Override
    public boolean dropStack(Screen screen, EmiIngredient ingredient, int x, int y) {
        if (!(screen instanceof IGhostItemTarget target)) {
            return false;
        }

        Rect2i bounds = target.getGhostItemSlotBounds();
        if (bounds == null) {
            return false;
        }

        // Check if drop is within bounds
        if (!isWithinBounds(x, y, bounds)) {
            return false;
        }

        // Try to convert the ingredient to an ItemStack
        for (EmiStack emiStack : ingredient.getEmiStacks()) {
            ItemStack itemStack = emiStack.getItemStack();
            if (!itemStack.isEmpty()) {
                target.acceptGhostItem(itemStack.copy());
                return true;
            }
        }

        return false;
    }

    @Override
    public void render(Screen screen, EmiIngredient dragged, GuiGraphics draw, int mouseX, int mouseY, float delta) {
        if (!(screen instanceof IGhostItemTarget target)) {
            return;
        }

        Rect2i bounds = target.getGhostItemSlotBounds();
        if (bounds == null) {
            return;
        }

        // Check if the dragged ingredient contains any items
        boolean hasItems = dragged.getEmiStacks().stream()
                .anyMatch(stack -> !stack.getItemStack().isEmpty());

        if (!hasItems) {
            return;
        }

        // Highlight the drop target
        draw.fill(
                bounds.getX(),
                bounds.getY(),
                bounds.getX() + bounds.getWidth(),
                bounds.getY() + bounds.getHeight(),
                0x8822BB33 // Semi-transparent green highlight
        );
    }

    private boolean isWithinBounds(int x, int y, Rect2i bounds) {
        return x >= bounds.getX() && x < bounds.getX() + bounds.getWidth()
                && y >= bounds.getY() && y < bounds.getY() + bounds.getHeight();
    }
}
