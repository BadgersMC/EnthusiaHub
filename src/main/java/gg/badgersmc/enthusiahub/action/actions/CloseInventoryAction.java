package gg.badgersmc.enthusiahub.action.actions;

import gg.badgersmc.enthusiahub.EnthusiaHubPlugin;
import gg.badgersmc.enthusiahub.action.Action;
import org.bukkit.entity.Player;

public class CloseInventoryAction implements Action {

	@Override
	public String getIdentifier() {
		return "CLOSE";
	}

	@Override
	public void execute(EnthusiaHubPlugin plugin, Player player, String data) {
		player.closeInventory();
	}
}
