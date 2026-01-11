package ac.dylanisa.minecraftai.ui;

import ac.dylanisa.minecraftai.MinecraftAI;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Keybind registration for the Minecraft AI mod.
 */
@EventBusSubscriber(modid = MinecraftAI.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModKeybinds {

    public static final String KEY_CATEGORY = "key.categories.minecraftai";

    public static final KeyMapping OPEN_CHAT = new KeyMapping(
        "key.minecraftai.open_chat",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_BACKSLASH,  // \ key
        KEY_CATEGORY
    );

    public static final KeyMapping OPEN_SCRATCHPAD = new KeyMapping(
        "key.minecraftai.open_scratchpad",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_APOSTROPHE,  // ' key
        KEY_CATEGORY
    );

    public static final KeyMapping TOGGLE_CHAT_OVERLAY = new KeyMapping(
        "key.minecraftai.toggle_chat_overlay",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_MINUS,  // - key for pinning chat overlay
        KEY_CATEGORY
    );

    public static final KeyMapping TOGGLE_SCRATCHPAD_OVERLAY = new KeyMapping(
        "key.minecraftai.toggle_scratchpad_overlay",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_EQUAL,  // = key for pinning scratch pad overlay
        KEY_CATEGORY
    );

    public static final KeyMapping HIDE_ALL_OVERLAYS = new KeyMapping(
        "key.minecraftai.hide_all_overlays",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_0,  // 0 key to hide all overlays
        KEY_CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CHAT);
        event.register(OPEN_SCRATCHPAD);
        event.register(TOGGLE_CHAT_OVERLAY);
        event.register(TOGGLE_SCRATCHPAD_OVERLAY);
        event.register(HIDE_ALL_OVERLAYS);
        MinecraftAI.LOGGER.info("Registered Minecraft AI keybinds");
    }
}
