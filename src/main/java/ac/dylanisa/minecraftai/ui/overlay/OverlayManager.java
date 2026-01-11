package ac.dylanisa.minecraftai.ui.overlay;

import ac.dylanisa.minecraftai.MinecraftAI;
import net.minecraft.client.Minecraft;

import java.util.EnumMap;
import java.util.Map;

/**
 * Manages pinnable overlay panels that stay visible during gameplay.
 */
public class OverlayManager {

    private static final OverlayManager INSTANCE = new OverlayManager();

    public enum OverlayType {
        CHAT,
        SCRATCH_PAD
    }

    private final Map<OverlayType, OverlayState> overlayStates = new EnumMap<>(OverlayType.class);

    private OverlayManager() {
        // Initialize all overlays as hidden
        for (OverlayType type : OverlayType.values()) {
            overlayStates.put(type, new OverlayState());
        }
    }

    public static OverlayManager getInstance() {
        return INSTANCE;
    }

    /**
     * Toggle the visibility of an overlay.
     */
    public void toggleOverlay(OverlayType type) {
        OverlayState state = overlayStates.get(type);
        state.visible = !state.visible;
        MinecraftAI.LOGGER.debug("Toggled {} overlay: {}", type, state.visible);
    }

    /**
     * Check if an overlay is visible.
     */
    public boolean isOverlayVisible(OverlayType type) {
        return overlayStates.get(type).visible;
    }

    /**
     * Set overlay visibility.
     */
    public void setOverlayVisible(OverlayType type, boolean visible) {
        overlayStates.get(type).visible = visible;
    }

    /**
     * Get the position of an overlay.
     */
    public OverlayPosition getOverlayPosition(OverlayType type) {
        return overlayStates.get(type).position;
    }

    /**
     * Set the position of an overlay.
     */
    public void setOverlayPosition(OverlayType type, int x, int y) {
        OverlayState state = overlayStates.get(type);
        state.position.x = x;
        state.position.y = y;
    }

    /**
     * Get the size of an overlay.
     */
    public OverlaySize getOverlaySize(OverlayType type) {
        return overlayStates.get(type).size;
    }

    /**
     * Set the size of an overlay.
     */
    public void setOverlaySize(OverlayType type, int width, int height) {
        OverlayState state = overlayStates.get(type);
        state.size.width = width;
        state.size.height = height;
    }

    /**
     * Check if any overlay is visible.
     */
    public boolean hasVisibleOverlays() {
        for (OverlayState state : overlayStates.values()) {
            if (state.visible) {
                return true;
            }
        }
        return false;
    }

    /**
     * Hide all overlays.
     */
    public void hideAllOverlays() {
        for (OverlayState state : overlayStates.values()) {
            state.visible = false;
        }
    }

    /**
     * Check if any overlay contains the given screen coordinates.
     */
    public OverlayType getOverlayAt(double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            return null; // Don't interact when a screen is open
        }

        for (Map.Entry<OverlayType, OverlayState> entry : overlayStates.entrySet()) {
            OverlayState state = entry.getValue();
            if (state.visible && state.containsPoint(mouseX, mouseY)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * State for a single overlay.
     */
    private static class OverlayState {
        boolean visible = false;
        final OverlayPosition position = new OverlayPosition();
        final OverlaySize size = new OverlaySize();

        boolean containsPoint(double x, double y) {
            return x >= position.x && x <= position.x + size.width
                && y >= position.y && y <= position.y + size.height;
        }
    }

    /**
     * Position of an overlay.
     */
    public static class OverlayPosition {
        public int x = 10;
        public int y = 10;
    }

    /**
     * Size of an overlay.
     */
    public static class OverlaySize {
        public int width = 250;
        public int height = 180;
    }
}
