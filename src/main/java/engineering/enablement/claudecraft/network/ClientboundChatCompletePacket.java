package engineering.enablement.claudecraft.network;

import engineering.enablement.claudecraft.ClaudeCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent from server to client when AI response is complete.
 */
public record ClientboundChatCompletePacket(
    boolean success,
    String errorMessage
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientboundChatCompletePacket> TYPE =
        new CustomPacketPayload.Type<>(ModNetworking.id("chat_complete"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundChatCompletePacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.BOOL, ClientboundChatCompletePacket::success,
            ByteBufCodecs.STRING_UTF8, ClientboundChatCompletePacket::errorMessage,
            ClientboundChatCompletePacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientboundChatCompletePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Client-side: mark response as complete
            ClientChatHandler.onChatComplete(packet.success(), packet.errorMessage());
        });
    }
}
