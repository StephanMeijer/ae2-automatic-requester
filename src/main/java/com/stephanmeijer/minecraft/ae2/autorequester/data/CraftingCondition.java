package com.stephanmeijer.minecraft.ae2.autorequester.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Represents a single condition for a crafting rule.
 * A condition checks if an item count in the ME network matches a comparison.
 */
public class CraftingCondition {
    private Item item;
    private ComparisonOperator operator;
    private long threshold;

    public CraftingCondition() {
        this.item = Items.AIR;
        this.operator = ComparisonOperator.LESS_THAN;
        this.threshold = 1000;
    }

    public CraftingCondition(Item item, ComparisonOperator operator, long threshold) {
        this.item = item;
        this.operator = operator;
        this.threshold = threshold;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public ComparisonOperator getOperator() {
        return operator;
    }

    public void setOperator(ComparisonOperator operator) {
        this.operator = operator;
    }

    public long getThreshold() {
        return threshold;
    }

    public void setThreshold(long threshold) {
        this.threshold = Math.max(0, threshold);
    }

    public boolean isValid() {
        return item != null && item != Items.AIR;
    }

    public boolean evaluate(long itemCount) {
        return operator.evaluate(itemCount, threshold);
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        tag.putString("item", itemId.toString());
        tag.putInt("operator", operator.ordinal());
        tag.putLong("threshold", threshold);
        return tag;
    }

    public static CraftingCondition fromNbt(CompoundTag tag) {
        CraftingCondition condition = new CraftingCondition();

        String itemId = tag.getString("item");
        condition.item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        condition.operator = ComparisonOperator.fromOrdinal(tag.getInt("operator"));
        condition.threshold = tag.getLong("threshold");

        return condition;
    }

    public CraftingCondition copy() {
        return new CraftingCondition(item, operator, threshold);
    }

    public ItemStack getItemStack() {
        return new ItemStack(item);
    }
}
