package engineering.enablement.claudecraft;

import engineering.enablement.claudecraft.data.PlayerDataManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Claude Craft - An AI-powered assistant using Claude Code in headless mode.
 *
 * This mod provides:
 * - Chat interface for talking to Claude
 * - File-based memory system (CLAUDE.md, scratch.md, notes/)
 * - Skills for accessing Minecraft game state
 * - LDLib2-based UI with pinnable overlays
 */
@Mod(ClaudeCraft.MOD_ID)
public class ClaudeCraft {
    public static final String MOD_ID = "claudecraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static PlayerDataManager dataManager;
    private int tickCounter = 0;
    private static final int STATE_UPDATE_INTERVAL = 20; // Update every second (20 ticks)

    public ClaudeCraft(IEventBus modEventBus, ModContainer modContainer) {
        // Register mod lifecycle events
        modEventBus.addListener(this::commonSetup);

        // Register server events
        NeoForge.EVENT_BUS.register(this);

        LOGGER.info("Claude Craft mod initialized");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Claude Craft common setup");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Claude Craft: Server starting, initializing plugin directory...");

        try {
            dataManager = new PlayerDataManager(event.getServer());
            dataManager.initialize();
            LOGGER.info("Claude Craft: Plugin directory initialized at {}",
                dataManager.getPluginDir());
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Claude Craft plugin directory", e);
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        LOGGER.info("Player {} joined, initializing AI data...", player.getName().getString());

        try {
            if (dataManager != null) {
                dataManager.initializePlayer(player.getUUID());
                dataManager.updatePlayerState(player);
            }

            // Start fresh conversation on join (player can resume old ones via History)
            engineering.enablement.claudecraft.network.ChatSessionManager.newConversation(player.getUUID());
            LOGGER.info("Started fresh conversation for player {}", player.getName().getString());

        } catch (Exception e) {
            LOGGER.error("Failed to initialize player data for {}", player.getName().getString(), e);
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        java.util.UUID playerUuid = player.getUUID();
        LOGGER.info("Player {} disconnected, cleaning up AI resources...",
            player.getName().getString());

        // Clean up session manager state and cancel active processes
        engineering.enablement.claudecraft.network.ChatSessionManager.onPlayerDisconnect(playerUuid);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter < STATE_UPDATE_INTERVAL) {
            return;
        }
        tickCounter = 0;

        // Update state files for all online players
        if (dataManager == null) {
            return;
        }

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            try {
                dataManager.updatePlayerState(player);
            } catch (Exception e) {
                // Don't spam logs, just log debug
                LOGGER.debug("Failed to update state for {}: {}",
                    player.getName().getString(), e.getMessage());
            }
        }
    }

    /**
     * Get the data manager instance.
     */
    public static PlayerDataManager getDataManager() {
        return dataManager;
    }
}
