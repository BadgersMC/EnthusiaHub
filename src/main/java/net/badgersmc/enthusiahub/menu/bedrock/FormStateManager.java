package net.badgersmc.enthusiahub.menu.bedrock;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages form state persistence for multi-step workflows and timeout recovery.
 * Thread-safe with automatic expiration and cleanup.
 */
public class FormStateManager {

    /**
     * Holds state data with expiration time.
     */
    private static class StateEntry {
        final Map<String, Object> data;
        final long expirationTime;

        StateEntry(Map<String, Object> data, long expirationTime) {
            this.data = new HashMap<>(data);
            this.expirationTime = expirationTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }

    // Player UUID -> State key -> State entry
    private final Map<UUID, Map<String, StateEntry>> playerStates = new ConcurrentHashMap<>();

    // Default expiration time in minutes
    private final int defaultExpirationMinutes;

    public FormStateManager(int defaultExpirationMinutes) {
        this.defaultExpirationMinutes = defaultExpirationMinutes;
        startCleanupTask();
    }

    /**
     * Saves state for a player with default expiration.
     *
     * @param playerId The player's UUID
     * @param key      The state key
     * @param state    The state data to save
     */
    public void saveState(UUID playerId, String key, Map<String, Object> state) {
        saveState(playerId, key, state, defaultExpirationMinutes);
    }

    /**
     * Saves state for a player with custom expiration.
     *
     * @param playerId          The player's UUID
     * @param key               The state key
     * @param state             The state data to save
     * @param expirationMinutes Minutes until the state expires
     */
    public void saveState(UUID playerId, String key, Map<String, Object> state, int expirationMinutes) {
        if (state == null || state.isEmpty()) {
            return;
        }

        long expirationTime = System.currentTimeMillis() + (expirationMinutes * 60 * 1000L);
        StateEntry entry = new StateEntry(state, expirationTime);

        playerStates.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(key, entry);
    }

    /**
     * Restores state for a player if it exists and hasn't expired.
     *
     * @param playerId The player's UUID
     * @param key      The state key
     * @return The state data, or null if not found or expired
     */
    public Map<String, Object> restoreState(UUID playerId, String key) {
        Map<String, StateEntry> states = playerStates.get(playerId);
        if (states == null) {
            return null;
        }

        StateEntry entry = states.get(key);
        if (entry == null) {
            return null;
        }

        if (entry.isExpired()) {
            states.remove(key);
            return null;
        }

        return new HashMap<>(entry.data);
    }

    /**
     * Restores state with a fallback value if not found or expired.
     *
     * @param playerId The player's UUID
     * @param key      The state key
     * @param fallback Fallback value if state not found
     * @return The state data or fallback
     */
    public Map<String, Object> restoreStateWithFallback(UUID playerId, String key, Map<String, Object> fallback) {
        Map<String, Object> state = restoreState(playerId, key);
        return state != null ? state : fallback;
    }

    /**
     * Updates existing state by merging new data.
     *
     * @param playerId The player's UUID
     * @param key      The state key
     * @param newData  New data to merge
     */
    public void updateState(UUID playerId, String key, Map<String, Object> newData) {
        Map<String, Object> existing = restoreState(playerId, key);
        if (existing == null) {
            existing = new HashMap<>();
        }
        existing.putAll(newData);
        saveState(playerId, key, existing);
    }

    /**
     * Clears a specific state for a player.
     *
     * @param playerId The player's UUID
     * @param key      The state key to clear
     */
    public void clearState(UUID playerId, String key) {
        Map<String, StateEntry> states = playerStates.get(playerId);
        if (states != null) {
            states.remove(key);
        }
    }

    /**
     * Gets all state keys for a player.
     *
     * @param playerId The player's UUID
     * @return List of state keys
     */
    public List<String> getPlayerStateKeys(UUID playerId) {
        Map<String, StateEntry> states = playerStates.get(playerId);
        return states != null ? new ArrayList<>(states.keySet()) : new ArrayList<>();
    }

    /**
     * Clears all states for a player.
     *
     * @param playerId The player's UUID
     */
    public void clearPlayerStates(UUID playerId) {
        playerStates.remove(playerId);
    }

    /**
     * Gets the number of active states across all players.
     *
     * @return The total number of active states
     */
    public int getActiveStateCount() {
        return playerStates.values().stream()
                .mapToInt(Map::size)
                .sum();
    }

    /**
     * Starts a background cleanup task to remove expired states.
     */
    private void startCleanupTask() {
        Timer cleanupTimer = new Timer("FormStateCleanup", true);
        cleanupTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                cleanupExpiredStates();
            }
        }, 10 * 60 * 1000L, 10 * 60 * 1000L); // Every 10 minutes
    }

    /**
     * Removes all expired states.
     */
    private void cleanupExpiredStates() {
        playerStates.forEach((playerId, states) -> {
            states.entrySet().removeIf(entry -> entry.getValue().isExpired());
        });

        // Remove empty player entries
        playerStates.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /**
     * Checks if a player has any saved state.
     *
     * @param playerId The player's UUID
     * @return true if the player has saved state, false otherwise
     */
    public boolean hasState(UUID playerId) {
        Map<String, StateEntry> states = playerStates.get(playerId);
        return states != null && !states.isEmpty();
    }

    /**
     * Checks if a specific state key exists for a player.
     *
     * @param playerId The player's UUID
     * @param key      The state key
     * @return true if the state exists and hasn't expired, false otherwise
     */
    public boolean hasState(UUID playerId, String key) {
        Map<String, StateEntry> states = playerStates.get(playerId);
        if (states == null) {
            return false;
        }

        StateEntry entry = states.get(key);
        if (entry == null) {
            return false;
        }

        if (entry.isExpired()) {
            states.remove(key);
            return false;
        }

        return true;
    }
}
