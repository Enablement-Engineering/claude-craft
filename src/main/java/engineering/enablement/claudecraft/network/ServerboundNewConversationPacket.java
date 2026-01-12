package engineering.enablement.claudecraft.network;

import engineering.enablement.claudecraft.ClaudeCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent from client to server to start a new conversation.
 */
public record ServerboundNewConversationPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerboundNewConversationPacket> TYPE =
        new CustomPacketPayload.Type<>(ModNetworking.id("new_conversation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundNewConversationPacket> STREAM_CODEC =
        StreamCodec.unit(new ServerboundNewConversationPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundNewConversationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ChatSessionManager.newConversation(player.getUUID());
            ClaudeCraft.LOGGER.info("Player {} started new conversation",
                player.getName().getString());

            // Send updated conversation list to client
            var sessions = ChatSessionManager.getConversationSummaries(player.getUUID());
            context.reply(new ClientboundConversationListPacket(sessions, null));
        });
    }
}
