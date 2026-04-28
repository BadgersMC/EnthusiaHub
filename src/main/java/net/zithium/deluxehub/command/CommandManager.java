package net.zithium.deluxehub.command;

import cl.bgmp.bukkit.util.BukkitCommandsManager;
import cl.bgmp.bukkit.util.CommandsManagerRegistration;
import cl.bgmp.minecraft.util.commands.CommandsManager;
import cl.bgmp.minecraft.util.commands.exceptions.CommandException;
import cl.bgmp.minecraft.util.commands.injection.SimpleInjector;
import net.zithium.deluxehub.DeluxeHubPlugin;
import net.zithium.deluxehub.command.commands.DeluxeHubCommand;
import net.zithium.deluxehub.command.commands.FlyCommand;
import net.zithium.deluxehub.command.commands.LobbyCommand;
import net.zithium.deluxehub.command.commands.SetLobbyCommand;
import net.zithium.deluxehub.command.commands.VanishCommand;
import net.zithium.deluxehub.command.commands.gamemode.AdventureCommand;
import net.zithium.deluxehub.command.commands.gamemode.CreativeCommand;
import net.zithium.deluxehub.command.commands.gamemode.GamemodeCommand;
import net.zithium.deluxehub.command.commands.gamemode.SpectatorCommand;
import net.zithium.deluxehub.command.commands.gamemode.SurvivalCommand;
import net.zithium.deluxehub.config.ConfigType;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public class CommandManager {

    private final DeluxeHubPlugin plugin;
    private final FileConfiguration config;

    private CommandsManager commands;
    private CommandsManagerRegistration commandRegistry;

    public CommandManager(DeluxeHubPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager().getFile(ConfigType.COMMANDS).getConfig();
    }

    public void reload() {
        if (commandRegistry != null) {
            commandRegistry.unregisterCommands();
        }

        commands = new BukkitCommandsManager();
        commandRegistry = new CommandsManagerRegistration(plugin, commands);
        commands.setInjector(new SimpleInjector(plugin));

        commandRegistry.register(DeluxeHubCommand.class);

        for (String command : config.getConfigurationSection("commands").getKeys(false)) {
            if (!config.getBoolean("commands." + command + ".enabled")) continue;

            registerCommand(command, config.getStringList("commands." + command + ".aliases").toArray(new String[0]));
        }
    }

    public void execute(String cmd, String[] args, CommandSender sender) throws CommandException {
        commands.execute(cmd, args, sender, sender);
    }

    private void registerCommand(String cmd, String[] aliases) {
        switch (cmd.toUpperCase()) {
            case "GAMEMODE" -> commandRegistry.register(GamemodeCommand.class, aliases);
            case "GMS" -> commandRegistry.register(SurvivalCommand.class, aliases);
            case "GMC" -> commandRegistry.register(CreativeCommand.class, aliases);
            case "GMA" -> commandRegistry.register(AdventureCommand.class, aliases);
            case "GMSP" -> commandRegistry.register(SpectatorCommand.class, aliases);
            case "FLY" -> commandRegistry.register(FlyCommand.class, aliases);
            case "SETLOBBY" -> commandRegistry.register(SetLobbyCommand.class, aliases);
            case "LOBBY" -> commandRegistry.register(LobbyCommand.class, aliases);
            case "VANISH" -> commandRegistry.register(VanishCommand.class, aliases);
        }
    }
}
