package net.badgersmc.enthusiahub.module.modules.chat;

import net.badgersmc.enthusiahub.EnthusiaHubPlugin;
import net.badgersmc.enthusiahub.Permissions;
import net.badgersmc.enthusiahub.config.ConfigType;
import net.badgersmc.enthusiahub.config.Messages;
import net.badgersmc.enthusiahub.module.Module;
import net.badgersmc.enthusiahub.module.ModuleType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatLock extends Module {

	private boolean isChatLocked;

	public ChatLock(EnthusiaHubPlugin plugin) {
		super(plugin, ModuleType.CHAT_LOCK);
	}

	@Override
	public void onEnable() {
		isChatLocked = getPlugin().getConfigManager().getFile(ConfigType.DATA).getConfig().getBoolean("chat_locked");
	}

	@Override
	public void onDisable() {
		getPlugin().getConfigManager().getFile(ConfigType.DATA).getConfig().set("chat_locked", isChatLocked);
	}

	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();

		if (!isChatLocked || player.hasPermission(Permissions.LOCK_CHAT_BYPASS.getPermission())) return;

		event.setCancelled(true);
		Messages.CHAT_LOCKED.send(player);
	}

	public boolean isChatLocked() {
		return isChatLocked;
	}

	public void setChatLocked(boolean chatLocked) {
		isChatLocked = chatLocked;
	}
}
