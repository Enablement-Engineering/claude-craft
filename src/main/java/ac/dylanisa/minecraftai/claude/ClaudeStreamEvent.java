package ac.dylanisa.minecraftai.claude;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Represents a parsed event from Claude Code's stream-json output.
 */
public record ClaudeStreamEvent(
    String type,
    String subtype,
    String sessionId,
    String text,
    JsonObject raw
) {
    /**
     * Parse a line of stream-json output into a ClaudeStreamEvent.
     */
    public static ClaudeStreamEvent parse(String jsonLine) {
        if (jsonLine == null || jsonLine.isBlank()) {
            return null;
        }

        try {
            JsonObject json = JsonParser.parseString(jsonLine).getAsJsonObject();
            String type = json.has("type") ? json.get("type").getAsString() : "";
            String subtype = json.has("subtype") ? json.get("subtype").getAsString() : "";
            String sessionId = json.has("session_id") ? json.get("session_id").getAsString() : "";

            // Extract text from various event types
            String text = "";
            if ("assistant".equals(type) && json.has("message")) {
                // Full assistant message with content array
                JsonObject message = json.getAsJsonObject("message");
                if (message.has("content") && message.get("content").isJsonArray()) {
                    var content = message.getAsJsonArray("content");
                    StringBuilder sb = new StringBuilder();
                    for (var element : content) {
                        if (element.isJsonObject()) {
                            JsonObject block = element.getAsJsonObject();
                            if (block.has("type") && "text".equals(block.get("type").getAsString())) {
                                if (block.has("text")) {
                                    sb.append(block.get("text").getAsString());
                                }
                            }
                        }
                    }
                    text = sb.toString();
                }
            } else if ("content_block_delta".equals(type) && json.has("delta")) {
                // Streaming delta
                JsonObject delta = json.getAsJsonObject("delta");
                if (delta.has("text")) {
                    text = delta.get("text").getAsString();
                }
            } else if ("result".equals(type) && json.has("result")) {
                text = json.get("result").getAsString();
            }

            return new ClaudeStreamEvent(type, subtype, sessionId, text, json);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if this is a text content event (for streaming display).
     */
    public boolean isTextDelta() {
        return ("assistant".equals(type) || "content_block_delta".equals(type)) && !text.isEmpty();
    }

    /**
     * Check if this is the final result.
     */
    public boolean isResult() {
        return "result".equals(type);
    }

    /**
     * Check if this is the system init event (contains session ID).
     */
    public boolean isInit() {
        return "system".equals(type) && "init".equals(subtype);
    }
}
