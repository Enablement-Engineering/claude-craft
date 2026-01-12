package engineering.enablement.claudecraft.data;

import engineering.enablement.claudecraft.ClaudeCraft;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.UUID;

/**
 * Manages player data directories and state files for the AI.
 */
public class PlayerDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path pluginDir;
    private final Path playersDir;

    public PlayerDataManager(MinecraftServer server) {
        // Use the server's world directory
        this.pluginDir = server.getServerDirectory().resolve("plugins").resolve("claude-craft");
        this.playersDir = pluginDir.resolve("players");
    }

    /**
     * Initialize the plugin directory structure.
     * Copies default files from resources if they don't exist.
     */
    public void initialize() throws IOException {
        ClaudeCraft.LOGGER.info("Initializing Claude Craft plugin directory: {}", pluginDir);

        // Create directories
        Files.createDirectories(pluginDir);
        Files.createDirectories(playersDir);
        Files.createDirectories(pluginDir.resolve(".claude").resolve("hooks"));
        Files.createDirectories(pluginDir.resolve(".claude").resolve("skills"));
        Files.createDirectories(pluginDir.resolve("bin"));

        // Copy/update mod-provided system files (always overwrite to get updates)
        ClaudeCraft.LOGGER.info("Updating Claude Craft system files from mod resources...");

        copyResource("claude-craft-defaults/CLAUDE.md", pluginDir.resolve("CLAUDE.md"));
        copyResource("claude-craft-defaults/.claude/settings.json",
            pluginDir.resolve(".claude").resolve("settings.json"));
        copyResource("claude-craft-defaults/.claude/hooks/validate_write.sh",
            pluginDir.resolve(".claude").resolve("hooks").resolve("validate_write.sh"));
        copyResource("claude-craft-defaults/.claude/hooks/validate_bash.sh",
            pluginDir.resolve(".claude").resolve("hooks").resolve("validate_bash.sh"));

        // Copy/update skills (always overwrite)
        copySkill("mc-get-position");
        copySkill("mc-get-inventory");
        copySkill("mc-get-nearby");
        copySkill("mc-get-world-info");

        // Copy/update bin scripts (always overwrite)
        copyResource("claude-craft-defaults/bin/mc-get-position.sh",
            pluginDir.resolve("bin").resolve("mc-get-position.sh"));
        copyResource("claude-craft-defaults/bin/mc-get-inventory.sh",
            pluginDir.resolve("bin").resolve("mc-get-inventory.sh"));
        copyResource("claude-craft-defaults/bin/mc-get-nearby.sh",
            pluginDir.resolve("bin").resolve("mc-get-nearby.sh"));
        copyResource("claude-craft-defaults/bin/mc-get-world-info.sh",
            pluginDir.resolve("bin").resolve("mc-get-world-info.sh"));

        // Make scripts executable
        makeExecutable(pluginDir.resolve(".claude").resolve("hooks").resolve("validate_write.sh"));
        makeExecutable(pluginDir.resolve(".claude").resolve("hooks").resolve("validate_bash.sh"));
        makeExecutable(pluginDir.resolve("bin").resolve("mc-get-position.sh"));
        makeExecutable(pluginDir.resolve("bin").resolve("mc-get-inventory.sh"));
        makeExecutable(pluginDir.resolve("bin").resolve("mc-get-nearby.sh"));
        makeExecutable(pluginDir.resolve("bin").resolve("mc-get-world-info.sh"));

        ClaudeCraft.LOGGER.info("Claude Craft plugin directory initialized");
    }

    /**
     * Copy a skill from resources, always overwriting to get updates.
     */
    private void copySkill(String skillName) throws IOException {
        Path skillDir = pluginDir.resolve(".claude").resolve("skills").resolve(skillName);
        Files.createDirectories(skillDir);
        copyResource(
            "claude-craft-defaults/.claude/skills/" + skillName + "/SKILL.md",
            skillDir.resolve("SKILL.md")
        );
    }

    /**
     * Copy a resource file, overwriting if it exists.
     * Use for mod-provided system files (skills, hooks, bin scripts).
     */
    private void copyResource(String resourcePath, Path targetPath) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                ClaudeCraft.LOGGER.warn("Resource not found: {}", resourcePath);
                return;
            }
            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            ClaudeCraft.LOGGER.debug("Copied {} to {}", resourcePath, targetPath);
        }
    }

    /**
     * Copy a resource file only if target doesn't exist.
     * Use for user data templates (player directories).
     */
    private void copyResourceIfMissing(String resourcePath, Path targetPath) throws IOException {
        if (Files.exists(targetPath)) {
            return;
        }
        copyResource(resourcePath, targetPath);
    }

    private void makeExecutable(Path path) {
        try {
            if (Files.exists(path)) {
                path.toFile().setExecutable(true);
            }
        } catch (Exception e) {
            ClaudeCraft.LOGGER.warn("Failed to make {} executable: {}", path, e.getMessage());
        }
    }

    /**
     * Initialize a player's data directory.
     */
    public void initializePlayer(UUID playerUuid) throws IOException {
        Path playerDir = getPlayerDir(playerUuid);
        if (Files.exists(playerDir)) {
            return;
        }

        ClaudeCraft.LOGGER.info("Creating player directory for {}", playerUuid);
        Files.createDirectories(playerDir);
        Files.createDirectories(playerDir.resolve("notes"));
        Files.createDirectories(playerDir.resolve("sessions"));

        // Copy player template files
        copyResourceIfMissing("claude-craft-defaults/player-template/CLAUDE.md",
            playerDir.resolve("CLAUDE.md"));
        copyResourceIfMissing("claude-craft-defaults/player-template/scratch.md",
            playerDir.resolve("scratch.md"));
    }

    /**
     * Update a player's state.json with current game state.
     */
    public void updatePlayerState(ServerPlayer player) throws IOException {
        Path playerDir = getPlayerDir(player.getUUID());
        Files.createDirectories(playerDir);

        JsonObject state = new JsonObject();

        // Position
        JsonObject position = new JsonObject();
        position.addProperty("x", player.getX());
        position.addProperty("y", player.getY());
        position.addProperty("z", player.getZ());
        state.add("position", position);

        // Dimension
        state.addProperty("dimension", player.level().dimension().location().toString());

        // Rotation
        JsonObject rotation = new JsonObject();
        rotation.addProperty("yaw", player.getYRot());
        rotation.addProperty("pitch", player.getXRot());
        state.add("rotation", rotation);

        // Biome
        state.addProperty("biome", player.level().getBiome(player.blockPosition())
            .unwrapKey().map(k -> k.location().toString()).orElse("unknown"));

        // Health and hunger
        state.addProperty("health", player.getHealth());
        state.addProperty("max_health", player.getMaxHealth());
        state.addProperty("hunger", player.getFoodData().getFoodLevel());
        state.addProperty("saturation", player.getFoodData().getSaturationLevel());

        // Game mode
        state.addProperty("gamemode", player.gameMode.getGameModeForPlayer().getName());

        // Experience
        state.addProperty("xp_level", player.experienceLevel);
        state.addProperty("xp_progress", player.experienceProgress);

        // Inventory
        state.add("inventory", serializeInventory(player.getInventory()));

        // World info
        state.add("world", serializeWorldInfo(player));

        // Nearby (placeholder - would need more complex logic)
        JsonObject nearby = new JsonObject();
        state.add("nearby", nearby);

        // Timestamp
        state.addProperty("updated_at", java.time.Instant.now().toString());

        // Write state file atomically (write to temp, then rename)
        // This prevents Claude from reading a partially-written file
        Path stateFile = playerDir.resolve("state.json");
        Path tempFile = playerDir.resolve("state.json.tmp");

        Files.writeString(tempFile, GSON.toJson(state),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING);

        try {
            Files.move(tempFile, stateFile,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // Fallback to non-atomic move (still better than direct write)
            Files.move(tempFile, stateFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private JsonObject serializeInventory(Inventory inventory) {
        JsonObject inv = new JsonObject();

        // Hotbar (slots 0-8)
        com.google.gson.JsonArray hotbar = new com.google.gson.JsonArray();
        for (int i = 0; i < 9; i++) {
            hotbar.add(serializeItemStack(inventory.getItem(i), i));
        }
        inv.add("hotbar", hotbar);

        // Main inventory (slots 9-35)
        com.google.gson.JsonArray main = new com.google.gson.JsonArray();
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                main.add(serializeItemStack(stack, i));
            }
        }
        inv.add("main", main);

        // Armor
        JsonObject armor = new JsonObject();
        armor.add("head", serializeItemStack(inventory.getArmor(3), -1));
        armor.add("chest", serializeItemStack(inventory.getArmor(2), -1));
        armor.add("legs", serializeItemStack(inventory.getArmor(1), -1));
        armor.add("feet", serializeItemStack(inventory.getArmor(0), -1));
        inv.add("armor", armor);

        // Offhand
        inv.add("offhand", serializeItemStack(inventory.offhand.get(0), -1));

        // Selected slot
        inv.addProperty("selected_slot", inventory.selected);

        return inv;
    }

    private JsonObject serializeItemStack(ItemStack stack, int slot) {
        if (stack.isEmpty()) {
            return null;
        }

        JsonObject item = new JsonObject();
        if (slot >= 0) {
            item.addProperty("slot", slot);
        }
        item.addProperty("item", stack.getItem().builtInRegistryHolder().key().location().toString());
        item.addProperty("count", stack.getCount());

        if (stack.isDamageableItem()) {
            item.addProperty("durability", stack.getMaxDamage() - stack.getDamageValue());
            item.addProperty("max_durability", stack.getMaxDamage());
        }

        return item;
    }

    private JsonObject serializeWorldInfo(ServerPlayer player) {
        JsonObject world = new JsonObject();

        // Time
        JsonObject time = new JsonObject();
        long dayTime = player.level().getDayTime() % 24000;
        time.addProperty("ticks", dayTime);
        time.addProperty("day", player.level().getDayTime() / 24000);
        time.addProperty("is_day", dayTime < 13000);
        time.addProperty("phase", getTimePhase(dayTime));
        world.add("time", time);

        // Weather
        JsonObject weather = new JsonObject();
        weather.addProperty("clear", !player.level().isRaining() && !player.level().isThundering());
        weather.addProperty("raining", player.level().isRaining());
        weather.addProperty("thundering", player.level().isThundering());
        world.add("weather", weather);

        // World settings
        world.addProperty("name", player.server.getWorldData().getLevelName());
        world.addProperty("difficulty", player.server.getWorldData().getDifficulty().getKey());
        world.addProperty("hardcore", player.server.getWorldData().isHardcore());

        // Server info
        JsonObject server = new JsonObject();
        server.addProperty("player_count", player.server.getPlayerCount());
        server.addProperty("max_players", player.server.getMaxPlayers());
        world.add("server", server);

        return world;
    }

    private String getTimePhase(long dayTime) {
        if (dayTime < 1000) return "dawn";
        if (dayTime < 6000) return "morning";
        if (dayTime < 7000) return "noon";
        if (dayTime < 11000) return "afternoon";
        if (dayTime < 13000) return "dusk";
        if (dayTime < 18000) return "night";
        if (dayTime < 19000) return "midnight";
        return "night";
    }

    /**
     * Get the plugin directory path.
     */
    public Path getPluginDir() {
        return pluginDir;
    }

    /**
     * Get a player's data directory path.
     */
    public Path getPlayerDir(UUID playerUuid) {
        return playersDir.resolve(playerUuid.toString());
    }
}
