package engineering.enablement.claudecraft.network;

import engineering.enablement.claudecraft.ClaudeCraft;
import engineering.enablement.claudecraft.claude.ClaudeProcess;
import engineering.enablement.claudecraft.claude.ClaudeProcessTracker;
import engineering.enablement.claudecraft.data.PlayerDataManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sent from client to server when player sends a chat message to the AI.
 */
public record ServerboundChatPacket(String message) implements CustomPacketPayload {

    // Rate limiting: max 1 concurrent process per player, min 2 seconds between messages
    private static final int MAX_CONCURRENT_PROCESSES = 1;
    private static final long MIN_MESSAGE_INTERVAL_MS = 2000;
    private static final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();

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

    /**
     * Clean up rate limit tracking for a player (called on disconnect).
     */
    public static void cleanupPlayer(UUID playerUuid) {
        lastMessageTime.remove(playerUuid);
    }

    public static void handle(ServerboundChatPacket packet, IPayloadContext context) {
        // Capture player and data on main thread
        ServerPlayer player = (ServerPlayer) context.player();
        UUID playerUuid = player.getUUID();

        // Rate limit check: concurrent processes
        int activeCount = ClaudeProcessTracker.getActiveCount(playerUuid);
        if (activeCount >= MAX_CONCURRENT_PROCESSES) {
            ClaudeCraft.LOGGER.warn("Player {} rate limited: {} active processes",
                player.getName().getString(), activeCount);
            context.reply(new ClientboundChatCompletePacket(false,
                "Please wait for your current message to complete."));
            return;
        }

        // Rate limit check: message interval
        long now = System.currentTimeMillis();
        Long lastTime = lastMessageTime.get(playerUuid);
        if (lastTime != null && (now - lastTime) < MIN_MESSAGE_INTERVAL_MS) {
            ClaudeCraft.LOGGER.warn("Player {} rate limited: message too soon",
                player.getName().getString());
            context.reply(new ClientboundChatCompletePacket(false,
                "Please wait a moment before sending another message."));
            return;
        }
        lastMessageTime.put(playerUuid, now);

        PlayerDataManager dataManager = ClaudeCraft.getDataManager();

        if (dataManager == null) {
            ClaudeCraft.LOGGER.error("DataManager not initialized");
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

        ClaudeCraft.LOGGER.info("Player {} sent AI message: {}",
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
                ClaudeCraft.LOGGER.error("Claude error for {}: {}",
                    player.getName().getString(), error.getMessage());
                context.reply(new ClientboundChatCompletePacket(false, error.getMessage()));
            }
        );
    }
}
