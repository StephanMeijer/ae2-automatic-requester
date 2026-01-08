package com.stephanmeijer.minecraft.ae2.autorequester.gui;

import java.util.ArrayList;
import java.util.List;

import com.stephanmeijer.minecraft.ae2.autorequester.AutorequesterConfig;
import com.stephanmeijer.minecraft.ae2.autorequester.data.CraftingRule;
import com.stephanmeijer.minecraft.ae2.autorequester.data.RuleStatus;
import com.stephanmeijer.minecraft.ae2.autorequester.network.OpenAutorequesterPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutorequesterScreen extends AbstractContainerScreen<AutorequesterMenu> {
    private static final Logger LOG = LoggerFactory.getLogger(AutorequesterScreen.class);

    // GUI dimensions
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 222;
    private static final int PADDING = 8;

    // Rule list area
    private static final int RULE_LIST_X = PADDING;
    private static final int RULE_LIST_Y = 18;
    private static final int RULE_LIST_WIDTH = 228;
    private static final int RULE_ENTRY_HEIGHT = 24;
    private static final int MAX_VISIBLE_RULES = 7;
    private static final int RULE_LIST_HEIGHT = MAX_VISIBLE_RULES * RULE_ENTRY_HEIGHT;

    // Scrollbar
    private static final int SCROLLBAR_X = RULE_LIST_X + RULE_LIST_WIDTH + 2;
    private static final int SCROLLBAR_WIDTH = 10;

    // Button dimensions
    private static final int BUTTON_SIZE = 20;
    private static final int BUTTON_SPACING = 4;
    private static final int BUTTON_GROUP_GAP = 6;
    private static final int BOTTOM_BUTTON_Y_OFFSET = 28;

    // Rule entry toggle
    private static final int TOGGLE_WIDTH = 28;
    private static final int TOGGLE_HEIGHT = 14;
    private static final int TOGGLE_MARGIN = 4;
    private static final int TOGGLE_X_OFFSET = RULE_LIST_WIDTH - TOGGLE_WIDTH - TOGGLE_MARGIN;
    private static final int TOGGLE_CLICK_THRESHOLD = TOGGLE_WIDTH + TOGGLE_MARGIN + 3;

    // Status icon
    private static final int STATUS_ICON_SIZE = 12;
    private static final int STATUS_ICON_MARGIN = 8;

    // Rule entry layout (within RULE_ENTRY_HEIGHT = 24)
    private static final int ENTRY_STATUS_ICON_X = 0;
    private static final int ENTRY_STATUS_ICON_Y = 4;
    private static final int ENTRY_STATUS_ICON_SIZE = 12;
    private static final int ENTRY_ITEM_X = 16;
    private static final int ENTRY_ITEM_Y = 2;
    private static final int ENTRY_TEXT_X = 36;
    private static final int ENTRY_TEXT_LINE1_Y = 2;
    private static final int ENTRY_TEXT_LINE2_Y = 12;
    private static final int ENTRY_TOGGLE_Y = 4;

    // Screen state context (preserved across reopens)
    private static int contextScrollOffset = -1;
    private static int contextSelectedIndex = -1;
    private static boolean contextScrollToBottom;

    /**
     * Sets the context for screen state preservation when reopening.
     *
     * @param scrollOffset The scroll offset to restore (-1 to not restore)
     * @param selectedIndex The selected rule index to restore (-1 for no selection)
     * @param scrollToBottom If true, scroll to bottom and select last rule (overrides other params)
     */
    public static void setReopenContext(int scrollOffset, int selectedIndex, boolean scrollToBottom) {
        contextScrollOffset = scrollOffset;
        contextSelectedIndex = selectedIndex;
        contextScrollToBottom = scrollToBottom;
    }

    private int scrollOffset;
    private int selectedRuleIndex = -1;
    private final DoubleClickHandler ruleDoubleClick = new DoubleClickHandler();
    private boolean isDraggingScrollbar;

    // Button references for enabling/disabling
    private Button addRuleButton;
    private Button editButton;
    private Button deleteButton;
    private Button duplicateButton;
    private Button moveUpButton;
    private Button moveDownButton;

    public AutorequesterScreen(AutorequesterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94; // Hide inventory label
    }

    @Override
    protected void init() {
        super.init();

        LOG.info("[MainScreen] init() - menu has {} rules", menu.getRules().size());
        for (int i = 0; i < menu.getRules().size(); i++) {
            CraftingRule r = menu.getRules().get(i);
            LOG.info("[MainScreen]   Rule[{}]: id={}, name='{}', target={}, batchSize={}, conditions={}",
                    i, r.getId(), r.getName(),
                    r.getTargetItem() != null ? r.getTargetItem().toString() : "null",
                    r.getBatchSize(), r.getConditions().size());
        }

        int buttonY = topPos + imageHeight - BOTTOM_BUTTON_Y_OFFSET;
        int buttonX = leftPos + PADDING;

        // Add Rule button
        addRuleButton = addRenderableWidget(Button.builder(Component.literal("+"), button -> onAddRule())
                .bounds(buttonX, buttonY, BUTTON_SIZE, BUTTON_SIZE)
                .build());
        buttonX += BUTTON_SIZE + BUTTON_SPACING;

        // Edit button
        editButton = addRenderableWidget(Button.builder(Component.literal("✎"), button -> onEditRule())
                .bounds(buttonX, buttonY, BUTTON_SIZE, BUTTON_SIZE)
                .tooltip(Tooltip.create(Component.translatable("ae2_autorequester.gui.edit_rule")))
                .build());
        buttonX += BUTTON_SIZE + BUTTON_SPACING;

        // Delete button
        deleteButton = addRenderableWidget(Button.builder(Component.literal("✕"), button -> onDeleteRule())
                .bounds(buttonX, buttonY, BUTTON_SIZE, BUTTON_SIZE)
                .tooltip(Tooltip.create(Component.translatable("ae2_autorequester.gui.delete_rule")))
                .build());
        buttonX += BUTTON_SIZE + BUTTON_SPACING;

        // Duplicate button
        duplicateButton = addRenderableWidget(Button.builder(Component.literal("⧉"), button -> onDuplicateRule())
                .bounds(buttonX, buttonY, BUTTON_SIZE, BUTTON_SIZE)
                .build());
        buttonX += BUTTON_SIZE + BUTTON_GROUP_GAP;

        // Move up button
        moveUpButton = addRenderableWidget(Button.builder(Component.literal("▲"), button -> onMoveUp())
                .bounds(buttonX, buttonY, BUTTON_SIZE, BUTTON_SIZE)
                .tooltip(Tooltip.create(Component.translatable("ae2_autorequester.gui.move_up")))
                .build());
        buttonX += BUTTON_SIZE + BUTTON_SPACING;

        // Move down button
        moveDownButton = addRenderableWidget(Button.builder(Component.literal("▼"), button -> onMoveDown())
                .bounds(buttonX, buttonY, BUTTON_SIZE, BUTTON_SIZE)
                .tooltip(Tooltip.create(Component.translatable("ae2_autorequester.gui.move_down")))
                .build());

        // Load preserved state from context (if any)
        loadStateFromContext();

        // Initial button state update
        updateButtonStates();
    }

    private void loadStateFromContext() {
        if (contextScrollToBottom) {
            // After add: scroll to show last rule, select it
            scrollOffset = Math.max(0, menu.getRules().size() - MAX_VISIBLE_RULES);
            selectedRuleIndex = menu.getRules().size() - 1;
        } else if (contextScrollOffset >= 0) {
            // After edit/cancel: restore previous state
            scrollOffset = Math.min(contextScrollOffset, Math.max(0, menu.getRules().size() - MAX_VISIBLE_RULES));
            selectedRuleIndex = Math.min(contextSelectedIndex, menu.getRules().size() - 1);
        }
        // Reset context
        contextScrollOffset = -1;
        contextSelectedIndex = -1;
        contextScrollToBottom = false;
    }

    private void updateButtonStates() {
        boolean hasSelection = selectedRuleIndex >= 0 && selectedRuleIndex < menu.getRules().size();
        boolean canAddRule = menu.canAddRule();
        boolean canMoveUp = hasSelection && selectedRuleIndex > 0;
        boolean canMoveDown = hasSelection && selectedRuleIndex < menu.getRules().size() - 1;

        addRuleButton.active = canAddRule;
        editButton.active = hasSelection;
        deleteButton.active = hasSelection;
        duplicateButton.active = hasSelection && canAddRule; // Duplicate also needs room for new rule
        moveUpButton.active = canMoveUp;
        moveDownButton.active = canMoveDown;

        // Update tooltips dynamically - show "(max: n)" only when at limit
        if (!canAddRule && AutorequesterConfig.hasRulesLimit()) {
            addRuleButton.setTooltip(Tooltip.create(
                    Component.translatable("ae2_autorequester.gui.add_rule_limit", AutorequesterConfig.getMaxRules())));
            duplicateButton.setTooltip(Tooltip.create(
                    Component.translatable("ae2_autorequester.gui.duplicate_rule_limit", AutorequesterConfig.getMaxRules())));
        } else {
            addRuleButton.setTooltip(Tooltip.create(
                    Component.translatable("ae2_autorequester.gui.add_rule")));
            duplicateButton.setTooltip(Tooltip.create(
                    Component.translatable("ae2_autorequester.gui.duplicate_rule")));
        }

        // Clamp scroll offset to valid range
        int maxScroll = Math.max(0, menu.getRules().size() - MAX_VISIBLE_RULES);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Draw background
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, GuiColors.BACKGROUND_BORDER);
        guiGraphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, GuiColors.BACKGROUND_FILL);

        // Draw title bar
        guiGraphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + 16, GuiColors.TITLE_BAR);

        // Draw rule list background
        guiGraphics.fill(leftPos + RULE_LIST_X, topPos + RULE_LIST_Y,
                leftPos + RULE_LIST_X + RULE_LIST_WIDTH, topPos + RULE_LIST_Y + RULE_LIST_HEIGHT,
                GuiColors.LIST_BACKGROUND);

        // Draw scrollbar track
        guiGraphics.fill(leftPos + SCROLLBAR_X, topPos + RULE_LIST_Y,
                leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH, topPos + RULE_LIST_Y + RULE_LIST_HEIGHT,
                GuiColors.SCROLLBAR_TRACK);

        // Draw scrollbar thumb
        List<CraftingRule> rules = menu.getRules();
        if (rules.size() > MAX_VISIBLE_RULES) {
            int maxScroll = rules.size() - MAX_VISIBLE_RULES;
            int thumbHeight = Math.max(20, RULE_LIST_HEIGHT * MAX_VISIBLE_RULES / rules.size());
            int thumbY = topPos + RULE_LIST_Y + (RULE_LIST_HEIGHT - thumbHeight) * scrollOffset / maxScroll;
            guiGraphics.fill(leftPos + SCROLLBAR_X + 1, thumbY,
                    leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight,
                    GuiColors.SCROLLBAR_THUMB);
        }

        // Draw rules
        for (int i = 0; i < MAX_VISIBLE_RULES && i + scrollOffset < rules.size(); i++) {
            int ruleIndex = i + scrollOffset;
            CraftingRule rule = rules.get(ruleIndex);
            int y = topPos + RULE_LIST_Y + (i * RULE_ENTRY_HEIGHT);

            // Highlight selected rule
            if (ruleIndex == selectedRuleIndex) {
                guiGraphics.fill(leftPos + RULE_LIST_X, y,
                        leftPos + RULE_LIST_X + RULE_LIST_WIDTH, y + RULE_ENTRY_HEIGHT - 1,
                        GuiColors.SELECTION_HIGHLIGHT);
            }

            // Draw rule entry
            renderRuleEntry(guiGraphics, rule, leftPos + RULE_LIST_X + 2, y + 2);
        }

        // Status icon (top-right corner) - Priority: error > warning > success
        int statusIconX = leftPos + imageWidth - STATUS_ICON_MARGIN - STATUS_ICON_SIZE;
        int statusIconY = topPos + 4;
        List<String> missingPatternItems = getMissingPatternItems();

        if (!menu.isNetworkConnected()) {
            // Error icon (red) - network disconnected
            long time = System.currentTimeMillis();
            float pulse = (float) (Math.sin(time / 200.0) * 0.3 + 0.7);
            int red = (int) (255 * pulse);
            int errorColor = 0xFF000000 | (red << 16) | 0x2222;

            guiGraphics.fill(statusIconX, statusIconY, statusIconX + STATUS_ICON_SIZE, statusIconY + STATUS_ICON_SIZE, GuiColors.STATUS_ICON_BORDER);
            guiGraphics.fill(statusIconX + 1, statusIconY + 1, statusIconX + STATUS_ICON_SIZE - 1, statusIconY + STATUS_ICON_SIZE - 1, errorColor);
            guiGraphics.drawCenteredString(font, "!", statusIconX + STATUS_ICON_SIZE / 2, statusIconY + 2, GuiColors.TEXT_PRIMARY);
        } else if (!missingPatternItems.isEmpty()) {
            // Warning icon (yellow) - missing patterns
            long time = System.currentTimeMillis();
            float pulse = (float) (Math.sin(time / 300.0) * 0.2 + 0.8);
            int yellow = (int) (255 * pulse);
            int warningColor = 0xFF000000 | (yellow << 16) | (yellow << 8);

            guiGraphics.fill(statusIconX, statusIconY, statusIconX + STATUS_ICON_SIZE, statusIconY + STATUS_ICON_SIZE, GuiColors.STATUS_ICON_BORDER);
            guiGraphics.fill(statusIconX + 1, statusIconY + 1, statusIconX + STATUS_ICON_SIZE - 1, statusIconY + STATUS_ICON_SIZE - 1, warningColor);
            guiGraphics.drawCenteredString(font, "!", statusIconX + STATUS_ICON_SIZE / 2, statusIconY + 2, 0x000000);
        } else {
            // Success icon (green) - all good
            guiGraphics.fill(statusIconX, statusIconY, statusIconX + STATUS_ICON_SIZE, statusIconY + STATUS_ICON_SIZE, GuiColors.STATUS_ICON_BORDER);
            guiGraphics.fill(statusIconX + 1, statusIconY + 1, statusIconX + STATUS_ICON_SIZE - 1, statusIconY + STATUS_ICON_SIZE - 1, GuiColors.STATUS_SUCCESS);
        }

        // Empty list message
        if (rules.isEmpty()) {
            guiGraphics.drawCenteredString(font,
                    Component.translatable("ae2_autorequester.gui.no_rules"),
                    leftPos + RULE_LIST_X + RULE_LIST_WIDTH / 2,
                    topPos + RULE_LIST_Y + RULE_LIST_HEIGHT / 2,
                    GuiColors.TEXT_SECONDARY);
        }
    }

    private void renderRuleEntry(GuiGraphics guiGraphics, CraftingRule rule, int x, int y) {
        // Status icon (colored square) - green for enabled, red for disabled
        int statusColor = rule.isEnabled() ? GuiColors.STATUS_SUCCESS : GuiColors.STATUS_ERROR;
        int statusX = x + ENTRY_STATUS_ICON_X;
        int statusY = y + ENTRY_STATUS_ICON_Y;
        guiGraphics.fill(statusX, statusY, statusX + ENTRY_STATUS_ICON_SIZE, statusY + ENTRY_STATUS_ICON_SIZE, GuiColors.STATUS_ICON_BORDER);
        guiGraphics.fill(statusX + 1, statusY + 1, statusX + ENTRY_STATUS_ICON_SIZE - 1, statusY + ENTRY_STATUS_ICON_SIZE - 1, statusColor);

        // Target item icon
        if (rule.getTargetItem() != null && !rule.getTargetItemStack().isEmpty()) {
            guiGraphics.renderItem(rule.getTargetItemStack(), x + ENTRY_ITEM_X, y + ENTRY_ITEM_Y);
        }

        // Rule name/target
        String displayName = rule.getDisplayName();
        if (displayName.length() > 20) {
            displayName = displayName.substring(0, 17) + "...";
        }
        int textColor = rule.isEnabled() ? GuiColors.TEXT_PRIMARY : GuiColors.TEXT_SECONDARY;
        guiGraphics.drawString(font, displayName, x + ENTRY_TEXT_X, y + ENTRY_TEXT_LINE1_Y, textColor);

        // Condition count and batch size
        String info = rule.getConditions().size() + " cond. | ×" + rule.getBatchSize();
        guiGraphics.drawString(font, info, x + ENTRY_TEXT_X, y + ENTRY_TEXT_LINE2_Y, GuiColors.TEXT_SECONDARY);

        // Enable/disable toggle - colored box with text
        int toggleX = x + TOGGLE_X_OFFSET;
        int toggleY = y + ENTRY_TOGGLE_Y;
        int toggleColor = rule.isEnabled() ? GuiColors.STATUS_SUCCESS : GuiColors.STATUS_DISABLED;
        guiGraphics.fill(toggleX, toggleY, toggleX + TOGGLE_WIDTH, toggleY + TOGGLE_HEIGHT, GuiColors.STATUS_ICON_BORDER);
        guiGraphics.fill(toggleX + 1, toggleY + 1, toggleX + TOGGLE_WIDTH - 1, toggleY + TOGGLE_HEIGHT - 1, toggleColor);
        String enableText = rule.isEnabled() ? "ON" : "OFF";
        guiGraphics.drawCenteredString(font, enableText, toggleX + TOGGLE_WIDTH / 2, toggleY + 3, GuiColors.TEXT_PRIMARY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, PADDING, 6, GuiColors.TEXT_PRIMARY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Render status tooltips
        List<CraftingRule> rules = menu.getRules();
        for (int i = 0; i < MAX_VISIBLE_RULES && i + scrollOffset < rules.size(); i++) {
            int ruleIndex = i + scrollOffset;
            CraftingRule rule = rules.get(ruleIndex);
            int x = leftPos + RULE_LIST_X + 2;
            int y = topPos + RULE_LIST_Y + (i * RULE_ENTRY_HEIGHT) + 2;

            // Status icon tooltip
            int statusX = x + ENTRY_STATUS_ICON_X;
            int statusY = y + ENTRY_STATUS_ICON_Y;
            if (mouseX >= statusX && mouseX < statusX + ENTRY_STATUS_ICON_SIZE &&
                    mouseY >= statusY && mouseY < statusY + ENTRY_STATUS_ICON_SIZE) {
                guiGraphics.renderComponentTooltip(font, getStatusTooltip(rule), mouseX, mouseY);
            }

            // Target item tooltip (16x16 item at ENTRY_ITEM_X, ENTRY_ITEM_Y)
            int itemX = x + ENTRY_ITEM_X;
            int itemY = y + ENTRY_ITEM_Y;
            if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
                if (!rule.getTargetItemStack().isEmpty()) {
                    guiGraphics.renderTooltip(font, rule.getTargetItemStack(), mouseX, mouseY);
                }
            }
        }

        // Status icon tooltip - Priority: error > warning > success
        int statusIconX = leftPos + imageWidth - STATUS_ICON_MARGIN - STATUS_ICON_SIZE;
        int statusIconY = topPos + 4;
        if (mouseX >= statusIconX && mouseX < statusIconX + STATUS_ICON_SIZE && mouseY >= statusIconY && mouseY < statusIconY + STATUS_ICON_SIZE) {
            List<String> missingPatternItems = getMissingPatternItems();

            if (!menu.isNetworkConnected()) {
                guiGraphics.renderTooltip(font, Component.translatable("ae2_autorequester.gui.no_network"), mouseX, mouseY);
            } else if (!missingPatternItems.isEmpty()) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.translatable("ae2_autorequester.gui.missing_patterns").withStyle(s -> s.withColor(0xFFFF00)));
                for (String itemName : missingPatternItems) {
                    tooltip.add(Component.literal("  - " + itemName).withStyle(s -> s.withColor(0xAAAAAA)));
                }
                guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            } else {
                guiGraphics.renderTooltip(font, Component.translatable("ae2_autorequester.gui.network_connected"), mouseX, mouseY);
            }
        }
    }

    private List<Component> getStatusTooltip(CraftingRule rule) {
        RuleStatus status = rule.getStatus();
        List<Component> tooltip = new java.util.ArrayList<>();

        if (!rule.isEnabled()) {
            tooltip.add(Component.translatable("ae2_autorequester.gui.disabled").withStyle(s -> s.withColor(0x888888)));
        } else {
            tooltip.add(status.getDisplayName().copy().withStyle(s -> s.withColor(status.getColor())));

            // Add additional context based on status
            switch (status) {
                case MISSING_PATTERN -> tooltip.add(Component.translatable("ae2_autorequester.gui.no_pattern").withStyle(s -> s.withColor(0xAAAAAA)));
                case NO_CPU -> tooltip.add(Component.translatable("ae2_autorequester.gui.no_cpu").withStyle(s -> s.withColor(0xAAAAAA)));
                default -> { }
            }
        }

        return tooltip;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Scrollbar click
        if (mouseX >= leftPos + SCROLLBAR_X && mouseX < leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH &&
                mouseY >= topPos + RULE_LIST_Y && mouseY < topPos + RULE_LIST_Y + RULE_LIST_HEIGHT) {
            isDraggingScrollbar = true;
            updateScrollFromMouse(mouseY);
            return true;
        }

        // Check if clicked on rule list
        if (mouseX >= leftPos + RULE_LIST_X && mouseX < leftPos + RULE_LIST_X + RULE_LIST_WIDTH &&
                mouseY >= topPos + RULE_LIST_Y && mouseY < topPos + RULE_LIST_Y + RULE_LIST_HEIGHT) {

            int relativeY = (int) mouseY - (topPos + RULE_LIST_Y);
            int clickedIndex = relativeY / RULE_ENTRY_HEIGHT + scrollOffset;

            if (clickedIndex < menu.getRules().size()) {
                // Check for enable/disable toggle FIRST (right side of entry)
                int localX = (int) mouseX - leftPos - RULE_LIST_X - 2;
                if (localX >= RULE_LIST_WIDTH - TOGGLE_CLICK_THRESHOLD) {
                    // Toggle enabled state - don't trigger double-click
                    selectedRuleIndex = clickedIndex;
                    CraftingRule rule = menu.getRules().get(clickedIndex);
                    rule.setEnabled(!rule.isEnabled());
                    menu.updateRule(rule);
                    updateButtonStates();
                    ruleDoubleClick.reset();
                    return true;
                }

                // Handle click with double-click detection
                selectedRuleIndex = clickedIndex;
                updateButtonStates();
                if (ruleDoubleClick.onClick(clickedIndex)) {
                    onEditRule();
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
        List<CraftingRule> rules = menu.getRules();
        if (rules.size() <= MAX_VISIBLE_RULES) {
            scrollOffset = 0;
            return;
        }

        double relativeY = mouseY - (topPos + RULE_LIST_Y);
        double ratio = Math.max(0, Math.min(1, relativeY / RULE_LIST_HEIGHT));
        int maxScroll = rules.size() - MAX_VISIBLE_RULES;
        scrollOffset = (int) (ratio * maxScroll);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, menu.getRules().size() - MAX_VISIBLE_RULES);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) scrollY));
        return true;
    }

    private void onAddRule() {
        BlockPos pos = menu.getBlockEntity().getBlockPos();
        CraftingRule newRule = new CraftingRule();
        RuleEditorScreen.open(newRule, rule -> {
            LOG.info("[MainScreen] onAddRule callback received - Rule: name='{}', target={}, batchSize={}, conditions={}",
                    rule.getName(),
                    rule.getTargetItem() != null ? rule.getTargetItem().toString() : "null",
                    rule.getBatchSize(),
                    rule.getConditions().size());

            // Will be called after editor saves - rule operations happen in callback
            LOG.info("[MainScreen] Calling menu.addRule()");
            menu.addRule();

            List<CraftingRule> rules = menu.getRules();
            LOG.info("[MainScreen] Rules count after addRule: {}", rules.size());

            if (!rules.isEmpty()) {
                CraftingRule lastRule = rules.get(rules.size() - 1);
                LOG.info("[MainScreen] lastRule before copy - id={}, name='{}', target={}",
                        lastRule.getId(), lastRule.getName(),
                        lastRule.getTargetItem() != null ? lastRule.getTargetItem().toString() : "null");

                lastRule.setName(rule.getName());
                lastRule.setTargetItem(rule.getTargetItem());
                lastRule.setBatchSize(rule.getBatchSize());
                lastRule.getConditions().clear();
                rule.getConditions().forEach(lastRule::addCondition);

                LOG.info("[MainScreen] lastRule after copy - id={}, name='{}', target={}, batchSize={}, conditions={}",
                        lastRule.getId(), lastRule.getName(),
                        lastRule.getTargetItem() != null ? lastRule.getTargetItem().toString() : "null",
                        lastRule.getBatchSize(),
                        lastRule.getConditions().size());

                LOG.info("[MainScreen] Calling menu.updateRule()");
                menu.updateRule(lastRule);
            }
            // Set context to scroll to bottom after reopen
            AutorequesterScreen.setReopenContext(0, -1, true);
            // Request server to reopen the main screen
            LOG.info("[MainScreen] Sending OpenAutorequesterPacket to server");
            PacketDistributor.sendToServer(new OpenAutorequesterPacket(pos));
        }, () -> {
            // Cancel - preserve current state
            AutorequesterScreen.setReopenContext(scrollOffset, selectedRuleIndex, false);
            PacketDistributor.sendToServer(new OpenAutorequesterPacket(pos));
        }, Component.translatable("ae2_autorequester.gui.create_rule"));
    }

    private void onEditRule() {
        if (selectedRuleIndex >= 0 && selectedRuleIndex < menu.getRules().size()) {
            BlockPos pos = menu.getBlockEntity().getBlockPos();
            CraftingRule rule = menu.getRules().get(selectedRuleIndex);
            final int editingIndex = selectedRuleIndex;
            final int currentScroll = scrollOffset;
            RuleEditorScreen.open(rule, updatedRule -> {
                menu.updateRule(updatedRule);
                // Preserve current state
                AutorequesterScreen.setReopenContext(currentScroll, editingIndex, false);
                // Request server to reopen the main screen
                PacketDistributor.sendToServer(new OpenAutorequesterPacket(pos));
            }, () -> {
                // Cancel - preserve current state
                AutorequesterScreen.setReopenContext(currentScroll, editingIndex, false);
                PacketDistributor.sendToServer(new OpenAutorequesterPacket(pos));
            }, Component.translatable("ae2_autorequester.gui.edit_rule"));
        }
    }

    private void onDeleteRule() {
        if (selectedRuleIndex >= 0 && selectedRuleIndex < menu.getRules().size()) {
            menu.removeRule(selectedRuleIndex);
            if (selectedRuleIndex >= menu.getRules().size()) {
                selectedRuleIndex = menu.getRules().size() - 1;
            }
            updateButtonStates();
        }
    }

    private void onDuplicateRule() {
        if (selectedRuleIndex >= 0 && selectedRuleIndex < menu.getRules().size()) {
            menu.duplicateRule(selectedRuleIndex);
            selectedRuleIndex++;
            ensureRuleVisible(selectedRuleIndex);
            updateButtonStates();
        }
    }

    /**
     * Adjusts scroll offset to ensure the given rule index is visible.
     */
    private void ensureRuleVisible(int ruleIndex) {
        if (ruleIndex < scrollOffset) {
            // Rule is above visible area
            scrollOffset = ruleIndex;
        } else if (ruleIndex >= scrollOffset + MAX_VISIBLE_RULES) {
            // Rule is below visible area
            scrollOffset = ruleIndex - MAX_VISIBLE_RULES + 1;
        }
    }

    private void onMoveUp() {
        if (selectedRuleIndex > 0) {
            menu.moveRuleUp(selectedRuleIndex);
            selectedRuleIndex--;
            ensureRuleVisible(selectedRuleIndex);
        }
    }

    private void onMoveDown() {
        if (selectedRuleIndex >= 0 && selectedRuleIndex < menu.getRules().size() - 1) {
            menu.moveRuleDown(selectedRuleIndex);
            selectedRuleIndex++;
            ensureRuleVisible(selectedRuleIndex);
        }
    }

    /**
     * Get list of unique item names that are missing patterns (enabled rules with MISSING_PATTERN status).
     */
    private List<String> getMissingPatternItems() {
        // Use LinkedHashSet to deduplicate while preserving insertion order
        java.util.Set<String> items = new java.util.LinkedHashSet<>();
        for (CraftingRule rule : menu.getRules()) {
            if (rule.isEnabled() && rule.getStatus() == RuleStatus.MISSING_PATTERN) {
                items.add(rule.getDisplayName());
            }
        }
        return new ArrayList<>(items);
    }
}
