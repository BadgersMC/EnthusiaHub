package dev.strafbefehl.deluxehubreloaded.menu;

import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages menu navigation for players with a stack-based history system.
 * Allows for back navigation and multi-step workflows.
 */
public class MenuNavigator {

    // Player UUID -> Menu stack
    private final Map<UUID, Deque<Menu>> menuStacks = new ConcurrentHashMap<>();

    private final Player player;

    public MenuNavigator(Player player) {
        this.player = player;
    }

    /**
     * Opens a new menu and adds it to the navigation stack.
     *
     * @param menu The menu to open
     */
    public void openMenu(Menu menu) {
        Deque<Menu> stack = menuStacks.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>());
        stack.push(menu);
        menu.open();
    }

    /**
     * Opens a new menu with data passed to it.
     *
     * @param menu The menu to open
     * @param data Data to pass to the menu
     */
    public void openMenu(Menu menu, Map<String, Object> data) {
        menu.passData(data);
        openMenu(menu);
    }

    /**
     * Goes back to the previous menu in the stack.
     * If there is no previous menu, closes all menus.
     */
    public void goBack() {
        Deque<Menu> stack = menuStacks.get(player.getUniqueId());
        if (stack == null || stack.isEmpty()) {
            return;
        }

        // Pop current menu
        Menu current = stack.poll();
        if (current != null) {
            current.onClose();
        }

        // Open previous menu if exists
        if (!stack.isEmpty()) {
            Menu previous = stack.peek();
            if (previous != null) {
                previous.open();
            }
        }
    }

    /**
     * Goes back to the previous menu and passes data to it.
     *
     * @param data Data to pass to the previous menu
     */
    public void goBackWithData(Map<String, Object> data) {
        Deque<Menu> stack = menuStacks.get(player.getUniqueId());
        if (stack == null || stack.isEmpty()) {
            return;
        }

        // Pop current menu
        Menu current = stack.poll();
        if (current != null) {
            current.onClose();
        }

        // Open previous menu with data if exists
        if (!stack.isEmpty()) {
            Menu previous = stack.peek();
            if (previous != null) {
                previous.passData(data);
                previous.open();
            }
        }
    }

    /**
     * Closes all menus and clears the navigation stack.
     */
    public void closeAll() {
        Deque<Menu> stack = menuStacks.get(player.getUniqueId());
        if (stack != null) {
            while (!stack.isEmpty()) {
                Menu menu = stack.poll();
                if (menu != null) {
                    menu.onClose();
                }
            }
        }
        menuStacks.remove(player.getUniqueId());
    }

    /**
     * Gets the current menu in the stack without removing it.
     *
     * @return The current menu, or null if stack is empty
     */
    public Menu getCurrentMenu() {
        Deque<Menu> stack = menuStacks.get(player.getUniqueId());
        return (stack != null && !stack.isEmpty()) ? stack.peek() : null;
    }

    /**
     * Checks if there is a previous menu to go back to.
     *
     * @return true if there is a previous menu, false otherwise
     */
    public boolean canGoBack() {
        Deque<Menu> stack = menuStacks.get(player.getUniqueId());
        return stack != null && stack.size() > 1;
    }

    /**
     * Gets the depth of the menu stack.
     *
     * @return The number of menus in the stack
     */
    public int getStackDepth() {
        Deque<Menu> stack = menuStacks.get(player.getUniqueId());
        return stack != null ? stack.size() : 0;
    }

    /**
     * Gets the player this navigator is for.
     *
     * @return The player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Clears the navigation stack for cleanup when player disconnects.
     */
    public void cleanup() {
        closeAll();
    }

    /**
     * Replaces the current menu with a new one without adding to stack.
     * Useful for refreshing the current menu.
     *
     * @param menu The menu to replace with
     */
    public void replaceCurrentMenu(Menu menu) {
        Deque<Menu> stack = menuStacks.get(player.getUniqueId());
        if (stack != null && !stack.isEmpty()) {
            Menu current = stack.poll();
            if (current != null) {
                current.onClose();
            }
        }

        if (stack == null) {
            stack = new ArrayDeque<>();
            menuStacks.put(player.getUniqueId(), stack);
        }

        stack.push(menu);
        menu.open();
    }
}
