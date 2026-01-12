package engineering.enablement.claudecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Sent from server to client with the message history for a resumed conversation.
 */
public record ClientboundMessageHistoryPacket(
    String sessionId,
    List<ClientChatHandler.ChatMessageRecord> messages
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientboundMessageHistoryPacket> TYPE =
        new CustomPacketPayload.Type<>(ModNetworking.id("message_history"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundMessageHistoryPacket> STREAM_CODEC =
        new StreamCodec<>() {
            @Override
            public ClientboundMessageHistoryPacket decode(RegistryFriendlyByteBuf buf) {
                String sessionId = buf.readUtf();
                int count = buf.readVarInt();
                List<ClientChatHandler.ChatMessageRecord> messages = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    boolean isUser = buf.readBoolean();
                    String content = buf.readUtf(32767);  // Allow longer messages
                    messages.add(new ClientChatHandler.ChatMessageRecord(isUser, content));
                }
                return new ClientboundMessageHistoryPacket(sessionId, messages);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, ClientboundMessageHistoryPacket packet) {
                buf.writeUtf(packet.sessionId);
                buf.writeVarInt(packet.messages.size());
                for (var msg : packet.messages) {
                    buf.writeBoolean(msg.isUser());
                    buf.writeUtf(msg.content(), 32767);
                }
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientboundMessageHistoryPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientChatHandler.onMessageHistory(packet.sessionId(), packet.messages());
        });
    }
}
