package com.stephanmeijer.minecraft.ae2.autorequester.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.stephanmeijer.minecraft.ae2.autorequester.AutorequesterConfig;
import com.stephanmeijer.minecraft.ae2.autorequester.compat.IGhostItemTarget;
import com.stephanmeijer.minecraft.ae2.autorequester.data.CraftingCondition;
import com.stephanmeijer.minecraft.ae2.autorequester.data.CraftingRule;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Screen for editing or creating a rule.
 *
 * Data flow:
 * - Parent passes a rule (copy) and callbacks
 * - This screen edits the rule copy
 * - On save: calls onSave with the edited rule
 * - On cancel: calls onCancel
 * - Parent decides what to do with the result
 */
public class RuleEditorScreen extends AbstractContainerScreen<RuleEditorMenu> implements IGhostItemTarget {
    private static final Logger LOG = LoggerFactory.getLogger(RuleEditorScreen.class);

    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 222;
    private static final int PADDING = 8;
    private static final int BUTTON_SIZE = 20;
    private static final int BUTTON_SPACING = 4;

    // Name field
    private static final int NAME_FIELD_Y = 20;
    private static final int FIELD_X = 65;
    private static final int NAME_FIELD_WIDTH = 180;
    private static final int NAME_FIELD_HEIGHT = 16;

    // Batch size and target item row
    private static final int BATCH_ROW_Y = 42;
    private static final int BATCH_FIELD_WIDTH = 60;
    private static final int TARGET_ITEM_LABEL_X = 150;
    private static final int TARGET_ITEM_SLOT_X = 226;
    private static final int TARGET_ITEM_SLOT_SIZE = 20;

    // Conditions section
    private static final int CONDITIONS_HEADER_Y = 68;
    private static final int CONDITION_LIST_X = PADDING;
    private static final int CONDITION_LIST_Y = 80;
    private static final int CONDITION_LIST_WIDTH = 228;
    private static final int SCROLLBAR_X = CONDITION_LIST_X + CONDITION_LIST_WIDTH + 2;
    private static final int SCROLLBAR_WIDTH = 10;
    private static final int CONDITION_HEIGHT = 24;
    private static final int CONDITION_LIST_END_Y = 186;
    private static final int CONDITION_LIST_HEIGHT = CONDITION_LIST_END_Y - CONDITION_LIST_Y;
    private static final int MAX_VISIBLE_CONDITIONS = CONDITION_LIST_HEIGHT / CONDITION_HEIGHT;

    // Bottom button row
    private static final int BOTTOM_BUTTON_Y_OFFSET = GUI_HEIGHT - 28;

    // ==================== Static Context for Screen Opening ====================
    // (Necessary because Minecraft's menu system limits constructor parameters)

    private static CraftingRule contextRule;
    private static Consumer<CraftingRule> contextOnSave;
    private static Runnable contextOnCancel;

    /**
     * Opens the rule editor screen.
     *
     * @param rule The rule to edit (will be copied)
     * @param onSave Called with the edited rule when user saves
     * @param onCancel Called when user cancels
     * @param title Screen title
     */
    public static void open(CraftingRule rule, Consumer<CraftingRule> onSave, Runnable onCancel, Component title) {
        Minecraft mc = Minecraft.getInstance();

        // Set context for the screen (use copyForEditing to preserve UUID)
        contextRule = rule.copyForEditing();
        contextOnSave = onSave;
        contextOnCancel = onCancel;

        RuleEditorMenu menu = new RuleEditorMenu(0, mc.player.getInventory());
        mc.setScreen(new RuleEditorScreen(menu, mc.player.getInventory(), title));
    }

    // ==================== Instance Fields ====================

    // The rule we're editing (a copy)
    private CraftingRule editingRule;

    // Callbacks
    private Consumer<CraftingRule> onSave;
    private Runnable onCancel;

    // UI state
    private EditBox nameField;
    private EditBox batchSizeField;
    private final List<CraftingCondition> conditions = new ArrayList<>();
    private int conditionScrollOffset;
    private int selectedConditionIndex = -1;
    private boolean isDraggingScrollbar;

    // Buttons
    private Button addConditionButton;
    private Button editConditionButton;
    private Button deleteConditionButton;
    private Button duplicateConditionButton;
    private Button moveUpConditionButton;
    private Button moveDownConditionButton;
    private Button saveButton;

