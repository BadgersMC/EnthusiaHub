# EnthusiaHub Phase 1 — Java Patch Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Update EnthusiaHub to Paper 1.21.11 and fix three bug clusters: PvP sword event priority, hotbar item movement/drop, and item loss on death.

**Architecture:** All changes are surgical patches to existing Java classes — no new files, no structural changes. Each task is self-contained and independently buildable.

**Tech Stack:** Java 21, Paper 1.21.11, Gradle + Shadow, item-nbt-api, XSeries, FoliaLib

---

> **No unit test framework is configured in this project.** Each task includes a build step and a manual verification checklist to run on a local test server. Build command: `./gradlew shadowJar` from `D:/BadgersMC-Dev/DeluxeHub/`. Output jar: `build/libs/EnthusiaHub-<version>.jar`.

---

### Task 1: Bump Paper API to 1.21.11

**Files:**
- Modify: `build.gradle.kts`
- Modify: `src/main/resources/plugin.yml`

- [ ] **Step 1: Update paper-api version in build.gradle.kts**

In `build.gradle.kts`, change:
```kotlin
compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
```
to:
```kotlin
compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
```

- [ ] **Step 2: Update item-nbt-api version**

In `build.gradle.kts`, change:
```kotlin
implementation("de.tr7zw:item-nbt-api:2.15.3") // UPDATE THIS FOR EACH NEW MC VERSION
```
to:
```kotlin
implementation("de.tr7zw:item-nbt-api:2.15.3") // verified compatible with 1.21.x
```
> item-nbt-api 2.15.3 is compatible with 1.21.x. No version bump needed — the comment is the fix.

- [ ] **Step 3: Update api-version in plugin.yml**

In `src/main/resources/plugin.yml`, change:
```yaml
api-version: 1.20
```
to:
```yaml
api-version: 1.21
```

- [ ] **Step 4: Build and verify compilation**

```bash
cd D:/BadgersMC-Dev/DeluxeHub
./gradlew shadowJar
```
Expected: `BUILD SUCCESSFUL`. If compilation errors appear, note them — they will be addressed in Task 2.

- [ ] **Step 5: Commit**

```bash
git add build.gradle.kts src/main/resources/plugin.yml
git commit -m "chore: bump paper-api to 1.21.11, update api-version"
```

---

### Task 2: Fix Deprecated Enchantment API (1.21.11 compat)

**Files:**
- Modify: `src/main/java/net/zithium/deluxehub/module/modules/hotbar/items/PvPSwordItem.java`

`Enchantment.getByName(String)` is removed/deprecated in 1.21.11. Must use `Enchantment.getByKey(NamespacedKey)`.

- [ ] **Step 1: Update the import in PvPSwordItem.java**

At the top of `PvPSwordItem.java`, add this import if not already present:
```java
import org.bukkit.NamespacedKey;
```

- [ ] **Step 2: Replace getByName with getByKey in createArmor()**

In `PvPSwordItem.java`, find the `createArmor()` method. Replace:
```java
Enchantment enchant = Enchantment.getByName(parts[0]);
int level = Integer.parseInt(parts[1]);
if (enchant != null) {
    meta.addEnchant(enchant, level, true);
}
```
with:
```java
Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft(parts[0].toLowerCase()));
int level = Integer.parseInt(parts[1]);
if (enchant != null) {
    meta.addEnchant(enchant, level, true);
}
```

- [ ] **Step 3: Build and verify**

```bash
./gradlew shadowJar
```
Expected: `BUILD SUCCESSFUL` with no deprecation errors related to `Enchantment.getByName`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/net/zithium/deluxehub/module/modules/hotbar/items/PvPSwordItem.java
git commit -m "fix: replace deprecated Enchantment.getByName with getByKey for 1.21.11"
```

---

### Task 3: Add PVP_BYPASS Permission

**Files:**
- Modify: `src/main/java/net/zithium/deluxehub/Permissions.java`
- Modify: `src/main/resources/plugin.yml`

This permission allows LumaSG (or any minigame) to bypass WorldProtect PvP cancellation without needing the hub PvP sword.

- [ ] **Step 1: Add PVP_BYPASS to the Permissions enum**

In `Permissions.java`, add the new entry after `EVENT_PLAYER_PVP`:
```java
EVENT_PLAYER_PVP("player.pvp"),
PVP_BYPASS("bypass.pvp");
```

Full updated enum (replace the existing `EVENT_BLOCK_PLACE` line and below):
```java
    EVENT_ITEM_DROP("item.drop"),
    EVENT_ITEM_PICKUP("item.pickup"),
    EVENT_PLAYER_PVP("player.pvp"),
    PVP_BYPASS("bypass.pvp"),
    EVENT_BLOCK_INTERACT("block.interact"),
    EVENT_BLOCK_BREAK("block.break"),
    EVENT_BLOCK_PLACE("block.place");
