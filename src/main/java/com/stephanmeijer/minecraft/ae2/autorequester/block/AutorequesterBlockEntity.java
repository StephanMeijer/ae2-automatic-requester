package com.stephanmeijer.minecraft.ae2.autorequester.block;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.IStackWatcher;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.CraftingSubmitErrorCode;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.util.AECableType;
import com.google.common.collect.ImmutableSet;

import com.stephanmeijer.minecraft.ae2.autorequester.AutorequesterConfig;
import com.stephanmeijer.minecraft.ae2.autorequester.ModBlocks;
import com.stephanmeijer.minecraft.ae2.autorequester.data.CraftingCondition;
import com.stephanmeijer.minecraft.ae2.autorequester.data.CraftingRule;
import com.stephanmeijer.minecraft.ae2.autorequester.data.RuleStatus;
import com.stephanmeijer.minecraft.ae2.autorequester.gui.AutorequesterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

public class AutorequesterBlockEntity extends BlockEntity implements MenuProvider, IInWorldGridNodeHost, IStorageWatcherNode, ICraftingRequester, ICraftingSimulationRequester {
    private static final Logger LOG = LoggerFactory.getLogger(AutorequesterBlockEntity.class);

    // ==================== Grid Node Listener ====================

    /**
     * Listener for grid node state changes.
     * Implemented as a singleton to avoid creating unnecessary objects.
     */
    private static final IGridNodeListener<AutorequesterBlockEntity> NODE_LISTENER = new IGridNodeListener<>() {
        @Override
        public void onStateChanged(AutorequesterBlockEntity owner, IGridNode node, State state) {
            owner.onGridStateChanged();
        }

        @Override
        public void onGridChanged(AutorequesterBlockEntity owner, IGridNode node) {
            // Called when grid topology changes (e.g., cable removed/added)
            owner.onGridStateChanged();
        }

        @Override
        public void onSaveChanges(AutorequesterBlockEntity owner, IGridNode node) {
            owner.setChanged();
        }
    };

    // ==================== Instance Fields ====================

    private final List<CraftingRule> rules = new ArrayList<>();
    private boolean gridReady = false;

    // AE2 Grid Node - manages connection to ME network
    private final IManagedGridNode mainNode;

    // Stack watcher - receives notifications when watched items change
    @Nullable
    private IStackWatcher stackWatcher;

    // Track which items we're watching (targets and condition items)
    private final Set<AEKey> watchedKeys = new HashSet<>();

    // Action source for ME operations
    private final IActionSource actionSource;

    // Track active crafting jobs: rule ID -> crafting link
    private final Map<UUID, ICraftingLink> activeCraftingJobs = new HashMap<>();

    // Track pending crafting calculations
    private final Map<UUID, Future<ICraftingPlan>> pendingCalculations = new HashMap<>();

    // Tick counter for throttled operations
    private int tickCounter = 0;
    private static final int CRAFTING_CHECK_INTERVAL = 20; // Check crafting every 20 ticks (1 second)

