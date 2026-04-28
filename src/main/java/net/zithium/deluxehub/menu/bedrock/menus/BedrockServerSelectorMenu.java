package net.zithium.deluxehub.menu.bedrock.menus;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.zithium.deluxehub.DeluxeHubPlugin;
import net.zithium.deluxehub.menu.MenuNavigator;
import net.zithium.deluxehub.menu.bedrock.BaseBedrockMenu;
import net.zithium.deluxehub.menu.bedrock.BedrockFormUtils;
import net.zithium.deluxehub.menu.bedrock.BedrockMenuNavigator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.SimpleForm;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

/**
 * Bedrock Edition server selector menu.
 * Reads from serverselector.yml and displays servers as buttons in a SimpleForm.
 */
public class BedrockServerSelectorMenu extends BaseBedrockMenu {

    private final BedrockMenuNavigator bedrockNavigator;
    private final YamlConfiguration config;
    private final List<ServerEntry> servers;

    /**
     * Represents a server in the selector
     */
    private static class ServerEntry {
        final String id;
        final String displayName;
        final List<String> lore;
        final List<String> actions;
        final boolean glow;

        ServerEntry(String id, String displayName, List<String> lore, List<String> actions, boolean glow) {
            this.id = id;
            this.displayName = displayName;
            this.lore = lore;
            this.actions = actions;
            this.glow = glow;
        }

        String getButtonText() {
            StringBuilder text = new StringBuilder();

            // Add display name
            text.append(displayName);

            // Add lore as description if available
            if (lore != null && !lore.isEmpty()) {
                text.append("\n");
                for (int i = 0; i < Math.min(lore.size(), 2); i++) { // Limit to 2 lines
                    text.append(lore.get(i));
                    if (i < Math.min(lore.size(), 2) - 1) {
                        text.append("\n");
                    }
                }
            }

            return text.toString();
        }
    }

    public BedrockServerSelectorMenu(MenuNavigator menuNavigator, Player player, Logger logger) {
        super(menuNavigator, player, logger);
        this.bedrockNavigator = new BedrockMenuNavigator(menuNavigator);

        // Load serverselector.yml configuration
        File menuFile = new File(plugin.getDataFolder(), "menus/serverselector.yml");
        if (!menuFile.exists()) {
            logger.warning("serverselector.yml not found, creating default configuration");
            this.config = new YamlConfiguration();
            this.servers = new ArrayList<>();
        } else {
            this.config = YamlConfiguration.loadConfiguration(menuFile);
            this.servers = loadServersFromConfig();
        }
    }

    /**
     * Loads server entries from the configuration file
     */
    private List<ServerEntry> loadServersFromConfig() {
        List<ServerEntry> serverList = new ArrayList<>();

        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) {
            logger.warning("No items section found in serverselector.yml");
            return serverList;
        }

        for (String key : itemsSection.getKeys(false)) {
            // Skip filler items
            if (key.equalsIgnoreCase("filler")) {
                continue;
            }

            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection == null) {
                continue;
            }

            // Extract server information
            String displayName = itemSection.getString("display_name", "&7" + key);
            List<String> lore = itemSection.getStringList("lore");
            List<String> actions = itemSection.getStringList("actions");
            boolean glow = itemSection.getBoolean("glow", false);

            // Strip color codes and clean up text for Bedrock
            displayName = stripColorCodes(displayName);
            lore = lore.stream()
                    .map(this::stripColorCodes)
                    .toList();

