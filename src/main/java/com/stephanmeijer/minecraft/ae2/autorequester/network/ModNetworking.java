package com.stephanmeijer.minecraft.ae2.autorequester.network;

import com.stephanmeijer.minecraft.ae2.autorequester.AE2Autorequester;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = AE2Autorequester.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetworking {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(AE2Autorequester.MODID).versioned("1.0");

        // Register packets
        registrar.playToServer(
                SyncRulesPacket.TYPE,
                SyncRulesPacket.STREAM_CODEC,
                SyncRulesPacket::handle
        );

        registrar.playToServer(
                OpenAutorequesterPacket.TYPE,
                OpenAutorequesterPacket.STREAM_CODEC,
                OpenAutorequesterPacket::handle
        );
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(AE2Autorequester.MODID, path);
    }
}
