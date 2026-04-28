package gg.badgersmc.enthusiahub.action.actions;

import gg.badgersmc.enthusiahub.EnthusiaHubPlugin;
import gg.badgersmc.enthusiahub.action.Action;
import gg.badgersmc.enthusiahub.utility.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BroadcastMessageAction implements Action {

	@Override
	public String getIdentifier() {
		return "BROADCAST";
	}

	@Override
	public void execute(EnthusiaHubPlugin plugin, Player player, String data) {
		if (data.contains("<center>") && data.contains("</center>")) data = TextUtil.getCenteredMessage(data);

		for (Player p : Bukkit.getOnlinePlayers()) {
			p.sendMessage(TextUtil.color(data));
		}
	}
}
