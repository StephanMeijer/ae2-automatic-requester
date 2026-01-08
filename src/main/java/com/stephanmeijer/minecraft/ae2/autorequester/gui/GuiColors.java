package com.stephanmeijer.minecraft.ae2.autorequester.gui;

/**
 * Shared color constants for all GUI screens.
 * Provides a single source of truth for the UI color palette.
 */
public final class GuiColors {
    private GuiColors() {} // Prevent instantiation

    // Background colors
    public static final int BACKGROUND_BORDER = 0xFF8B8B8B;
    public static final int BACKGROUND_FILL = 0xFF373737;
    public static final int TITLE_BAR = 0xFF1E1E1E;

    // List and scrollbar colors
    public static final int LIST_BACKGROUND = 0xFF1E1E1E;
    public static final int SCROLLBAR_TRACK = 0xFF2A2A2A;
    public static final int SCROLLBAR_THUMB = 0xFF6A6A6A;
    public static final int SELECTION_HIGHLIGHT = 0xFF4A4A4A;

    // Slot colors
    public static final int SLOT_BORDER = 0xFF1E1E1E;
    public static final int SLOT_EMPTY = 0xFF373737;
    public static final int SLOT_FILLED = 0xFF6A6A6A;
    public static final int SLOT_INNER = 0xFF2A2A2A;

    // Text colors
    public static final int TEXT_PRIMARY = 0xFFFFFF;
    public static final int TEXT_SECONDARY = 0x888888;
    public static final int TEXT_LABEL = 0xAAAAAA;
    public static final int TEXT_HIGHLIGHT = 0xFFFF55;

    // Status colors
    public static final int STATUS_SUCCESS = 0xFF55FF55;
    public static final int STATUS_ERROR = 0xFFFF5555;
    public static final int STATUS_DISABLED = 0xFF555555;

    // Status icon colors (with alpha)
    public static final int STATUS_ICON_BORDER = 0xFF000000;
}
