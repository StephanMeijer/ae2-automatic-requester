package com.stephanmeijer.minecraft.ae2.autorequester.gui;

import com.stephanmeijer.minecraft.ae2.autorequester.data.ComparisonOperator;
import com.stephanmeijer.minecraft.ae2.autorequester.data.CraftingCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * Screen for editing or creating a condition.
 *
 * Data flow:
 * - Parent passes a condition (copy) and callbacks
 * - This screen edits the condition copy
 * - On save: calls onSave with the edited condition
 * - On cancel: calls onCancel
 * - Parent decides what to do with the result
 */
public class ConditionEditorScreen extends AbstractContainerScreen<ConditionEditorMenu> {
    // Dialog dimensions
    private static final int GUI_WIDTH = 220;
    private static final int GUI_HEIGHT = 150;
    private static final int PADDING = 10;

    // Button/element dimensions
    private static final int BUTTON_HEIGHT = 20;
    private static final int ADJUSTMENT_BUTTON_WIDTH = 45;
    private static final int ADJUSTMENT_BUTTON_LARGE_WIDTH = 50;
    private static final int ADJUSTMENT_BUTTON_SPACING = 50;
    private static final int ITEM_SLOT_SIZE = 20;
    private static final int OPERATOR_BUTTON_WIDTH = 30;
    private static final int OPERATOR_BUTTON_X = PADDING + ITEM_SLOT_SIZE + 4;
    private static final int THRESHOLD_FIELD_X = OPERATOR_BUTTON_X + OPERATOR_BUTTON_WIDTH + 4;
    private static final int CONFIRM_CANCEL_BUTTON_SIZE = 20;

    // Row positions
    private static final int ROW_GAP = 6;
    private static final int PLUS_ROW_Y_OFFSET = 28;
    private static final int MAIN_ROW_Y_OFFSET = PLUS_ROW_Y_OFFSET + BUTTON_HEIGHT + ROW_GAP;
    private static final int BOTTOM_ROW_Y_OFFSET = GUI_HEIGHT - 28;

    // ==================== Static Context for Screen Opening ====================

    private static CraftingCondition contextCondition;
    private static Consumer<CraftingCondition> contextOnSave;
    private static Runnable contextOnCancel;
    private static Component contextTitle;

    /**
     * Opens the condition editor screen.
     *
     * @param condition The condition to edit (will be copied)
     * @param onSave Called with the edited condition when user saves
     * @param onCancel Called when user cancels
     * @param title Screen title
     */
    public static void open(CraftingCondition condition, Consumer<CraftingCondition> onSave,
                            Runnable onCancel, Component title) {
        Minecraft mc = Minecraft.getInstance();

        // Set context for the screen
        contextCondition = condition.copy();
        contextOnSave = onSave;
        contextOnCancel = onCancel;
        contextTitle = title;

        ConditionEditorMenu menu = new ConditionEditorMenu(0, mc.player.getInventory());
        mc.setScreen(new ConditionEditorScreen(menu, mc.player.getInventory(), title));
    }

    // ==================== Instance Fields ====================

    // The condition we're editing (a copy)
    private CraftingCondition editingCondition;

    // Callbacks
    private Consumer<CraftingCondition> onSave;
    private Runnable onCancel;

    // UI components
    private EditBox thresholdField;
    private Button operatorButton;
    private Button saveButton;

    // Working state
    private ComparisonOperator currentOperator;
    private long currentThreshold;
    private ItemStack currentItem;

    public ConditionEditorScreen(ConditionEditorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = this.imageHeight; // Hide inventory label
    }

