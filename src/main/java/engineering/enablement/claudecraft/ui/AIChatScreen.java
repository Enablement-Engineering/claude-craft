package engineering.enablement.claudecraft.ui;

import engineering.enablement.claudecraft.ClaudeCraft;
import engineering.enablement.claudecraft.network.ClientChatHandler;
import engineering.enablement.claudecraft.network.ClientboundConversationListPacket.ConversationSummary;
import engineering.enablement.claudecraft.network.ServerboundChatPacket;
import engineering.enablement.claudecraft.network.ServerboundDeleteConversationPacket;
import engineering.enablement.claudecraft.network.ServerboundNewConversationPacket;
import engineering.enablement.claudecraft.network.ServerboundRequestConversationsPacket;
import engineering.enablement.claudecraft.network.ServerboundResumeConversationPacket;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextArea;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Main chat screen for interacting with the AI assistant.
 * Features a collapsible side panel for conversation history.
 * UI scales responsively to screen size.
 */
public class AIChatScreen extends ModularUIScreen {

    // Layout proportions (calculated from screen size)
    private static final float SCREEN_WIDTH_RATIO = 0.75f;   // Use 75% of screen width
    private static final float SCREEN_HEIGHT_RATIO = 0.75f;  // Use 75% of screen height
    private static final float PANEL_WIDTH_RATIO = 0.22f;    // Panel is 22% of chat width when expanded
    private static final int PANEL_COLLAPSED_WIDTH = 24;     // Fixed width when collapsed
    private static final int MIN_WIDTH = 400;                // Minimum total width
    private static final int MIN_HEIGHT = 250;               // Minimum total height

    // Calculated dimensions (set in init())
    private int totalWidth;
    private int totalHeight;
    private int panelExpandedWidth;
    private int mainContentWidth;

    // State
    private final List<ChatMessage> messages = new ArrayList<>();
    private final StringBuilder currentStreamingMessage = new StringBuilder();
    private boolean panelExpanded = false;
    private List<ConversationSummary> conversations = new ArrayList<>();

    // UI elements
    private ScrollerView messageContainer;
    private UIElement messageContent;
    private TextArea inputField;
    private Button sendButton;
    private Label statusLabel;
    private UIElement sidePanel;
    private UIElement conversationListContent;

    public AIChatScreen() {
        // Initial placeholder UI - will be rebuilt in init() with proper dimensions
        super(ModularUI.of(createPlaceholderUI()), Component.translatable("screen.claudecraft.chat"));
    }

    private static UI createPlaceholderUI() {
        var root = new UIElement();
        root.layout(layout -> layout
            .width(MIN_WIDTH)
            .height(MIN_HEIGHT)
            .paddingAll(10)
            .gapAll(5)
            .flexDirection(YogaFlexDirection.ROW)
        );
        root.style(style -> style.background(ColorPattern.T_DARK_GRAY.rectTexture()));
        return UI.of(root);
    }

    @Override
    public void init() {
        super.init();
        calculateDimensions();
        setupUI();
        registerCallbacks();
        loadMessages();
        loadConversations();
    }

    /**
     * Calculate UI dimensions based on screen size.
     */
    private void calculateDimensions() {
        // Calculate total dimensions from screen size
        totalWidth = Math.max(MIN_WIDTH, (int)(this.width * SCREEN_WIDTH_RATIO));
        totalHeight = Math.max(MIN_HEIGHT, (int)(this.height * SCREEN_HEIGHT_RATIO));

        // Calculate panel and content widths
        int contentAreaWidth = totalWidth - 20; // Account for padding
        panelExpandedWidth = Math.max(100, (int)(contentAreaWidth * PANEL_WIDTH_RATIO));
        mainContentWidth = contentAreaWidth - (panelExpanded ? panelExpandedWidth : PANEL_COLLAPSED_WIDTH) - 5;
    }

    private void loadMessages() {
        messages.clear();
        for (var msg : ClientChatHandler.getChatMessages()) {
            messages.add(new ChatMessage(msg.isUser(), msg.content()));
        }
        refreshMessages();
    }

    private void loadConversations() {
        conversations = new ArrayList<>(ClientChatHandler.getConversationHistory());
        refreshConversationList();
    }

