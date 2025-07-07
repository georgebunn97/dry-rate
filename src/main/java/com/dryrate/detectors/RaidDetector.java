package com.dryrate.detectors;

import com.dryrate.RaidType;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.api.events.VarbitChanged;

/**
 * Base interface for raid detection implementations
 * Each raid type should have its own detector that implements this interface
 */
public interface RaidDetector
{
    /**
     * Callback interface for UI updates
     */
    interface UIUpdateCallback
    {
        void updateUI();
    }

    /**
     * Get the raid type this detector handles
     */
    RaidType getRaidType();

    /**
     * Get the region ID for this raid
     */
    int getRaidRegion();

    /**
     * Check if the detector is currently in an active raid state
     */
    boolean isInRaid();

    /**
     * Reset the detector state (called when leaving raids)
     */
    void reset();

    /**
     * Set the UI update callback
     */
    void setUIUpdateCallback(UIUpdateCallback callback);

    /**
     * Handle game object spawned events
     * @return true if the event was handled, false otherwise
     */
    boolean handleGameObjectSpawned(GameObjectSpawned event);

    /**
     * Handle wall object spawned events
     * @return true if the event was handled, false otherwise
     */
    default boolean handleWallObjectSpawned(WallObjectSpawned event)
    {
        return false; // Most raids don't need wall object detection
    }

    /**
     * Handle game tick events for ongoing state management
     */
    default void handleGameTick(GameTick event)
    {
        // Most raids don't need tick handling
    }

    /**
     * Handle varbit changed events
     * @return true if the event was handled, false otherwise
     */
    default boolean handleVarbitChanged(VarbitChanged event)
    {
        return false; // Most raids don't need varbit handling
    }

    /**
     * Update the raid state based on current region
     */
    void updateRaidState(int currentRegion);
} 