```

- [ ] **Step 2: Register the permission in plugin.yml**

In `src/main/resources/plugin.yml`, under `deluxehub.bypass.*` children, add:
```yaml
  deluxehub.bypass.*:
    description: Gives access to all bypass permissions
    children:
      deluxehub.bypass.antiwdl: true
      deluxehub.bypass.doublejump: false
      deluxehub.bypass.pvp: false
```

- [ ] **Step 3: Build**

```bash
./gradlew shadowJar
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/net/zithium/deluxehub/Permissions.java src/main/resources/plugin.yml
git commit -m "feat: add deluxehub.bypass.pvp permission for minigame PvP bypass"
```

---

### Task 4: Fix PvPSwordModule Lazy Initialization

**Files:**
- Modify: `src/main/java/net/zithium/deluxehub/module/modules/hotbar/PvPSwordModule.java`

**Root cause:** `PvPSwordModule.onEnable()` caches `pvpSwordItem` at startup. If `PvPSwordModule` enables before `HotbarManager`, `pvpSwordItem` is null. `isInPvPMode()` returns `false` for null → WorldProtect always sees "neither player in PvP mode" → damage always cancelled.

**Fix:** Fetch `pvpSwordItem` lazily on first use.

- [ ] **Step 1: Replace onEnable body and add lazy getter**

In `PvPSwordModule.java`, replace the entire class body with:

```java
public class PvPSwordModule extends Module {

    private final PlatformScheduler scheduler = DeluxeHubPlugin.scheduler();
    private PvPSwordItem pvpSwordItem;

    public PvPSwordModule(DeluxeHubPlugin plugin) {
        super(plugin, ModuleType.PVP_SWORD);
    }

    @Override
    public void onEnable() {
        // pvpSwordItem resolved lazily — HotbarManager may not be enabled yet
    }

    @Override
    public void onDisable() {
        pvpSwordItem = null;
    }

