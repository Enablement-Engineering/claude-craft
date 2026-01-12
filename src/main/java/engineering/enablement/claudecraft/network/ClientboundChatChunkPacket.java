package engineering.enablement.claudecraft.network;

import engineering.enablement.claudecraft.ClaudeCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent from server to client with a chunk of streaming AI response.
 */
public record ClientboundChatChunkPacket(String text) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientboundChatChunkPacket> TYPE =
        new CustomPacketPayload.Type<>(ModNetworking.id("chat_chunk"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundChatChunkPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ClientboundChatChunkPacket::text,
            ClientboundChatChunkPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientboundChatChunkPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Client-side: append text to current response
            // This will be handled by the UI when we implement it
            ClientChatHandler.onChatChunk(packet.text());
        });
    }
}
