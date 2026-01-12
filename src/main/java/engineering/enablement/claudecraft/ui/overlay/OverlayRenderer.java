package engineering.enablement.claudecraft.ui.overlay;

import engineering.enablement.claudecraft.ClaudeCraft;
import engineering.enablement.claudecraft.network.ClientChatHandler;
import engineering.enablement.claudecraft.ui.MarkdownToMinecraft;
import engineering.enablement.claudecraft.ui.overlay.OverlayManager.OverlayPosition;
import engineering.enablement.claudecraft.ui.overlay.OverlayManager.OverlaySize;
import engineering.enablement.claudecraft.ui.overlay.OverlayManager.OverlayType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders overlay panels on the HUD during gameplay.
 */
@EventBusSubscriber(modid = ClaudeCraft.MOD_ID, value = Dist.CLIENT)
public class OverlayRenderer {

    private static final int BACKGROUND_COLOR = 0xCC1a1a1a;  // Semi-transparent dark gray
    private static final int BORDER_COLOR = 0xFF3a3a3a;      // Lighter gray border
    private static final int TITLE_BAR_COLOR = 0xFF2a2a2a;   // Slightly lighter for title
    private static final int TEXT_COLOR = 0xFFFFFFFF;        // White text
    private static final int TITLE_HEIGHT = 14;
    private static final int PADDING = 4;

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        // Only render after the hotbar layer to ensure overlays are on top
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        // Don't render if a screen is open or player is null
        if (mc.screen != null || mc.player == null) {
            return;
        }

        OverlayManager manager = OverlayManager.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();

        // Render each visible overlay
        if (manager.isOverlayVisible(OverlayType.CHAT)) {
            renderChatOverlay(graphics, manager);
        }

        if (manager.isOverlayVisible(OverlayType.SCRATCH_PAD)) {
            renderScratchPadOverlay(graphics, manager);
        }
    }

    private static void renderChatOverlay(GuiGraphics graphics, OverlayManager manager) {
        OverlayPosition pos = manager.getOverlayPosition(OverlayType.CHAT);
        OverlaySize size = manager.getOverlaySize(OverlayType.CHAT);

        // Draw background
        drawOverlayBackground(graphics, pos.x, pos.y, size.width, size.height, "AI Chat");

        // Draw chat content
        Minecraft mc = Minecraft.getInstance();
        int contentY = pos.y + TITLE_HEIGHT + PADDING;
        int contentHeight = size.height - TITLE_HEIGHT - PADDING * 2;
        int maxLines = contentHeight / 10;

        // Get recent messages from client handler
        String currentResponse = ClientChatHandler.getCurrentResponse();
        List<String> lines = new ArrayList<>();

        if (currentResponse != null && !currentResponse.isEmpty()) {
            // Wrap long lines
            lines.addAll(wrapText(currentResponse, size.width - PADDING * 2, mc));
        } else {
            lines.add("Press \\ to open chat");
        }

        // Show only the last N lines that fit, with markdown rendering
        int startLine = Math.max(0, lines.size() - maxLines);
        int y = contentY;
        for (int i = startLine; i < lines.size() && y < pos.y + size.height - PADDING; i++) {
            Component formatted = MarkdownToMinecraft.convert(lines.get(i));
            graphics.drawString(mc.font, formatted, pos.x + PADDING, y, TEXT_COLOR, false);
            y += 10;
        }

        // Show status if receiving
        if (ClientChatHandler.isReceiving()) {
            String status = "Thinking...";
            graphics.drawString(mc.font, status, pos.x + PADDING, pos.y + size.height - 12, 0xFF88FF88, false);
        }
    }

    private static void renderScratchPadOverlay(GuiGraphics graphics, OverlayManager manager) {
        OverlayPosition pos = manager.getOverlayPosition(OverlayType.SCRATCH_PAD);
        OverlaySize size = manager.getOverlaySize(OverlayType.SCRATCH_PAD);

        // Offset scratch pad to not overlap with chat
        int actualX = pos.x;
        if (manager.isOverlayVisible(OverlayType.CHAT)) {
            OverlayPosition chatPos = manager.getOverlayPosition(OverlayType.CHAT);
            OverlaySize chatSize = manager.getOverlaySize(OverlayType.CHAT);
            if (pos.x < chatPos.x + chatSize.width + 10) {
                actualX = chatPos.x + chatSize.width + 10;
            }
        }

        // Draw background
        drawOverlayBackground(graphics, actualX, pos.y, size.width, size.height, "Scratch Pad");

        // Draw scratch pad content
        Minecraft mc = Minecraft.getInstance();
        int contentY = pos.y + TITLE_HEIGHT + PADDING;
        int contentHeight = size.height - TITLE_HEIGHT - PADDING * 2;
        int maxLines = contentHeight / 10;

        String content = ClientChatHandler.getScratchPadContent();
        List<String> lines = new ArrayList<>();

        if (content != null && !content.isEmpty()) {
            String[] contentLines = content.split("\n");
            for (String line : contentLines) {
                lines.addAll(wrapText(line, size.width - PADDING * 2, mc));
            }
        } else {
            lines.add("Press ' to edit");
        }

        // Show only lines that fit, with markdown rendering
        int y = contentY;
        for (int i = 0; i < lines.size() && i < maxLines && y < pos.y + size.height - PADDING; i++) {
            Component formatted = MarkdownToMinecraft.convert(lines.get(i));
            graphics.drawString(mc.font, formatted, actualX + PADDING, y, TEXT_COLOR, false);
            y += 10;
        }

        // Show ellipsis if content is truncated
        if (lines.size() > maxLines) {
            graphics.drawString(mc.font, "...", actualX + PADDING, pos.y + size.height - 12, 0xFF888888, false);
        }
    }

    private static void drawOverlayBackground(GuiGraphics graphics, int x, int y, int width, int height, String title) {
        Minecraft mc = Minecraft.getInstance();

        // Main background
        graphics.fill(x, y, x + width, y + height, BACKGROUND_COLOR);

        // Border
        graphics.fill(x, y, x + width, y + 1, BORDER_COLOR);                    // Top
        graphics.fill(x, y + height - 1, x + width, y + height, BORDER_COLOR);  // Bottom
        graphics.fill(x, y, x + 1, y + height, BORDER_COLOR);                    // Left
        graphics.fill(x + width - 1, y, x + width, y + height, BORDER_COLOR);   // Right

        // Title bar
        graphics.fill(x + 1, y + 1, x + width - 1, y + TITLE_HEIGHT, TITLE_BAR_COLOR);

        // Title text
        graphics.drawString(mc.font, Component.literal(title), x + PADDING, y + 3, TEXT_COLOR, false);

        // Pin indicator
        graphics.drawString(mc.font, "\u25CB", x + width - 12, y + 3, 0xFF88FF88, false);  // Circle = pinned
    }

    private static List<String> wrapText(String text, int maxWidth, Minecraft mc) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }

        StringBuilder currentLine = new StringBuilder();
        String[] words = text.split(" ");

        for (String word : words) {
            String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
            int width = mc.font.width(testLine);

            if (width > maxWidth && currentLine.length() > 0) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }
}
