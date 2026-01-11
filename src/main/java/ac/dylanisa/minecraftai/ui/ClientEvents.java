package ac.dylanisa.minecraftai.ui;

import ac.dylanisa.minecraftai.MinecraftAI;
import ac.dylanisa.minecraftai.network.ServerboundScratchPadRequestPacket;
import ac.dylanisa.minecraftai.ui.overlay.OverlayManager;
import ac.dylanisa.minecraftai.ui.overlay.OverlayManager.OverlayType;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-side event handling for the Minecraft AI mod.
 */
@EventBusSubscriber(modid = MinecraftAI.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        // Check for chat keybind (opens full screen)
        while (ModKeybinds.OPEN_CHAT.consumeClick()) {
            openChatScreen();
        }

        // Check for scratchpad keybind (opens full screen)
        while (ModKeybinds.OPEN_SCRATCHPAD.consumeClick()) {
            openScratchPadScreen();
        }

        // Check for overlay toggle keybinds
        while (ModKeybinds.TOGGLE_CHAT_OVERLAY.consumeClick()) {
            toggleChatOverlay();
        }

        while (ModKeybinds.TOGGLE_SCRATCHPAD_OVERLAY.consumeClick()) {
            toggleScratchPadOverlay();
        }

        while (ModKeybinds.HIDE_ALL_OVERLAYS.consumeClick()) {
            hideAllOverlays();
        }
    }

    private static void openChatScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) {
            mc.setScreen(new AIChatScreen());
        }
    }

    private static void openScratchPadScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) {
            mc.setScreen(new ScratchPadScreen());
        }
    }

    private static void toggleChatOverlay() {
        OverlayManager manager = OverlayManager.getInstance();
        manager.toggleOverlay(OverlayType.CHAT);
        MinecraftAI.LOGGER.debug("Chat overlay toggled: {}", manager.isOverlayVisible(OverlayType.CHAT));
    }

    private static void toggleScratchPadOverlay() {
        OverlayManager manager = OverlayManager.getInstance();
        manager.toggleOverlay(OverlayType.SCRATCH_PAD);
        boolean visible = manager.isOverlayVisible(OverlayType.SCRATCH_PAD);
        MinecraftAI.LOGGER.debug("Scratch pad overlay toggled: {}", visible);

        // Request content from server when showing overlay
        if (visible) {
            PacketDistributor.sendToServer(new ServerboundScratchPadRequestPacket());
        }
    }

    private static void hideAllOverlays() {
        OverlayManager manager = OverlayManager.getInstance();
        manager.hideAllOverlays();
        MinecraftAI.LOGGER.debug("All overlays hidden");
    }
}
