package net.badgersmc.enthusiahub.action.actions;

import net.badgersmc.enthusiahub.EnthusiaHubPlugin;
import net.badgersmc.enthusiahub.action.Action;
import net.badgersmc.enthusiahub.utility.TextUtil;
import net.badgersmc.enthusiahub.utility.reflection.ActionBar;
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
