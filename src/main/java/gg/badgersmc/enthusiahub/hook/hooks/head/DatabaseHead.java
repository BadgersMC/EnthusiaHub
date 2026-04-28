package gg.badgersmc.enthusiahub.hook.hooks.head;

import gg.badgersmc.enthusiahub.EnthusiaHubPlugin;
import gg.badgersmc.enthusiahub.hook.PluginHook;
import me.arcaniax.hdb.api.DatabaseLoadEvent;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class DatabaseHead implements PluginHook, HeadHook, Listener {

	private EnthusiaHubPlugin plugin;
	private HeadDatabaseAPI api;

	@Override
	public void onEnable(EnthusiaHubPlugin plugin) {
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		api = new HeadDatabaseAPI();
	}

	@Override
	public ItemStack getHead(String data) {
		return api.getItemHead(data);
	}

	@EventHandler
	public void onDatabaseLoad(DatabaseLoadEvent event) {
		plugin.getInventoryManager().onEnable(plugin);
	}

}
