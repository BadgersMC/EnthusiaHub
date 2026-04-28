package dev.strafbefehl.deluxehubreloaded.menu.bedrock;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Function;

/**
 * Utility class for building Bedrock forms with validation and common components.
 */
public class BedrockFormUtils {

    /**
     * Validates that a string is not empty.
     */
    public static final Function<String, String> NOT_EMPTY_VALIDATOR = value -> {
        if (value == null || value.trim().isEmpty()) {
            return "This field cannot be empty";
        }
        return null;
    };

    /**
     * Creates a length validator.
     *
     * @param min Minimum length
     * @param max Maximum length
     * @return Validator function
     */
    public static Function<String, String> lengthValidator(int min, int max) {
        return value -> {
            if (value == null) {
                return "This field cannot be empty";
            }
            int length = value.length();
            if (length < min) {
                return "Must be at least " + min + " characters";
            }
            if (length > max) {
                return "Must be at most " + max + " characters";
            }
            return null;
        };
    }

    /**
     * Creates a numeric range validator.
     *
     * @param min Minimum value
     * @param max Maximum value
     * @return Validator function
     */
    public static Function<Number, String> rangeValidator(double min, double max) {
        return value -> {
            if (value == null) {
                return "Value cannot be null";
            }
            double num = value.doubleValue();
            if (num < min) {
                return "Value must be at least " + min;
            }
            if (num > max) {
                return "Value must be at most " + max;
            }
            return null;
        };
    }

    /**
     * Creates a pattern validator using regex.
     *
     * @param pattern     The regex pattern
     * @param errorMessage Error message when pattern doesn't match
     * @return Validator function
     */
    public static Function<String, String> patternValidator(String pattern, String errorMessage) {
        return value -> {
            if (value == null) {
                return "This field cannot be empty";
            }
            if (!value.matches(pattern)) {
                return errorMessage;
            }
            return null;
        };
    }

    /**
     * Combines multiple validators.
     *
     * @param validators The validators to combine
     * @return Combined validator function
     */
    @SafeVarargs
    public static Function<String, String> combineValidators(Function<String, String>... validators) {
        return value -> {
            for (Function<String, String> validator : validators) {
                String error = validator.apply(value);
                if (error != null) {
                    return error;
                }
            }
            return null;
        };
    }

    /**
     * Formats text with color codes for Bedrock display.
     *
     * @param text   The text to format
     * @param player The player (for PlaceholderAPI)
     * @return Formatted text
     */
    public static String formatText(String text, Player player) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Formats text with placeholders and color codes.
     *
     * @param text   The text to format
     * @param player The player (for PlaceholderAPI)
     * @return Formatted text
     */
    public static String formatTextWithPlaceholders(String text, Player player) {
        // Apply color codes
        text = ChatColor.translateAlternateColorCodes('&', text);

        // Replace player placeholders
        text = text.replace("%player%", player.getName());
        text = text.replace("%player_displayname%", player.getDisplayName());

        return text;
    }

    /**
     * Creates a section header label with formatting.
     *
     * @param title The section title
     * @return Formatted section header
     */
    public static String createSectionHeader(String title) {
        return "§l§e" + title;
    }

    /**
     * Creates a description label with formatting.
     *
     * @param description The description text
     * @return Formatted description
     */
    public static String createDescription(String description) {
        return "§7" + description;
    }

    /**
     * Creates an info label with icon.
     *
     * @param info The info text
     * @return Formatted info label
     */
    public static String createInfoLabel(String info) {
        return "§b§lℹ §7" + info;
    }

    /**
     * Creates a warning label with icon.
     *
     * @param warning The warning text
     * @return Formatted warning label
     */
    public static String createWarningLabel(String warning) {
        return "§e§l⚠ §e" + warning;
    }

    /**
     * Creates an error label with icon.
     *
     * @param error The error text
     * @return Formatted error label
     */
    public static String createErrorLabel(String error) {
        return "§c§l✖ §c" + error;
    }

    /**
     * Creates a success label with icon.
     *
     * @param success The success text
     * @return Formatted success label
     */
    public static String createSuccessLabel(String success) {
        return "§a§l✔ §a" + success;
    }

    /**
     * Wraps text to fit Bedrock form width.
     *
     * @param text      The text to wrap
     * @param maxLength Maximum characters per line
     * @return Wrapped text with newlines
     */
    public static String wrapText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }

        StringBuilder wrapped = new StringBuilder();
        String[] words = text.split(" ");
        int currentLineLength = 0;

        for (String word : words) {
            if (currentLineLength + word.length() > maxLength) {
                wrapped.append("\n");
                currentLineLength = 0;
            }

            if (currentLineLength > 0) {
                wrapped.append(" ");
                currentLineLength++;
            }

            wrapped.append(word);
            currentLineLength += word.length();
        }

        return wrapped.toString();
    }

    /**
     * Creates a multi-line list from items.
     *
     * @param items  The items to list
     * @param bullet The bullet character
     * @return Formatted list
     */
    public static String createList(List<String> items, String bullet) {
        if (items == null || items.isEmpty()) {
            return "";
        }

        StringBuilder list = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            list.append(bullet).append(" ").append(items.get(i));
            if (i < items.size() - 1) {
                list.append("\n");
            }
        }

        return list.toString();
    }

    /**
     * Creates a numbered list from items.
     *
     * @param items The items to list
     * @return Formatted numbered list
     */
    public static String createNumberedList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }

        StringBuilder list = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            list.append((i + 1)).append(". ").append(items.get(i));
            if (i < items.size() - 1) {
                list.append("\n");
            }
        }

        return list.toString();
    }

    /**
     * Escapes special characters for form content.
     *
     * @param text The text to escape
     * @return Escaped text
     */
    public static String escapeFormText(String text) {
        if (text == null) {
            return "";
        }

        // Escape any problematic characters for Bedrock forms
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\r", "")
                   .replace("\t", "    ");
    }

    /**
     * Truncates text to a maximum length with ellipsis.
     *
     * @param text      The text to truncate
     * @param maxLength Maximum length
     * @return Truncated text
     */
    public static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Creates a progress bar visual.
     *
     * @param current Current value
     * @param max     Maximum value
     * @param length  Bar length in characters
     * @return Visual progress bar
     */
    public static String createProgressBar(int current, int max, int length) {
        if (max <= 0) {
            return "§c[Error: Invalid max value]";
        }

        int filled = (int) ((current / (double) max) * length);
        filled = Math.max(0, Math.min(length, filled));

        StringBuilder bar = new StringBuilder("§a");
        for (int i = 0; i < filled; i++) {
            bar.append("█");
        }

        bar.append("§7");
        for (int i = filled; i < length; i++) {
            bar.append("█");
        }

        bar.append(" §f").append(current).append("/").append(max);

        return bar.toString();
    }

    /**
     * Formats a boolean as Yes/No with colors.
     *
     * @param value The boolean value
     * @return Formatted string
     */
    public static String formatBoolean(boolean value) {
        return value ? "§aYes" : "§cNo";
    }

    /**
     * Formats a boolean as Enabled/Disabled with colors.
     *
     * @param value The boolean value
     * @return Formatted string
     */
    public static String formatEnabled(boolean value) {
        return value ? "§aEnabled" : "§cDisabled";
    }

    /**
     * Creates a confirmation message.
     *
     * @param action The action being confirmed
     * @return Formatted confirmation message
     */
    public static String createConfirmationMessage(String action) {
        return "§eAre you sure you want to " + action + "?";
    }
}
