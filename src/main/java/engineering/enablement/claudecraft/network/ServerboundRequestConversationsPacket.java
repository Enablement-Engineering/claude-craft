package engineering.enablement.claudecraft.network;

import engineering.enablement.claudecraft.ClaudeCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent from client to server to request the conversation list.
 */
public record ServerboundRequestConversationsPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerboundRequestConversationsPacket> TYPE =
        new CustomPacketPayload.Type<>(ModNetworking.id("request_conversations"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundRequestConversationsPacket> STREAM_CODEC =
        StreamCodec.unit(new ServerboundRequestConversationsPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundRequestConversationsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ClaudeCraft.LOGGER.info("Player {} requested conversation list",
                player.getName().getString());

            // Get current session and conversation summaries
            String currentSessionId = ChatSessionManager.getSessionId(player.getUUID());
            var sessions = ChatSessionManager.getConversationSummaries(player.getUUID());

            ClaudeCraft.LOGGER.info("Sending {} conversations to player, current session: {}",
                sessions.size(), currentSessionId);

            context.reply(new ClientboundConversationListPacket(sessions, currentSessionId));
        });
    }
}