    private PvPSwordItem resolvePvPSwordItem() {
        if (pvpSwordItem == null) {
            HotbarManager hotbarManager = (HotbarManager) getPlugin().getModuleManager().getModule(ModuleType.HOTBAR_ITEMS);
            if (hotbarManager != null) {
                pvpSwordItem = hotbarManager.getPvPSwordItem();
            }
        }
        return pvpSwordItem;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        PvPSwordItem item = resolvePvPSwordItem();
        if (item == null) return;

        Player player = event.getEntity();
        if (!item.isInPvPMode(player)) return;

        FileConfiguration config = getConfig(ConfigType.SETTINGS);
        Location deathLoc = player.getLocation();

        if (config.getBoolean("pvp_sword.death.lightning", true)) {
            scheduler.runAtLocation(deathLoc, task -> {
                deathLoc.getWorld().strikeLightningEffect(deathLoc);
            });
        }

        if (config.getBoolean("pvp_sword.death.knockback.enabled", true)) {
            boolean onlyPvPPlayers = config.getBoolean("pvp_sword.death.knockback.only_pvp_players", true);
            double knockbackStrength = config.getDouble("pvp_sword.death.knockback.strength", 1.5);
            double radius = 5.0;

            scheduler.runAtLocation(deathLoc, task -> {
                for (Entity entity : deathLoc.getWorld().getNearbyEntities(deathLoc, radius, radius, radius)) {
                    if (entity instanceof Player nearby && !nearby.equals(player)) {
                        if (onlyPvPPlayers && !item.isInPvPMode(nearby)) {
                            continue;
                        }
                        Vector direction = nearby.getLocation().toVector().subtract(deathLoc.toVector()).normalize();
                        direction.setY(0.5);
                        nearby.setVelocity(direction.multiply(knockbackStrength));
                    }
                }
            });
        }

        if (config.getBoolean("pvp_sword.death.remove_items", true)) {
            event.getDrops().clear();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Cleanup handled by PvPSwordItem
    }

    public boolean isInPvPMode(Player player) {
        PvPSwordItem item = resolvePvPSwordItem();
        return item != null && item.isInPvPMode(player);
    }
}
```

- [ ] **Step 2: Build**

```bash
./gradlew shadowJar
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/net/zithium/deluxehub/module/modules/hotbar/PvPSwordModule.java
git commit -m "fix: lazy-init pvpSwordItem in PvPSwordModule to eliminate init-order bug"
```

---

### Task 5: Fix WorldProtect PvP Event Priority + Bypass

**Files:**
- Modify: `src/main/java/net/zithium/deluxehub/module/modules/world/WorldProtect.java`

Two changes:
1. Raise `onPvPDamage` from `LOWEST` to `HIGH` so it fires after other plugins (e.g. LumaSG) that may uncancel at `LOW`/`NORMAL`.
2. Add `PVP_BYPASS` permission check so minigame players skip WorldProtect PvP entirely.

- [ ] **Step 1: Change onPvPDamage priority to HIGH**

In `WorldProtect.java`, find:
```java
@EventHandler(priority = EventPriority.LOWEST)
public void onPvPDamage(EntityDamageByEntityEvent event) {
```
Replace with:
```java
@EventHandler(priority = EventPriority.HIGH)
public void onPvPDamage(EntityDamageByEntityEvent event) {
```

- [ ] **Step 2: Add PVP_BYPASS check before pvpMode check**

In `onPvPDamage`, find the attacker-not-null block:
```java
if (attacker != null) {
    Module pvpModule = getPlugin().getModuleManager().getModule(ModuleType.PVP_SWORD);
    if (pvpModule instanceof net.zithium.deluxehub.module.modules.hotbar.PvPSwordModule pvpSword) {
        if (pvpSword.isInPvPMode(player) && pvpSword.isInPvPMode(attacker)) {
            return; // Allow damage - both in PvP mode
        }
    }

    // Check permission bypass
    if (attacker.hasPermission(Permissions.EVENT_PLAYER_PVP.getPermission())) {
        return;
    }
}
```
Replace with:
```java
if (attacker != null) {
    // Minigame bypass (LumaSG grants this permission during active games)
    if (attacker.hasPermission(Permissions.PVP_BYPASS.getPermission())) {
        return;
    }

    Module pvpModule = getPlugin().getModuleManager().getModule(ModuleType.PVP_SWORD);
    if (pvpModule instanceof net.zithium.deluxehub.module.modules.hotbar.PvPSwordModule pvpSword) {
        if (pvpSword.isInPvPMode(player) && pvpSword.isInPvPMode(attacker)) {
            return; // Allow damage - both in PvP mode
        }
    }

    // Admin bypass
    if (attacker.hasPermission(Permissions.EVENT_PLAYER_PVP.getPermission())) {
        return;
    }
}
```

- [ ] **Step 3: Build**

```bash
./gradlew shadowJar
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Manual test on server**

Deploy jar to a local 1.21.11 Paper test server. In-game checks:
1. Two players without PvP sword — neither can hit the other. ✓
2. Both players activate PvP sword (right-click → wait countdown) — they can hit each other. ✓
3. Grant one player `deluxehub.bypass.pvp` — that player can hit anyone regardless of PvP mode. ✓
4. Revoke `deluxehub.bypass.pvp` — attacks blocked again. ✓

- [ ] **Step 5: Commit**

```bash
git add src/main/java/net/zithium/deluxehub/module/modules/world/WorldProtect.java
git commit -m "fix: raise onPvPDamage to HIGH priority, add bypass.pvp permission for minigames"
```

---

### Task 6: Fix allowMovement Inversion in HotbarManager

**Files:**
- Modify: `src/main/java/net/zithium/deluxehub/module/modules/hotbar/HotbarManager.java`

**Root cause:** `setAllowMovement(config.getBoolean("disable_inventory_movement"))` passes `true` when movement should be disabled. The field name `allowMovement` means "is movement allowed?" — so `disable=true` must set `allow=false`.

- [ ] **Step 1: Fix inversion for custom join items**

In `HotbarManager.java`, find:
```java
customItem.setAllowMovement(config.getBoolean("custom_join_items.disable_inventory_movement"));
```
Replace with:
```java
customItem.setAllowMovement(!config.getBoolean("custom_join_items.disable_inventory_movement"));
```

- [ ] **Step 2: Fix inversion for player hider**

In `HotbarManager.java`, find:
```java
playerHider.setAllowMovement(config.getBoolean("player_hider.disable_inventory_movement"));
```
Replace with:
```java
playerHider.setAllowMovement(!config.getBoolean("player_hider.disable_inventory_movement"));
```

> The PvP sword item already has `setAllowMovement(false)` hardcoded — correct, no change needed.

- [ ] **Step 3: Build**

```bash
./gradlew shadowJar
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/net/zithium/deluxehub/module/modules/hotbar/HotbarManager.java
git commit -m "fix: negate allowMovement flag — disable_inventory_movement:true was allowing movement"
```

---

### Task 7: Fix HotbarItem Cursor Check + Strip Drops on Death

**Files:**
- Modify: `src/main/java/net/zithium/deluxehub/module/modules/hotbar/HotbarItem.java`

Two sub-fixes:
1. `onInventoryClick` only checked the clicked slot item, not the cursor (item being dragged onto the slot). Players could drag items out.
2. No `PlayerDeathEvent` listener — hub items dropped as loot on player death.

- [ ] **Step 1: Add cursor check to onInventoryClick**

In `HotbarItem.java`, find the full `onInventoryClick` method:
```java
@EventHandler
public void onInventoryClick(InventoryClickEvent event) {
    if (allowMovement) {
        return;
    }

    Player player = (Player) event.getWhoClicked();
    if (getHotbarManager().inDisabledWorld(player.getLocation())) {
        return;
    }

    ItemStack clicked = event.getCurrentItem();
    if (clicked == null || clicked.getType() == Material.AIR) {
        return;
    }

    if (new NBTItem(clicked).getString("hotbarItem").equals(key)) {
        event.setCancelled(true);
    }
}
```
Replace with:
```java
@EventHandler
public void onInventoryClick(InventoryClickEvent event) {
    if (allowMovement) {
        return;
    }

    Player player = (Player) event.getWhoClicked();
    if (getHotbarManager().inDisabledWorld(player.getLocation())) {
        return;
    }

    ItemStack clicked = event.getCurrentItem();
    if (clicked != null && clicked.getType() != Material.AIR) {
        if (new NBTItem(clicked).getString("hotbarItem").equals(key)) {
            event.setCancelled(true);
            return;
        }
    }

    // Also block dragging a hotbar item onto another slot via cursor
    ItemStack cursor = event.getCursor();
    if (cursor != null && cursor.getType() != Material.AIR) {
        if (new NBTItem(cursor).getString("hotbarItem").equals(key)) {
            event.setCancelled(true);
        }
    }
}
```

- [ ] **Step 2: Add PlayerDeathEvent to strip hotbar items from drops**

In `HotbarItem.java`, after the `onItemDrop` method, add:
```java
@EventHandler
public void onPlayerDeath(PlayerDeathEvent event) {
    if (allowMovement) {
        return;
    }

    event.getDrops().removeIf(drop ->
        drop != null && drop.getType() != Material.AIR &&
        new NBTItem(drop).getString("hotbarItem").equals(key)
    );
}
```

- [ ] **Step 3: Build**

```bash
./gradlew shadowJar
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Manual test on server**

Deploy jar. In-game checks:
1. Open inventory — try to click and drag hub items to other slots. Blocked. ✓
2. Press Q while holding a hub item — item not dropped. ✓
3. Die (void, fall, etc.) — hub items not in the death drops. ✓
4. Respawn — hub items restored to correct slots. ✓
5. World with `disable_inventory_movement: false` configured — items ARE moveable. ✓

- [ ] **Step 5: Commit**

```bash
git add src/main/java/net/zithium/deluxehub/module/modules/hotbar/HotbarItem.java
git commit -m "fix: block cursor drag of hotbar items, strip hotbar items from death drops"
```

---

### Task 8: Final Integration Build + Smoke Test

- [ ] **Step 1: Clean build**

```bash
./gradlew clean shadowJar
```
Expected: `BUILD SUCCESSFUL`. Jar at `build/libs/EnthusiaHub-3.7.1.jar`.

- [ ] **Step 2: Full smoke test checklist on 1.21.11 server**

Deploy jar to a Paper 1.21.11 test server. Run all checks:

**PvP sword:**
- [ ] Two players without PvP mode — cannot hit each other
- [ ] Both activate PvP sword — can hit each other, damage registers
- [ ] One player dies in PvP — lightning effect, knockback applied, no drops
- [ ] Player with `deluxehub.bypass.pvp` — can hit non-PvP players freely

**Hotbar items:**
- [ ] Hub items cannot be clicked/dragged out of hotbar slots
- [ ] Hub items cannot be dropped with Q
- [ ] Dying does not drop hub items
- [ ] Respawning restores hub items to correct slots
- [ ] World change removes/restores items correctly

**General 1.21.11:**
- [ ] Plugin loads without errors in console
- [ ] No deprecation warnings relating to Enchantment API
- [ ] PvP sword armor enchantments apply correctly (check config enchantment names are lowercase minecraft keys e.g. `sharpness:1` not `DAMAGE_ALL:1`)

- [ ] **Step 3: Bump version and commit**

In `build.gradle.kts`, change:
```kotlin
version = "3.7.1"
```
to:
```kotlin
version = "3.8.0"
```

```bash
git add build.gradle.kts
git commit -m "chore: bump version to 3.8.0 — 1.21.11 update + bug fixes"
```

> **Note on enchantment config:** In 1.21.11, `Enchantment.getByKey` uses lowercase minecraft keys. If your config has `pvp_sword.armor.<piece>.enchantments` entries like `SHARPNESS:5`, change them to `sharpness:5`. The new code does `parts[0].toLowerCase()` which handles this automatically.
