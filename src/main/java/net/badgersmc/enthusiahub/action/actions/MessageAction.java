package net.badgersmc.enthusiahub.action.actions;

import net.badgersmc.enthusiahub.EnthusiaHubPlugin;
import net.badgersmc.enthusiahub.action.Action;
import net.badgersmc.enthusiahub.utility.TextUtil;
import org.bukkit.entity.Player;

public class MessageAction implements Action {

	@Override
	public String getIdentifier() {
		return "MESSAGE";
	}

	@Override
	public void execute(EnthusiaHubPlugin plugin, Player player, String data) {
		if (data.contains("<center>") && data.contains("</center>")) data = TextUtil.getCenteredMessage(data);
		player.sendMessage(TextUtil.color(data));
	}
}
