package dev.strafbefehl.deluxehubreloaded.action.actions;

import dev.strafbefehl.deluxehubreloaded.DeluxeHubPlugin;
import dev.strafbefehl.deluxehubreloaded.action.Action;
import dev.strafbefehl.deluxehubreloaded.inventory.AbstractInventory;
import dev.strafbefehl.deluxehubreloaded.menu.MenuNavigator;
import dev.strafbefehl.deluxehubreloaded.menu.bedrock.menus.BedrockServerSelectorMenu;
import org.bukkit.entity.Player;

public class MenuAction implements Action {

	@Override
	public String getIdentifier() {
		return "MENU";
	}

	@Override
	public void execute(DeluxeHubPlugin plugin, Player player, String data) {
		if (data.equalsIgnoreCase("serverselector") && shouldUseBedrockMenu(plugin, player)) {
			openBedrockServerSelector(plugin, player);
			return;
		}

		AbstractInventory inventory = plugin.getInventoryManager().getInventory(data);

		if (inventory != null) {
			inventory.openInventory(player);
		} else {
			plugin.getLogger().warning("[MENU] Action Failed: Menu '" + data + "' not found.");
		}
	}

	private boolean shouldUseBedrockMenu(DeluxeHubPlugin plugin, Player player) {
		if (!plugin.getConfig().getBoolean("bedrock_menus.enabled", true)) return false;
		if (!plugin.getConfig().getBoolean("bedrock_menus.server_selector.enabled", true)) return false;
		return plugin.getPlatformDetection() != null
				&& plugin.getPlatformDetection().shouldUseBedrockMenus(player);
	}

	private void openBedrockServerSelector(DeluxeHubPlugin plugin, Player player) {
		MenuNavigator navigator = new MenuNavigator(player);
		BedrockServerSelectorMenu menu = new BedrockServerSelectorMenu(
				navigator, player, plugin.getLogger());
		navigator.openMenu(menu);
	}
}
