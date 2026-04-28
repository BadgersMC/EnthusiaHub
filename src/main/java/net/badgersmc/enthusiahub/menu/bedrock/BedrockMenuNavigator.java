package net.badgersmc.enthusiahub.menu.bedrock;

import net.badgersmc.enthusiahub.menu.Menu;
import net.badgersmc.enthusiahub.menu.MenuNavigator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Helper class providing convenience methods for common Bedrock menu navigation patterns.
 */
public class BedrockMenuNavigator {

    private final MenuNavigator navigator;

    public BedrockMenuNavigator(MenuNavigator navigator) {
        this.navigator = navigator;
    }

    /**
     * Creates a simple back button handler.
     *
     * @return Consumer that goes back to previous menu
     */
    public Runnable createBackHandler() {
        return () -> navigator.goBack();
    }

    /**
     * Creates a back button handler that passes data to the previous menu.
     *
     * @param data Data to pass to the previous menu
     * @return Consumer that goes back with data
     */
    public Runnable createBackWithDataHandler(Map<String, Object> data) {
        return () -> navigator.goBackWithData(data);
    }

    /**
     * Creates a navigation handler to a new menu.
     *
     * @param menu The menu to navigate to
     * @return Consumer that opens the new menu
     */
    public Runnable createNavigationHandler(Menu menu) {
        return () -> navigator.openMenu(menu);
    }

    /**
     * Creates a navigation handler to a new menu with data.
     *
     * @param menu The menu to navigate to
     * @param data Data to pass to the menu
     * @return Consumer that opens the new menu with data
     */
    public Runnable createNavigationHandler(Menu menu, Map<String, Object> data) {
        return () -> navigator.openMenu(menu, data);
    }

    /**
     * Creates a confirmation handler that executes an action and navigates.
     *
     * @param action         The action to execute
     * @param successMessage Message to send on success
     * @param shouldGoBack   Whether to go back after action
     * @return Consumer for the confirmation
     */
    public Consumer<Boolean> createConfirmationHandler(
            Runnable action,
            String successMessage,
            boolean shouldGoBack
    ) {
        return confirmed -> {
            if (confirmed) {
                action.run();
                if (successMessage != null && !successMessage.isEmpty()) {
                    navigator.getPlayer().sendMessage(successMessage);
                }
                if (shouldGoBack) {
                    navigator.goBack();
                }
            } else {
                navigator.goBack();
            }
        };
    }

    /**
     * Creates a refresh handler that replaces current menu with same menu type.
     *
     * @param menuSupplier Supplier for creating a fresh menu instance
     * @return Consumer that refreshes the menu
     */
    public Runnable createRefreshHandler(Menu menuSupplier) {
        return () -> navigator.replaceCurrentMenu(menuSupplier);
    }

    /**
     * Creates a cancel handler that closes all menus.
     *
     * @return Consumer that closes all menus
     */
    public Runnable createCancelHandler() {
        return () -> navigator.closeAll();
    }

    /**
     * Creates a cancel handler with a message.
     *
     * @param cancelMessage Message to send when cancelling
     * @return Consumer that sends message and closes all menus
     */
    public Runnable createCancelHandler(String cancelMessage) {
        return () -> {
            if (cancelMessage != null && !cancelMessage.isEmpty()) {
                navigator.getPlayer().sendMessage(cancelMessage);
            }
            navigator.closeAll();
        };
    }

    /**
     * Creates a workflow cancel handler for multi-step forms.
     *
     * @param cancelMessage Message to send
     * @param cleanup       Cleanup action to run before cancelling
     * @return Consumer that runs cleanup and cancels
     */
    public Runnable createCancelWorkflowHandler(String cancelMessage, Runnable cleanup) {
        return () -> {
            if (cleanup != null) {
                cleanup.run();
            }
            if (cancelMessage != null && !cancelMessage.isEmpty()) {
                navigator.getPlayer().sendMessage(cancelMessage);
            }
            navigator.closeAll();
        };
    }

    /**
     * Creates a recovery handler for timeout recovery.
     *
     * @param menu          The menu to reopen
     * @param recoveredData Data recovered from timeout
     * @return Consumer that reopens menu with recovered data
     */
    public Runnable createRecoveryHandler(Menu menu, Map<String, Object> recoveredData) {
        return () -> {
            navigator.getPlayer().sendMessage("§aRecovering your progress...");
            navigator.openMenu(menu, recoveredData);
        };
    }

    /**
     * Creates a step navigation handler for multi-step workflows.
     *
     * @param nextMenu  The next menu in the workflow
     * @param stepData  Data from current step
     * @param validator Optional validator that returns error message or null if valid
     * @return Consumer that validates and navigates to next step
     */
    public Consumer<Map<String, Object>> createStepNavigationHandler(
            Menu nextMenu,
            Map<String, Object> stepData,
            java.util.function.Function<Map<String, Object>, String> validator
    ) {
        return formData -> {
            // Validate if validator provided
            if (validator != null) {
                String error = validator.apply(formData);
                if (error != null) {
                    navigator.getPlayer().sendMessage("§c" + error);
                    return;
                }
            }

            // Combine step data and form data
            Map<String, Object> combinedData = new HashMap<>(stepData);
            combinedData.putAll(formData);

            // Navigate to next step
            navigator.openMenu(nextMenu, combinedData);
        };
    }

    /**
     * Simple method to go back to previous menu.
     */
    public void goBack() {
        navigator.goBack();
    }

    /**
     * Simple method to open a menu.
     *
     * @param menu The menu to open
     */
    public void openMenu(Menu menu) {
        navigator.openMenu(menu);
    }

    /**
     * Simple method to open a menu with data.
     *
     * @param menu The menu to open
     * @param data Data to pass
     */
    public void openMenu(Menu menu, Map<String, Object> data) {
        navigator.openMenu(menu, data);
    }

    /**
     * Simple method to close all menus.
     */
    public void closeAll() {
        navigator.closeAll();
    }

    /**
     * Gets the underlying MenuNavigator.
     *
     * @return The menu navigator
     */
    public MenuNavigator getNavigator() {
        return navigator;
    }
}
