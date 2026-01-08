package com.stephanmeijer.minecraft.ae2.autorequester.network;

import com.stephanmeijer.minecraft.ae2.autorequester.block.AutorequesterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Packet sent from client to server to request reopening the Autorequester menu.
 * Used after closing editor screens to return to the main screen.
 */
public record OpenAutorequesterPacket(BlockPos pos) implements CustomPacketPayload {
    private static final Logger LOG = LoggerFactory.getLogger(OpenAutorequesterPacket.class);

    public static final Type<OpenAutorequesterPacket> TYPE = new Type<>(ModNetworking.id("open_autorequester"));

    public static final StreamCodec<FriendlyByteBuf, OpenAutorequesterPacket> STREAM_CODEC = StreamCodec.of(
            OpenAutorequesterPacket::encode,
            OpenAutorequesterPacket::decode
    );

    private static void encode(FriendlyByteBuf buf, OpenAutorequesterPacket packet) {
        buf.writeBlockPos(packet.pos);
    }

    private static OpenAutorequesterPacket decode(FriendlyByteBuf buf) {
        return new OpenAutorequesterPacket(buf.readBlockPos());
    }

    public static void handle(OpenAutorequesterPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) return;

            LOG.info("[OpenAutorequesterPacket] SERVER received request to open menu at {}", packet.pos);

            BlockEntity be = player.level().getBlockEntity(packet.pos);
            if (be instanceof AutorequesterBlockEntity autorequester) {
                LOG.info("[OpenAutorequesterPacket] Block entity has {} rules", autorequester.getRules().size());
                for (int i = 0; i < autorequester.getRules().size(); i++) {
                    var r = autorequester.getRules().get(i);
                    LOG.info("[OpenAutorequesterPacket]   Rule[{}]: id={}, name='{}', target={}, batchSize={}, conditions={}",
                            i, r.getId(), r.getName(),
                            r.getTargetItem() != null ? r.getTargetItem().toString() : "null",
                            r.getBatchSize(), r.getConditions().size());
                }

                // Verify player is close enough to the block
                if (player.distanceToSqr(packet.pos.getX() + 0.5, packet.pos.getY() + 0.5, packet.pos.getZ() + 0.5) < 64) {
                    LOG.info("[OpenAutorequesterPacket] Opening menu for player");
                    autorequester.openMenu(player);
                }
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