    // Double-click tracking for conditions
    private final DoubleClickHandler conditionDoubleClick = new DoubleClickHandler();

    public RuleEditorScreen(RuleEditorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = this.imageHeight; // Hide inventory label
    }

    @Override
    protected void init() {
        super.init();

        // Load from context (first time) or restore state
        loadFromContext();

        // Rule name field
        nameField = new EditBox(font, leftPos + FIELD_X, topPos + NAME_FIELD_Y,
                NAME_FIELD_WIDTH, NAME_FIELD_HEIGHT, Component.literal(""));
        nameField.setMaxLength(50);
        nameField.setValue(editingRule.getName());
        nameField.setHint(Component.translatable("ae2_autorequester.gui.rule_name"));
        addRenderableWidget(nameField);

        // Batch size field
        batchSizeField = new EditBox(font, leftPos + FIELD_X, topPos + BATCH_ROW_Y,
                BATCH_FIELD_WIDTH, NAME_FIELD_HEIGHT, Component.literal(""));
        batchSizeField.setMaxLength(10);
        batchSizeField.setValue(String.valueOf(editingRule.getBatchSize()));
        batchSizeField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        if (AutorequesterConfig.hasBatchSizeLimit()) {
            batchSizeField.setTooltip(Tooltip.create(
                    Component.translatable("ae2_autorequester.gui.batch_size_limit", AutorequesterConfig.getMaxBatchSize())));
        }
        // Add responder to clamp value to valid range (1 to max)
        batchSizeField.setResponder(value -> {
            if (!value.isEmpty()) {
                try {
                    int parsed = Integer.parseInt(value);
                    int maxBatchSize = AutorequesterConfig.getMaxBatchSize();
                    if (parsed < 1) {
                        batchSizeField.setValue("1");
                    } else if (maxBatchSize != -1 && parsed > maxBatchSize) {
                        batchSizeField.setValue(String.valueOf(maxBatchSize));
                    }
                } catch (NumberFormatException ignored) {
                    // Filter already ensures only digits
                }
            }
        });
        addRenderableWidget(batchSizeField);

        // Bottom button row
        int bottomY = topPos + BOTTOM_BUTTON_Y_OFFSET;
        int buttonX = leftPos + PADDING;

        // Add condition button
        addConditionButton = addRenderableWidget(Button.builder(Component.literal("+"), button -> onAddCondition())
                .bounds(buttonX, bottomY, BUTTON_SIZE, BUTTON_SIZE)
                .build());
        buttonX += BUTTON_SIZE + BUTTON_SPACING;

        // Edit condition button
        editConditionButton = addRenderableWidget(Button.builder(Component.literal("\u270E"), button -> onEditCondition())
                .bounds(buttonX, bottomY, BUTTON_SIZE, BUTTON_SIZE)
                .tooltip(Tooltip.create(Component.translatable("ae2_autorequester.gui.edit_condition")))
                .build());
        buttonX += BUTTON_SIZE + BUTTON_SPACING;

        // Delete condition button
        deleteConditionButton = addRenderableWidget(Button.builder(Component.literal("\u2715"), button -> onDeleteCondition())
                .bounds(buttonX, bottomY, BUTTON_SIZE, BUTTON_SIZE)
                .tooltip(Tooltip.create(Component.translatable("ae2_autorequester.gui.delete_condition")))
                .build());
        buttonX += BUTTON_SIZE + BUTTON_SPACING;

        // Duplicate condition button
        duplicateConditionButton = addRenderableWidget(Button.builder(Component.literal("\u29C9"), button -> onDuplicateCondition())
                .bounds(buttonX, bottomY, BUTTON_SIZE, BUTTON_SIZE)
                .build());
        buttonX += BUTTON_SIZE + 10;

        // Move up button
        moveUpConditionButton = addRenderableWidget(Button.builder(Component.literal("\u25B2"), button -> onMoveConditionUp())
                .bounds(buttonX, bottomY, BUTTON_SIZE, BUTTON_SIZE)
                .tooltip(Tooltip.create(Component.translatable("ae2_autorequester.gui.move_up")))
                .build());
        buttonX += BUTTON_SIZE + BUTTON_SPACING;

        // Move down button
        moveDownConditionButton = addRenderableWidget(Button.builder(Component.literal("\u25BC"), button -> onMoveConditionDown())
                .bounds(buttonX, bottomY, BUTTON_SIZE, BUTTON_SIZE)
                .tooltip(Tooltip.create(Component.translatable("ae2_autorequester.gui.move_down")))
                .build());

        // Save button
        saveButton = addRenderableWidget(Button.builder(Component.literal("\u2713"), button -> onSaveClicked())
                .bounds(leftPos + GUI_WIDTH - PADDING - BUTTON_SIZE - BUTTON_SPACING - BUTTON_SIZE,
                        bottomY, BUTTON_SIZE, BUTTON_SIZE)
                .tooltip(Tooltip.create(Component.translatable("ae2_autorequester.gui.save")))
                .build());

        // Cancel button
        addRenderableWidget(Button.builder(Component.literal("\u2715"), button -> onCancelClicked())
                .bounds(leftPos + GUI_WIDTH - PADDING - BUTTON_SIZE,
                        bottomY, BUTTON_SIZE, BUTTON_SIZE)
                .tooltip(Tooltip.create(Component.translatable("ae2_autorequester.gui.cancel")))
                .build());

        updateButtonStates();
    }

