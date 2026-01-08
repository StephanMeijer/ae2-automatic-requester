package com.stephanmeijer.minecraft.ae2.autorequester.client;

import com.stephanmeijer.minecraft.ae2.autorequester.AE2Autorequester;
import com.stephanmeijer.minecraft.ae2.autorequester.ModMenus;
import com.stephanmeijer.minecraft.ae2.autorequester.gui.AutorequesterScreen;
import com.stephanmeijer.minecraft.ae2.autorequester.gui.ConditionEditorScreen;
import com.stephanmeijer.minecraft.ae2.autorequester.gui.RuleEditorScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = AE2Autorequester.MODID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.AUTOREQUESTER_MENU.get(), AutorequesterScreen::new);
        event.register(ModMenus.RULE_EDITOR_MENU.get(), RuleEditorScreen::new);
        event.register(ModMenus.CONDITION_EDITOR_MENU.get(), ConditionEditorScreen::new);
    }
}
