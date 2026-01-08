package com.stephanmeijer.minecraft.ae2.autorequester.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.stephanmeijer.minecraft.ae2.autorequester.AE2Autorequester;
import com.stephanmeijer.minecraft.ae2.autorequester.compat.IGhostItemTarget;
import com.stephanmeijer.minecraft.ae2.autorequester.gui.AutorequesterScreen;
import com.stephanmeijer.minecraft.ae2.autorequester.gui.ConditionEditorScreen;
import com.stephanmeijer.minecraft.ae2.autorequester.gui.RuleEditorScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI plugin for ghost ingredient support in Autorequester screens.
 * This allows dragging items from JEI into item slots in the editor screens.
 */
@mezz.jei.api.JeiPlugin
public class JeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID = ResourceLocation.fromNamespaceAndPath(AE2Autorequester.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // Register ghost ingredient handlers for screens implementing IGhostItemTarget
        registration.addGhostIngredientHandler(AutorequesterScreen.class, new EmptyGhostIngredientHandler<>());
        registration.addGhostIngredientHandler(RuleEditorScreen.class, new GhostItemTargetHandler<>());
        registration.addGhostIngredientHandler(ConditionEditorScreen.class, new GhostItemTargetHandler<>());
    }

    /**
     * Empty handler for screens with no ghost ingredient targets.
     */
    private static class EmptyGhostIngredientHandler<T extends Screen> implements IGhostIngredientHandler<T> {
        @Override
        public <I> List<Target<I>> getTargetsTyped(T screen, ITypedIngredient<I> ingredient, boolean doStart) {
            return List.of();
        }

        @Override
        public void onComplete() {
        }
    }

    /**
     * Generic ghost ingredient handler for screens implementing IGhostItemTarget.
     * Uses the shared interface to provide drop targets.
     */
    private static class GhostItemTargetHandler<T extends Screen & IGhostItemTarget> implements IGhostIngredientHandler<T> {

        @Override
        public <I> List<Target<I>> getTargetsTyped(T screen, ITypedIngredient<I> ingredient, boolean doStart) {
            List<Target<I>> targets = new ArrayList<>();

            if (ingredient.getItemStack().isEmpty()) {
                return targets;
            }

            Rect2i bounds = screen.getGhostItemSlotBounds();
            if (bounds != null) {
                targets.add(new ItemSlotTarget<>(bounds, screen));
            }

            return targets;
        }

        @Override
        public void onComplete() {
        }
    }

    /**
     * Target for an item slot that accepts ghost ingredients via IGhostItemTarget.
     */
    private static class ItemSlotTarget<I> implements IGhostIngredientHandler.Target<I> {
        private final Rect2i area;
        private final IGhostItemTarget target;

        ItemSlotTarget(Rect2i area, IGhostItemTarget target) {
            this.area = area;
            this.target = target;
        }

        @Override
        public Rect2i getArea() {
            return area;
        }

        @Override
        public void accept(I ingredient) {
            if (ingredient instanceof ItemStack stack) {
                target.acceptGhostItem(stack);
            }
        }
    }
}
