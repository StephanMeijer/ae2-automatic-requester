package com.stephanmeijer.minecraft.ae2.autorequester.datagen;

import java.util.concurrent.CompletableFuture;

import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import com.stephanmeijer.minecraft.ae2.autorequester.AE2Autorequester;
import com.stephanmeijer.minecraft.ae2.autorequester.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        // Autorequester recipe using AE2 components:
        // L E L     L = Logic Processor
        // C P C     E = Engineering Processor
        // L E L     C = Calculation Processor
        //           P = Pattern Provider
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.AUTOREQUESTER.get())
                .pattern("LEL")
                .pattern("CPC")
                .pattern("LEL")
                .define('L', AEItems.LOGIC_PROCESSOR)
                .define('E', AEItems.ENGINEERING_PROCESSOR)
                .define('C', AEItems.CALCULATION_PROCESSOR)
                .define('P', AEBlocks.PATTERN_PROVIDER)
                .unlockedBy("has_pattern_provider", has(AEBlocks.PATTERN_PROVIDER))
                .unlockedBy("has_engineering_processor", has(AEItems.ENGINEERING_PROCESSOR))
                .save(output, ResourceLocation.fromNamespaceAndPath(AE2Autorequester.MODID, "autorequester"));
    }
}
