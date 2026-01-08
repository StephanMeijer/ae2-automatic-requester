package com.stephanmeijer.minecraft.ae2.autorequester.compat.jei;

import com.stephanmeijer.minecraft.ae2.autorequester.AE2Autorequester;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
        // Register ghost ingredient handlers for each screen type
        registration.addGhostIngredientHandler(AutorequesterScreen.class, new EmptyGhostIngredientHandler<>());
        registration.addGhostIngredientHandler(RuleEditorScreen.class, new RuleEditorGhostIngredientHandler());
        registration.addGhostIngredientHandler(ConditionEditorScreen.class, new ConditionEditorGhostIngredientHandler());
    }

    /**
     * Empty handler for AutorequesterScreen - no ghost ingredient targets on main screen.
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
     * Ghost ingredient handler for RuleEditorScreen.
     * Provides a drop target for the target item slot.
     */
    private static class RuleEditorGhostIngredientHandler implements IGhostIngredientHandler<RuleEditorScreen> {

        @Override
        public <I> List<Target<I>> getTargetsTyped(RuleEditorScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
            List<Target<I>> targets = new ArrayList<>();

            if (ingredient.getItemStack().isEmpty()) {
                return targets;
            }

            Rect2i bounds = screen.getGhostItemSlotBounds();
            if (bounds != null) {
                targets.add(new ItemSlotTarget<>(bounds, screen::acceptGhostItem));
            }

            return targets;
        }

        @Override
        public void onComplete() {
        }
    }

    /**
     * Ghost ingredient handler for ConditionEditorScreen.
     * Provides a drop target for the condition item slot.
     */
    private static class ConditionEditorGhostIngredientHandler implements IGhostIngredientHandler<ConditionEditorScreen> {

        @Override
        public <I> List<Target<I>> getTargetsTyped(ConditionEditorScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
            List<Target<I>> targets = new ArrayList<>();

            if (ingredient.getItemStack().isEmpty()) {
                return targets;
            }

            Rect2i bounds = screen.getGhostItemSlotBounds();
            if (bounds != null) {
                targets.add(new ItemSlotTarget<>(bounds, screen::acceptGhostItem));
            }

            return targets;
        }

        @Override
        public void onComplete() {
        }
    }

    /**
     * Generic target for an item slot that accepts ghost ingredients.
     */
    private static class ItemSlotTarget<I> implements IGhostIngredientHandler.Target<I> {
        private final Rect2i area;
        private final Consumer<ItemStack> acceptor;

        public ItemSlotTarget(Rect2i area, Consumer<ItemStack> acceptor) {
            this.area = area;
            this.acceptor = acceptor;
        }

        @Override
        public Rect2i getArea() {
            return area;
        }

        @Override
        public void accept(I ingredient) {
            if (ingredient instanceof ItemStack stack) {
                acceptor.accept(stack);
            }
        }
    }
}
