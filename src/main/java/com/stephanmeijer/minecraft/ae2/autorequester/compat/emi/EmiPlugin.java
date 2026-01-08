package com.stephanmeijer.minecraft.ae2.autorequester.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiRegistry;

/**
 * EMI plugin for ghost ingredient drag-drop support in Autorequester screens.
 * This enables dragging items from EMI into item slots in the editor screens.
 */
@EmiEntrypoint
public class EmiPlugin implements dev.emi.emi.api.EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        // Register generic drag-drop handler for all screens that implement IGhostItemTarget
        registry.addGenericDragDropHandler(new GhostItemDragDropHandler());
    }
}
