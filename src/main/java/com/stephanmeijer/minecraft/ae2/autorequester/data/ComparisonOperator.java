package com.stephanmeijer.minecraft.ae2.autorequester.data;

import net.minecraft.network.chat.Component;

public enum ComparisonOperator {
    LESS_THAN("<", "less_than"),
    LESS_THAN_OR_EQUAL("<=", "less_than_or_equal"),
    GREATER_THAN(">", "greater_than"),
    GREATER_THAN_OR_EQUAL(">=", "greater_than_or_equal"),
    EQUAL("=", "equal"),
    NOT_EQUAL("!=", "not_equal");

    private final String symbol;
    private final String translationKey;

    ComparisonOperator(String symbol, String translationKey) {
        this.symbol = symbol;
        this.translationKey = translationKey;
    }

    public String getSymbol() {
        return symbol;
    }

    public Component getDisplayName() {
        return Component.translatable("ae2_autorequester.operator." + translationKey);
    }

    public boolean evaluate(long itemCount, long threshold) {
        return switch (this) {
            case LESS_THAN -> itemCount < threshold;
            case LESS_THAN_OR_EQUAL -> itemCount <= threshold;
            case GREATER_THAN -> itemCount > threshold;
            case GREATER_THAN_OR_EQUAL -> itemCount >= threshold;
            case EQUAL -> itemCount == threshold;
            case NOT_EQUAL -> itemCount != threshold;
        };
    }

    /**
     * Deserialize from enum name string.
     */
    public static ComparisonOperator fromName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return LESS_THAN;
        }
    }

    /**
     * Get the next operator in the cycle (for UI cycling).
     */
    @SuppressWarnings("EnumOrdinal")  // Safe: cycling through enum values within the enum itself
    public ComparisonOperator next() {
        ComparisonOperator[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
