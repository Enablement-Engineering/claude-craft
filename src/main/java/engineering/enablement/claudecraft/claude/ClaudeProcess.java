package engineering.enablement.claudecraft.claude;

import engineering.enablement.claudecraft.ClaudeCraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Wrapper for invoking Claude Code as a subprocess.
 * Handles streaming output and session management.
 */
public class ClaudeProcess {
    private static final long TIMEOUT_SECONDS = 120;
    private static final String CLAUDE_COMMAND = findClaudeCommand();

    private final Path workingDirectory;
    private final UUID playerUuid;
    private final boolean isOp;
    private String sessionId;

    public ClaudeProcess(Path workingDirectory, UUID playerUuid, boolean isOp) {
        this.workingDirectory = workingDirectory;
        this.playerUuid = playerUuid;
        this.isOp = isOp;
    }

    /**
     * Set the session ID for continuing a conversation.
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Get the current session ID (may be updated after a run).
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Run a prompt and stream the response.
     *
     * @param prompt The user's message
     * @param onTextChunk Called for each text chunk (for streaming display)
     * @param onComplete Called when the response is complete
     * @param onError Called if an error occurs
     * @return CompletableFuture that completes when the process finishes
     */
    public CompletableFuture<String> run(
        String prompt,
        Consumer<String> onTextChunk,
        Consumer<String> onComplete,
        Consumer<Exception> onError
    ) {
        return CompletableFuture.supplyAsync(() -> {
            ClaudeCraft.LOGGER.info("ClaudeProcess.run() async block started");
            Process process = null;  // Declare outside try for finally block access
            try {
                List<String> command = buildCommand(prompt);
                ClaudeCraft.LOGGER.info("Built command: {}", String.join(" ", command));

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(workingDirectory.toFile());
                pb.redirectErrorStream(true);

                // Set environment variables for hooks
                pb.environment().put("MINECRAFT_PLAYER_UUID", playerUuid.toString());
                pb.environment().put("MINECRAFT_IS_OP", String.valueOf(isOp));
                pb.environment().put("CLAUDE_PROJECT_DIR", workingDirectory.toString());
                // Use global Claude config (for API key), but track sessions per-player
                pb.environment().put("TERM", "dumb");  // Non-interactive terminal
                pb.environment().put("CI", "true");    // Signal non-interactive environment

                ClaudeCraft.LOGGER.info("Starting Claude process in: {}", workingDirectory);
                process = pb.start();

                // Register process for tracking (enables cancellation on disconnect)
                ClaudeProcessTracker.register(playerUuid, process);

                ClaudeCraft.LOGGER.info("Claude process started with PID: {}", process.pid());

                // Close stdin immediately - we're using -p flag so no input needed
                process.getOutputStream().close();
                ClaudeCraft.LOGGER.info("Closed stdin, waiting for output...");

                StringBuilder fullResponse = new StringBuilder();

                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
                )) {
                    ClaudeCraft.LOGGER.info("Reading from process stdout...");

                    // Check if process exited immediately
                    if (!process.isAlive()) {
                        ClaudeCraft.LOGGER.error("Process exited immediately with code: {}", process.exitValue());
                    }

                    String line;
                    while ((line = reader.readLine()) != null) {
                        ClaudeCraft.LOGGER.info("Claude line: {} (length={})",
                            line.length() > 100 ? line.substring(0, 100) + "..." : line, line.length());

                        ClaudeStreamEvent event = ClaudeStreamEvent.parse(line);
                        if (event == null) {
                            ClaudeCraft.LOGGER.warn("Failed to parse Claude event");
                            continue;
                        }

                        ClaudeCraft.LOGGER.info("Parsed event type={}, text={}",
                            event.type(), event.text().length() > 50 ? event.text().substring(0, 50) + "..." : event.text());

                        // Capture session ID from init event
                        if (event.isInit() && event.sessionId() != null) {
                            this.sessionId = event.sessionId();
                            ClaudeCraft.LOGGER.info("Got session ID: {}", sessionId);
                        }

                        // Stream text chunks to the callback
                        if (event.isTextDelta() && onTextChunk != null) {
                            ClaudeCraft.LOGGER.info("Sending text chunk: {}", event.text());
                            onTextChunk.accept(event.text());
                            fullResponse.append(event.text());
                        }

                        // Capture final result
                        if (event.isResult()) {
                            String result = event.text();
                            ClaudeCraft.LOGGER.info("Got result: {}", result);
                            if (result != null && !result.isEmpty()) {
                                fullResponse.setLength(0);
                                fullResponse.append(result);
                            }
                        }
                    }
                }

                boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    throw new IOException("Claude process timed out after " + TIMEOUT_SECONDS + " seconds");
                }

                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    throw new IOException("Claude process exited with code " + exitCode);
                }

                String response = fullResponse.toString();
                if (onComplete != null) {
                    onComplete.accept(response);
                }
                return response;

            } catch (Exception e) {
                ClaudeCraft.LOGGER.error("Claude process error", e);
                if (onError != null) {
                    onError.accept(e);
                }
                throw new RuntimeException(e);
            } finally {
                // Always unregister the process when done (success or failure)
                if (process != null) {
                    ClaudeProcessTracker.unregister(playerUuid, process);
                }
            }
        });
    }

    /**
     * Build the Claude command with appropriate flags.
     */
    private List<String> buildCommand(String prompt) {
        List<String> command = new ArrayList<>();
        command.add(CLAUDE_COMMAND);
        command.add("-p");
        command.add(prompt);
        command.add("--output-format");
        command.add("stream-json");
        command.add("--verbose");  // Required for stream-json with -p
        command.add("--max-turns");
        command.add("100");
        command.add("--model");
        command.add("sonnet");  // Use Sonnet for faster responses

        // Allowed tools: Read, Write (restricted by hook), Bash (restricted to bin/)
        command.add("--allowedTools");
        command.add("Read,Write,Bash");

        // Resume session if we have one
        if (sessionId != null && !sessionId.isEmpty()) {
            command.add("--resume");
            command.add(sessionId);
        }

        return command;
    }

    /**
     * Run a prompt synchronously (blocking).
     */
    public String runSync(String prompt) throws Exception {
        return run(prompt, null, null, null).get(TIMEOUT_SECONDS + 10, TimeUnit.SECONDS);
    }

    /**
     * Find the Claude CLI executable.
     * Checks common installation paths since Java doesn't inherit shell PATH.
     */
    private static String findClaudeCommand() {
        String home = System.getProperty("user.home");
        String[] candidates = {
            home + "/.local/bin/claude",           // npm global install
            home + "/.claude/local/claude",        // claude-specific install
            "/usr/local/bin/claude",               // system install
            "/opt/homebrew/bin/claude",            // homebrew on Apple Silicon
            "claude"                               // fallback to PATH
        };

        for (String candidate : candidates) {
            if (new java.io.File(candidate).canExecute()) {
                ClaudeCraft.LOGGER.info("Found Claude CLI at: {}", candidate);
                return candidate;
            }
        }

        ClaudeCraft.LOGGER.warn("Claude CLI not found in common locations, falling back to PATH");
        return "claude";
    }
}