    private void setupUI() {
        var root = modularUI.ui.getRootElement();
        root.clearAllChildren();

        // Recalculate dimensions (main content width depends on panel state)
        calculateDimensions();

        // Update root size based on calculated dimensions
        root.layout(layout -> layout
            .width(totalWidth)
            .height(totalHeight)
            .paddingAll(10)
            .gapAll(5)
            .flexDirection(YogaFlexDirection.ROW)
        );

        // Side panel
        sidePanel = createSidePanel();
        root.addChild(sidePanel);

        // Main content area
        var mainContent = createMainContent();
        root.addChild(mainContent);
    }

    private UIElement createSidePanel() {
        var panel = new UIElement();
        int panelWidth = panelExpanded ? panelExpandedWidth : PANEL_COLLAPSED_WIDTH;

        panel.layout(layout -> layout
            .width(panelWidth)
            .flexDirection(YogaFlexDirection.COLUMN)
            .gapAll(4)
        );
        panel.style(style -> style.background(ColorPattern.T_SEAL_BLACK.rectTexture()));

        // Toggle button (always visible)
        var toggleButton = new Button();
        toggleButton.setText(Component.literal(panelExpanded ? "<" : ">"));
        toggleButton.setOnClick(e -> toggleSidePanel());
        toggleButton.layout(layout -> layout.width(20).height(20));
        panel.addChild(toggleButton);

        if (panelExpanded) {
            // Panel title
            var panelTitle = new Label();
            panelTitle.setText(Component.translatable("screen.claudecraft.chat.conversations"));
            panelTitle.layout(layout -> layout.height(15).paddingLeft(4));
            panelTitle.textStyle(style -> style.textColor(0xFFAAAAAA));
            panel.addChild(panelTitle);

            // Conversation list (scrollable)
            var conversationScroller = new ScrollerView();
            conversationScroller.layout(layout -> layout
                .flexGrow(1)
                .width(panelExpandedWidth - 8)
            );

            conversationListContent = new UIElement();
            conversationListContent.layout(layout -> layout
                .flexDirection(YogaFlexDirection.COLUMN)
                .gapAll(2)
                .paddingAll(2)
            );
            conversationScroller.addScrollViewChild(conversationListContent);
            panel.addChild(conversationScroller);

            // New conversation button
            var newButton = new Button();
            newButton.setText(Component.translatable("screen.claudecraft.chat.new"));
            newButton.setOnClick(e -> startNewConversation());
            newButton.layout(layout -> layout.width(panelExpandedWidth - 8).height(25));
            panel.addChild(newButton);
        }

        return panel;
    }

    private UIElement createMainContent() {
        var mainContent = new UIElement();
        mainContent.layout(layout -> layout
            .width(mainContentWidth)
            .flexDirection(YogaFlexDirection.COLUMN)
            .gapAll(8)
        );

        // Title bar
        var titleBar = createTitleBar();
        mainContent.addChild(titleBar);

        // Message area (scrollable)
        messageContainer = createMessageArea();
        mainContent.addChild(messageContainer);

        // Input area
        var inputArea = createInputArea();
        mainContent.addChild(inputArea);

        // Status bar
        statusLabel = new Label();
        statusLabel.setText(Component.literal("Ready"));
        statusLabel.layout(layout -> layout.height(15));
        mainContent.addChild(statusLabel);

        return mainContent;
    }

    private UIElement createTitleBar() {
        var titleBar = new UIElement();
        titleBar.layout(layout -> layout
            .height(25)
            .flexDirection(YogaFlexDirection.ROW)
            .gapAll(5)
        );

        var title = new Label();
        title.setText(Component.translatable("screen.claudecraft.chat.title"));
        title.layout(layout -> layout.flexGrow(1));
        titleBar.addChild(title);

        // Only show new/history buttons when panel is collapsed
        if (!panelExpanded) {
            var newButton = new Button();
            newButton.setText(Component.translatable("screen.claudecraft.chat.new"));
            newButton.setOnClick(e -> startNewConversation());
            newButton.layout(layout -> layout.width(50));
            titleBar.addChild(newButton);

            var historyButton = new Button();
            historyButton.setText(Component.translatable("screen.claudecraft.chat.history"));
            historyButton.setOnClick(e -> toggleSidePanel());
            historyButton.layout(layout -> layout.width(60));
            titleBar.addChild(historyButton);
        }

        return titleBar;
    }

