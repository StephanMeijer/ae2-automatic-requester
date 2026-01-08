package com.stephanmeijer.minecraft.ae2.autorequester.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Represents a crafting rule with a target item, batch size, and conditions.
 */
public class CraftingRule {
    private UUID id;
    private String name;
    private Item targetItem;
    private int batchSize;
    private boolean enabled;
    private final List<CraftingCondition> conditions;
    private RuleStatus status;
    private long lastTriggered;

    public CraftingRule() {
        this.id = UUID.randomUUID();
        this.name = "";
        this.targetItem = Items.AIR;
        this.batchSize = 64;
        this.enabled = false; // Default to disabled
        this.conditions = new ArrayList<>();
        this.status = RuleStatus.IDLE;
        this.lastTriggered = 0;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public String getDisplayName() {
        if (!name.isEmpty()) {
            return name;
        }
        if (targetItem != null && targetItem != Items.AIR) {
            return targetItem.getDescription().getString();
        }
        return "Empty Rule";
    }

    public Item getTargetItem() {
        return targetItem;
    }

    public void setTargetItem(Item targetItem) {
        this.targetItem = targetItem;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = Math.max(1, Math.min(batchSize, 10000)); // Min 1, max 10000
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<CraftingCondition> getConditions() {
        return conditions;
    }

    public void addCondition(CraftingCondition condition) {
        conditions.add(condition);
    }

    public void removeCondition(int index) {
        if (index >= 0 && index < conditions.size()) {
            conditions.remove(index);
        }
    }

    public void moveConditionUp(int index) {
        if (index > 0 && index < conditions.size()) {
            CraftingCondition condition = conditions.remove(index);
            conditions.add(index - 1, condition);
        }
    }

    public void moveConditionDown(int index) {
        if (index >= 0 && index < conditions.size() - 1) {
            CraftingCondition condition = conditions.remove(index);
            conditions.add(index + 1, condition);
        }
    }

    public RuleStatus getStatus() {
        return status;
    }

    public void setStatus(RuleStatus status) {
        this.status = status;
    }

    public long getLastTriggered() {
        return lastTriggered;
    }

    public void setLastTriggered(long lastTriggered) {
        this.lastTriggered = lastTriggered;
    }

    /**
     * A rule is valid if it has a target item.
     * Rules with no conditions are valid and will trigger unconditionally.
     */
    public boolean isValid() {
        return targetItem != null && targetItem != Items.AIR;
    }

    public boolean hasValidConditions() {
        return conditions.stream().allMatch(CraftingCondition::isValid);
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();

        tag.putUUID("id", id);
        tag.putString("name", name);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(targetItem);
        tag.putString("targetItem", itemId.toString());
        tag.putInt("batchSize", batchSize);
        tag.putBoolean("enabled", enabled);
        tag.putString("status", status.name());
        tag.putLong("lastTriggered", lastTriggered);

        ListTag conditionList = new ListTag();
        for (CraftingCondition condition : conditions) {
            conditionList.add(condition.toNbt());
        }
        tag.put("conditions", conditionList);

        return tag;
    }

    public static CraftingRule fromNbt(CompoundTag tag) {
        CraftingRule rule = new CraftingRule();

        if (tag.hasUUID("id")) {
            rule.id = tag.getUUID("id");
        }
        rule.name = tag.getString("name");
        String itemId = tag.getString("targetItem");
        rule.targetItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        rule.batchSize = tag.getInt("batchSize");
        rule.enabled = tag.getBoolean("enabled");
        rule.status = RuleStatus.fromName(tag.getString("status"));
        rule.lastTriggered = tag.getLong("lastTriggered");

        ListTag conditionList = tag.getList("conditions", Tag.TAG_COMPOUND);
        for (int i = 0; i < conditionList.size(); i++) {
            rule.conditions.add(CraftingCondition.fromNbt(conditionList.getCompound(i)));
        }

        return rule;
    }

    /**
     * Creates a copy for duplication (new UUID, disabled by default).
     */
    public CraftingRule copy() {
        CraftingRule copy = new CraftingRule();
        copy.id = UUID.randomUUID(); // New ID for copy
        copy.name = this.name.isEmpty() ? "" : this.name + " (Copy)";
        copy.targetItem = this.targetItem;
        copy.batchSize = this.batchSize;
        copy.enabled = false; // Disabled by default when duplicated
        copy.status = RuleStatus.IDLE;
        copy.lastTriggered = 0;

        for (CraftingCondition condition : this.conditions) {
            copy.conditions.add(condition.copy());
        }

        return copy;
    }

    /**
     * Creates a copy for editing (preserves UUID so updates work).
     */
    public CraftingRule copyForEditing() {
        CraftingRule copy = new CraftingRule();
        copy.id = this.id; // Preserve ID for updating
        copy.name = this.name;
        copy.targetItem = this.targetItem;
        copy.batchSize = this.batchSize;
        copy.enabled = this.enabled;
        copy.status = this.status;
        copy.lastTriggered = this.lastTriggered;

        for (CraftingCondition condition : this.conditions) {
            copy.conditions.add(condition.copy());
        }

        return copy;
    }

    public ItemStack getTargetItemStack() {
        return new ItemStack(targetItem);
    }
}
