package net.badgersmc.enthusiahub.action.actions;

import net.badgersmc.enthusiahub.EnthusiaHubPlugin;
import net.badgersmc.enthusiahub.action.Action;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ConsoleCommandAction implements Action {

	@Override
	public String getIdentifier() {
		return "CONSOLE";
	}

	@Override
	public void execute(EnthusiaHubPlugin plugin, Player player, String data) {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), data);
	}
}
