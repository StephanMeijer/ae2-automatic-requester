package com.stephanmeijer.minecraft.ae2.autorequester.gui;

/**
 * Reusable double-click detection logic.
 */
public class DoubleClickHandler {
    private static final long DOUBLE_CLICK_THRESHOLD_MS = 500;

    private long lastClickTime = 0;
    private int lastClickedIndex = -1;

    /**
     * Call this when an item is clicked.
     * @param clickedIndex The index of the clicked item
     * @return true if this was a double-click, false if single-click
     */
    public boolean onClick(int clickedIndex) {
        long currentTime = System.currentTimeMillis();
        boolean isDoubleClick = clickedIndex == lastClickedIndex &&
                currentTime - lastClickTime < DOUBLE_CLICK_THRESHOLD_MS;

        if (isDoubleClick) {
            lastClickTime = 0;
            lastClickedIndex = -1;
        } else {
            lastClickTime = currentTime;
            lastClickedIndex = clickedIndex;
        }

        return isDoubleClick;
    }

    /**
     * Reset the click state (e.g., after a toggle action that shouldn't trigger double-click).
     */
    public void reset() {
        lastClickTime = 0;
        lastClickedIndex = -1;
    }
}
