package ac.dylanisa.minecraftai.network;

import ac.dylanisa.minecraftai.MinecraftAI;
import ac.dylanisa.minecraftai.claude.ClaudeProcessTracker;
import ac.dylanisa.minecraftai.claude.ClaudeSessionReader;
import ac.dylanisa.minecraftai.data.PlayerDataManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Claude Code sessions for each player.
 * Tracks which session IDs belong to each player, while Claude's global
 * storage handles the actual conversation data.
 */
public class ChatSessionManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, String> activeSessions = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> playerSessionIds = new ConcurrentHashMap<>();

    /**
     * Get the current session ID for a player.
     */
    public static String getSessionId(UUID playerUuid) {
        // Check in-memory cache first
        String cached = activeSessions.get(playerUuid);
        if (cached != null) {
            return cached;
        }

        // Try to load from disk
        PlayerDataManager dataManager = MinecraftAI.getDataManager();
        if (dataManager == null) {
            return null;
        }

        Path currentFile = dataManager.getPlayerDir(playerUuid)
            .resolve("sessions").resolve("current.txt");

        if (Files.exists(currentFile)) {
            try {
                String sessionId = Files.readString(currentFile).trim();
                if (!sessionId.isEmpty()) {
                    activeSessions.put(playerUuid, sessionId);
                    return sessionId;
                }
            } catch (IOException e) {
                MinecraftAI.LOGGER.debug("Failed to read session file: {}", e.getMessage());
            }
        }

        return null;
    }

    /**
     * Set the current session ID for a player.
     * Also tracks this session ID as belonging to this player.
     */
    public static void setSessionId(UUID playerUuid, String sessionId) {
        activeSessions.put(playerUuid, sessionId);

        // Track this session as belonging to this player
        addPlayerSession(playerUuid, sessionId);

        // Persist current session to disk
        PlayerDataManager dataManager = MinecraftAI.getDataManager();
        if (dataManager == null) {
            return;
        }

        try {
            Path sessionsDir = dataManager.getPlayerDir(playerUuid).resolve("sessions");
            Files.createDirectories(sessionsDir);

            Path currentFile = sessionsDir.resolve("current.txt");
            Files.writeString(currentFile, sessionId);
        } catch (IOException e) {
            MinecraftAI.LOGGER.error("Failed to save session ID: {}", e.getMessage());
        }
    }

    /**
     * Add a session ID to a player's tracked sessions.
     */
    private static void addPlayerSession(UUID playerUuid, String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) return;

        // Add to in-memory cache
        playerSessionIds.computeIfAbsent(playerUuid, k -> loadPlayerSessions(playerUuid))
            .add(sessionId);

        // Persist to disk
        savePlayerSessions(playerUuid);
    }

    /**
     * Get all session IDs that belong to a player.
     */
    public static Set<String> getPlayerSessionIds(UUID playerUuid) {
        return playerSessionIds.computeIfAbsent(playerUuid, k -> loadPlayerSessions(playerUuid));
    }

    /**
     * Load player's session IDs from disk.
     */
    private static Set<String> loadPlayerSessions(UUID playerUuid) {
        PlayerDataManager dataManager = MinecraftAI.getDataManager();
        if (dataManager == null) {
            return new HashSet<>();
        }

        Path sessionsFile = dataManager.getPlayerDir(playerUuid)
            .resolve("sessions").resolve("sessions.json");

        if (Files.exists(sessionsFile)) {
            try {
                String json = Files.readString(sessionsFile);
                List<String> sessions = GSON.fromJson(json, new TypeToken<List<String>>(){}.getType());
                return new HashSet<>(sessions != null ? sessions : List.of());
            } catch (IOException e) {
                MinecraftAI.LOGGER.debug("Failed to load player sessions: {}", e.getMessage());
            }
        }

        return new HashSet<>();
    }

    /**
     * Save player's session IDs to disk.
     */
    private static void savePlayerSessions(UUID playerUuid) {
        PlayerDataManager dataManager = MinecraftAI.getDataManager();
        if (dataManager == null) {
            return;
        }

        Set<String> sessions = playerSessionIds.get(playerUuid);
        if (sessions == null) return;

        try {
            Path sessionsDir = dataManager.getPlayerDir(playerUuid).resolve("sessions");
            Files.createDirectories(sessionsDir);

            Path sessionsFile = sessionsDir.resolve("sessions.json");
            Files.writeString(sessionsFile, GSON.toJson(new ArrayList<>(sessions)));
        } catch (IOException e) {
            MinecraftAI.LOGGER.error("Failed to save player sessions: {}", e.getMessage());
        }
    }

    /**
     * Start a new conversation for a player.
     * Clears the active session (new one will be created on first message).
     */
    public static void newConversation(UUID playerUuid) {
        activeSessions.remove(playerUuid);

        PlayerDataManager dataManager = MinecraftAI.getDataManager();
        if (dataManager == null) {
            return;
        }

        try {
            Path sessionsDir = dataManager.getPlayerDir(playerUuid).resolve("sessions");
            Files.createDirectories(sessionsDir);

            // Clear current session file
            Path currentFile = sessionsDir.resolve("current.txt");
            Files.deleteIfExists(currentFile);

        } catch (IOException e) {
            MinecraftAI.LOGGER.error("Failed to clear session: {}", e.getMessage());
        }
    }

    /**
     * Resume a previous conversation.
     */
    public static void resumeConversation(UUID playerUuid, String sessionId) {
        setSessionId(playerUuid, sessionId);
    }

    /**
     * Get conversation summaries with preview text and timestamps.
     * Uses global Claude storage but filters by player's session IDs.
     */
    public static List<ClientboundConversationListPacket.ConversationSummary> getConversationSummaries(UUID playerUuid) {
        Set<String> playerSessions = getPlayerSessionIds(playerUuid);

        var claudeSummaries = ClaudeSessionReader.getConversationSummaries(playerSessions);

        // Convert to packet-friendly format
        return claudeSummaries.stream()
            .map(s -> new ClientboundConversationListPacket.ConversationSummary(
                s.sessionId(),
                s.preview(),
                s.timestamp()
            ))
            .toList();
    }

    /**
     * Load messages for a session from Claude's global storage.
     */
    public static List<ClientChatHandler.ChatMessageRecord> loadMessages(UUID playerUuid, String sessionId) {
        // Verify this session belongs to the player
        Set<String> playerSessions = getPlayerSessionIds(playerUuid);
        if (!playerSessions.contains(sessionId)) {
            MinecraftAI.LOGGER.warn("Player {} attempted to load session {} they don't own",
                playerUuid, sessionId);
            return List.of();
        }

        var claudeMessages = ClaudeSessionReader.loadMessages(sessionId);

        // Convert to packet-friendly format
        return claudeMessages.stream()
            .map(m -> new ClientChatHandler.ChatMessageRecord(m.isUser(), m.content()))
            .toList();
    }

    /**
     * Delete a conversation from the player's history.
     * This only removes the session from tracking - the actual Claude session file remains.
     */
    public static void deleteConversation(UUID playerUuid, String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) return;

        // Remove from in-memory cache
        Set<String> sessions = playerSessionIds.get(playerUuid);
        if (sessions != null) {
            sessions.remove(sessionId);
        }

        // If this was the active session, clear it
        String active = activeSessions.get(playerUuid);
        if (sessionId.equals(active)) {
            activeSessions.remove(playerUuid);

            // Also clear from disk
            PlayerDataManager dataManager = MinecraftAI.getDataManager();
            if (dataManager != null) {
                try {
                    Path currentFile = dataManager.getPlayerDir(playerUuid)
                        .resolve("sessions").resolve("current.txt");
                    Files.deleteIfExists(currentFile);
                } catch (IOException e) {
                    MinecraftAI.LOGGER.debug("Failed to clear current session: {}", e.getMessage());
                }
            }
        }

        // Persist to disk
        savePlayerSessions(playerUuid);
        MinecraftAI.LOGGER.info("Deleted conversation {} for player {}", sessionId, playerUuid);
    }

    /**
     * Clean up when player disconnects.
     * Cancels any active Claude processes and clears in-memory caches.
     */
    public static void onPlayerDisconnect(UUID playerUuid) {
        MinecraftAI.LOGGER.info("Cleaning up sessions for player {}", playerUuid);

        // Cancel any active Claude processes for this player
        ClaudeProcessTracker.cancelPlayerProcesses(playerUuid);

        // Clear rate limit tracking
        ServerboundChatPacket.cleanupPlayer(playerUuid);

        // Clear in-memory caches (data is persisted to disk)
        activeSessions.remove(playerUuid);
        playerSessionIds.remove(playerUuid);
    }
}
