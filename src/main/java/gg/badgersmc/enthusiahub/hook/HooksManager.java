package gg.badgersmc.enthusiahub.hook;

import gg.badgersmc.enthusiahub.EnthusiaHubPlugin;
import gg.badgersmc.enthusiahub.hook.hooks.head.BaseHead;
import gg.badgersmc.enthusiahub.hook.hooks.head.DatabaseHead;
import gg.badgersmc.enthusiahub.utility.PlaceholderUtil;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class HooksManager {

	private final Map<String, PluginHook> hooks;

	public HooksManager(EnthusiaHubPlugin plugin) {
		hooks = new HashMap<>();

		// Base64 head
		hooks.put("BASE64", new BaseHead());

		// PlaceholderAPI
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			hooks.put("PLACEHOLDER_API", null);
			PlaceholderUtil.PAPI = true;
			plugin.getLogger().info(" Hooked into PlaceholderAPI");
		}

		if (Bukkit.getPluginManager().isPluginEnabled("HeadDatabase")) {
			hooks.put("HEAD_DATABASE", new DatabaseHead());
			plugin.getLogger().info(" Hooked into HeadDatabase");
		}

		hooks.values().stream().filter(Objects::nonNull).forEach(pluginHook -> pluginHook.onEnable(plugin));

	}

	public boolean isHookEnabled(String id) {
		return hooks.containsKey(id);
	}

	public PluginHook getPluginHook(String id) {
		return hooks.get(id);
	}

}
