package net.badgersmc.enthusiahub.command.commands;

import cl.bgmp.minecraft.util.commands.CommandContext;
import cl.bgmp.minecraft.util.commands.annotations.Command;
import cl.bgmp.minecraft.util.commands.exceptions.CommandException;
import net.badgersmc.enthusiahub.EnthusiaHubPlugin;
import net.badgersmc.enthusiahub.module.ModuleType;
import net.badgersmc.enthusiahub.module.modules.world.LobbySpawn;
import net.badgersmc.enthusiahub.utility.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LobbyCommand {

	private final EnthusiaHubPlugin plugin;

	public LobbyCommand(EnthusiaHubPlugin plugin) {
		this.plugin = plugin;
	}

	@Command(
			aliases = {"lobby"},
			desc = "Teleport to the lobby (if set)"
	)
	public void lobby(final CommandContext args, final CommandSender sender) throws CommandException {

		if (!(sender instanceof Player)) {
			sender.sendMessage("Console cannot teleport to spawn");
			return;
		}

		Location location = ((LobbySpawn) plugin.getModuleManager().getModule(ModuleType.LOBBY)).getLocation();
		if (location == null) {
			sender.sendMessage(TextUtil.color("&cThe spawn location has not been set &7(/setlobby)&c."));
			return;
		}

		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> ((Player) sender).teleport(location), 3L);

	}

}
