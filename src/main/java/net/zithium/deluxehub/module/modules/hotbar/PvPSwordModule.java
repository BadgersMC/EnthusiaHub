package net.zithium.deluxehub.module.modules.hotbar;

import com.tcoded.folialib.impl.PlatformScheduler;
import net.zithium.deluxehub.DeluxeHubPlugin;
import net.zithium.deluxehub.config.ConfigType;
import net.zithium.deluxehub.module.Module;
import net.zithium.deluxehub.module.ModuleType;
import net.zithium.deluxehub.module.modules.hotbar.items.PvPSwordItem;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

public class PvPSwordModule extends Module {

    private final PlatformScheduler scheduler = DeluxeHubPlugin.scheduler();
    private PvPSwordItem pvpSwordItem;

    public PvPSwordModule(DeluxeHubPlugin plugin) {
        super(plugin, ModuleType.PVP_SWORD);
    }

    @Override
    public void onEnable() {
        // Get reference to PvPSwordItem from HotbarManager
        HotbarManager hotbarManager = (HotbarManager) getPlugin().getModuleManager().getModule(ModuleType.HOTBAR_ITEMS);
        if (hotbarManager != null) {
            pvpSwordItem = hotbarManager.getPvPSwordItem();
        }
    }

    @Override
    public void onDisable() {
        pvpSwordItem = null;
    }

    // Damage handling is done by WorldProtect module - no need to duplicate it here

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (pvpSwordItem == null) return;

        Player player = event.getEntity();

        // Only handle death effects if player was in PvP mode
        if (!pvpSwordItem.isInPvPMode(player)) return;

        FileConfiguration config = getConfig(ConfigType.SETTINGS);
        Location deathLoc = player.getLocation();

        // Strike lightning effect
        if (config.getBoolean("pvp_sword.death.lightning", true)) {
            scheduler.runAtLocation(deathLoc, task -> {
                deathLoc.getWorld().strikeLightningEffect(deathLoc);
            });
        }

        // Apply knockback to nearby PvP players
        if (config.getBoolean("pvp_sword.death.knockback.enabled", true)) {
            boolean onlyPvPPlayers = config.getBoolean("pvp_sword.death.knockback.only_pvp_players", true);
            double knockbackStrength = config.getDouble("pvp_sword.death.knockback.strength", 1.5);
            double radius = 5.0;

            scheduler.runAtLocation(deathLoc, task -> {
                for (Entity entity : deathLoc.getWorld().getNearbyEntities(deathLoc, radius, radius, radius)) {
                    if (entity instanceof Player nearby && !nearby.equals(player)) {
                        // Only knockback PvP players if configured
                        if (onlyPvPPlayers && !pvpSwordItem.isInPvPMode(nearby)) {
                            continue;
                        }

                        // Calculate knockback direction
                        Vector direction = nearby.getLocation().toVector().subtract(deathLoc.toVector()).normalize();
                        direction.setY(0.5); // Upward component
                        nearby.setVelocity(direction.multiply(knockbackStrength));
                    }
                }
            });
        }

        // Remove items from death drops if configured
        if (config.getBoolean("pvp_sword.death.remove_items", true)) {
            event.getDrops().clear();
        }

        // Player will be removed from PvP mode by PvPSwordItem's death handler
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Cleanup is handled by PvPSwordItem
    }

    public boolean isInPvPMode(Player player) {
        return pvpSwordItem != null && pvpSwordItem.isInPvPMode(player);
    }
}
