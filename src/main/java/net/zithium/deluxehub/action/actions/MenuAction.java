package net.zithium.deluxehub.action.actions;

import com.tcoded.folialib.impl.PlatformScheduler;
import net.zithium.deluxehub.DeluxeHubPlugin;
import net.zithium.deluxehub.action.Action;
import net.zithium.deluxehub.menu.MenuNavigator;
import net.zithium.deluxehub.menu.bedrock.menus.BedrockServerSelectorMenu;
import org.bukkit.entity.Player;

public class MenuAction implements Action {

    private final PlatformScheduler scheduler = DeluxeHubPlugin.scheduler();

    @Override
    public String getIdentifier() {
        return "MENU";
    }

    @Override
    public void execute(DeluxeHubPlugin plugin, Player player, String data) {
        // Check if this is the server selector menu and player is on Bedrock
        if (data.equalsIgnoreCase("serverselector") && shouldUseBedrockMenu(plugin, player)) {
            openBedrockServerSelector(plugin, player);
            return;
        }

        // Fall back to regular inventory menu
        plugin.getInventoryManager().getInventory(data).ifPresentOrElse(
                inventory -> scheduler.runAtEntity(player, task -> inventory.openInventory(player)),
                () -> plugin.getLogger().warning("[MENU] Action Failed: Menu '" + data + "' not found.")
        );
    }

    /**
     * Checks if Bedrock menu should be used for this player
     */
    private boolean shouldUseBedrockMenu(DeluxeHubPlugin plugin, Player player) {
        // Check if bedrock menus are enabled globally
        if (!plugin.getConfig().getBoolean("bedrock_menus.enabled", true)) {
            return false;
        }

        // Check if server selector bedrock menu is enabled specifically
        if (!plugin.getConfig().getBoolean("bedrock_menus.server_selector.enabled", true)) {
            return false;
        }

        // Use platform detection to check if player is on Bedrock
        return plugin.getPlatformDetection() != null &&
               plugin.getPlatformDetection().shouldUseBedrockMenus(player);
    }

    /**
     * Opens the Bedrock server selector menu
     */
    private void openBedrockServerSelector(DeluxeHubPlugin plugin, Player player) {
        scheduler.runAtEntity(player, task -> {
            MenuNavigator navigator = new MenuNavigator(player);
            BedrockServerSelectorMenu menu = new BedrockServerSelectorMenu(
                    navigator, player, plugin.getLogger()
            );
            navigator.openMenu(menu);
        });
    }
}
