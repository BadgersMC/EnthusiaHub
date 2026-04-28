package gg.badgersmc.enthusiahub.action.actions;

import gg.badgersmc.enthusiahub.EnthusiaHubPlugin;
import gg.badgersmc.enthusiahub.action.Action;
import org.bukkit.entity.Player;

public class CommandAction implements Action {

	@Override
	public String getIdentifier() {
		return "COMMAND";
	}

	@Override
	public void execute(EnthusiaHubPlugin plugin, Player player, String data) {
		player.chat(data.contains("/") ? data : "/" + data);
	}
}
