package engineering.enablement.claudecraft.ui;

import engineering.enablement.claudecraft.ClaudeCraft;
import engineering.enablement.claudecraft.network.ClientChatHandler;
import engineering.enablement.claudecraft.network.ServerboundScratchPadRequestPacket;
import engineering.enablement.claudecraft.network.ServerboundScratchPadUpdatePacket;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextArea;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.appliedenergistics.yoga.YogaFlexDirection;

import java.util.Arrays;
import java.util.List;

/**
 * Scratch pad screen for taking notes that persist across sessions.
 * The AI can also read and write to this scratch pad.
 */
public class ScratchPadScreen extends ModularUIScreen {

    private TextArea textArea;
    private Label statusLabel;
    private boolean isDirty = false;

    public ScratchPadScreen() {
        super(ModularUI.of(createUI()), Component.translatable("screen.claudecraft.scratchpad"));
    }

    private static UI createUI() {
        var root = new UIElement();
        root.layout(layout -> layout
            .width(350)
            .height(320)
            .paddingAll(10)
            .gapAll(8)
            .flexDirection(YogaFlexDirection.COLUMN)
        );
        root.style(style -> style.background(ColorPattern.T_DARK_GRAY.rectTexture()));

        return UI.of(root);
    }

    @Override
    public void init() {
        super.init();
        setupUI();
        registerCallbacks();
        loadContent();
    }

    private void setupUI() {
        var root = modularUI.ui.getRootElement();
        root.clearAllChildren();

        // Title bar
        var titleBar = createTitleBar();
        root.addChild(titleBar);

        // Text area
        textArea = new TextArea();
        textArea.layout(layout -> layout
            .flexGrow(1)
            .width(330)
        );
        textArea.style(style -> style.background(ColorPattern.T_BLACK.rectTexture()));
        textArea.setLinesResponder(lines -> isDirty = true);
        root.addChild(textArea);

        // Button bar
        var buttonBar = createButtonBar();
        root.addChild(buttonBar);

        // Status bar
        statusLabel = new Label();
        statusLabel.setText(Component.literal(""));
        statusLabel.layout(layout -> layout.height(20).width(330));
        root.addChild(statusLabel);
    }

    private UIElement createTitleBar() {
        var titleBar = new UIElement();
        titleBar.layout(layout -> layout
            .height(25)
            .flexDirection(YogaFlexDirection.ROW)
        );

        var title = new Label();
        title.setText(Component.translatable("screen.claudecraft.scratchpad.title"));
        title.layout(layout -> layout.flexGrow(1));
        titleBar.addChild(title);

        return titleBar;
    }

    private UIElement createButtonBar() {
        var buttonBar = new UIElement();
        buttonBar.layout(layout -> layout
            .height(30)
            .flexDirection(YogaFlexDirection.ROW)
            .gapAll(5)
        );

        var saveButton = new Button();
        saveButton.setText(Component.translatable("screen.claudecraft.scratchpad.save"));
        saveButton.setOnClick(e -> saveContent());
        saveButton.layout(layout -> layout.width(60).height(25));
        buttonBar.addChild(saveButton);

        var clearButton = new Button();
        clearButton.setText(Component.translatable("screen.claudecraft.scratchpad.clear"));
        clearButton.setOnClick(e -> clearContent());
        clearButton.layout(layout -> layout.width(60).height(25));
        buttonBar.addChild(clearButton);

        // Spacer
        var spacer = new UIElement();
        spacer.layout(layout -> layout.flexGrow(1));
        buttonBar.addChild(spacer);

        var closeButton = new Button();
        closeButton.setText(Component.translatable("screen.claudecraft.scratchpad.close"));
        closeButton.setOnClick(e -> onClose());
        closeButton.layout(layout -> layout.width(60).height(25));
        buttonBar.addChild(closeButton);

        return buttonBar;
    }

    private void registerCallbacks() {
        ClientChatHandler.setCallbacks(
            null,  // chunk callback not needed
            null,  // complete callback not needed
            this::onScratchPadSync,
            null,  // conversation list callback not needed
            null   // message history callback not needed
        );
    }

    private void onScratchPadSync(String content) {
        ClaudeCraft.LOGGER.info("ScratchPadScreen: onScratchPadSync called with {} chars, textArea={}, isDirty={}",
            content != null ? content.length() : 0, textArea != null, isDirty);
        if (textArea != null && !isDirty) {
            List<String> lines = Arrays.asList(content.split("\n", -1));
            ClaudeCraft.LOGGER.info("ScratchPadScreen: Setting {} lines in textArea", lines.size());
            textArea.setLines(lines);
            statusLabel.setText(Component.literal("Synced from server"));
        }
    }

    private void loadContent() {
        // Request content from server
        ClaudeCraft.LOGGER.info("ScratchPadScreen: Requesting scratch pad content from server");
        PacketDistributor.sendToServer(new ServerboundScratchPadRequestPacket());

        // Also load any cached content while waiting
        String content = ClientChatHandler.getScratchPadContent();
        ClaudeCraft.LOGGER.info("ScratchPadScreen: Cached content = '{}' ({} chars)",
            content != null && content.length() > 30 ? content.substring(0, 30) + "..." : content,
            content != null ? content.length() : 0);
        if (content != null && !content.isEmpty()) {
            List<String> lines = Arrays.asList(content.split("\n", -1));
            textArea.setLines(lines);
        }
        isDirty = false;
    }

    private void saveContent() {
        String[] lines = textArea.getValue();
        ClaudeCraft.LOGGER.info("TextArea getValue returned: {} lines", lines != null ? lines.length : "null");

        String content = lines != null ? String.join("\n", lines) : "";
        ClaudeCraft.LOGGER.info("Saving scratch pad content ({} chars): {}", content.length(),
            content.length() > 50 ? content.substring(0, 50) + "..." : content);

        // Update local cache so overlay refreshes immediately
        ClientChatHandler.onScratchPadSync(content);

        PacketDistributor.sendToServer(new ServerboundScratchPadUpdatePacket(content));
        statusLabel.setText(Component.literal("Saved!").withStyle(ChatFormatting.GREEN));
        isDirty = false;
    }

    private void clearContent() {
        textArea.setLines(List.of(""));
        saveContent();
        statusLabel.setText(Component.literal("Cleared!").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void removed() {
        super.removed();
        // Auto-save on close if dirty
        if (isDirty) {
            saveContent();
        }
        ClientChatHandler.clearCallbacks();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
