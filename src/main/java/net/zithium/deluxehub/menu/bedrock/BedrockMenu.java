package net.zithium.deluxehub.menu.bedrock;

import net.zithium.deluxehub.menu.Menu;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.Form;

/**
 * Interface for Bedrock Edition specific menus using Cumulus forms.
 */
public interface BedrockMenu extends Menu {

    /**
     * Creates and returns the Cumulus form for this menu.
     *
     * @return The form to display to the Bedrock player
     */
    Form getForm();

    /**
     * Handles the response from the Bedrock player.
     *
     * @param player   The player who responded
     * @param response The response data from the form
     */
    void handleResponse(Player player, Object response);

    /**
     * Whether this menu should be cached for performance.
     *
     * @return true to cache the form, false otherwise
     */
    default boolean shouldCacheForm() {
        return false;
    }

    /**
     * Creates a cache key for this menu if caching is enabled.
     *
     * @return The cache key
     */
    default String createCacheKey() {
        return getClass().getSimpleName() + ":" + getPlayer().getUniqueId();
    }

    /**
     * Whether this menu should be built asynchronously.
     * Use this for complex forms that may take time to build.
     *
     * @return true to build async, false otherwise
     */
    default boolean shouldBuildAsync() {
        return false;
    }

    /**
     * Gets the timeout duration in seconds for this form.
     *
     * @return Timeout in seconds (default: 300 = 5 minutes)
     */
    default int getFormTimeoutSeconds() {
        return 300;
    }
}