    private void loadFromContext() {
        if (contextRule != null) {
            // First time opening - load from static context
            this.editingRule = contextRule;
            this.onSave = contextOnSave;
            this.onCancel = contextOnCancel;

            // Copy conditions to local list
            this.conditions.clear();
            for (CraftingCondition c : editingRule.getConditions()) {
                this.conditions.add(c.copy());
            }

            // Clear context
            contextRule = null;
            contextOnSave = null;
            contextOnCancel = null;
        }
        // If context is null, we're being restored after condition editor - state already set
    }

    private void updateButtonStates() {
        boolean hasSelection = selectedConditionIndex >= 0 && selectedConditionIndex < conditions.size();
        boolean canMoveUp = hasSelection && selectedConditionIndex > 0;
        boolean canMoveDown = hasSelection && selectedConditionIndex < conditions.size() - 1;
        boolean canAddCondition = AutorequesterConfig.validConditionCount(conditions.size());

        addConditionButton.active = canAddCondition;
        editConditionButton.active = hasSelection;
        deleteConditionButton.active = hasSelection;
        duplicateConditionButton.active = hasSelection && canAddCondition; // Duplicate also needs room for new condition
        moveUpConditionButton.active = canMoveUp;
        moveDownConditionButton.active = canMoveDown;

        // Update tooltips dynamically - show "(max: n)" only when at limit
        if (!canAddCondition && AutorequesterConfig.hasConditionsLimit()) {
            addConditionButton.setTooltip(Tooltip.create(
                    Component.translatable("ae2_autorequester.gui.add_condition_limit", AutorequesterConfig.getMaxConditions())));
            duplicateConditionButton.setTooltip(Tooltip.create(
                    Component.translatable("ae2_autorequester.gui.duplicate_condition_limit", AutorequesterConfig.getMaxConditions())));
        } else {
            addConditionButton.setTooltip(Tooltip.create(
                    Component.translatable("ae2_autorequester.gui.add_condition")));
            duplicateConditionButton.setTooltip(Tooltip.create(
                    Component.translatable("ae2_autorequester.gui.duplicate_condition")));
        }

        // Clamp scroll offset to valid range
        int maxScroll = Math.max(0, conditions.size() - MAX_VISIBLE_CONDITIONS);
        if (conditionScrollOffset > maxScroll) {
            conditionScrollOffset = maxScroll;
        }

        // Can only save if target item is set
        saveButton.active = !editingRule.getTargetItemStack().isEmpty();
    }

    // ==================== Condition Operations ====================

    private void onAddCondition() {
        // Check if we've hit the configured condition limit
        if (!AutorequesterConfig.validConditionCount(conditions.size())) {
            return;
        }

        // Save current UI state to the rule
        syncUIToRule();

        // Create new condition
        CraftingCondition newCondition = new CraftingCondition();

        // Open condition editor with CREATE callback
        ConditionEditorScreen.open(
                newCondition,
                savedCondition -> {
                    // Add the new condition to our list
                    conditions.add(savedCondition);
                    selectedConditionIndex = conditions.size() - 1;
                    // Reopen this screen
                    reopenSelf();
                },
                this::reopenSelf, // On cancel, just reopen
                Component.translatable("ae2_autorequester.gui.create_condition")
        );
    }