    private ScrollerView createMessageArea() {
        var scrollable = new ScrollerView();
        scrollable.layout(layout -> layout
            .flexGrow(1)
            .width(mainContentWidth - 10)
        );
        scrollable.style(style -> style.background(ColorPattern.T_BLACK.rectTexture()));

        messageContent = new UIElement();
        messageContent.layout(layout -> layout
            .flexDirection(YogaFlexDirection.COLUMN)
            .gapAll(5)
            .paddingAll(5)
        );
        scrollable.addScrollViewChild(messageContent);

        return scrollable;
    }

    private UIElement createInputArea() {
        var inputArea = new UIElement();
        inputArea.layout(layout -> layout
            .height(50)
            .flexDirection(YogaFlexDirection.ROW)
            .gapAll(5)
        );

        inputField = new TextArea();
        inputField.layout(layout -> layout.flexGrow(1).height(45));
        inputArea.addChild(inputField);

        sendButton = new Button();
        sendButton.setText(Component.translatable("screen.claudecraft.chat.send"));
        sendButton.setOnClick(e -> sendMessage());
        sendButton.layout(layout -> layout.width(50).height(45));
        inputArea.addChild(sendButton);

        return inputArea;
    }

    private void toggleSidePanel() {
        panelExpanded = !panelExpanded;
        setupUI();
        // Re-render messages after UI rebuild
        refreshMessages();
        if (panelExpanded) {
            // Request conversation list from server
            PacketDistributor.sendToServer(new ServerboundRequestConversationsPacket());
            refreshConversationList();
        }
    }

    private void refreshConversationList() {
        if (conversationListContent == null) return;

        conversationListContent.clearAllChildren();
        String currentSessionId = ClientChatHandler.getCurrentSessionId();

        for (ConversationSummary conv : conversations) {
            var item = createConversationItem(conv, conv.sessionId().equals(currentSessionId));
            conversationListContent.addChild(item);
        }
    }

    private UIElement createConversationItem(ConversationSummary conv, boolean isCurrent) {
        var item = new UIElement();
        item.layout(layout -> layout
            .height(45)
            .width(panelExpandedWidth - 16)
            .paddingAll(4)
            .flexDirection(YogaFlexDirection.COLUMN)
        );

        // Highlight current conversation
        if (isCurrent) {
            item.style(style -> style.background(ColorPattern.T_GRAY.rectTexture()));
        }

        // Preview text (truncated)
        var preview = new Label();
        String previewText = conv.preview().isEmpty() ? "(empty)" : conv.preview();
        preview.setText(Component.literal(previewText));
        preview.layout(layout -> layout.height(14).width(panelExpandedWidth - 24));
        item.addChild(preview);

        // Timestamp
        var timestamp = new Label();
        timestamp.setText(Component.literal(formatTimestamp(conv.timestamp())));
        timestamp.layout(layout -> layout.height(12));
        timestamp.textStyle(style -> style.textColor(0xFF888888));
        item.addChild(timestamp);

        // Button row with open and delete
        var buttonRow = new UIElement();
        buttonRow.layout(layout -> layout
            .height(16)
            .flexDirection(YogaFlexDirection.ROW)
            .gapAll(4)
        );

        var openButton = new Button();
        openButton.setText(Component.literal("Open"));
        openButton.layout(layout -> layout.width(40).height(14));
        openButton.setOnClick(e -> resumeConversation(conv.sessionId()));
        buttonRow.addChild(openButton);

        // Don't allow deleting the current conversation
        if (!isCurrent) {
            var deleteButton = new Button();
            deleteButton.setText(Component.literal("X"));
            deleteButton.layout(layout -> layout.width(16).height(14));
            deleteButton.setOnClick(e -> deleteConversation(conv.sessionId()));
            buttonRow.addChild(deleteButton);
        }

        item.addChild(buttonRow);

        return item;
    }

    private String formatTimestamp(long millis) {
        if (millis == 0) return "";
        var instant = Instant.ofEpochMilli(millis);
        var formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm")
            .withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }

    private void resumeConversation(String sessionId) {
        ClaudeCraft.LOGGER.info("Resuming conversation: {}", sessionId);
        messages.clear();
        currentStreamingMessage.setLength(0);
        statusLabel.setText(Component.literal("Loading conversation..."));

        // Send resume request to server
        PacketDistributor.sendToServer(new ServerboundResumeConversationPacket(sessionId));
    }

