package engineering.enablement.claudecraft.claude;

import engineering.enablement.claudecraft.ClaudeCraft;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Reads Claude Code's native session storage to extract conversation data.
 * Claude stores sessions as JSONL files in ~/.claude/projects/<hash>/<sessionId>.jsonl
 *
 * This reader uses the global Claude storage but filters by a set of session IDs
 * that belong to a specific player.
 */
public class ClaudeSessionReader {
    private static final Gson GSON = new Gson();
    private static final int MAX_PREVIEW_LENGTH = 50;

    /**
     * Summary of a conversation for display in the UI.
     */
    public record ConversationSummary(
        String sessionId,
        String preview,
        long timestamp
    ) {}

    /**
     * A single chat message.
     */
    public record ChatMessage(
        String role,  // "user" or "assistant"
        String content,
        long timestamp
    ) {
        public boolean isUser() {
            return "user".equals(role);
        }
    }

    /**
     * Get the global Claude projects directory.
     */
    public static Path getGlobalClaudeProjectsDir() {
        String home = System.getProperty("user.home");
        // Check new location first, then fall back to old
        Path newPath = Path.of(home, ".config", "claude", "projects");
        Path oldPath = Path.of(home, ".claude", "projects");

        if (Files.exists(newPath)) {
            return newPath;
        }
        return oldPath;
    }

    /**
     * Get summaries of conversations for a player, filtered by their session IDs.
     *
     * @param playerSessionIds Set of session IDs that belong to this player
     * @return List of conversation summaries sorted by timestamp (newest first)
     */
    public static List<ConversationSummary> getConversationSummaries(Set<String> playerSessionIds) {
        List<ConversationSummary> summaries = new ArrayList<>();

        if (playerSessionIds == null || playerSessionIds.isEmpty()) {
            return summaries;
        }

        Path projectsDir = getGlobalClaudeProjectsDir();
        if (!Files.exists(projectsDir)) {
            ClaudeCraft.LOGGER.debug("Claude projects dir not found: {}", projectsDir);
            return summaries;
        }

        try (Stream<Path> projectDirs = Files.list(projectsDir)) {
            projectDirs.filter(Files::isDirectory).forEach(projectDir -> {
                try (Stream<Path> sessionFiles = Files.list(projectDir)) {
                    sessionFiles
                        .filter(p -> p.toString().endsWith(".jsonl"))
                        .filter(p -> !p.getParent().getFileName().toString().equals("subagents"))
                        .forEach(sessionFile -> {
                            String sessionId = sessionFile.getFileName().toString().replace(".jsonl", "");
                            // Only include sessions that belong to this player
                            if (playerSessionIds.contains(sessionId)) {
                                try {
                                    ConversationSummary summary = parseSessionSummary(sessionFile);
                                    if (summary != null) {
                                        summaries.add(summary);
                                    }
                                } catch (Exception e) {
                                    ClaudeCraft.LOGGER.debug("Failed to parse session {}: {}",
                                        sessionFile.getFileName(), e.getMessage());
                                }
                            }
                        });
                } catch (IOException e) {
                    ClaudeCraft.LOGGER.debug("Failed to list sessions in {}: {}",
                        projectDir, e.getMessage());
                }
            });
        } catch (IOException e) {
            ClaudeCraft.LOGGER.debug("Failed to list project dirs: {}", e.getMessage());
        }

        // Sort by timestamp, newest first
        summaries.sort(Comparator.comparingLong(ConversationSummary::timestamp).reversed());
        return summaries;
    }