    private void onEditCondition() {
        if (selectedConditionIndex < 0 || selectedConditionIndex >= conditions.size()) {
            return;
        }

        // Save current UI state to the rule
        syncUIToRule();

        CraftingCondition conditionToEdit = conditions.get(selectedConditionIndex);
        final int editIndex = selectedConditionIndex;

        // Open condition editor with EDIT callback
        ConditionEditorScreen.open(
                conditionToEdit,
                savedCondition -> {
                    // Replace the condition in our list
                    conditions.set(editIndex, savedCondition);
                    // Reopen this screen
                    reopenSelf();
                },
                this::reopenSelf, // On cancel, just reopen
                Component.translatable("ae2_autorequester.gui.edit_condition")
        );
    }

    private void onDeleteCondition() {
        if (selectedConditionIndex >= 0 && selectedConditionIndex < conditions.size()) {
            conditions.remove(selectedConditionIndex);
            if (selectedConditionIndex >= conditions.size()) {
                selectedConditionIndex = conditions.size() - 1;
            }
            updateButtonStates();
        }
    }

    private void onDuplicateCondition() {
        if (selectedConditionIndex >= 0 && selectedConditionIndex < conditions.size()) {
            CraftingCondition copy = conditions.get(selectedConditionIndex).copy();
            conditions.add(selectedConditionIndex + 1, copy);
            selectedConditionIndex++;
            updateButtonStates();
        }
    }

    private void onMoveConditionUp() {
        if (selectedConditionIndex > 0 && selectedConditionIndex < conditions.size()) {
            CraftingCondition condition = conditions.remove(selectedConditionIndex);
            conditions.add(selectedConditionIndex - 1, condition);
            selectedConditionIndex--;
            updateButtonStates();
        }
    }

    private void onMoveConditionDown() {
        if (selectedConditionIndex >= 0 && selectedConditionIndex < conditions.size() - 1) {
            CraftingCondition condition = conditions.remove(selectedConditionIndex);
            conditions.add(selectedConditionIndex + 1, condition);
            selectedConditionIndex++;
            updateButtonStates();
        }
    }

    // ==================== Save/Cancel ====================

    private void syncUIToRule() {
        // Sync UI fields to the editing rule
        editingRule.setName(nameField.getValue());
        try {
            int batchSize = Integer.parseInt(batchSizeField.getValue());
            batchSize = Math.max(1, batchSize);
            // Clamp to max if value exceeds limit
            if (!AutorequesterConfig.validBatchSize(batchSize)) {
                batchSize = AutorequesterConfig.getMaxBatchSize();
            }
            editingRule.setBatchSize(batchSize);
        } catch (NumberFormatException e) {
            editingRule.setBatchSize(64);
        }
        // Target item is already synced via click handlers

        // Sync conditions
        editingRule.getConditions().clear();
        for (CraftingCondition c : conditions) {
            editingRule.addCondition(c);
        }
    }

    private void reopenSelf() {
        // Sync current state
        syncUIToRule();

        // Store in context for reopening
        contextRule = editingRule;
        contextOnSave = onSave;
        contextOnCancel = onCancel;

        // Reopen
        Minecraft mc = Minecraft.getInstance();
        RuleEditorMenu menu = new RuleEditorMenu(0, mc.player.getInventory());
        mc.setScreen(new RuleEditorScreen(menu, mc.player.getInventory(), getTitle()));
    }

    private void onSaveClicked() {
        // Sync UI to rule
        syncUIToRule();

        LOG.info("[RuleEditor] onSaveClicked - Rule: name='{}', target={}, batchSize={}, conditions={}",
                editingRule.getName(),
                editingRule.getTargetItem() != null ? editingRule.getTargetItem().toString() : "null",
                editingRule.getBatchSize(),
                editingRule.getConditions().size());

        // Call the save callback with the edited rule
        if (onSave != null) {
            LOG.info("[RuleEditor] Calling onSave callback");
            onSave.accept(editingRule);
        } else {
            LOG.warn("[RuleEditor] onSave callback is null!");
        }
    }

