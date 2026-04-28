# Bedrock Menus System Guide

This guide explains how to use the Bedrock menu system in DeluxeHub to create form-based menus for Bedrock Edition players.

## Table of Contents

1. [Overview](#overview)
2. [Requirements](#requirements)
3. [Configuration](#configuration)
4. [Architecture](#architecture)
5. [Creating Menus](#creating-menus)
6. [Menu Types](#menu-types)
7. [Navigation](#navigation)
8. [State Management](#state-management)
9. [Validation](#validation)
10. [Examples](#examples)

---

## Overview

The Bedrock menu system provides a platform-aware menu framework that automatically detects Bedrock players and shows them native Bedrock forms instead of inventory-based menus. This provides a better user experience for Bedrock Edition players.

### Features

- **Automatic Platform Detection** - Uses Floodgate API to detect Bedrock players
- **Three Form Types** - SimpleForm (buttons), CustomForm (inputs), ModalForm (yes/no)
- **Navigation Stack** - Built-in back navigation and menu history
- **State Management** - Persistent state for multi-step workflows
- **Timeout Handling** - Automatic form timeout with recovery
- **Validation** - Built-in validators for form inputs
- **Fallback Support** - Falls back to Java menus when Bedrock unavailable

---

## Requirements

### Server Requirements

1. **Geyser** - Required for Bedrock player support
2. **Floodgate** - Required for Bedrock player detection
3. **Paper/Spigot 1.20.1+** - Plugin base

### Dependencies (Already Included)

```gradle
compileOnly("org.geysermc.floodgate:api:2.2.3-SNAPSHOT")
compileOnly("org.geysermc.cumulus:cumulus:1.1.2")
```

---

## Configuration

Configuration is located in `config.yml`:

```yaml
bedrock_menus:
  # Enable Bedrock Edition form-based menus
  enabled: true

  # Force bedrock menus for all players (testing/debug)
  force_for_all: false

  # Fallback to Java Edition inventory menus when unavailable
  fallback_to_java: true

  # Form timeout in seconds
  form_timeout_seconds: 300  # 5 minutes

  # Form state expiration in minutes (for multi-step workflows)
  state_expiration_minutes: 30

  # Debug mode
  debug: false
```

---

## Architecture

### Core Classes

| Class | Purpose |
|-------|---------|
| `Menu` | Base interface for all menus |
| `BedrockMenu` | Interface for Bedrock-specific menus |
| `BaseBedrockMenu` | Abstract base class with lifecycle management |
| `MenuNavigator` | Manages menu navigation stack |
| `BedrockMenuNavigator` | Helper with convenience navigation methods |
| `FormStateManager` | Persistent state storage for workflows |
| `FloodgatePlatformDetection` | Detects Bedrock players and services |
| `BedrockFormUtils` | Utilities for formatting and validation |

### File Structure

```
src/main/java/net/zithium/deluxehub/menu/
├── Menu.java                           # Base menu interface
├── MenuNavigator.java                  # Navigation stack manager
└── bedrock/
    ├── BedrockMenu.java                # Bedrock menu interface
    ├── BaseBedrockMenu.java            # Abstract base implementation
    ├── BedrockMenuNavigator.java       # Navigation helpers
    ├── FormStateManager.java           # State persistence
    ├── FloodgatePlatformDetection.java # Platform detection
    ├── BedrockFormUtils.java           # Utilities
    └── menus/
        └── ExampleBedrockMenu.java     # Example implementations
```

---

## Creating Menus

### Basic Menu Structure

Every Bedrock menu extends `BaseBedrockMenu` and implements `getForm()`:

```java
public class MyBedrockMenu extends BaseBedrockMenu {

    private final BedrockMenuNavigator bedrockNavigator;

    public MyBedrockMenu(MenuNavigator menuNavigator, Player player, Logger logger) {
        super(menuNavigator, player, logger);
        this.bedrockNavigator = new BedrockMenuNavigator(menuNavigator);
    }

    @Override
    public Form getForm() {
        // Build and return your form here
        return SimpleForm.builder()
                .title("Menu Title")
                .content("Description")
                .button("Option 1")
                .validResultHandler(response -> {
                    onFormResponseReceived(); // IMPORTANT: Call this first
                    // Handle response
                })
                .build();
    }

    @Override
    public void handleResponse(Player player, Object response) {
        // Usually handled in validResultHandler
    }
}
```

### Opening a Menu

```java
// Create menu navigator for player
MenuNavigator navigator = new MenuNavigator(player);

// Create and open menu
MyBedrockMenu menu = new MyBedrockMenu(navigator, player, plugin.getLogger());
navigator.openMenu(menu);
```

---

## Menu Types

### 1. SimpleForm (Button Menu)

Use for basic navigation menus with buttons.

```java
@Override
public Form getForm() {
    return SimpleForm.builder()
            .title("§6§lHub Menu")
            .content("§7Select an option")
            .button("§aServer Selector")
            .button("§bSettings")
            .button("§cClose")
            .validResultHandler(response -> {
                onFormResponseReceived();

                int buttonId = response.clickedButtonId();
                switch (buttonId) {
                    case 0 -> openServerSelector();
                    case 1 -> openSettings();
                    case 2 -> bedrockNavigator.closeAll();
                }
            })
            .closedOrInvalidResultHandler((form, result) -> {
                onFormResponseReceived();
                player.sendMessage("§7Menu closed");
            })
            .build();
}
```

### 2. CustomForm (Input Form)

Use for collecting input from players.

```java
@Override
public Form getForm() {
    return CustomForm.builder()
            .title("§6§lEdit Profile")
            .label("§7Customize your settings")
            .input("Nickname", "Enter nickname", "DefaultName")
            .toggle("Enable Flying", false)
            .dropdown("Server", Arrays.asList("Survival", "Creative"), 0)
            .slider("Render Distance", 2, 16, 1, 8)
            .validResultHandler(response -> {
                onFormResponseReceived();

                // Extract values by index (label is index 0, not retrievable)
                String nickname = response.asInput(1);
                boolean flying = response.asToggle(2);
                int serverIdx = response.asDropdown(3);
                float distance = response.asSlider(4);

                // Validate
                String error = BedrockFormUtils.lengthValidator(3, 16).apply(nickname);
                if (error != null) {
                    player.sendMessage("§c" + error);
                    reopen("§cInvalid input!");
                    return;
                }

                // Apply changes
                player.setDisplayName(nickname);
                player.sendMessage("§aProfile updated!");
                bedrockNavigator.goBack();
            })
            .closedOrInvalidResultHandler((form, result) -> {
                onFormResponseReceived();
                bedrockNavigator.goBack();
            })
            .build();
}
```

### 3. ModalForm (Yes/No Confirmation)

Use for simple confirmations.

```java
@Override
public Form getForm() {
    return ModalForm.builder()
            .title("§c§lConfirm Action")
            .content("§eAre you sure you want to delete this?")
            .button1("§aYes, Delete")
            .button2("§cNo, Cancel")
            .validResultHandler(response -> {
                onFormResponseReceived();

                if (response.clickedButtonId() == 0) {
                    // Button 1 (Yes)
                    performDeletion();
                    player.sendMessage("§aDeleted!");
                } else {
                    // Button 2 (No)
                    player.sendMessage("§cCancelled");
                }

                bedrockNavigator.goBack();
            })
            .closedOrInvalidResultHandler((form, result) -> {
                onFormResponseReceived();
                bedrockNavigator.goBack();
            })
            .build();
}
```

---

## Navigation

### MenuNavigator Methods

```java
// Open a menu
navigator.openMenu(menu);

// Open with data
navigator.openMenu(menu, Map.of("key", "value"));

// Go back to previous menu
navigator.goBack();

// Go back with data
navigator.goBackWithData(Map.of("result", "success"));

// Close all menus
navigator.closeAll();

// Replace current menu (refresh)
navigator.replaceCurrentMenu(newMenu);

// Check if can go back
if (navigator.canGoBack()) {
    navigator.goBack();
}
```

### BedrockMenuNavigator Helpers

```java
BedrockMenuNavigator nav = new BedrockMenuNavigator(menuNavigator);

// Simple navigation
nav.goBack();
nav.openMenu(menu);
nav.closeAll();

// Create handlers
Runnable backHandler = nav.createBackHandler();
Runnable cancelHandler = nav.createCancelHandler("§cCancelled");
Runnable navHandler = nav.createNavigationHandler(nextMenu);

// Use in form
.button("Back")
.validResultHandler(response -> {
    backHandler.run();
})
```

---

## State Management

### Saving and Restoring State

```java
// Save state
Map<String, Object> state = Map.of(
    "step", 1,
    "username", "Player123",
    "completed", false
);
saveFormState("mykey", state);

// Restore state
Map<String, Object> restored = restoreFormState("mykey");
if (restored != null) {
    int step = (int) restored.get("step");
    String username = (String) restored.get("username");
}
```

### Multi-Step Workflows

```java
// Save workflow step
saveWorkflowStep("step1", Map.of("name", playerName));
saveWorkflowStep("step2", Map.of("age", playerAge));

// Restore entire workflow
Map<String, Object> workflow = restoreWorkflow();
Map<String, Object> step1Data = (Map) workflow.get("step1");

// Clear workflow when done
clearWorkflow();
```

### Passing Data Between Menus

```java
// Menu A passes data to Menu B
Map<String, Object> data = Map.of("itemId", 123, "action", "edit");
navigator.openMenu(new MenuB(...), data);

// Menu B receives data
public class MenuB extends BaseBedrockMenu {
    @Override
    public Form getForm() {
        Integer itemId = getData("itemId");
        String action = getData("action", "view");

        // Use the data...
    }
}
```

---

## Validation

### Built-in Validators

```java
// Not empty
var validator = BedrockFormUtils.NOT_EMPTY_VALIDATOR;
String error = validator.apply(input);

// Length
var lengthValidator = BedrockFormUtils.lengthValidator(3, 16);
error = lengthValidator.apply(nickname);

// Numeric range
var rangeValidator = BedrockFormUtils.rangeValidator(1, 100);
error = rangeValidator.apply(age);

// Pattern (regex)
var patternValidator = BedrockFormUtils.patternValidator(
    "^[a-zA-Z0-9_]+$",
    "Only letters, numbers, and underscores allowed"
);
error = patternValidator.apply(username);

// Combine validators
var combined = BedrockFormUtils.combineValidators(
    BedrockFormUtils.NOT_EMPTY_VALIDATOR,
    BedrockFormUtils.lengthValidator(3, 16),
    BedrockFormUtils.patternValidator("^[a-zA-Z0-9_]+$", "Invalid characters")
);
error = combined.apply(input);
```

### Using Validators in Forms

```java
.validResultHandler(response -> {
    onFormResponseReceived();

    String username = response.asInput(1);

    // Validate
    var validator = BedrockFormUtils.combineValidators(
        BedrockFormUtils.NOT_EMPTY_VALIDATOR,
        BedrockFormUtils.lengthValidator(3, 16)
    );

    String error = validator.apply(username);
    if (error != null) {
        player.sendMessage("§c" + error);
        reopen("§cValidation failed!");
        return;
    }

    // Process valid input
    applyUsername(username);
})
```

---

## Examples

### Example 1: Simple Server Selector

```java
public class ServerSelectorMenu extends BaseBedrockMenu {

    @Override
    public Form getForm() {
        return SimpleForm.builder()
                .title("§6§lServer Selector")
                .content("§7Choose a server to join")
                .button("§aSurvival\n§7100 players online")
                .button("§bCreative\n§750 players online")
                .button("§eSkyblock\n§775 players online")
                .button("§cBack")
                .validResultHandler(response -> {
                    onFormResponseReceived();

                    String[] servers = {"survival", "creative", "skyblock"};
                    int id = response.clickedButtonId();

                    if (id < servers.length) {
                        // Connect to server
                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                        out.writeUTF("Connect");
                        out.writeUTF(servers[id]);
                        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
                    } else {
                        bedrockNavigator.goBack();
                    }
                })
                .build();
    }
}
```

### Example 2: Player Report Form

```java
public class PlayerReportMenu extends BaseBedrockMenu {

    @Override
    public Form getForm() {
        // Get online players
        List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());

        return CustomForm.builder()
                .title("§c§lReport Player")
                .label("§7Please provide details about the issue")
                .dropdown("Player", playerNames, 0)
                .dropdown("Reason",
                    Arrays.asList("Cheating", "Griefing", "Harassment", "Other"),
                    0)
                .input("Details", "Describe what happened", "")
                .validResultHandler(response -> {
                    onFormResponseReceived();

                    int playerIdx = response.asDropdown(1);
                    int reasonIdx = response.asDropdown(2);
                    String details = response.asInput(3);

                    // Validate
                    var validator = BedrockFormUtils.lengthValidator(10, 500);
                    String error = validator.apply(details);
                    if (error != null) {
                        player.sendMessage("§cPlease provide more details (10-500 characters)");
                        reopen(null);
                        return;
                    }

                    // Submit report
                    String reportedPlayer = playerNames.get(playerIdx);
                    String[] reasons = {"Cheating", "Griefing", "Harassment", "Other"};
                    String reason = reasons[reasonIdx];

                    submitReport(player, reportedPlayer, reason, details);
                    player.sendMessage("§aReport submitted! Thank you.");

                    bedrockNavigator.closeAll();
                })
                .closedOrInvalidResultHandler((form, result) -> {
                    onFormResponseReceived();
                    bedrockNavigator.goBack();
                })
                .build();
    }

    private void submitReport(Player reporter, String reported, String reason, String details) {
        // Send to moderators, log to database, etc.
        plugin.getLogger().info(String.format(
            "REPORT: %s reported %s for %s - %s",
            reporter.getName(), reported, reason, details
        ));
    }
}
```

### Example 3: Multi-Step Teleport Setup

```java
public class TeleportSetupMenu extends BaseBedrockMenu {

    private final int step;

    public TeleportSetupMenu(MenuNavigator nav, Player p, Logger log, int step) {
        super(nav, p, log);
        this.step = step;
    }

    @Override
    public Form getForm() {
        if (step == 1) {
            return buildStep1();
        } else if (step == 2) {
            return buildStep2();
        } else {
            return buildStep3();
        }
    }

    private Form buildStep1() {
        return CustomForm.builder()
                .title("§6Teleport Setup - Step 1/3")
                .label("§7Enter teleport point name")
                .input("Name", "e.g., Spawn Point", "")
                .validResultHandler(response -> {
                    onFormResponseReceived();

                    String name = response.asInput(1);
                    var validator = BedrockFormUtils.lengthValidator(3, 32);
                    String error = validator.apply(name);

                    if (error != null) {
                        player.sendMessage("§c" + error);
                        reopen(null);
                        return;
                    }

                    saveWorkflowStep("step1", Map.of("name", name));
                    bedrockNavigator.openMenu(new TeleportSetupMenu(
                        menuNavigator, player, logger, 2
                    ));
                })
                .build();
    }

    private Form buildStep2() {
        Map<String, Object> workflow = restoreWorkflow();
        String name = (String) ((Map) workflow.get("step1")).get("name");

        return SimpleForm.builder()
                .title("§6Teleport Setup - Step 2/3")
                .content("§7Set location for: §f" + name)
                .button("§aUse Current Location")
                .button("§eChoose Different Location")
                .button("§cCancel")
                .validResultHandler(response -> {
                    onFormResponseReceived();

                    int id = response.clickedButtonId();
                    if (id == 0) {
                        Location loc = player.getLocation();
                        saveWorkflowStep("step2", Map.of(
                            "world", loc.getWorld().getName(),
                            "x", loc.getX(),
                            "y", loc.getY(),
                            "z", loc.getZ(),
                            "yaw", loc.getYaw(),
                            "pitch", loc.getPitch()
                        ));

                        bedrockNavigator.openMenu(new TeleportSetupMenu(
                            menuNavigator, player, logger, 3
                        ));
                    } else if (id == 1) {
                        player.sendMessage("§7Walk to the location and run: /tpsetup location");
                        bedrockNavigator.closeAll();
                    } else {
                        clearWorkflow();
                        bedrockNavigator.closeAll();
                    }
                })
                .build();
    }

    private Form buildStep3() {
        Map<String, Object> workflow = restoreWorkflow();
        String name = (String) ((Map) workflow.get("step1")).get("name");

        return ModalForm.builder()
                .title("§6Teleport Setup - Step 3/3")
                .content("§7Create teleport point: §f" + name + "§7?")
                .button1("§aYes, Create")
                .button2("§cNo, Cancel")
                .validResultHandler(response -> {
                    onFormResponseReceived();

                    if (response.clickedButtonId() == 0) {
                        createTeleportPoint(workflow);
                        player.sendMessage("§aTeleport point created!");
                        clearWorkflow();
                    }

                    bedrockNavigator.closeAll();
                })
                .build();
    }

    private void createTeleportPoint(Map<String, Object> workflow) {
        // Extract data and create teleport point
        Map<String, Object> step1 = (Map) workflow.get("step1");
        Map<String, Object> step2 = (Map) workflow.get("step2");

        String name = (String) step1.get("name");
        // ... create the teleport point
    }
}
```

---

## Best Practices

1. **Always call `onFormResponseReceived()`** at the start of result handlers to cancel timeout
2. **Validate all user input** before processing
3. **Provide clear feedback** with colored messages
4. **Use BedrockFormUtils** for consistent formatting
5. **Handle form closure** in `closedOrInvalidResultHandler`
6. **Clean up workflows** with `clearWorkflow()` when complete
7. **Test with both Java and Bedrock** clients
8. **Provide fallback Java menus** when possible

---

## Troubleshooting

### Forms not showing for Bedrock players

1. Check Floodgate is installed and running
2. Verify `bedrock_menus.enabled: true` in config
3. Check console for Floodgate/Cumulus detection messages

### Forms timing out

1. Increase `form_timeout_seconds` in config
2. Implement timeout recovery with `getCurrentFormState()`

### Validation not working

1. Ensure validators are applied before processing
2. Use `reopen()` to show the form again with error message

### State not persisting

1. Check `state_expiration_minutes` in config
2. Verify you're calling `saveFormState()` correctly
3. Use `clearWorkflow()` only when workflow is complete

---

## Additional Resources

- **Cumulus API Docs**: https://github.com/GeyserMC/Cumulus
- **Floodgate API Docs**: https://github.com/GeyserMC/Floodgate
- **Example Menus**: See `ExampleBedrockMenu.java`

---

*Created for DeluxeHub - Bedrock Menu System*