    /**
     * Load all messages from a session.
     *
     * @param sessionId The session ID to load
     * @return List of messages in chronological order
     */
    public static List<ChatMessage> loadMessages(String sessionId) {
        List<ChatMessage> messages = new ArrayList<>();
        Path sessionFile = findSessionFile(sessionId);

        if (sessionFile == null) {
            ClaudeCraft.LOGGER.debug("Session file not found for: {}", sessionId);
            return messages;
        }

        try (BufferedReader reader = Files.newBufferedReader(sessionFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                try {
                    JsonObject obj = GSON.fromJson(line, JsonObject.class);
                    String type = obj.has("type") ? obj.get("type").getAsString() : null;

                    if ("user".equals(type) || "assistant".equals(type)) {
                        ChatMessage msg = parseMessage(obj);
                        if (msg != null && !msg.content().isEmpty()) {
                            messages.add(msg);
                        }
                    }
                } catch (Exception e) {
                    // Skip malformed lines
                }
            }
        } catch (IOException e) {
            ClaudeCraft.LOGGER.error("Failed to load messages for session {}: {}",
                sessionId, e.getMessage());
        }

        return messages;
    }

    /**
     * Parse a session file to extract summary info (first message preview + timestamp).
     */
    private static ConversationSummary parseSessionSummary(Path sessionFile) throws IOException {
        String sessionId = sessionFile.getFileName().toString().replace(".jsonl", "");
        long timestamp = Files.getLastModifiedTime(sessionFile).toMillis();
        String preview = "";

        // Read first user message for preview
        try (BufferedReader reader = Files.newBufferedReader(sessionFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                try {
                    JsonObject obj = GSON.fromJson(line, JsonObject.class);
                    String type = obj.has("type") ? obj.get("type").getAsString() : null;

                    if ("user".equals(type)) {
                        String content = extractMessageContent(obj);
                        if (content != null && !content.isEmpty()) {
                            preview = truncate(content, MAX_PREVIEW_LENGTH);
                            break;
                        }
                    }
                } catch (Exception e) {
                    // Skip malformed lines
                }
            }
        }

        // Skip sessions with no messages
        if (preview.isEmpty()) {
            return null;
        }

        return new ConversationSummary(sessionId, preview, timestamp);
    }

    /**
     * Parse a JSONL line into a ChatMessage.
     */
    private static ChatMessage parseMessage(JsonObject obj) {
        String type = obj.get("type").getAsString();
        String role = "user".equals(type) ? "user" : "assistant";
        String content = extractMessageContent(obj);

        // Use current time as we don't have timestamps in the file
        long timestamp = System.currentTimeMillis();

        if (content == null || content.isEmpty()) {
            return null;
        }

        return new ChatMessage(role, content, timestamp);
    }

    /**
     * Extract message content from a JSONL object.
     * Handles both string content (user) and array content (assistant).
     */
    private static String extractMessageContent(JsonObject obj) {
        if (!obj.has("message")) {
            return null;
        }

        JsonObject message = obj.getAsJsonObject("message");
        if (!message.has("content")) {
            return null;
        }

        JsonElement contentEl = message.get("content");

        // User messages: content is a string
        if (contentEl.isJsonPrimitive()) {
            return contentEl.getAsString();
        }

        // Assistant messages: content is an array of {type, text} objects
        if (contentEl.isJsonArray()) {
            JsonArray contentArr = contentEl.getAsJsonArray();
            StringBuilder text = new StringBuilder();
            for (JsonElement el : contentArr) {
                if (el.isJsonObject()) {
                    JsonObject contentObj = el.getAsJsonObject();
                    if (contentObj.has("text")) {
                        if (text.length() > 0) text.append("\n");
                        text.append(contentObj.get("text").getAsString());
                    }
                }
            }
            return text.toString();
        }

        return null;
    }

    /**
     * Find the session file for a given session ID in global Claude storage.
     */
    private static Path findSessionFile(String sessionId) {
        Path projectsDir = getGlobalClaudeProjectsDir();

        if (!Files.exists(projectsDir)) {
            return null;
        }

        try (Stream<Path> projectDirs = Files.list(projectsDir)) {
            return projectDirs
                .filter(Files::isDirectory)
                .map(dir -> dir.resolve(sessionId + ".jsonl"))
                .filter(Files::exists)
                .findFirst()
                .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Truncate a string to a maximum length, adding ellipsis if needed.
     */
    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        // Remove newlines for preview
        text = text.replace("\n", " ").trim();
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
