package engineering.enablement.claudecraft.network;

import engineering.enablement.claudecraft.ClaudeCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent from client to server to delete a conversation from history.
 */
public record ServerboundDeleteConversationPacket(String sessionId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerboundDeleteConversationPacket> TYPE =
        new CustomPacketPayload.Type<>(ModNetworking.id("delete_conversation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundDeleteConversationPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ServerboundDeleteConversationPacket::sessionId,
            ServerboundDeleteConversationPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundDeleteConversationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ClaudeCraft.LOGGER.info("Player {} deleting conversation {}",
                player.getName().getString(), packet.sessionId());

            // Remove from player's session list
            ChatSessionManager.deleteConversation(player.getUUID(), packet.sessionId());

            // Send updated conversation list to client
            String currentSessionId = ChatSessionManager.getSessionId(player.getUUID());
            var sessions = ChatSessionManager.getConversationSummaries(player.getUUID());
            context.reply(new ClientboundConversationListPacket(sessions, currentSessionId));
        });
    }
}