    private void onCancelClicked() {
        // Call the cancel callback
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

        // Labels
        guiGraphics.drawString(font, Component.translatable("ae2_autorequester.gui.rule_name_label"),
                leftPos + PADDING, topPos + NAME_FIELD_Y + 4, 0xAAAAAA);
        guiGraphics.drawString(font, Component.translatable("ae2_autorequester.gui.batch_size"),
                leftPos + PADDING, topPos + BATCH_ROW_Y + 4, 0xAAAAAA);
        guiGraphics.drawString(font, Component.translatable("ae2_autorequester.gui.target_item"),
                leftPos + TARGET_ITEM_LABEL_X, topPos + BATCH_ROW_Y + 4, 0xAAAAAA);

        // Target item slot
        int targetSlotX = leftPos + TARGET_ITEM_SLOT_X;
        int targetSlotY = topPos + BATCH_ROW_Y - 2;
        ItemStack targetItem = editingRule.getTargetItemStack();
        int slotInnerColor = targetItem.isEmpty() ? 0xFF373737 : 0xFF6A6A6A;
        guiGraphics.fill(targetSlotX, targetSlotY, targetSlotX + TARGET_ITEM_SLOT_SIZE,
                targetSlotY + TARGET_ITEM_SLOT_SIZE, 0xFF1E1E1E);
        guiGraphics.fill(targetSlotX + 1, targetSlotY + 1, targetSlotX + TARGET_ITEM_SLOT_SIZE - 1,
                targetSlotY + TARGET_ITEM_SLOT_SIZE - 1, slotInnerColor);

        if (!targetItem.isEmpty()) {
            guiGraphics.renderItem(targetItem, targetSlotX + 2, targetSlotY + 2);
        }

        // Conditions header
        guiGraphics.drawString(font, Component.translatable("ae2_autorequester.gui.conditions"),
                leftPos + CONDITION_LIST_X, topPos + CONDITIONS_HEADER_Y, 0xFFFFFF);

        // Condition list background
        int condListY = topPos + CONDITION_LIST_Y;
        guiGraphics.fill(leftPos + CONDITION_LIST_X, condListY,
                leftPos + CONDITION_LIST_X + CONDITION_LIST_WIDTH, condListY + CONDITION_LIST_HEIGHT, 0xFF1E1E1E);

        // Scrollbar track
        guiGraphics.fill(leftPos + SCROLLBAR_X, condListY,
                leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH, condListY + CONDITION_LIST_HEIGHT, 0xFF2A2A2A);

        // Scrollbar thumb
        if (conditions.size() > MAX_VISIBLE_CONDITIONS) {
            int maxScroll = conditions.size() - MAX_VISIBLE_CONDITIONS;
            int thumbHeight = Math.max(20, CONDITION_LIST_HEIGHT * MAX_VISIBLE_CONDITIONS / conditions.size());
            int thumbY = condListY + (CONDITION_LIST_HEIGHT - thumbHeight) * conditionScrollOffset / maxScroll;
            guiGraphics.fill(leftPos + SCROLLBAR_X + 1, thumbY,
                    leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight, 0xFF6A6A6A);
        }

        // Render visible conditions
        for (int i = 0; i < MAX_VISIBLE_CONDITIONS && i + conditionScrollOffset < conditions.size(); i++) {
            int index = i + conditionScrollOffset;
            CraftingCondition condition = conditions.get(index);
            int y = condListY + (i * CONDITION_HEIGHT);

            if (index == selectedConditionIndex) {
                guiGraphics.fill(leftPos + CONDITION_LIST_X, y,
                        leftPos + CONDITION_LIST_X + CONDITION_LIST_WIDTH, y + CONDITION_HEIGHT - 1, 0xFF4A4A4A);
            }

            renderConditionEntry(guiGraphics, condition, leftPos + CONDITION_LIST_X + 2, y + 2);
        }

        // Empty list message
        if (conditions.isEmpty()) {
            guiGraphics.drawCenteredString(font,
                    Component.translatable("ae2_autorequester.gui.no_conditions"),
                    leftPos + CONDITION_LIST_X + CONDITION_LIST_WIDTH / 2,
                    condListY + CONDITION_LIST_HEIGHT / 2, 0x888888);
        }
    }

    private void renderConditionEntry(GuiGraphics guiGraphics, CraftingCondition condition, int x, int y) {
        // Item slot
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF2A2A2A);
        if (!condition.getItemStack().isEmpty()) {
            guiGraphics.renderItem(condition.getItemStack(), x + 1, y + 1);
        }

        // Operator symbol
        guiGraphics.drawString(font, condition.getOperator().getSymbol(), x + 24, y + 5, 0xFFFFFF);

