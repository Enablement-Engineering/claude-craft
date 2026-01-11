package ac.dylanisa.minecraftai.claude;

import ac.dylanisa.minecraftai.MinecraftAI;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks active Claude processes per player for cleanup on disconnect.
 * Thread-safe - all methods can be called from any thread.
 */
public class ClaudeProcessTracker {

    // Map of player UUID -> Set of active processes
    private static final Map<UUID, Set<TrackedProcess>> activeProcesses =
        new ConcurrentHashMap<>();

    /**
     * Register a process for tracking.
     * Called when a new Claude process starts.
     */
    public static void register(UUID playerUuid, Process process) {
        activeProcesses
            .computeIfAbsent(playerUuid, k -> ConcurrentHashMap.newKeySet())
            .add(new TrackedProcess(process, System.currentTimeMillis()));
        MinecraftAI.LOGGER.debug("Registered Claude process {} for player {}",
            process.pid(), playerUuid);
    }

    /**
     * Unregister a process (called when it completes normally).
     */
    public static void unregister(UUID playerUuid, Process process) {
        Set<TrackedProcess> processes = activeProcesses.get(playerUuid);
        if (processes != null) {
            processes.removeIf(tp -> tp.process.equals(process));
        }
    }

    /**
     * Cancel all active processes for a player.
     * Called on player disconnect.
     */
    public static void cancelPlayerProcesses(UUID playerUuid) {
        Set<TrackedProcess> processes = activeProcesses.remove(playerUuid);
        if (processes == null || processes.isEmpty()) {
            return;
        }

        MinecraftAI.LOGGER.info("Cancelling {} active Claude process(es) for player {}",
            processes.size(), playerUuid);

        for (TrackedProcess tp : processes) {
            try {
                if (tp.process.isAlive()) {
                    tp.process.destroyForcibly();
                    MinecraftAI.LOGGER.info("Killed Claude process {} (running for {}ms)",
                        tp.process.pid(), System.currentTimeMillis() - tp.startTime);
                }
            } catch (Exception e) {
                MinecraftAI.LOGGER.warn("Failed to kill Claude process: {}", e.getMessage());
            }
        }
    }

    /**
     * Get count of active processes for a player (for rate limiting).
     */
    public static int getActiveCount(UUID playerUuid) {
        Set<TrackedProcess> processes = activeProcesses.get(playerUuid);
        if (processes == null) {
            return 0;
        }
        // Clean up dead processes while counting
        processes.removeIf(tp -> !tp.process.isAlive());
        return processes.size();
    }

    /**
     * Get total active process count across all players (for monitoring).
     */
    public static int getTotalActiveCount() {
        return activeProcesses.values().stream()
            .mapToInt(Set::size)
            .sum();
    }

    private record TrackedProcess(Process process, long startTime) {}
}
