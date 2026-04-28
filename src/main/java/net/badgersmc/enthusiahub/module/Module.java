package net.badgersmc.enthusiahub.module;

import net.badgersmc.enthusiahub.EnthusiaHubPlugin;
import net.badgersmc.enthusiahub.config.ConfigType;
import net.badgersmc.enthusiahub.cooldown.CooldownManager;
import net.badgersmc.enthusiahub.cooldown.CooldownType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class Module implements Listener {

	private final EnthusiaHubPlugin plugin;
	private final ModuleType moduleType;
	private List<String> disabledWorlds;
	private final CooldownManager cooldownManager;

	public Module(EnthusiaHubPlugin plugin, ModuleType type) {
		this.plugin = plugin;
		this.moduleType = type;
		this.cooldownManager = plugin.getCooldownManager();
		this.disabledWorlds = new ArrayList<>();
	}

	public void setDisabledWorlds(List<String> disabledWorlds) {
		this.disabledWorlds = disabledWorlds;
	}

	public EnthusiaHubPlugin getPlugin() {
		return plugin;
	}

	public boolean inDisabledWorld(Location location) {
		return disabledWorlds.contains(location.getWorld().getName());
	}

	public boolean inDisabledWorld(World world) {
		return disabledWorlds.contains(world.getName());
	}

	public boolean tryCooldown(UUID uuid, CooldownType type, long delay) {
		return cooldownManager.tryCooldown(uuid, type, delay);
	}

	public String getCooldown(UUID uuid, CooldownType type) {
		return String.valueOf(cooldownManager.getCooldown(uuid, type) / 1000);
	}

	public FileConfiguration getConfig(ConfigType type) {
		return getPlugin().getConfigManager().getFile(type).getConfig();
	}

	public void executeActions(Player player, List<String> actions) {
		getPlugin().getActionManager().executeActions(player, actions);
	}

	public ModuleType getModuleType() {
		return moduleType;
	}

	public abstract void onEnable();

	public abstract void onDisable();

}
