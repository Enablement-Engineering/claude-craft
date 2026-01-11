package ac.dylanisa.minecraftai.network;

import ac.dylanisa.minecraftai.MinecraftAI;
import ac.dylanisa.minecraftai.claude.ClaudeProcess;
import ac.dylanisa.minecraftai.data.PlayerDataManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.nio.file.Path;

/**
 * Sent from client to server when player sends a chat message to the AI.
 */
public record ServerboundChatPacket(String message) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerboundChatPacket> TYPE =
        new CustomPacketPayload.Type<>(ModNetworking.id("chat"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundChatPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ServerboundChatPacket::message,
            ServerboundChatPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerboundChatPacket packet, IPayloadContext context) {
        // Capture player and data on main thread
        ServerPlayer player = (ServerPlayer) context.player();
        PlayerDataManager dataManager = MinecraftAI.getDataManager();

        if (dataManager == null) {
            MinecraftAI.LOGGER.error("DataManager not initialized");
            return;
        }

        Path pluginDir = dataManager.getPluginDir();
        boolean isOp = player.hasPermissions(2); // OP level 2+

        // Get or create session for this player
        String sessionId = ChatSessionManager.getSessionId(player.getUUID());

        ClaudeProcess claude = new ClaudeProcess(pluginDir, player.getUUID(), isOp);
        if (sessionId != null) {
            claude.setSessionId(sessionId);
        }

        MinecraftAI.LOGGER.info("Player {} sent AI message: {}",
            player.getName().getString(), packet.message());

        // Run Claude asynchronously and stream results back
        // The callbacks are called from the async thread, but context.reply() is thread-safe
        claude.run(
            packet.message(),
            // On each text chunk, send to client
            chunk -> {
                context.reply(new ClientboundChatChunkPacket(chunk));
            },
            // On complete
            fullResponse -> {
                // Save session ID for future messages
                String newSessionId = claude.getSessionId();
                boolean isNewSession = sessionId == null && newSessionId != null;

                if (newSessionId != null) {
                    ChatSessionManager.setSessionId(player.getUUID(), newSessionId);
                }
                context.reply(new ClientboundChatCompletePacket(true, ""));

                // If this was a new conversation (first message), send updated conversation list
                if (isNewSession) {
                    var sessions = ChatSessionManager.getConversationSummaries(player.getUUID());
                    context.reply(new ClientboundConversationListPacket(sessions, newSessionId));
                }
            },
            // On error
            error -> {
                MinecraftAI.LOGGER.error("Claude error for {}: {}",
                    player.getName().getString(), error.getMessage());
                context.reply(new ClientboundChatCompletePacket(false, error.getMessage()));
            }
        );
    }
}
