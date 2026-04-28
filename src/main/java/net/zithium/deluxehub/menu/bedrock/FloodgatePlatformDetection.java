package net.zithium.deluxehub.menu.bedrock;

import net.zithium.deluxehub.DeluxeHubPlugin;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.logging.Level;

/**
 * Service for detecting if a player is using Bedrock Edition and if Bedrock menus are available.
 */
public class FloodgatePlatformDetection {

    private final DeluxeHubPlugin plugin;
    private boolean floodgateAvailable = false;
    private boolean cumulusAvailable = false;
    private FloodgateApi floodgateApi = null;

    public FloodgatePlatformDetection(DeluxeHubPlugin plugin) {
        this.plugin = plugin;
        checkAvailability();
    }

    /**
     * Checks if Floodgate and Cumulus are available on the server.
     */
    private void checkAvailability() {
        // Check Floodgate
        try {
            floodgateApi = FloodgateApi.getInstance();
            floodgateAvailable = true;
            plugin.getLogger().info("Floodgate API detected - Bedrock player support enabled");
        } catch (Exception e) {
            plugin.getLogger().info("Floodgate API not found - Bedrock menus will be unavailable");
            floodgateAvailable = false;
        }

        // Check Cumulus
        try {
            Class.forName("org.geysermc.cumulus.form.Form");
            cumulusAvailable = true;
            plugin.getLogger().info("Cumulus API detected - Bedrock forms support enabled");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("Cumulus API not found - Bedrock menus will be unavailable");
            cumulusAvailable = false;
        }
    }

    /**
     * Checks if a player is using Bedrock Edition.
     *
     * @param player The player to check
     * @return true if the player is using Bedrock Edition, false otherwise
     */
    public boolean isBedrockPlayer(Player player) {
        if (!floodgateAvailable || floodgateApi == null) {
            return false;
        }

        try {
            return floodgateApi.isFloodgatePlayer(player.getUniqueId());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking if player is Bedrock player: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checks if Bedrock menus should be used for a player.
     * Takes into account configuration settings and platform availability.
     *
     * @param player The player to check
     * @return true if Bedrock menus should be used, false otherwise
     */
    public boolean shouldUseBedrockMenus(Player player) {
        // Check if bedrock menus are enabled in config
        if (!plugin.getConfig().getBoolean("bedrock_menus.enabled", true)) {
            return false;
        }

        // Check if we should force bedrock menus for all players (testing/debug)
        if (plugin.getConfig().getBoolean("bedrock_menus.force_for_all", false)) {
            return isBedrockServicesAvailable();
        }

        // Check if player is actually using Bedrock
        if (!isBedrockPlayer(player)) {
            return false;
        }

        // Check if services are available
        return isBedrockServicesAvailable();
    }

    /**
     * Checks if both Floodgate and Cumulus services are available.
     *
     * @return true if both are available, false otherwise
     */
    public boolean isBedrockServicesAvailable() {
        return floodgateAvailable && cumulusAvailable;
    }

    /**
     * Checks if Floodgate is available.
     *
     * @return true if Floodgate is available, false otherwise
     */
    public boolean isFloodgateAvailable() {
        return floodgateAvailable;
    }

    /**
     * Checks if Cumulus is available.
     *
     * @return true if Cumulus is available, false otherwise
     */
    public boolean isCumulusAvailable() {
        return cumulusAvailable;
    }

    /**
     * Gets the Floodgate API instance.
     *
     * @return The FloodgateApi instance, or null if not available
     */
    public FloodgateApi getFloodgateApi() {
        return floodgateApi;
    }

    /**
     * Checks if fallback to Java menus is enabled when Bedrock menus are unavailable.
     *
     * @return true if fallback is enabled, false otherwise
     */
    public boolean shouldFallbackToJavaMenus() {
        return plugin.getConfig().getBoolean("bedrock_menus.fallback_to_java", true);
    }
}
