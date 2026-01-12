package engineering.enablement.claudecraft.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Sent from server to client with the list of previous conversations.
 * Includes conversation summaries with preview text and timestamps.
 */
public record ClientboundConversationListPacket(
    List<ConversationSummary> sessions,
    String currentSessionId
) implements CustomPacketPayload {

    /**
     * Summary of a conversation for display in the UI.
     */
    public record ConversationSummary(
        String sessionId,
        String preview,
        long timestamp
    ) {}

    public static final CustomPacketPayload.Type<ClientboundConversationListPacket> TYPE =
        new CustomPacketPayload.Type<>(ModNetworking.id("conversation_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundConversationListPacket> STREAM_CODEC =
        new StreamCodec<>() {
            @Override
            public ClientboundConversationListPacket decode(RegistryFriendlyByteBuf buf) {
                int size = buf.readVarInt();
                List<ConversationSummary> sessions = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    String id = buf.readUtf();
                    String preview = buf.readUtf();
                    long timestamp = buf.readLong();
                    sessions.add(new ConversationSummary(id, preview, timestamp));
                }
                String current = buf.readUtf();
                return new ClientboundConversationListPacket(
                    sessions,
                    current.isEmpty() ? null : current
                );
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, ClientboundConversationListPacket packet) {
                buf.writeVarInt(packet.sessions.size());
                for (ConversationSummary s : packet.sessions) {
                    buf.writeUtf(s.sessionId());
                    buf.writeUtf(s.preview());
                    buf.writeLong(s.timestamp());
                }
                buf.writeUtf(packet.currentSessionId != null ? packet.currentSessionId : "");
            }
        };

    // Handle null currentSessionId by using empty string
    public ClientboundConversationListPacket(List<ConversationSummary> sessions, String currentSessionId) {
        this.sessions = sessions != null ? sessions : List.of();
        this.currentSessionId = currentSessionId;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientboundConversationListPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Client-side: update conversation list UI
            ClientChatHandler.onConversationList(packet.sessions(), packet.currentSessionId());
        });
    }
}
