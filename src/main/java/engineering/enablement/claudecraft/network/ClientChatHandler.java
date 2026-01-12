package engineering.enablement.claudecraft.network;

import engineering.enablement.claudecraft.ClaudeCraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Client-side handler for chat-related packets.
 * Maintains state and notifies UI when updates arrive.
 */
public class ClientChatHandler {
    private static final StringBuilder currentResponse = new StringBuilder();
    private static boolean isReceiving = false;
    private static String scratchPadContent = "";
    private static List<ClientboundConversationListPacket.ConversationSummary> conversationHistory = new ArrayList<>();
    private static String currentSessionId = null;

    // Chat messages for current conversation (persists across screen opens)
    private static final List<ChatMessageRecord> chatMessages = new ArrayList<>();

    public record ChatMessageRecord(boolean isUser, String content) {}

    // Callbacks for UI updates
    private static Consumer<String> onChunkCallback = null;
    private static Consumer<Boolean> onCompleteCallback = null;
    private static Consumer<String> onScratchPadCallback = null;
    private static Consumer<List<ClientboundConversationListPacket.ConversationSummary>> onConversationListCallback = null;
    private static Consumer<List<ChatMessageRecord>> onMessageHistoryCallback = null;

    /**
     * Register callbacks for UI updates.
     */
    public static void setCallbacks(
        Consumer<String> onChunk,
        Consumer<Boolean> onComplete,
        Consumer<String> onScratchPad,
        Consumer<List<ClientboundConversationListPacket.ConversationSummary>> onConversationList,
        Consumer<List<ChatMessageRecord>> onMessageHistory
    ) {
        onChunkCallback = onChunk;
        onCompleteCallback = onComplete;
        onScratchPadCallback = onScratchPad;
        onConversationListCallback = onConversationList;
        onMessageHistoryCallback = onMessageHistory;
    }

    /**
     * Clear all callbacks (call when closing UI).
     */
    public static void clearCallbacks() {
        onChunkCallback = null;
        onCompleteCallback = null;
        onScratchPadCallback = null;
        onConversationListCallback = null;
        onMessageHistoryCallback = null;
    }

    /**
     * Called when a chat chunk is received from server.
     */
    public static void onChatChunk(String text) {
        isReceiving = true;
        currentResponse.append(text);

        if (onChunkCallback != null) {
            onChunkCallback.accept(text);
        }

        ClaudeCraft.LOGGER.debug("Received chat chunk: {}", text);
    }

    /**
     * Called when chat response is complete.
     */
    public static void onChatComplete(boolean success, String errorMessage) {
        isReceiving = false;

        if (!success) {
            ClaudeCraft.LOGGER.warn("Chat failed: {}", errorMessage);
        }

        if (onCompleteCallback != null) {
            onCompleteCallback.accept(success);
        }

        // Clear the response buffer for next message
        currentResponse.setLength(0);
    }

    /**
     * Called when scratch pad content is synced from server.
     */
    public static void onScratchPadSync(String content) {
        ClaudeCraft.LOGGER.info("ClientChatHandler: onScratchPadSync received {} chars, callback={}",
            content != null ? content.length() : 0, onScratchPadCallback != null);
        scratchPadContent = content;

        if (onScratchPadCallback != null) {
            onScratchPadCallback.accept(content);
        }
    }

    /**
     * Called when conversation list is received from server.
     */
    public static void onConversationList(List<ClientboundConversationListPacket.ConversationSummary> sessions, String current) {
        conversationHistory = new ArrayList<>(sessions);
        currentSessionId = current;

        if (onConversationListCallback != null) {
            onConversationListCallback.accept(sessions);
        }
    }

    /**
     * Called when message history is received from server (on resume).
     */
    public static void onMessageHistory(String sessionId, List<ChatMessageRecord> messages) {
        currentSessionId = sessionId;
        chatMessages.clear();
        chatMessages.addAll(messages);

        if (onMessageHistoryCallback != null) {
            onMessageHistoryCallback.accept(messages);
        }
    }

    // Getters for UI

    public static String getCurrentResponse() {
        return currentResponse.toString();
    }

    public static boolean isReceiving() {
        return isReceiving;
    }

    public static String getScratchPadContent() {
        return scratchPadContent;
    }

    public static List<ClientboundConversationListPacket.ConversationSummary> getConversationHistory() {
        return new ArrayList<>(conversationHistory);
    }

    public static String getCurrentSessionId() {
        return currentSessionId;
    }

    // Chat message management

    public static void addMessage(boolean isUser, String content) {
        chatMessages.add(new ChatMessageRecord(isUser, content));
    }

    public static List<ChatMessageRecord> getChatMessages() {
        return new ArrayList<>(chatMessages);
    }

    public static void clearChatMessages() {
        chatMessages.clear();
    }
}
