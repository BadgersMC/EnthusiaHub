package net.zithium.deluxehub.module.modules.hotbar;

import net.zithium.deluxehub.DeluxeHubPlugin;
import net.zithium.deluxehub.config.ConfigType;
import net.zithium.deluxehub.module.Module;
import net.zithium.deluxehub.module.ModuleType;
import net.zithium.deluxehub.module.modules.hotbar.items.CustomItem;
import net.zithium.deluxehub.module.modules.hotbar.items.PlayerHider;
import net.zithium.deluxehub.module.modules.hotbar.items.PvPSwordItem;
import net.zithium.deluxehub.utility.ItemStackBuilder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HotbarManager extends Module {

    private List<HotbarItem> hotbarItems;
    private PvPSwordItem pvpSwordItem;

    public HotbarManager(DeluxeHubPlugin plugin) {
        super(plugin, ModuleType.HOTBAR_ITEMS);
    }

    @Override
    public void onEnable() {
        hotbarItems = new CopyOnWriteArrayList<>();
        FileConfiguration config = getConfig(ConfigType.SETTINGS);

        if (config.getBoolean("custom_join_items.enabled")) {

            for (String entry : config.getConfigurationSection("custom_join_items.items").getKeys(false)) {
                ItemStack item = ItemStackBuilder.getItemStack(config.getConfigurationSection("custom_join_items.items." + entry)).build();
                CustomItem customItem = new CustomItem(this, item, config.getInt("custom_join_items.items." + entry + ".slot"), entry);

                if (config.contains("custom_join_items.items." + entry + ".permission")) {
                    customItem.setPermission(config.getString("custom_join_items.items." + entry + ".permission"));
                }

                customItem.setConfigurationSection(config.getConfigurationSection("custom_join_items.items." + entry));
                customItem.setAllowMovement(!config.getBoolean("custom_join_items.disable_inventory_movement"));
                registerHotbarItem(customItem);
            }
        }

        if (config.getBoolean("player_hider.enabled")) {
            ItemStack item = ItemStackBuilder.getItemStack(config.getConfigurationSection("player_hider.not_hidden")).build();
            PlayerHider playerHider = new PlayerHider(this, item, config.getInt("player_hider.slot"), "PLAYER_HIDER");

            playerHider.setAllowMovement(!config.getBoolean("player_hider.disable_inventory_movement"));

            registerHotbarItem(playerHider);
        }

        if (config.getBoolean("pvp_sword.enabled")) {
            ItemStack item = ItemStackBuilder.getItemStack(config.getConfigurationSection("pvp_sword.sword")).build();
            pvpSwordItem = new PvPSwordItem(this, item, config.getInt("pvp_sword.slot"), "PVP_SWORD");

            if (config.contains("pvp_sword.permission")) {
                pvpSwordItem.setPermission(config.getString("pvp_sword.permission"));
            }

            pvpSwordItem.setAllowMovement(false); // Don't allow moving the PvP sword

            registerHotbarItem(pvpSwordItem);
        }

        giveItems();
    }

    @Override
    public void onDisable() {
        removeItems();
    }

    public List<HotbarItem> getHotbarItems() {
        return hotbarItems;
    }

    public void registerHotbarItem(HotbarItem hotbarItem) {
        getPlugin().getServer().getPluginManager().registerEvents(hotbarItem, getPlugin());
        hotbarItems.add(hotbarItem);
    }

    private void giveItems() {
        Bukkit.getOnlinePlayers().stream().filter(player -> !inDisabledWorld(player.getLocation())).forEach(player -> hotbarItems.forEach(hotbarItem -> hotbarItem.giveItem(player)));
    }

    private void removeItems() {
        Bukkit.getOnlinePlayers().stream().filter(player -> !inDisabledWorld(player.getLocation())).forEach(player -> hotbarItems.forEach(hotbarItem -> hotbarItem.removeItem(player)));
    }

    public PvPSwordItem getPvPSwordItem() {
        return pvpSwordItem;
    }
}
