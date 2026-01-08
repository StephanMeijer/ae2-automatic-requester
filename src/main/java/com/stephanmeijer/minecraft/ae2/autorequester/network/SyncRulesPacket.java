package com.stephanmeijer.minecraft.ae2.autorequester.network;

import java.util.ArrayList;
import java.util.List;

import com.stephanmeijer.minecraft.ae2.autorequester.block.AutorequesterBlockEntity;
import com.stephanmeijer.minecraft.ae2.autorequester.data.CraftingRule;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Packet sent from client to server to sync all rules for a block entity.
 */
public record SyncRulesPacket(BlockPos pos, List<CraftingRule> rules) implements CustomPacketPayload {
    private static final Logger LOG = LoggerFactory.getLogger(SyncRulesPacket.class);

    public static final Type<SyncRulesPacket> TYPE = new Type<>(ModNetworking.id("sync_rules"));

    public static final StreamCodec<FriendlyByteBuf, SyncRulesPacket> STREAM_CODEC = StreamCodec.of(
            SyncRulesPacket::encode,
            SyncRulesPacket::decode
    );

    private static void encode(FriendlyByteBuf buf, SyncRulesPacket packet) {
        buf.writeBlockPos(packet.pos);

        // Encode rules as NBT list
        ListTag ruleList = new ListTag();
        for (CraftingRule rule : packet.rules) {
            ruleList.add(rule.toNbt());
        }
        CompoundTag tag = new CompoundTag();
        tag.put("rules", ruleList);
        buf.writeNbt(tag);
    }

    private static SyncRulesPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();

        CompoundTag tag = buf.readNbt();
        List<CraftingRule> rules = new ArrayList<>();
        if (tag != null && tag.contains("rules")) {
            ListTag ruleList = tag.getList("rules", Tag.TAG_COMPOUND);
            for (int i = 0; i < ruleList.size(); i++) {
                rules.add(CraftingRule.fromNbt(ruleList.getCompound(i)));
            }
        }

        return new SyncRulesPacket(pos, rules);
    }

    @SuppressWarnings("FutureReturnValueIgnored")  // NeoForge handles exceptions internally
    public static void handle(SyncRulesPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (player == null) {
                return;
            }

            LOG.info("[SyncRulesPacket] SERVER received {} rules at pos {}", packet.rules.size(), packet.pos);
            for (int i = 0; i < packet.rules.size(); i++) {
                CraftingRule r = packet.rules.get(i);
                LOG.info("[SyncRulesPacket]   Rule[{}]: id={}, name='{}', target={}, batchSize={}, conditions={}",
                        i, r.getId(), r.getName(),
                        r.getTargetItem() != null ? r.getTargetItem().toString() : "null",
                        r.getBatchSize(), r.getConditions().size());
            }

            BlockEntity be = player.level().getBlockEntity(packet.pos);
            if (be instanceof AutorequesterBlockEntity autorequester) {
                // Verify player is close enough to the block
                if (player.distanceToSqr(packet.pos.getX() + 0.5, packet.pos.getY() + 0.5, packet.pos.getZ() + 0.5) < 64) {
                    LOG.info("[SyncRulesPacket] Calling setRulesFromPacket");
                    autorequester.setRulesFromPacket(packet.rules);
                } else {
                    LOG.warn("[SyncRulesPacket] Player too far from block");
                }
            } else {
                LOG.warn("[SyncRulesPacket] Block entity not found or wrong type at {}", packet.pos);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
