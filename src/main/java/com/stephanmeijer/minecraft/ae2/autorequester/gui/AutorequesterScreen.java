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

    // Rule list area
    private static final int RULE_LIST_X = 8;
    private static final int RULE_LIST_Y = 18;
    private static final int RULE_LIST_WIDTH = 228;
    private static final int RULE_ENTRY_HEIGHT = 24;
    private static final int MAX_VISIBLE_RULES = 7;

    // Scrollbar
    private static final int SCROLLBAR_X = RULE_LIST_X + RULE_LIST_WIDTH + 2;
    private static final int SCROLLBAR_WIDTH = 10;

    private int scrollOffset = 0;
    private int selectedRuleIndex = -1;
    private final DoubleClickHandler ruleDoubleClick = new DoubleClickHandler();
    private boolean isDraggingScrollbar = false;

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

        int buttonY = topPos + imageHeight - 28;

        // Add Rule button
        addRuleButton = addRenderableWidget(Button.builder(Component.literal("+"), button -> onAddRule())
                .bounds(leftPos + 8, buttonY, 20, 20)
                .build());

        // Edit button
        editButton = addRenderableWidget(Button.builder(Component.literal("✎"), button -> onEditRule())
                .bounds(leftPos + 32, buttonY, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("ae2_autorequester.gui.edit_rule")))
                .build());

        // Delete button
        deleteButton = addRenderableWidget(Button.builder(Component.literal("✕"), button -> onDeleteRule())
                .bounds(leftPos + 56, buttonY, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("ae2_autorequester.gui.delete_rule")))
                .build());

        // Duplicate button
        duplicateButton = addRenderableWidget(Button.builder(Component.literal("⧉"), button -> onDuplicateRule())
                .bounds(leftPos + 80, buttonY, 20, 20)
                .build());

        // Move up button
        moveUpButton = addRenderableWidget(Button.builder(Component.literal("▲"), button -> onMoveUp())
                .bounds(leftPos + 110, buttonY, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("ae2_autorequester.gui.move_up")))
                .build());

        // Move down button
        moveDownButton = addRenderableWidget(Button.builder(Component.literal("▼"), button -> onMoveDown())
                .bounds(leftPos + 134, buttonY, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("ae2_autorequester.gui.move_down")))
                .build());

        // Initial button state update
        updateButtonStates();
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
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFF373737);

        // Draw title bar
        guiGraphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + 16, 0xFF1E1E1E);

        // Draw rule list background
        int listHeight = MAX_VISIBLE_RULES * RULE_ENTRY_HEIGHT;
        guiGraphics.fill(leftPos + RULE_LIST_X, topPos + RULE_LIST_Y,
                leftPos + RULE_LIST_X + RULE_LIST_WIDTH, topPos + RULE_LIST_Y + listHeight,
                0xFF1E1E1E);

        // Draw scrollbar track
        guiGraphics.fill(leftPos + SCROLLBAR_X, topPos + RULE_LIST_Y,
                leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH, topPos + RULE_LIST_Y + listHeight,
                0xFF2A2A2A);

        // Draw scrollbar thumb
        List<CraftingRule> rules = menu.getRules();
        if (rules.size() > MAX_VISIBLE_RULES) {
            int maxScroll = rules.size() - MAX_VISIBLE_RULES;
            int thumbHeight = Math.max(20, listHeight * MAX_VISIBLE_RULES / rules.size());
            int thumbY = topPos + RULE_LIST_Y + (listHeight - thumbHeight) * scrollOffset / maxScroll;
            guiGraphics.fill(leftPos + SCROLLBAR_X + 1, thumbY,
                    leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight,
                    0xFF6A6A6A);
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
                        0xFF4A4A4A);
            }

            // Draw rule entry
            renderRuleEntry(guiGraphics, rule, leftPos + RULE_LIST_X + 2, y + 2, mouseX, mouseY);
        }

        // Status icon (top-right corner, 12x12) - Priority: error > warning > success
        int statusIconX = leftPos + imageWidth - 20;
        int statusIconY = topPos + 4;
        List<String> missingPatternItems = getMissingPatternItems();

        if (!menu.isNetworkConnected()) {
            // Error icon (red) - network disconnected
            long time = System.currentTimeMillis();
            float pulse = (float) (Math.sin(time / 200.0) * 0.3 + 0.7);
            int red = (int) (255 * pulse);
            int errorColor = 0xFF000000 | (red << 16) | 0x2222;

            guiGraphics.fill(statusIconX, statusIconY, statusIconX + 12, statusIconY + 12, 0xFF000000);
            guiGraphics.fill(statusIconX + 1, statusIconY + 1, statusIconX + 11, statusIconY + 11, errorColor);
            guiGraphics.drawCenteredString(font, "!", statusIconX + 6, statusIconY + 2, 0xFFFFFF);
        } else if (!missingPatternItems.isEmpty()) {
            // Warning icon (yellow) - missing patterns
            long time = System.currentTimeMillis();
            float pulse = (float) (Math.sin(time / 300.0) * 0.2 + 0.8);
            int yellow = (int) (255 * pulse);
            int warningColor = 0xFF000000 | (yellow << 16) | (yellow << 8);

            guiGraphics.fill(statusIconX, statusIconY, statusIconX + 12, statusIconY + 12, 0xFF000000);
            guiGraphics.fill(statusIconX + 1, statusIconY + 1, statusIconX + 11, statusIconY + 11, warningColor);
            guiGraphics.drawCenteredString(font, "!", statusIconX + 6, statusIconY + 2, 0x000000);
        } else {
            // Success icon (green) - all good
            guiGraphics.fill(statusIconX, statusIconY, statusIconX + 12, statusIconY + 12, 0xFF000000);
            guiGraphics.fill(statusIconX + 1, statusIconY + 1, statusIconX + 11, statusIconY + 11, 0xFF55FF55);
        }

        // Empty list message
        if (rules.isEmpty()) {
            guiGraphics.drawCenteredString(font,
                    Component.translatable("ae2_autorequester.gui.no_rules"),
                    leftPos + RULE_LIST_X + RULE_LIST_WIDTH / 2,
                    topPos + RULE_LIST_Y + (MAX_VISIBLE_RULES * RULE_ENTRY_HEIGHT) / 2,
                    0x888888);
        }
    }

    private void renderRuleEntry(GuiGraphics guiGraphics, CraftingRule rule, int x, int y, int mouseX, int mouseY) {
        // Status icon (colored square) - green for enabled, red for disabled
        int statusColor = rule.isEnabled() ? 0x55FF55 : 0xFF5555;
        guiGraphics.fill(x, y + 4, x + 12, y + 16, 0xFF000000);
        guiGraphics.fill(x + 1, y + 5, x + 11, y + 15, 0xFF000000 | statusColor);

        // Target item icon
        if (rule.getTargetItem() != null && !rule.getTargetItemStack().isEmpty()) {
            guiGraphics.renderItem(rule.getTargetItemStack(), x + 16, y + 2);
        }

        // Rule name/target
        String displayName = rule.getDisplayName();
        if (displayName.length() > 20) {
            displayName = displayName.substring(0, 17) + "...";
        }
        int textColor = rule.isEnabled() ? 0xFFFFFF : 0x888888;
        guiGraphics.drawString(font, displayName, x + 36, y + 2, textColor);

        // Condition count and batch size
        String info = rule.getConditions().size() + " cond. | ×" + rule.getBatchSize();
        guiGraphics.drawString(font, info, x + 36, y + 12, 0x888888);

        // Enable/disable indicator - colored box with text
        int toggleX = x + RULE_LIST_WIDTH - 32;
        int toggleY = y + 4;
        int toggleColor = rule.isEnabled() ? 0xFF55FF55 : 0xFF555555;
        guiGraphics.fill(toggleX, toggleY, toggleX + 28, toggleY + 14, 0xFF000000);
        guiGraphics.fill(toggleX + 1, toggleY + 1, toggleX + 27, toggleY + 13, toggleColor);
        String enableText = rule.isEnabled() ? "ON" : "OFF";
        guiGraphics.drawCenteredString(font, enableText, toggleX + 14, toggleY + 3, 0xFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 6, 0xFFFFFF);
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
            if (mouseX >= x && mouseX < x + 12 && mouseY >= y + 4 && mouseY < y + 16) {
                guiGraphics.renderComponentTooltip(font, getStatusTooltip(rule), mouseX, mouseY);
            }

            // Target item tooltip
            if (mouseX >= x + 16 && mouseX < x + 32 && mouseY >= y + 2 && mouseY < y + 18) {
                if (!rule.getTargetItemStack().isEmpty()) {
                    guiGraphics.renderTooltip(font, rule.getTargetItemStack(), mouseX, mouseY);
                }
            }
        }

        // Status icon tooltip - Priority: error > warning > success
        int statusIconX = leftPos + imageWidth - 20;
        int statusIconY = topPos + 4;
        if (mouseX >= statusIconX && mouseX < statusIconX + 12 && mouseY >= statusIconY && mouseY < statusIconY + 12) {
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
        int listHeight = MAX_VISIBLE_RULES * RULE_ENTRY_HEIGHT;

        // Scrollbar click
        if (mouseX >= leftPos + SCROLLBAR_X && mouseX < leftPos + SCROLLBAR_X + SCROLLBAR_WIDTH &&
                mouseY >= topPos + RULE_LIST_Y && mouseY < topPos + RULE_LIST_Y + listHeight) {
            isDraggingScrollbar = true;
            updateScrollFromMouse(mouseY);
            return true;
        }

        // Check if clicked on rule list
        if (mouseX >= leftPos + RULE_LIST_X && mouseX < leftPos + RULE_LIST_X + RULE_LIST_WIDTH &&
                mouseY >= topPos + RULE_LIST_Y && mouseY < topPos + RULE_LIST_Y + listHeight) {

            int relativeY = (int) mouseY - (topPos + RULE_LIST_Y);
            int clickedIndex = relativeY / RULE_ENTRY_HEIGHT + scrollOffset;

            if (clickedIndex < menu.getRules().size()) {
                // Check for enable/disable toggle FIRST (right side of entry)
                int localX = (int) mouseX - leftPos - RULE_LIST_X - 2;
                if (localX >= RULE_LIST_WIDTH - 35) {
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

        int listHeight = MAX_VISIBLE_RULES * RULE_ENTRY_HEIGHT;
        double relativeY = mouseY - (topPos + RULE_LIST_Y);
        double ratio = Math.max(0, Math.min(1, relativeY / listHeight));
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
            // Request server to reopen the main screen
            LOG.info("[MainScreen] Sending OpenAutorequesterPacket to server");
            PacketDistributor.sendToServer(new OpenAutorequesterPacket(pos));
        }, () -> {
            // Cancel - just reopen main screen
            PacketDistributor.sendToServer(new OpenAutorequesterPacket(pos));
        }, Component.translatable("ae2_autorequester.gui.create_rule"));
    }

    private void onEditRule() {
        if (selectedRuleIndex >= 0 && selectedRuleIndex < menu.getRules().size()) {
            BlockPos pos = menu.getBlockEntity().getBlockPos();
            CraftingRule rule = menu.getRules().get(selectedRuleIndex);
            RuleEditorScreen.open(rule, updatedRule -> {
                menu.updateRule(updatedRule);
                // Request server to reopen the main screen
                PacketDistributor.sendToServer(new OpenAutorequesterPacket(pos));
            }, () -> {
                // Cancel - just reopen main screen
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
            updateButtonStates();
        }
    }

    private void onMoveUp() {
        if (selectedRuleIndex > 0) {
            menu.moveRuleUp(selectedRuleIndex);
            selectedRuleIndex--;
        }
    }

    private void onMoveDown() {
        if (selectedRuleIndex >= 0 && selectedRuleIndex < menu.getRules().size() - 1) {
            menu.moveRuleDown(selectedRuleIndex);
            selectedRuleIndex++;
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