    @Override
    protected void init() {
        super.init();

        // Load from context
        loadFromContext();

        int rightEdge = leftPos + PADDING + ADJUSTMENT_BUTTON_SPACING * 3 + ADJUSTMENT_BUTTON_LARGE_WIDTH;

        // Row 1: + adjustment buttons
        int plusY = topPos + PLUS_ROW_Y_OFFSET;
        addRenderableWidget(Button.builder(Component.literal("+1"), b -> adjustThreshold(1))
                .bounds(leftPos + PADDING, plusY, ADJUSTMENT_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.literal("+10"), b -> adjustThreshold(10))
                .bounds(leftPos + PADDING + ADJUSTMENT_BUTTON_SPACING, plusY, ADJUSTMENT_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.literal("+100"), b -> adjustThreshold(100))
                .bounds(leftPos + PADDING + ADJUSTMENT_BUTTON_SPACING * 2, plusY, ADJUSTMENT_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.literal("+1000"), b -> adjustThreshold(1000))
                .bounds(leftPos + PADDING + ADJUSTMENT_BUTTON_SPACING * 3, plusY, ADJUSTMENT_BUTTON_LARGE_WIDTH, BUTTON_HEIGHT).build());

        // Row 2: Operator button, Threshold field (item slot drawn manually)
        int mainRowY = topPos + MAIN_ROW_Y_OFFSET;

        operatorButton = addRenderableWidget(Button.builder(
                Component.literal(currentOperator.getSymbol()),
                b -> cycleOperator()
        ).bounds(leftPos + OPERATOR_BUTTON_X, mainRowY, OPERATOR_BUTTON_WIDTH, BUTTON_HEIGHT).build());

        int thresholdX = leftPos + THRESHOLD_FIELD_X;
        int thresholdWidth = rightEdge - thresholdX;
        thresholdField = new EditBox(font, thresholdX, mainRowY, thresholdWidth, BUTTON_HEIGHT, Component.literal(""));
        thresholdField.setMaxLength(10);
        thresholdField.setValue(String.valueOf(currentThreshold));
        thresholdField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        thresholdField.setResponder(s -> {
            try {
                currentThreshold = s.isEmpty() ? 0 : Long.parseLong(s);
            } catch (NumberFormatException e) {
                // Ignore
            }
        });
        addRenderableWidget(thresholdField);

        // Row 3: - adjustment buttons
        int minusY = mainRowY + BUTTON_HEIGHT + ROW_GAP;
        addRenderableWidget(Button.builder(Component.literal("-1"), b -> adjustThreshold(-1))
                .bounds(leftPos + PADDING, minusY, ADJUSTMENT_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.literal("-10"), b -> adjustThreshold(-10))
                .bounds(leftPos + PADDING + ADJUSTMENT_BUTTON_SPACING, minusY, ADJUSTMENT_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.literal("-100"), b -> adjustThreshold(-100))
                .bounds(leftPos + PADDING + ADJUSTMENT_BUTTON_SPACING * 2, minusY, ADJUSTMENT_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.literal("-1000"), b -> adjustThreshold(-1000))
                .bounds(leftPos + PADDING + ADJUSTMENT_BUTTON_SPACING * 3, minusY, ADJUSTMENT_BUTTON_LARGE_WIDTH, BUTTON_HEIGHT).build());

        // Bottom row: Confirm and Cancel buttons
        int bottomY = topPos + BOTTOM_ROW_Y_OFFSET;
        int buttonSpacing = 4;

        saveButton = addRenderableWidget(Button.builder(Component.literal("\u2713"), b -> onSaveClicked())
                .bounds(leftPos + GUI_WIDTH - PADDING - CONFIRM_CANCEL_BUTTON_SIZE - buttonSpacing - CONFIRM_CANCEL_BUTTON_SIZE,
                        bottomY, CONFIRM_CANCEL_BUTTON_SIZE, CONFIRM_CANCEL_BUTTON_SIZE)
                .tooltip(Tooltip.create(Component.translatable("ae2_autorequester.gui.save")))
                .build());

        addRenderableWidget(Button.builder(Component.literal("\u2715"), b -> onCancelClicked())
                .bounds(leftPos + GUI_WIDTH - PADDING - CONFIRM_CANCEL_BUTTON_SIZE,
                        bottomY, CONFIRM_CANCEL_BUTTON_SIZE, CONFIRM_CANCEL_BUTTON_SIZE)
                .tooltip(Tooltip.create(Component.translatable("ae2_autorequester.gui.cancel")))
                .build());

        updateSaveButtonState();
    }

    private void loadFromContext() {
        if (contextCondition != null) {
            this.editingCondition = contextCondition;
            this.onSave = contextOnSave;
            this.onCancel = contextOnCancel;

            // Copy working state
            this.currentOperator = editingCondition.getOperator();
            this.currentThreshold = editingCondition.getThreshold();
            this.currentItem = editingCondition.getItemStack().copy();

            // Clear context
            contextCondition = null;
            contextOnSave = null;
            contextOnCancel = null;
            contextTitle = null;
        }
    }

