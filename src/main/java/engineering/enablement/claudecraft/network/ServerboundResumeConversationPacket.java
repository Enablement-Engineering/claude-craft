package engineering.enablement.claudecraft.network;

import engineering.enablement.claudecraft.ClaudeCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent from client to server to resume a previous conversation.
 */
public record ServerboundResumeConversationPacket(String sessionId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerboundResumeConversationPacket> TYPE =
        new CustomPacketPayload.Type<>(ModNetworking.id("resume_conversation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundResumeConversationPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ServerboundResumeConversationPacket::sessionId,
            ServerboundResumeConversationPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundResumeConversationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ChatSessionManager.resumeConversation(player.getUUID(), packet.sessionId());
            ClaudeCraft.LOGGER.info("Player {} resumed conversation {}",
                player.getName().getString(), packet.sessionId());

            // Send updated conversation list with current session marked
            var sessions = ChatSessionManager.getConversationSummaries(player.getUUID());
            context.reply(new ClientboundConversationListPacket(sessions, packet.sessionId()));

            // Send message history for the resumed session
            var messages = ChatSessionManager.loadMessages(player.getUUID(), packet.sessionId());
            context.reply(new ClientboundMessageHistoryPacket(packet.sessionId(), messages));
        });
    }
}
