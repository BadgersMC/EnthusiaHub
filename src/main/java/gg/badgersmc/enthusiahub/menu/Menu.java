package gg.badgersmc.enthusiahub.menu;

import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Base interface for all menus in DeluxeHub.
 * Supports both Java Edition (inventory-based) and Bedrock Edition (form-based) menus.
 */
public interface Menu {

    /**
     * Opens the menu for the player.
     */
    void open();

    /**
     * Passes data to the menu before opening.
     * Useful for multi-step workflows or navigation with context.
     *
     * @param data Data to pass to the menu
     */
    void passData(Map<String, Object> data);

    /**
     * Gets the player this menu is for.
     *
     * @return The player
     */
    Player getPlayer();

    /**
     * Called when the menu is closed by the player.
     */
    default void onClose() {
        // Optional cleanup
    }

    /**
     * Gets the menu navigator associated with this menu.
     *
     * @return The menu navigator
     */
    MenuNavigator getMenuNavigator();
}
