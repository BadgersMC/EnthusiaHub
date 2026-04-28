package gg.badgersmc.enthusiahub.menu.bedrock;

import gg.badgersmc.enthusiahub.EnthusiaHubPlugin;
import gg.badgersmc.enthusiahub.menu.Menu;
import gg.badgersmc.enthusiahub.menu.MenuNavigator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.geysermc.cumulus.form.Form;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Abstract base class for all Bedrock menus.
 * Provides common functionality for form lifecycle, state management, and error handling.
 */
public abstract class BaseBedrockMenu implements BedrockMenu {

    protected final MenuNavigator menuNavigator;
    protected final Player player;
    protected final Logger logger;
    protected final EnthusiaHubPlugin plugin;
    protected final FormStateManager stateManager;

    // Data passed to this menu
    protected Map<String, Object> passedData = new HashMap<>();

    // Form timeout task
    private BukkitRunnable timeoutTask;

    public BaseBedrockMenu(MenuNavigator menuNavigator, Player player, Logger logger) {
        this.menuNavigator = menuNavigator;
        this.player = player;
        this.logger = logger;
        this.plugin = EnthusiaHubPlugin.getInstance();
        this.stateManager = plugin.getFormStateManager();
    }

    @Override
    public void open() {
        // Check if Bedrock services are available
        if (!isBedrockServicesAvailable()) {
            handleBedrockUnavailable();
            return;
        }

        // Build and send the form
        if (shouldBuildAsync()) {
            buildAndSendFormAsync();
        } else {
            buildAndSendFormSync();
        }
    }

    /**
     * Builds and sends the form synchronously.
     */
    private void buildAndSendFormSync() {
        try {
            Form form = getFormCached();
            sendForm(form);
        } catch (Exception e) {
            logger.severe("Error building Bedrock form for " + getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            handleFormBuildError();
        }
    }

    /**
     * Builds and sends the form asynchronously.
     */
    private void buildAndSendFormAsync() {
        player.sendMessage("§7Loading menu...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Form form = getFormCached();

                // Send on main thread
                Bukkit.getScheduler().runTask(plugin, () -> sendForm(form));
            } catch (Exception e) {
                logger.severe("Error building Bedrock form async for " + getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();

                Bukkit.getScheduler().runTask(plugin, this::handleFormBuildError);
            }
        });
    }

    /**
     * Gets the form, using cache if enabled.
     */
    private Form getFormCached() {
        // For now, just call getForm() directly
        // Caching can be implemented later with a cache service
        return getForm();
    }

