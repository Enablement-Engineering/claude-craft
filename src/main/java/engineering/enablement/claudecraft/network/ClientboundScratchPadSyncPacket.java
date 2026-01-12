package engineering.enablement.claudecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent from server to client to sync scratch pad content.
 */
public record ClientboundScratchPadSyncPacket(String content) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientboundScratchPadSyncPacket> TYPE =
        new CustomPacketPayload.Type<>(ModNetworking.id("scratch_pad_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundScratchPadSyncPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ClientboundScratchPadSyncPacket::content,
            ClientboundScratchPadSyncPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientboundScratchPadSyncPacket packet, IPayloadContext context) {
        engineering.enablement.claudecraft.ClaudeCraft.LOGGER.info(
            "ClientboundScratchPadSyncPacket: Received {} chars from server",
            packet.content() != null ? packet.content().length() : 0);
        context.enqueueWork(() -> {
            // Client-side: update local scratch pad content
            ClientChatHandler.onScratchPadSync(packet.content());
        });
    }
}