    private void deleteConversation(String sessionId) {
        ClaudeCraft.LOGGER.info("Deleting conversation: {}", sessionId);

        // Send delete request to server
        PacketDistributor.sendToServer(new ServerboundDeleteConversationPacket(sessionId));

        // UI will update when server sends back updated conversation list
        statusLabel.setText(Component.literal("Deleted conversation"));
    }

    private void registerCallbacks() {
        ClientChatHandler.setCallbacks(
            this::onChunkReceived,
            this::onResponseComplete,
            null,  // scratch pad callback not needed here
            this::onConversationListReceived,
            this::onMessageHistoryReceived
        );
    }

    private void onChunkReceived(String chunk) {
        currentStreamingMessage.append(chunk);
        updateStreamingMessage();
    }

    private void onResponseComplete(boolean success) {
        if (success && currentStreamingMessage.length() > 0) {
            String response = currentStreamingMessage.toString();
            messages.add(new ChatMessage(false, response));
            ClientChatHandler.addMessage(false, response);
            currentStreamingMessage.setLength(0);
            refreshMessages();
        }
        statusLabel.setText(Component.literal(success ? "Ready" : "Error occurred"));
        sendButton.setActive(true);
        inputField.setActive(true);
    }

    private void onConversationListReceived(List<ConversationSummary> sessions) {
        conversations = new ArrayList<>(sessions);
        refreshConversationList();
    }

    private void onMessageHistoryReceived(List<ClientChatHandler.ChatMessageRecord> history) {
        messages.clear();
        for (var msg : history) {
            messages.add(new ChatMessage(msg.isUser(), msg.content()));
        }
        refreshMessages();
        statusLabel.setText(Component.literal("Conversation loaded"));
    }

    private void updateStreamingMessage() {
        refreshMessages();
    }

    private void refreshMessages() {
        if (messageContent == null) return;

        messageContent.clearAllChildren();

        for (ChatMessage msg : messages) {
            var label = createMessageLabel(msg.isUser() ? "You: " : "AI: ", msg.content(), !msg.isUser());
            messageContent.addChild(label);
        }

        // Add streaming message placeholder if receiving
        if (ClientChatHandler.isReceiving() && currentStreamingMessage.length() > 0) {
            var streamLabel = createMessageLabel("AI: ", currentStreamingMessage.toString(), true);
            messageContent.addChild(streamLabel);
        }
    }

    private Label createMessageLabel(String prefix, String content, boolean isAI) {
        var label = new Label();

        if (isAI) {
            MutableComponent msg = Component.literal(prefix).withStyle(ChatFormatting.GRAY);
            msg.append(MarkdownToMinecraft.convert(content));
            label.setText(msg);
        } else {
            label.setText(Component.literal(prefix + content));
        }

        label.layout(layout -> layout
            .width(mainContentWidth - 30)
            .paddingAll(5)
        );
        label.textStyle(style -> style
            .textWrap(TextWrap.WRAP)
            .adaptiveHeight(true)
            .lineSpacing(2)
        );
        if (isAI) {
            label.style(style -> style.background(ColorPattern.T_SEAL_BLACK.rectTexture()));
        }
        return label;
    }

    private void sendMessage() {
        String[] lines = inputField.getValue();
        if (lines == null || lines.length == 0) {
            return;
        }

        String text = String.join("\n", lines).trim();
        if (text.isEmpty()) {
            return;
        }

        messages.add(new ChatMessage(true, text));
        ClientChatHandler.addMessage(true, text);
        inputField.setLines(List.of(""));
        refreshMessages();

        sendButton.setActive(false);
        inputField.setActive(false);
        statusLabel.setText(Component.literal("Thinking..."));

        PacketDistributor.sendToServer(new ServerboundChatPacket(text));
    }

    private void startNewConversation() {
        messages.clear();
        ClientChatHandler.clearChatMessages();
        currentStreamingMessage.setLength(0);
        refreshMessages();
        statusLabel.setText(Component.literal("New conversation started"));
        PacketDistributor.sendToServer(new ServerboundNewConversationPacket());
    }

    @Override
    public void removed() {
        super.removed();
        ClientChatHandler.clearCallbacks();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            boolean shiftHeld = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;

            if (!shiftHeld && inputField != null && sendButton.isActive()) {
                sendMessage();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private record ChatMessage(boolean isUser, String content) {}
}