    /**
     * Sends the form to the player and starts timeout tracking.
     */
    private void sendForm(Form form) {
        try {
            FloodgateApi api = FloodgateApi.getInstance();
            api.sendForm(player.getUniqueId(), form);

            // Start timeout task
            startTimeoutTask();
        } catch (Exception e) {
            logger.severe("Error sending form to player " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            handleFormSendError();
        }
    }

    /**
     * Starts a timeout task to handle form expiration.
     */
    private void startTimeoutTask() {
        cancelTimeoutTask(); // Cancel any existing task

        int timeoutSeconds = getFormTimeoutSeconds();
        timeoutTask = new BukkitRunnable() {
            @Override
            public void run() {
                handleFormTimeout();
            }
        };

        timeoutTask.runTaskLater(plugin, timeoutSeconds * 20L);
    }

    /**
     * Cancels the active timeout task if one exists.
     */
    protected void cancelTimeoutTask() {
        if (timeoutTask != null && !timeoutTask.isCancelled()) {
            timeoutTask.cancel();
            timeoutTask = null;
        }
    }

    /**
     * Called when form is successfully received and processed.
     */
    protected void onFormResponseReceived() {
        cancelTimeoutTask();
    }

    /**
     * Called when the form times out.
     */
    protected void handleFormTimeout() {
        if (!player.isOnline()) {
            return;
        }

        player.sendMessage("§cMenu timed out after " + getFormTimeoutSeconds() + " seconds.");

        // Save current state for recovery if needed
        Map<String, Object> currentState = getCurrentFormState();
        if (currentState != null && !currentState.isEmpty()) {
            saveFormState("timeout_recovery", currentState);
        }

        // Go back
        if (menuNavigator.canGoBack()) {
            menuNavigator.goBack();
        }
    }

    /**
     * Gets the current form state for timeout recovery.
     * Override this to provide state to save on timeout.
     *
     * @return The current state data
     */
    protected Map<String, Object> getCurrentFormState() {
        return new HashMap<>();
    }

    @Override
    public void passData(Map<String, Object> data) {
        if (data != null) {
            this.passedData = new HashMap<>(data);
        }
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public MenuNavigator getMenuNavigator() {
        return menuNavigator;
    }

    @Override
    public void onClose() {
        cancelTimeoutTask();
    }

    // ===== State Management Methods =====

    /**
     * Saves form state with a given key.
     *
     * @param key   The state key
     * @param state The state data
     */
    protected void saveFormState(String key, Map<String, Object> state) {
        stateManager.saveState(player.getUniqueId(), key, state);
    }

    /**
     * Restores form state by key.
     *
     * @param key The state key
     * @return The state data, or null if not found
     */
    protected Map<String, Object> restoreFormState(String key) {
        return stateManager.restoreState(player.getUniqueId(), key);
    }

    /**
     * Saves a workflow step for multi-step forms.
     *
     * @param stepName The step name
     * @param data     The step data
     */
    protected void saveWorkflowStep(String stepName, Map<String, Object> data) {
        String workflowKey = "workflow_" + getClass().getSimpleName();
        Map<String, Object> workflow = restoreFormState(workflowKey);
        if (workflow == null) {
            workflow = new HashMap<>();
        }
        workflow.put(stepName, data);
        saveFormState(workflowKey, workflow);
    }

    /**
     * Restores the entire workflow.
     *
     * @return The workflow data
     */
    protected Map<String, Object> restoreWorkflow() {
        String workflowKey = "workflow_" + getClass().getSimpleName();
        return restoreFormState(workflowKey);
    }

    /**
     * Clears the workflow data.
     */
    protected void clearWorkflow() {
        String workflowKey = "workflow_" + getClass().getSimpleName();
        stateManager.clearState(player.getUniqueId(), workflowKey);
    }

    // ===== Error Handling Methods =====

    /**
     * Checks if Bedrock services (Floodgate + Cumulus) are available.
     */
    private boolean isBedrockServicesAvailable() {
        try {
            FloodgateApi api = FloodgateApi.getInstance();
            if (api == null) {
                return false;
            }

            // Verify Cumulus is available
            Class.forName("org.geysermc.cumulus.form.Form");

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Handles when Bedrock services are unavailable.
     */
    private void handleBedrockUnavailable() {
        player.sendMessage("§cBedrock menus are currently unavailable.");

        // Check if fallback to Java menus is enabled
        if (plugin.getConfig().getBoolean("bedrock_menus.fallback_to_java", true)) {
            Menu fallbackMenu = createFallbackJavaMenu();
            if (fallbackMenu != null) {
                player.sendMessage("§7Opening Java version of menu...");
                menuNavigator.openMenu(fallbackMenu);
                return;
            }
        }

        // Close all menus
        menuNavigator.closeAll();
    }

    /**
     * Creates a fallback Java menu when Bedrock menus are unavailable.
     * Override this to provide a Java Edition inventory-based menu as fallback.
     *
     * @return The fallback menu, or null if not implemented
     */
    protected Menu createFallbackJavaMenu() {
        return null;
    }

    /**
     * Handles errors when building the form.
     */
    private void handleFormBuildError() {
        player.sendMessage("§cAn error occurred while loading the menu.");

        if (menuNavigator.canGoBack()) {
            menuNavigator.goBack();
        } else {
            menuNavigator.closeAll();
        }
    }

    /**
     * Handles errors when sending the form to the player.
     */
    private void handleFormSendError() {
        player.sendMessage("§cAn error occurred while opening the menu.");

        if (menuNavigator.canGoBack()) {
            menuNavigator.goBack();
        } else {
            menuNavigator.closeAll();
        }
    }

    /**
     * Reopens this menu with an optional error message.
     * Useful for validation failures.
     *
     * @param errorMessage The error message to display
     */
    protected void reopen(String errorMessage) {
        if (errorMessage != null && !errorMessage.isEmpty()) {
            player.sendMessage(errorMessage);
        }

        // Replace current menu with fresh instance
        open();
    }

    /**
     * Helper method to get a value from passed data.
     *
     * @param key The data key
     * @param <T> The expected type
     * @return The value, or null if not found
     */
    @SuppressWarnings("unchecked")
    protected <T> T getData(String key) {
        return (T) passedData.get(key);
    }

    /**
     * Helper method to get a value from passed data with a default.
     *
     * @param key          The data key
     * @param defaultValue The default value
     * @param <T>          The expected type
     * @return The value, or default if not found
     */
    @SuppressWarnings("unchecked")
    protected <T> T getData(String key, T defaultValue) {
        Object value = passedData.get(key);
        return value != null ? (T) value : defaultValue;
    }
}
