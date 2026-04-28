package net.badgersmc.enthusiahub.module.modules.hotbar.items;

import net.badgersmc.enthusiahub.config.ConfigType;
import net.badgersmc.enthusiahub.module.modules.hotbar.HotbarItem;
import net.badgersmc.enthusiahub.module.modules.hotbar.HotbarManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CustomItem extends HotbarItem {

	private final List<String> actions;

	public CustomItem(HotbarManager hotbarManager, ItemStack item, int slot, String key) {
		super(hotbarManager, item, slot, key);
		actions = getPlugin().getConfigManager().getFile(ConfigType.SETTINGS).getConfig().getStringList("custom_join_items.items." + key + ".actions");
	}

	@Override
	protected void onInteract(Player player) {
		getPlugin().getActionManager().executeActions(player, actions);
	}
}
