package net.badgersmc.enthusiahub.action.actions;

import net.badgersmc.enthusiahub.EnthusiaHubPlugin;
import net.badgersmc.enthusiahub.action.Action;
import net.badgersmc.enthusiahub.inventory.AbstractInventory;
import net.badgersmc.enthusiahub.menu.MenuNavigator;
import net.badgersmc.enthusiahub.menu.bedrock.menus.BedrockServerSelectorMenu;
import org.bukkit.entity.Player;

public class MenuAction implements Action {

	@Override
	public String getIdentifier() {
		return "MENU";
	}

	@Override
	public void execute(EnthusiaHubPlugin plugin, Player player, String data) {
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

	private boolean shouldUseBedrockMenu(EnthusiaHubPlugin plugin, Player player) {
		if (!plugin.getConfig().getBoolean("bedrock_menus.enabled", true)) return false;
		if (!plugin.getConfig().getBoolean("bedrock_menus.server_selector.enabled", true)) return false;
		return plugin.getPlatformDetection() != null
				&& plugin.getPlatformDetection().shouldUseBedrockMenus(player);
	}

	private void openBedrockServerSelector(EnthusiaHubPlugin plugin, Player player) {
		MenuNavigator navigator = new MenuNavigator(player);
		BedrockServerSelectorMenu menu = new BedrockServerSelectorMenu(
				navigator, player, plugin.getLogger());
		navigator.openMenu(menu);
	}
}
