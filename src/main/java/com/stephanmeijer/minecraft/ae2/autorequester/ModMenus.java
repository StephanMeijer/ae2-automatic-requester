package com.stephanmeijer.minecraft.ae2.autorequester;

import com.stephanmeijer.minecraft.ae2.autorequester.gui.AutorequesterMenu;
import com.stephanmeijer.minecraft.ae2.autorequester.gui.ConditionEditorMenu;
import com.stephanmeijer.minecraft.ae2.autorequester.gui.RuleEditorMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, AE2Autorequester.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<AutorequesterMenu>> AUTOREQUESTER_MENU = MENUS.register(
            "autorequester",
            () -> IMenuTypeExtension.create(AutorequesterMenu::new)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<RuleEditorMenu>> RULE_EDITOR_MENU = MENUS.register(
            "rule_editor",
            () -> IMenuTypeExtension.create(RuleEditorMenu::new)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<ConditionEditorMenu>> CONDITION_EDITOR_MENU = MENUS.register(
            "condition_editor",
            () -> IMenuTypeExtension.create(ConditionEditorMenu::new)
    );
}
