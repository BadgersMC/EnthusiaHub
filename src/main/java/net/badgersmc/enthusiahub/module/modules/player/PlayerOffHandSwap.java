package net.badgersmc.enthusiahub.module.modules.player;

import net.badgersmc.enthusiahub.EnthusiaHubPlugin;
import net.badgersmc.enthusiahub.module.modules.world.BuildMode;
import net.badgersmc.enthusiahub.module.Module;
import net.badgersmc.enthusiahub.module.ModuleType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class PlayerOffHandSwap extends Module {

	public PlayerOffHandSwap(EnthusiaHubPlugin plugin) {
		super(plugin, ModuleType.PLAYER_OFFHAND_LISTENER);
	}

	@Override
	public void onEnable() {
	}

	@Override
	public void onDisable() {
	}

	@EventHandler
	public void onPlayerSwapItem(PlayerSwapHandItemsEvent event) {
		if (BuildMode.getInstance().isPresent(event.getPlayer().getUniqueId())) return;
		event.setCancelled(true);
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (BuildMode.getInstance().isPresent(event.getWhoClicked().getUniqueId())) return;
		if (event.getRawSlot() != event.getSlot() && event.getCursor() != null && event.getSlot() == 40) {
			event.setCancelled(true);
		}
	}
}
