# EnthusiaHub Design

**Date:** 2026-04-27
**Project:** EnthusiaHub — BadgersMC in-house hub plugin
**Status:** Approved

---

## Context (Read First)

EnthusiaHub started as a fork of [ItsLewizzz/DeluxeHub](https://github.com/ItsLewizzz/DeluxeHub) via [FreshSMP/DeluxeHub](https://github.com/FreshSMP/DeluxeHub). **The upstream is dead** — last upstream commit was 1.20 support. Our fork is 18 commits ahead with no upstream activity. There is no base to stay compatible with.

This means Phase 2 is not a migration — it is a **greenfield BadgersMC plugin** that happens to share feature parity with the current Java codebase. EnthusiaHub is fully owned by BadgersMC. All architectural decisions, naming, and conventions are ours.

The Phase 1 Java codebase at `D:/BadgersMC-Dev/DeluxeHub` is a **temporary working hub** — kept alive only until Phase 2 reaches feature parity, then retired.

---

## Overview

Two-phase plan for EnthusiaHub:

- **Phase 1 (immediate):** Java patch — fix three bug clusters and update to Paper 1.21.11. Fastest path to a working hub on the live server.
- **Phase 2 (future):** Greenfield Kotlin rewrite in a new repo (`BadgersMC/EnthusiaHub`) using nexus-paper, InventoryFramework (IF), and SPEAR architecture. This is the permanent, in-house version.

---

## Phase 1 — Java Patch

### 1.1 MC Version Update

Bump all version-sensitive dependencies to Paper 1.21.11:

| Dependency | Current | Target |
|---|---|---|
| `paper-api` | `1.20.1-R0.1-SNAPSHOT` | `1.21.11-R0.1-SNAPSHOT` |
| `item-nbt-api` | `2.15.3` | latest compatible |
| `XSeries` | `13.0.0` | latest compatible |

Audit all deprecated API calls introduced between 1.20.1 and 1.21.11. Pay attention to `Enchantment` registry changes (moved to `Registry.ENCHANTMENT` in 1.21+) and any `ItemMeta` / `Component` API shifts.

---

### 1.2 Bug: PvP Sword Event Priority

**Root cause:** `PvPSwordModule.onEnable()` fetches `pvpSwordItem` from `HotbarManager.getPvPSwordItem()` at startup. If module initialization order places `PvPSwordModule` before `HotbarManager`, `pvpSwordItem` is null. `isInPvPMode()` returns `false` for null reference → WorldProtect always sees "neither player in PvP mode" → damage always cancelled.

Secondary issue: `WorldProtect.onPvPDamage` fires at `LOWEST`. Even when both players are in PvP mode and `onPvPDamage` correctly skips cancellation, unrelated handlers at `NORMAL`/`HIGH` can still interfere.

**Fix:**
1. Make `PvPSwordModule.isInPvPMode()` fetch `pvpSwordItem` lazily (on first call) rather than caching at `onEnable()`. This eliminates the init-order dependency.
2. Change `WorldProtect.onPvPDamage` to `EventPriority.HIGH` and add `ignoreCancelled = false` so it can explicitly uncancel events that other LOW-priority handlers cancelled first.
3. Add permission `enthusia.pvp.bypass` — checked in `onPvPDamage` to allow LumaSG (and any other minigame) to bypass WorldProtect PvP cancellation entirely. LumaSG grants this to its players on game start, revokes on end.

---

### 1.3 Bug: Hotbar Items Moveable in Inventory

**Root cause:** Logic inversion in `HotbarManager.onEnable()`:

```java
// Current — inverted
customItem.setAllowMovement(config.getBoolean("custom_join_items.disable_inventory_movement"));

// Fix — negate to match intent
customItem.setAllowMovement(!config.getBoolean("custom_join_items.disable_inventory_movement"));
```

`disable_inventory_movement: true` in config means "movement should be disabled" → `allowMovement` should be `false` → the `InventoryClickEvent` handler cancels the click. Currently it does the opposite.

Same inversion applies to `PlayerHider` item setup. Both fixed with negation.

`HotbarItem.onInventoryClick` also needs to handle the cursor item (item being dragged onto the slot), not just the clicked item. Add check: if cursor item has `hotbarItem` NBT key and target slot is a hotbar slot, cancel.

---

### 1.4 Bug: Hotbar Items Droppable

**Root cause:** Same `allowMovement` inversion as 1.3. `HotbarItem.onItemDrop` returns early when `allowMovement=true`, which is incorrectly set when `disable_inventory_movement: true`.

Fixed by the same negation in 1.3. No additional changes needed once 1.3 is applied.

---

### 1.5 Bug: Hotbar Items Lost on Player Death

**Root cause:** No `PlayerDeathEvent` listener on `HotbarItem` strips items from the drop list. `hotbarPlayerRespawn` restores items on respawn but items are still dropped as loot on death.

**Fix:** Add `@EventHandler` on `PlayerDeathEvent` in `HotbarItem`:
- Iterate `event.getDrops()`, remove any `ItemStack` whose NBT `hotbarItem` key matches `this.key`.
- `hotbarPlayerRespawn` already handles re-giving — no change needed there.

`PvPSwordItem` already has its own `onPlayerDeath` that clears `pvpMode` and cancels countdowns. That handler is correct and stays.

---

## Phase 2 — Greenfield Kotlin Rewrite

> This is a new BadgersMC plugin, not a port. The Java fork is a reference for feature parity only — its architecture, conventions, and upstream lineage are irrelevant. Build in a new repo (`BadgersMC/EnthusiaHub`). When feature parity is confirmed on staging, retire the Java fork entirely.

### 2.1 Stack

| Concern | Choice | Reason |
|---|---|---|
| Language | Kotlin 2.x | Team standard, nexus requires it |
| DI / lifecycle | `nexus-paper` | Already in-house, annotation-driven |
| Commands | `nexus-paper` command scanner | Replaces CommandFramework |
| GUI menus | InventoryFramework (IF) 0.12+ | Team familiar from LumaGuilds, pane-based, XML support |
| Scheduling | FoliaLib (kept) | Folia-compatible, well-integrated |
| Config | KAML | Replaces verbose Bukkit YAML boilerplate |
| NBT | `item-nbt-api` (kept) | No replacement needed |
| Placeholder support | PlaceholderAPI (kept compileOnly) | Server standard |
| Target API | Paper 1.21.11 | Aligns with nexus-paper's Paper target |

---

### 2.2 Architecture (Hexagonal / SPEAR)

```
domain          ← pure Kotlin, zero Bukkit imports
application     ← use cases, depends only on domain
infrastructure  ← Paper/Bukkit glue, nexus wiring, IF menus
```

**Layer rules (enforced by SPEAR):**
- `domain` imports nothing from `application` or `infrastructure`
- `application` imports only `domain`
- `infrastructure` imports all three + frameworks

---

### 2.3 Domain Layer

Pure Kotlin data classes and interfaces. No Bukkit.

```
domain/
  model/
    HotbarItem.kt          // data class: key, slot, material, displayName, lore, actions
    HubModule.kt           // sealed interface for all module types
    PlayerState.kt         // transient per-player state (pvpMode, hidden, etc.)
    SpawnPoint.kt          // value object: world, x, y, z, yaw, pitch
  action/
    HubAction.kt           // sealed class: Message, Sound, Command, Title, Actionbar, Menu, Server
  port/
    PlayerStateRepository.kt   // interface: get/set PlayerState by UUID
    ModuleRepository.kt        // interface: load module configs
```

---

### 2.4 Application Layer

Use cases — one class per operation. Depend only on domain ports.

```
application/
  usecase/
    GiveHotbarItems.kt
    OpenMenu.kt
    TeleportToSpawn.kt
    TogglePvPMode.kt
    TogglePlayerVisibility.kt
    HandlePlayerJoin.kt
    HandlePlayerDeath.kt
    HandlePlayerRespawn.kt
```

Each use case is a simple `operator fun invoke(...)` — no framework annotations.

---

### 2.5 Infrastructure Layer

Paper/Bukkit implementations of domain ports + nexus wiring + IF menus.

```
infrastructure/
  nexus/
    EnthusiaHubPlugin.kt       // JavaPlugin entry point, nexus bootstrapper
  listener/
    WorldProtectListener.kt    // all world-protect events, correct priorities
    HotbarListener.kt          // click/drop/death/respawn item protection
    PvPListener.kt             // PvP sword, clean priority model
  command/
    HubCommand.kt              // @Subcommand annotation-driven
    FlyCommand.kt
    VanishCommand.kt
  menu/
    ServerSelectorMenu.kt      // IF StaticPane / PaginatedPane
    PlayerHiderMenu.kt
  module/
    DoubleJumpModule.kt
    LaunchpadModule.kt
    HologramModule.kt
    WorldProtectModule.kt
    PvPSwordModule.kt          // owns pvp mode state, no external dependency
  config/
    KamlConfigLoader.kt
  repository/
    InMemoryPlayerStateRepository.kt
```

**PvP model in Phase 2 (clean design):**
- `PvPSwordModule` owns a `Set<UUID>` pvpMode state internally — not split across Module + Item.
- `WorldProtectListener` subscribes to a `PvPStateChangedEvent` (custom Paper event) rather than calling into modules directly.
- Bypass via permission `enthusia.pvp.bypass` retained from Phase 1.

**IF menu pattern:**
- Static menus (server selector, player hider) defined via XML loaded from `resources/menus/`
- Dynamic elements (online player counts, player heads) injected programmatically into named panes after XML load
- Consistent with LumaGuilds usage

---

### 2.6 Phase 2 Gate — Before Retiring Java Fork

The Java fork is the reference for what features exist, not how they should be built. Before retiring it:

- [ ] SPEAR `requirements.md` written for EnthusiaHub — all features from Java fork captured as REQ- IDs
- [ ] All REQ- IDs covered by failing tests before implementation begins (SPEAR TDD rule)
- [ ] Feature parity confirmed on staging: all modules, commands, menus
- [ ] PvP sword + `enthusia.pvp.bypass` verified with LumaSG on staging server
- [ ] No Folia-incompatible scheduling
- [ ] Java fork repo archived (not deleted) for historical reference

---

## Decision Log

| Decision | Reason |
|---|---|
| Phase 1 Java patch, not rewrite | Need working hub fast — Phase 2 is permanent solution |
| Phase 2 is greenfield, not a port | Upstream (ItsLewizzz/DeluxeHub) is dead. No compatibility obligation. Own the architecture fully. |
| New repo for Phase 2 (`BadgersMC/EnthusiaHub`) | Clean slate, no upstream git history, no fork conventions |
| Java fork archived not deleted | Historical reference for feature list during Phase 2 SPEAR spec writing |
| Keep FoliaLib in Phase 2 | Folia support required, no equivalent in nexus-paper yet |
| IF over custom inventory | Team familiar from LumaGuilds, actively maintained, supports 1.21+ |
| Lazy init for PvPSwordModule | Eliminates fragile module init-order dependency in Phase 1 |
| `enthusia.pvp.bypass` permission | Clean seam for LumaSG without coupling plugins — carried into Phase 2 |
