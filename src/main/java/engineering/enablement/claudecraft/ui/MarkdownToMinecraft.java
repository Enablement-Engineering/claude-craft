package engineering.enablement.claudecraft.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts markdown-style text to Minecraft formatted Components.
 */
public class MarkdownToMinecraft {

    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*(.+?)\\*");
    private static final Pattern CODE_PATTERN = Pattern.compile("`(.+?)`");
    private static final Pattern BULLET_PATTERN = Pattern.compile("^- (.+)$", Pattern.MULTILINE);

    /**
     * Convert markdown text to a Minecraft Component with formatting.
     */
    public static Component convert(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return Component.empty();
        }

        // Process line by line for better control
        String[] lines = markdown.split("\n");
        MutableComponent result = Component.empty();

        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                result.append(Component.literal("\n"));
            }
            result.append(convertLine(lines[i]));
        }

        return result;
    }

    private static Component convertLine(String line) {
        // Check for bullet points
        if (line.startsWith("- ")) {
            MutableComponent bullet = Component.literal("• ");
            bullet.append(convertInlineFormatting(line.substring(2)));
            return bullet;
        }

        return convertInlineFormatting(line);
    }

    private static Component convertInlineFormatting(String text) {
        MutableComponent result = Component.empty();
        int lastEnd = 0;

        // Find all formatting patterns and their positions
        // Process bold first (** before *)
        Matcher boldMatcher = BOLD_PATTERN.matcher(text);
        StringBuilder processed = new StringBuilder();
        int boldLastEnd = 0;

        while (boldMatcher.find()) {
            // Add text before this match
            processed.append(text, boldLastEnd, boldMatcher.start());
            // Add placeholder for bold text
            processed.append("\u0001BOLD\u0002").append(boldMatcher.group(1)).append("\u0001/BOLD\u0002");
            boldLastEnd = boldMatcher.end();
        }
        processed.append(text.substring(boldLastEnd));

        String withBold = processed.toString();

        // Process italic
        Matcher italicMatcher = ITALIC_PATTERN.matcher(withBold);
        processed = new StringBuilder();
        int italicLastEnd = 0;

        while (italicMatcher.find()) {
            processed.append(withBold, italicLastEnd, italicMatcher.start());
            processed.append("\u0001ITALIC\u0002").append(italicMatcher.group(1)).append("\u0001/ITALIC\u0002");
            italicLastEnd = italicMatcher.end();
        }
        processed.append(withBold.substring(italicLastEnd));

        String withItalic = processed.toString();

        // Process code
        Matcher codeMatcher = CODE_PATTERN.matcher(withItalic);
        processed = new StringBuilder();
        int codeLastEnd = 0;

        while (codeMatcher.find()) {
            processed.append(withItalic, codeLastEnd, codeMatcher.start());
            processed.append("\u0001CODE\u0002").append(codeMatcher.group(1)).append("\u0001/CODE\u0002");
            codeLastEnd = codeMatcher.end();
        }
        processed.append(withItalic.substring(codeLastEnd));

        // Now parse the result and build components
        return parseFormatted(processed.toString());
    }

    private static Component parseFormatted(String text) {
        MutableComponent result = Component.empty();

        int i = 0;
        while (i < text.length()) {
            int startTag = text.indexOf("\u0001", i);
            if (startTag == -1) {
                // No more tags, add remaining text
                if (i < text.length()) {
                    result.append(Component.literal(text.substring(i)));
                }
                break;
            }

            // Add text before tag
            if (startTag > i) {
                result.append(Component.literal(text.substring(i, startTag)));
            }

            // Find tag type
            int tagEnd = text.indexOf("\u0002", startTag);
            if (tagEnd == -1) break;

            String tag = text.substring(startTag + 1, tagEnd);

            // Find closing tag
            String closeTag = "\u0001/" + tag + "\u0002";
            int closeStart = text.indexOf(closeTag, tagEnd);
            if (closeStart == -1) break;

            // Get content between tags
            String content = text.substring(tagEnd + 1, closeStart);

            // Apply formatting based on tag type
            MutableComponent formatted = Component.literal(content);
            switch (tag) {
                case "BOLD":
                    formatted.withStyle(ChatFormatting.BOLD);
                    break;
                case "ITALIC":
                    formatted.withStyle(ChatFormatting.ITALIC);
                    break;
                case "CODE":
                    formatted.withStyle(ChatFormatting.AQUA);
                    break;
            }
            result.append(formatted);

            i = closeStart + closeTag.length();
        }

        return result;
    }

    /**
     * Simple conversion using section codes (for legacy compatibility).
     */
    public static String convertToLegacy(String markdown) {
        if (markdown == null) return "";

        String result = markdown;

        // Bold: **text** -> §ltext§r
        result = BOLD_PATTERN.matcher(result).replaceAll("§l$1§r");

        // Italic: *text* -> §otext§r
        result = ITALIC_PATTERN.matcher(result).replaceAll("§o$1§r");

        // Code: `text` -> §btext§r (aqua color)
        result = CODE_PATTERN.matcher(result).replaceAll("§b$1§r");

        // Bullets: - text -> • text
        result = BULLET_PATTERN.matcher(result).replaceAll("• $1");

        return result;
    }
}