        // Threshold value
        guiGraphics.drawString(font, String.valueOf(condition.getThreshold()), x + 50, y + 5, 0xFFFF55);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawCenteredString(font, title, imageWidth / 2, 6, 0xFFFFFF);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Target item tooltip
        int targetSlotX = leftPos + TARGET_ITEM_SLOT_X;
        int targetSlotY = topPos + BATCH_ROW_Y - 2;
        if (mouseX >= targetSlotX && mouseX < targetSlotX + TARGET_ITEM_SLOT_SIZE &&
                mouseY >= targetSlotY && mouseY < targetSlotY + TARGET_ITEM_SLOT_SIZE) {
            ItemStack targetItem = editingRule.getTargetItemStack();
            if (!targetItem.isEmpty()) {
                guiGraphics.renderTooltip(font, targetItem, mouseX, mouseY);
            } else {
                guiGraphics.renderTooltip(font, Component.translatable("ae2_autorequester.gui.select_item"), mouseX, mouseY);
            }
        }
    }

    // ==================== Input Handling ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Target item slot click
        int targetSlotX = leftPos + TARGET_ITEM_SLOT_X;
        int targetSlotY = topPos + BATCH_ROW_Y - 2;
        if (mouseX >= targetSlotX && mouseX < targetSlotX + TARGET_ITEM_SLOT_SIZE &&
                mouseY >= targetSlotY && mouseY < targetSlotY + TARGET_ITEM_SLOT_SIZE) {
            ItemStack carried = minecraft.player.containerMenu.getCarried();
            if (!carried.isEmpty()) {
                editingRule.setTargetItem(carried.getItem());
                updateButtonStates();
            } else if (button == 1) {
                editingRule.setTargetItem(null);
                updateButtonStates();
            }
            return true;
        }

        int condListY = topPos + CONDITION_LIST_Y;

        // Scrollbar click
        if (mouseX >= leftPos + SCROLLBAR_X && mouseX < leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH &&
                mouseY >= condListY && mouseY < condListY + CONDITION_LIST_HEIGHT) {
            isDraggingScrollbar = true;
            updateScrollFromMouse(mouseY);
            return true;
        }

        // Condition list click
        if (mouseX >= leftPos + CONDITION_LIST_X && mouseX < leftPos + CONDITION_LIST_X + CONDITION_LIST_WIDTH &&
                mouseY >= condListY && mouseY < condListY + CONDITION_LIST_HEIGHT) {

            int relY = (int) mouseY - condListY;
            int condIndex = relY / CONDITION_HEIGHT + conditionScrollOffset;

            if (condIndex < conditions.size()) {
                selectedConditionIndex = condIndex;
                updateButtonStates();
                if (conditionDoubleClick.onClick(condIndex)) {
                    onEditCondition();
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollbar) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void updateScrollFromMouse(double mouseY) {
        if (conditions.size() <= MAX_VISIBLE_CONDITIONS) {
            conditionScrollOffset = 0;
            return;
        }

        int condListY = topPos + CONDITION_LIST_Y;
        double relativeY = mouseY - condListY;
        double ratio = Math.max(0, Math.min(1, relativeY / CONDITION_LIST_HEIGHT));
        int maxScroll = conditions.size() - MAX_VISIBLE_CONDITIONS;
        conditionScrollOffset = (int) (ratio * maxScroll);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int condListY = topPos + CONDITION_LIST_Y;
        if (mouseY >= condListY && mouseY < condListY + CONDITION_LIST_HEIGHT) {
            int maxScroll = Math.max(0, conditions.size() - MAX_VISIBLE_CONDITIONS);
            conditionScrollOffset = Math.max(0, Math.min(maxScroll, conditionScrollOffset - (int) scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESCAPE
            onCancelClicked();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ==================== Ghost Item Target (JEI/EMI) ====================

    @Override
    public Rect2i getGhostItemSlotBounds() {
        return new Rect2i(leftPos + TARGET_ITEM_SLOT_X, topPos + BATCH_ROW_Y - 2,
                TARGET_ITEM_SLOT_SIZE, TARGET_ITEM_SLOT_SIZE);
    }

    @Override
    public void acceptGhostItem(ItemStack stack) {
        editingRule.setTargetItem(stack.getItem());
        updateButtonStates();
    }
}
