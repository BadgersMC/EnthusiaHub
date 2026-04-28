package net.badgersmc.enthusiahub.action;

import net.badgersmc.enthusiahub.EnthusiaHubPlugin;
import org.bukkit.entity.Player;

public interface Action {

	String getIdentifier();

	void execute(EnthusiaHubPlugin plugin, Player player, String data);

}
