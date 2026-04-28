package gg.badgersmc.enthusiahub.action.actions;

import gg.badgersmc.enthusiahub.EnthusiaHubPlugin;
import gg.badgersmc.enthusiahub.action.Action;
import gg.badgersmc.enthusiahub.utility.TextUtil;
import gg.badgersmc.enthusiahub.utility.reflection.ActionBar;
import org.bukkit.entity.Player;

public class ActionbarAction implements Action {

	@Override
	public String getIdentifier() {
		return "ACTIONBAR";
	}

	@Override
	public void execute(EnthusiaHubPlugin plugin, Player player, String data) {
		ActionBar.sendActionBar(player, TextUtil.color(data));
	}
}