            serverList.add(new ServerEntry(key, displayName, lore, actions, glow));
        }

        return serverList;
    }

    /**
     * Strips Minecraft color codes from text
     */
    private String stripColorCodes(String text) {
        if (text == null) {
            return "";
        }
        // Remove both & and § color codes
        return text.replaceAll("(?i)[&§][0-9A-FK-ORX]", "");
    }

    @Override
    public Form getForm() {
        // Get title from config or use default, strip color codes and make it bold gold
        String title = config.getString("title", "Server Selector");
        title = stripColorCodes(title);
        title = "§l§6" + title; // Bold gold

        SimpleForm.Builder formBuilder = SimpleForm.builder()
                .title(title)
                .content("Select a server to join");

        // Add server buttons
        for (ServerEntry server : servers) {
            String buttonText = server.getButtonText();
            formBuilder.button(buttonText);
        }

        // Add close button
        formBuilder.button("§cClose");

        formBuilder.validResultHandler(response -> {
            onFormResponseReceived();

            int buttonId = response.clickedButtonId();

            // Check if close button was clicked
            if (buttonId >= servers.size()) {
                bedrockNavigator.closeAll();
                return;
            }

            // Get selected server
            ServerEntry selectedServer = servers.get(buttonId);

            // Execute server actions
            executeServerActions(selectedServer);
        });

        formBuilder.closedOrInvalidResultHandler((form, result) -> {
            onFormResponseReceived();
            player.sendMessage("§7Server selector closed");
        });

        return formBuilder.build();
    }

    /**
     * Executes the actions for a selected server
     */
    private void executeServerActions(ServerEntry server) {
        if (server.actions == null || server.actions.isEmpty()) {
            logger.warning("No actions defined for server: " + server.id);
            player.sendMessage("§cThis server is not configured properly");
            return;
        }

        // Process each action
        for (String actionStr : server.actions) {
            executeAction(actionStr);
        }
    }

    /**
     * Executes a single action string
     */
    private void executeAction(String actionStr) {
        if (actionStr == null || actionStr.isEmpty()) {
            return;
        }

        // Parse action format: [ACTION_TYPE] data
        if (!actionStr.startsWith("[")) {
            logger.warning("Invalid action format: " + actionStr);
            return;
        }

        int endBracket = actionStr.indexOf("]");
        if (endBracket == -1) {
            logger.warning("Invalid action format: " + actionStr);
            return;
        }

        String actionType = actionStr.substring(1, endBracket).toUpperCase();
        String actionData = actionStr.substring(endBracket + 1).trim();

        // Handle common actions
        switch (actionType) {
            case "CLOSE" -> bedrockNavigator.closeAll();

            case "MESSAGE" -> {
                // Keep color codes for chat messages
                String message = org.bukkit.ChatColor.translateAlternateColorCodes('&', actionData);
                player.sendMessage(message);
            }

            case "PROXY", "BUNGEE" -> {
                // Send player to another server via BungeeCord
                try {
                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("Connect");
                    out.writeUTF(actionData);
                    player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
                } catch (Exception e) {
                    logger.warning("Failed to send player to server: " + e.getMessage());
                    player.sendMessage("§cFailed to connect to server");
                }
            }

            case "COMMAND" -> {
                // Execute command as player
                DeluxeHubPlugin.scheduler().runAtEntity(player, task ->
                    player.performCommand(actionData)
                );
            }

            case "CONSOLE" -> {
                // Execute command as console
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                    actionData.replace("%player%", player.getName()));
            }

            case "SOUND" -> {
                // Play sound (format: SOUND_NAME or SOUND_NAME:VOLUME:PITCH)
                try {
                    String[] parts = actionData.split(":");
                    String soundName = parts[0];
                    float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
                    float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;

                    player.playSound(player.getLocation(), soundName, volume, pitch);
                } catch (Exception e) {
                    logger.warning("Failed to play sound: " + e.getMessage());
                }
            }

            default -> {
                // Use the plugin's action manager for other actions
                // ActionManager expects a list of actions
                plugin.getActionManager().executeActions(player, java.util.List.of(actionStr));
            }
        }
    }

    @Override
    public void handleResponse(Player player, Object response) {
        // Response handled in validResultHandler
    }

    @Override
    public boolean shouldCacheForm() {
        // Cache the form since server list doesn't change often
        // Can be disabled if you want real-time player counts (if added later)
        return true;
    }
}
