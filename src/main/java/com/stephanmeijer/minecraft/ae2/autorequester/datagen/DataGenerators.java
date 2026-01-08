package com.stephanmeijer.minecraft.ae2.autorequester.datagen;

import com.stephanmeijer.minecraft.ae2.autorequester.AE2Autorequester;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = AE2Autorequester.MODID, bus = EventBusSubscriber.Bus.MOD)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();

        // Add recipe provider
        generator.addProvider(
                event.includeServer(),
                new ModRecipeProvider(output, event.getLookupProvider())
        );
    }
}
