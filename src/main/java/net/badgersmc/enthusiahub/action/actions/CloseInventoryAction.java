package net.badgersmc.enthusiahub.action.actions;

import net.badgersmc.enthusiahub.EnthusiaHubPlugin;
import net.badgersmc.enthusiahub.action.Action;
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