    public AutorequesterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlocks.AUTOREQUESTER_BLOCK_ENTITY.get(), pos, blockState);

        // Create the managed grid node
        this.mainNode = GridHelper.createManagedNode(this, NODE_LISTENER)
                .setVisualRepresentation(ModBlocks.AUTOREQUESTER.get())
                .setInWorldNode(true)
                .setTagName("node")
                .setIdlePowerUsage(5.0) // 5 AE/t idle power draw
                .addService(IStorageWatcherNode.class, this)
                .addService(ICraftingRequester.class, this);

        // Create action source for this block entity
        this.actionSource = IActionSource.ofMachine(mainNode::getNode);
    }

    // ==================== Grid Node Lifecycle ====================

    /**
     * Called when the grid node's state changes (connected, disconnected, topology change, etc.)
     */
    private void onGridStateChanged() {
        boolean currentlyReady = mainNode.isReady();
        LOG.debug("[Autorequester] Grid state changed: ready={}", currentlyReady);

        boolean wasReady = gridReady;
        gridReady = currentlyReady;

        if (gridReady && !wasReady) {
            LOG.info("[Autorequester] Connected to ME network at {}", worldPosition);
            updateWatchedItems();
            evaluateAllRules();
        } else if (!gridReady && wasReady) {
            LOG.info("[Autorequester] Disconnected from ME network at {}", worldPosition);
            // Mark all rules as error when disconnected
            for (CraftingRule rule : rules) {
                rule.setStatus(RuleStatus.ERROR);
            }
        }

        markDirtyAndSync();
    }

    /**
     * Called when block entity is ready to connect to the grid.
     * Use GridHelper.onFirstTick to defer this to the first tick.
     */
    public void onReady() {
        if (level != null && !level.isClientSide()) {
            mainNode.create(level, worldPosition);
        }
    }

    /**
     * Gets the cable connection type for a given side.
     */
    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.SMART;
    }

    /**
     * Gets the grid node exposed on the given side, or null if not exposed.
     */
    @Override
    @Nullable
    public IGridNode getGridNode(Direction dir) {
        return mainNode.getNode();
    }

    public IManagedGridNode getMainNode() {
        return mainNode;
    }

    // ==================== IStorageWatcherNode Implementation ====================

    @Override
    public void updateWatcher(IStackWatcher newWatcher) {
        this.stackWatcher = newWatcher;
        updateWatchedItems();
    }

    @Override
    public void onStackChange(AEKey what, long amount) {
        LOG.debug("[Autorequester] Stack changed: {} = {}", what, amount);

        // Re-evaluate rules that might be affected by this change
        for (CraftingRule rule : rules) {
            if (!rule.isEnabled() || !rule.isValid()) {
                continue;
            }

            // Check if this rule cares about this item
            if (isRelevantToRule(rule, what)) {
                evaluateRule(rule);
            }
        }
    }

    /**
     * Updates the stack watcher to monitor items relevant to our rules.
     * Called when rules change or when we connect to the grid.
     */
    private void updateWatchedItems() {
        if (stackWatcher == null) {
            return;
        }

        // Clear existing watches
        stackWatcher.reset();
        watchedKeys.clear();

        // Watch all items that appear in rules (targets and conditions)
        for (CraftingRule rule : rules) {
            if (!rule.isEnabled()) {
                continue;
            }

            // Watch the target item
            Item targetItem = rule.getTargetItem();
            if (targetItem != null) {
                AEKey targetKey = AEItemKey.of(targetItem.getDefaultInstance());
                if (targetKey != null && !watchedKeys.contains(targetKey)) {
                    stackWatcher.add(targetKey);
                    watchedKeys.add(targetKey);
                    LOG.debug("[Autorequester] Watching target item: {}", targetKey);
                }
            }

            // Watch all items in conditions
            for (CraftingCondition condition : rule.getConditions()) {
                Item conditionItem = condition.getItem();
                if (conditionItem != null) {
                    AEKey conditionKey = AEItemKey.of(conditionItem.getDefaultInstance());
                    if (conditionKey != null && !watchedKeys.contains(conditionKey)) {
                        stackWatcher.add(conditionKey);
                        watchedKeys.add(conditionKey);
                        LOG.debug("[Autorequester] Watching condition item: {}", conditionKey);
                    }
                }
            }
        }

        LOG.info("[Autorequester] Updated watcher with {} items", watchedKeys.size());
    }

    /**
     * Checks if a given AEKey is relevant to a rule (either as target or in conditions).
     */
    private boolean isRelevantToRule(CraftingRule rule, AEKey key) {
        if (!(key instanceof AEItemKey itemKey)) {
            return false;
        }

        Item item = itemKey.getItem();

        // Check if it's the target item
        if (item.equals(rule.getTargetItem())) {
            return true;
        }

        // Check if it's in any condition
        for (CraftingCondition condition : rule.getConditions()) {
            if (item.equals(condition.getItem())) {
                return true;
            }
        }

        return false;
    }

    // ==================== Rule Evaluation ====================

    /**
     * Evaluates all rules. Called when connecting to the grid.
     */
    private void evaluateAllRules() {
        for (CraftingRule rule : rules) {
            if (rule.isEnabled() && rule.isValid()) {
                evaluateRule(rule);
            } else if (!rule.isEnabled()) {
                rule.setStatus(RuleStatus.IDLE);
            }
        }
    }

    /**
     * Evaluates a single rule and updates its status.
     */
    private void evaluateRule(CraftingRule rule) {
        if (!gridReady) {
            rule.setStatus(RuleStatus.ERROR);
            return;
        }

        if (!rule.isValid()) {
            rule.setStatus(RuleStatus.IDLE);
            return;
        }

        var node = mainNode.getNode();
        if (node == null || node.getGrid() == null) {
            rule.setStatus(RuleStatus.ERROR);
            return;
        }

        // Check if already crafting this item
        ICraftingLink activeJob = activeCraftingJobs.get(rule.getId());
        if (activeJob != null && !activeJob.isDone()) {
            rule.setStatus(RuleStatus.CRAFTING);
            return;
        }

        // Clean up completed job
        if (activeJob != null && activeJob.isDone()) {
            activeCraftingJobs.remove(rule.getId());
        }

        // Evaluate conditions
        boolean conditionsMet = evaluateConditions(rule);
        if (!conditionsMet) {
            rule.setStatus(RuleStatus.CONDITIONS_NOT_MET);
            return;
        }

        // Check for pattern availability
        AEKey targetKey = AEItemKey.of(rule.getTargetItem().getDefaultInstance());
        if (targetKey == null) {
            rule.setStatus(RuleStatus.MISSING_PATTERN);
            return;
        }

        ICraftingService craftingService = node.getGrid().getCraftingService();
        if (!craftingService.isCraftable(targetKey)) {
            rule.setStatus(RuleStatus.MISSING_PATTERN);
            return;
        }

        // Check if we're already calculating for this rule
        Future<ICraftingPlan> pendingCalc = pendingCalculations.get(rule.getId());
        if (pendingCalc != null) {
            if (pendingCalc.isDone()) {
                pendingCalculations.remove(rule.getId());
                try {
                    ICraftingPlan plan = pendingCalc.get();
                    submitCraftingJob(rule, plan);
                } catch (Exception e) {
                    LOG.warn("[Autorequester] Crafting calculation failed for rule '{}': {}",
                            rule.getName(), e.getMessage());
                    rule.setStatus(RuleStatus.ERROR);
                }
            } else {
                // Still calculating
                rule.setStatus(RuleStatus.READY);
            }
            return;
        }

        // Start crafting calculation
        startCraftingCalculation(rule, targetKey, craftingService);
        rule.setStatus(RuleStatus.READY);
    }

    /**
     * Starts an async crafting calculation for a rule.
     */
    private void startCraftingCalculation(CraftingRule rule, AEKey targetKey, ICraftingService craftingService) {
        long amount = rule.getBatchSize();

        LOG.info("[Autorequester] Starting crafting calculation for {} x{} (rule: {})",
                targetKey, amount, rule.getName());

        Future<ICraftingPlan> calculation = craftingService.beginCraftingCalculation(
                level,
                this, // ICraftingSimulationRequester
                targetKey,
                amount,
                CalculationStrategy.CRAFT_LESS
        );

        pendingCalculations.put(rule.getId(), calculation);
    }

    /**
     * Submits a crafting job from a completed plan.
     */
    private void submitCraftingJob(CraftingRule rule, ICraftingPlan plan) {
        var node = mainNode.getNode();
        if (node == null || node.getGrid() == null) {
            return;
        }

        if (plan.simulation()) {
            LOG.debug("[Autorequester] Crafting plan is simulation-only, cannot submit");
            return;
        }

        if (plan.missingItems().isEmpty() == false) {
            LOG.debug("[Autorequester] Crafting plan has missing items, cannot submit");
            rule.setStatus(RuleStatus.MISSING_PATTERN);
            return;
        }

        ICraftingService craftingService = node.getGrid().getCraftingService();
        var result = craftingService.submitJob(
                plan,
                this, // ICraftingRequester
                null, // No specific CPU
                true, // Prioritize power
                actionSource
        );

        if (result.successful()) {
            ICraftingLink link = result.link();
            if (link != null) {
                activeCraftingJobs.put(rule.getId(), link);
                rule.setStatus(RuleStatus.CRAFTING);
                LOG.info("[Autorequester] Started crafting job for rule '{}'", rule.getName());
            }
        } else {
            LOG.warn("[Autorequester] Failed to submit crafting job for rule '{}': {}",
                    rule.getName(), result.errorCode());
            if (result.errorCode() == CraftingSubmitErrorCode.NO_CPU_FOUND) {
                rule.setStatus(RuleStatus.NO_CPU);
            } else {
                rule.setStatus(RuleStatus.ERROR);
            }
        }
    }

    // ==================== ICraftingSimulationRequester Implementation ====================

    @Override
    @Nullable
    public IActionSource getActionSource() {
        return actionSource;
    }

    // ==================== ICraftingRequester Implementation ====================

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return ImmutableSet.copyOf(activeCraftingJobs.values());
    }

    @Override
    public long insertCraftedItems(ICraftingLink link, AEKey what, long amount, Actionable mode) {
        // We don't need the crafted items - they go directly to ME storage
        // This is called when items are ready to be delivered to the requester
        // Since we just want them in the network, we return 0 (accept nothing)
        // The items will stay in the network storage
        return 0;
    }

    @Override
    public void jobStateChange(ICraftingLink link) {
        // Find which rule this link belongs to and update its status
        UUID ruleId = null;
        for (Map.Entry<UUID, ICraftingLink> entry : activeCraftingJobs.entrySet()) {
            if (entry.getValue() == link) {
                ruleId = entry.getKey();
                break;
            }
        }

        if (ruleId != null) {
            activeCraftingJobs.remove(ruleId);

            if (link.isCanceled()) {
                LOG.info("[Autorequester] Crafting job canceled for rule");
            } else {
                LOG.info("[Autorequester] Crafting job completed for rule");
            }

            // Find the rule and update its status
            for (CraftingRule rule : rules) {
                if (rule.getId().equals(ruleId)) {
                    // Re-evaluate to see if we need to craft more
                    evaluateRule(rule);
                    break;
                }
            }
        }
    }

    @Override
    public IGridNode getActionableNode() {
        return mainNode.getNode();
    }

    /**
     * Evaluates all conditions for a rule.
     * Returns true if all conditions are satisfied.
     */
    private boolean evaluateConditions(CraftingRule rule) {
        var node = mainNode.getNode();
        if (node == null || node.getGrid() == null) {
            return false;
        }

        var storageService = node.getGrid().getStorageService();
        var cachedInventory = storageService.getCachedInventory();

        for (CraftingCondition condition : rule.getConditions()) {
            Item item = condition.getItem();
            if (item == null) {
                continue;
            }

            AEKey key = AEItemKey.of(item.getDefaultInstance());
            if (key == null) {
                continue;
            }

            long currentAmount = cachedInventory.get(key);
            long threshold = condition.getThreshold();

            boolean satisfied = condition.getOperator().evaluate(currentAmount, threshold);
            if (!satisfied) {
                LOG.debug("[Autorequester] Condition not met: {} {} {} (current: {})",
                        item, condition.getOperator().getSymbol(), threshold, currentAmount);
                return false;
            }
        }

        return true;
    }

    // ==================== Tick (Minimal - mostly event-driven) ====================

    public void serverTick() {
        tickCounter++;

        // Throttle crafting operations to every 20 ticks (1 second)
        if (tickCounter >= CRAFTING_CHECK_INTERVAL) {
            tickCounter = 0;

            // Check pending crafting calculations (async completions)
            if (!pendingCalculations.isEmpty()) {
                checkPendingCalculations();
            }

            // Periodic rule evaluation as fallback for missed storage events
            if (gridReady) {
                evaluateAllRules();
            }
        }
    }

    /**
     * Checks pending async crafting calculations and submits completed ones.
     * This is needed because calculations complete asynchronously and we need
     * to poll for their completion.
     */
    private void checkPendingCalculations() {
        // Iterate over a copy to avoid ConcurrentModificationException
        for (var entry : List.copyOf(pendingCalculations.entrySet())) {
            UUID ruleId = entry.getKey();
            Future<ICraftingPlan> calculation = entry.getValue();

            if (calculation.isDone()) {
                pendingCalculations.remove(ruleId);

                // Find the rule
                CraftingRule rule = null;
                for (CraftingRule r : rules) {
                    if (r.getId().equals(ruleId)) {
                        rule = r;
                        break;
                    }
                }

                if (rule == null || !rule.isEnabled()) {
                    continue;
                }

                try {
                    ICraftingPlan plan = calculation.get();
                    submitCraftingJob(rule, plan);
                } catch (Exception e) {
                    LOG.warn("[Autorequester] Crafting calculation failed for rule '{}': {}",
                            rule.getName(), e.getMessage());
                    rule.setStatus(RuleStatus.ERROR);
                }
            }
        }
    }

    // Rule management methods
    public List<CraftingRule> getRules() {
        return rules;
    }

    public void addRule(CraftingRule rule) {
        if (rules.size() < AutorequesterConfig.maxRulesPerBlock) {
            rules.add(rule);
            onRulesChanged();
        }
    }

    public void removeRule(UUID ruleId) {
        rules.removeIf(r -> r.getId().equals(ruleId));
        onRulesChanged();
    }

    public void removeRule(int index) {
        if (index >= 0 && index < rules.size()) {
            rules.remove(index);
            onRulesChanged();
        }
    }

    public void updateRule(CraftingRule rule) {
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).getId().equals(rule.getId())) {
                rules.set(i, rule);
                onRulesChanged();
                return;
            }
        }
    }

    /**
     * Called from network packet to replace all rules.
     * This runs on the server side.
     */
    public void setRulesFromPacket(List<CraftingRule> newRules) {
        LOG.info("[BlockEntity] setRulesFromPacket - receiving {} rules", newRules.size());
        rules.clear();
        for (CraftingRule rule : newRules) {
            if (rules.size() < AutorequesterConfig.maxRulesPerBlock) {
                rules.add(rule);
            }
        }
        LOG.info("[BlockEntity] After setRulesFromPacket - have {} rules", rules.size());
        for (int i = 0; i < rules.size(); i++) {
            CraftingRule r = rules.get(i);
            LOG.info("[BlockEntity]   Rule[{}]: id={}, name='{}', target={}, batchSize={}, conditions={}",
                    i, r.getId(), r.getName(),
                    r.getTargetItem() != null ? r.getTargetItem().toString() : "null",
                    r.getBatchSize(), r.getConditions().size());
        }
        onRulesChanged();
    }

    /**
     * Called whenever rules change to update watchers and re-evaluate.
     */
    private void onRulesChanged() {
        markDirtyAndSync();
        updateWatchedItems();
        evaluateAllRules();
    }

    public void moveRuleUp(int index) {
        if (index > 0 && index < rules.size()) {
            CraftingRule rule = rules.remove(index);
            rules.add(index - 1, rule);
            onRulesChanged();
        }
    }

    public void moveRuleDown(int index) {
        if (index >= 0 && index < rules.size() - 1) {
            CraftingRule rule = rules.remove(index);
            rules.add(index + 1, rule);
            onRulesChanged();
        }
    }

    public void duplicateRule(int index) {
        if (index >= 0 && index < rules.size() && rules.size() < AutorequesterConfig.maxRulesPerBlock) {
            CraftingRule copy = rules.get(index).copy();
            rules.add(index + 1, copy);
            onRulesChanged();
        }
    }

    public boolean isNetworkConnected() {
        // On server: query directly; on client: use synced cached value
        if (level != null && !level.isClientSide()) {
            return mainNode.isReady();
        }
        return gridReady;
    }

    /**
     * Marks block as changed and syncs to clients.
     */
    private void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // NBT persistence
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // Save rules
        ListTag ruleList = new ListTag();
        for (CraftingRule rule : rules) {
            ruleList.add(rule.toNbt());
        }
        tag.put("rules", ruleList);

        // Save connection status for client sync
        tag.putBoolean("gridReady", gridReady);

        // Save grid node data
        mainNode.saveToNBT(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // Load rules
        rules.clear();
        ListTag ruleList = tag.getList("rules", Tag.TAG_COMPOUND);
        for (int i = 0; i < ruleList.size(); i++) {
            rules.add(CraftingRule.fromNbt(ruleList.getCompound(i)));
        }

        // Load connection status (for client sync)
        gridReady = tag.getBoolean("gridReady");

        // Load grid node data
        mainNode.loadFromNBT(tag);
    }

    // Client-server sync methods
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // Save rules to item NBT when block is broken
    public CompoundTag saveToItem() {
        CompoundTag tag = new CompoundTag();
        ListTag ruleList = new ListTag();
        for (CraftingRule rule : rules) {
            ruleList.add(rule.toNbt());
        }
        tag.put("rules", ruleList);
        return tag;
    }

    public void loadFromItem(CompoundTag tag) {
        rules.clear();
        if (tag.contains("rules")) {
            ListTag ruleList = tag.getList("rules", Tag.TAG_COMPOUND);
            for (int i = 0; i < ruleList.size(); i++) {
                rules.add(CraftingRule.fromNbt(ruleList.getCompound(i)));
            }
        }
    }

    // ==================== Block Entity Lifecycle ====================

    @Override
    public void setRemoved() {
        super.setRemoved();
        mainNode.destroy();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        // Schedule grid node creation for first tick
        GridHelper.onFirstTick(this, AutorequesterBlockEntity::onReady);
    }

    public void onChunkUnloaded() {
        mainNode.destroy();
    }

    public void onRemoved() {
        mainNode.destroy();
    }

    // Menu handling
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.ae2_autorequester.autorequester");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AutorequesterMenu(containerId, playerInventory, this);
    }

    public void openMenu(ServerPlayer player) {
        player.openMenu(this, buf -> buf.writeBlockPos(worldPosition));
    }
}
