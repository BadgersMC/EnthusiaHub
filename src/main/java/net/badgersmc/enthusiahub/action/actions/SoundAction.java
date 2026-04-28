package net.badgersmc.enthusiahub.action.actions;

import net.badgersmc.enthusiahub.EnthusiaHubPlugin;
import net.badgersmc.enthusiahub.action.Action;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;

public class SoundAction implements Action {

	@Override
	public String getIdentifier() {
		return "SOUND";
	}

	@Override
	public void execute(EnthusiaHubPlugin plugin, Player player, String data) {
		try {
			if (EnthusiaHubPlugin.IsCompatible()) return;
			player.playSound(player.getLocation(), Registry.SOUNDS.getOrThrow(NamespacedKey.minecraft(data.toLowerCase().replaceFirst("^_", ".").replaceFirst("_$", ".").replaceAll("_(?=.*_)", "."))), 1L, 1L);
		} catch (Exception ex) {
			Bukkit.getLogger().warning("[DeluxeHub Action] Invalid sound name: " + data.toUpperCase());
		}
	}
}