    private void updateSaveButtonState() {
        // Can only save if item is set
        saveButton.active = !currentItem.isEmpty();
    }

    private void adjustThreshold(int delta) {
        currentThreshold = Math.max(0, currentThreshold + delta);
        thresholdField.setValue(String.valueOf(currentThreshold));
    }

    private void cycleOperator() {
        ComparisonOperator[] ops = ComparisonOperator.values();
        int nextOrd = (currentOperator.ordinal() + 1) % ops.length;
        currentOperator = ops[nextOrd];
        operatorButton.setMessage(Component.literal(currentOperator.getSymbol()));
    }

    private void onSaveClicked() {
        // Sync working state to condition
        editingCondition.setItem(currentItem.getItem());
        editingCondition.setOperator(currentOperator);
        editingCondition.setThreshold(currentThreshold);

        // Call save callback
        if (onSave != null) {
            onSave.accept(editingCondition);
        }
    }

    private void onCancelClicked() {
        if (onCancel != null) {
            onCancel.run();
        }
    }

    // ==================== Rendering ====================

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Draw panel background
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFF373737);

        // Draw title bar
        guiGraphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + 16, 0xFF1E1E1E);

        // Item slot (Row 2) - light gray background when item is set
        int mainRowY = topPos + MAIN_ROW_Y_OFFSET;
        int itemSlotX = leftPos + PADDING;
        int slotInnerColor = currentItem.isEmpty() ? 0xFF373737 : 0xFF6A6A6A;
        guiGraphics.fill(itemSlotX, mainRowY, itemSlotX + ITEM_SLOT_SIZE, mainRowY + BUTTON_HEIGHT, 0xFF1E1E1E);
        guiGraphics.fill(itemSlotX + 1, mainRowY + 1, itemSlotX + ITEM_SLOT_SIZE - 1, mainRowY + BUTTON_HEIGHT - 1, slotInnerColor);

        if (!currentItem.isEmpty()) {
            guiGraphics.renderItem(currentItem, itemSlotX + 2, mainRowY + 2);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawCenteredString(font, title, imageWidth / 2, 6, 0xFFFFFF);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int mainRowY = topPos + MAIN_ROW_Y_OFFSET;
        int itemSlotX = leftPos + PADDING;

        // Item slot tooltip
        if (mouseX >= itemSlotX && mouseX < itemSlotX + ITEM_SLOT_SIZE &&
                mouseY >= mainRowY && mouseY < mainRowY + BUTTON_HEIGHT) {
            if (!currentItem.isEmpty()) {
                guiGraphics.renderTooltip(font, currentItem, mouseX, mouseY);
            } else {
                guiGraphics.renderTooltip(font, Component.translatable("ae2_autorequester.gui.select_item"), mouseX, mouseY);
            }
        }

        // Operator tooltip
        if (operatorButton.isHovered()) {
            guiGraphics.renderTooltip(font, Component.translatable("ae2_autorequester.tooltip.operator." + currentOperator.name().toLowerCase()), mouseX, mouseY);
        }
    }

    // ==================== Input Handling ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Item slot click
        int mainRowY = topPos + MAIN_ROW_Y_OFFSET;
        int itemSlotX = leftPos + PADDING;
        if (mouseX >= itemSlotX && mouseX < itemSlotX + ITEM_SLOT_SIZE &&
                mouseY >= mainRowY && mouseY < mainRowY + BUTTON_HEIGHT) {
            ItemStack carried = minecraft.player.containerMenu.getCarried();
            if (!carried.isEmpty()) {
                currentItem = carried.copyWithCount(1);
                updateSaveButtonState();
            } else if (button == 1) { // Right click to clear
                currentItem = ItemStack.EMPTY;
                updateSaveButtonState();
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESCAPE
            onCancelClicked();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ==================== JEI Ghost Ingredient Support ====================

    public Rect2i getGhostItemSlotBounds() {
        int mainRowY = topPos + MAIN_ROW_Y_OFFSET;
        return new Rect2i(leftPos + PADDING, mainRowY, ITEM_SLOT_SIZE, BUTTON_HEIGHT);
    }

    public void acceptGhostItem(ItemStack stack) {
        this.currentItem = stack.copyWithCount(1);
        updateSaveButtonState();
    }
}
