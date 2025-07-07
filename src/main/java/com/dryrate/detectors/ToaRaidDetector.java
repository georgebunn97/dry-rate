package com.dryrate.detectors;

import com.dryrate.DryRateConfig;
import com.dryrate.DryRateManager;
import com.dryrate.RaidType;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameObjectSpawned;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tombs of Amascut raid detector
 * Handles TOA loot room object detection and raid completion tracking
 */
@Slf4j
@Singleton
public class ToaRaidDetector implements RaidDetector
{
    // TOA loot room region ID
    private static final int TOA_REGION = 14672; // Tombs of Amascut loot room
    
    // TOA object IDs (confirmed from actual raids)
    private static final int TOA_SARCOPHAGUS_PURPLE = 44826;    // Purple sarcophagus (indicates unique) - TBD
    private static final int TOA_SARCOPHAGUS_NON_PURPLE = 46220; // Non-purple sarcophagus (no unique) - CONFIRMED
    private static final int TOA_PLAYER_CHEST = 29994;          // Player's unopened chest - CONFIRMED
    private static final int TOA_VAULT_CHEST_OPEN = 44787;      // Opened vault chest (toa_vault_chest_open) - CONFIRMED
    
    // Complete list of ALL object IDs that can spawn
    private static final List<Integer> TOA_ALL_OBJECT_IDS = Arrays.asList(
        TOA_SARCOPHAGUS_PURPLE,      // 44826 - Purple sarcophagus (TBD)
        TOA_SARCOPHAGUS_NON_PURPLE,  // 46220 - Non-purple sarcophagus (CONFIRMED)
        TOA_PLAYER_CHEST,            // 29994 - Player's unopened chest (CONFIRMED)
        TOA_VAULT_CHEST_OPEN,        // 44787 - Opened vault chest (CONFIRMED)
        44788                        // 44788 - Additional vault chest variant (observed)
    );
    
    // Purple detection IDs for analysis
    private static final List<Integer> TOA_PURPLE_SARCOPHAGUS_IDS = Arrays.asList(
        TOA_SARCOPHAGUS_PURPLE
    );

    @Inject
    private Client client;

    @Inject
    private DryRateManager dryRateManager;

    @Inject
    private DryRateConfig config;

    // State tracking
    private boolean inRaid = false;
    private boolean chestsHandled = false;
    private final List<Integer> loadedObjects = new ArrayList<>();
    private final Set<Integer> seenObjectIds = new HashSet<>();
    
    // UI update callback
    private UIUpdateCallback uiUpdateCallback;

    @Override
    public RaidType getRaidType()
    {
        return RaidType.TOA;
    }

    @Override
    public int getRaidRegion()
    {
        return TOA_REGION;
    }

    @Override
    public boolean isInRaid()
    {
        return inRaid;
    }

    @Override
    public void setUIUpdateCallback(UIUpdateCallback callback)
    {
        this.uiUpdateCallback = callback;
    }

    @Override
    public void updateRaidState(int currentRegion)
    {
        boolean wasInRaid = inRaid;
        inRaid = (currentRegion == TOA_REGION);
        
        // Only log when actually entering/leaving TOA
        if (!wasInRaid && inRaid)
        {
            		log.debug("*** TOA *** ENTERING loot room - region {}", currentRegion);
		chestsHandled = false;
		loadedObjects.clear();
            seenObjectIds.clear();
        }
        else if (wasInRaid && !inRaid)
        {
            		log.debug("*** TOA *** LEAVING loot room - region {}", currentRegion);
		reset();
        }
    }
    
    @Override
    public boolean handleGameObjectSpawned(GameObjectSpawned event)
    {
        if (!inRaid)
        {
            return false;
        }
        
        int objectId = event.getGameObject().getId();
        
        // Check if objectId is in expected object list
        if (TOA_ALL_OBJECT_IDS.contains(objectId))
        {
            log.debug("*** TOA *** Found object: {}", objectId);
            
            // Use the object ID directly for processing
            handleObject(objectId);
            return true;
        }
        
        // Log unknown objects once only (reduced spam)
        if (!seenObjectIds.contains(objectId))
        {
            log.debug("TOA: Unknown object {} (expected: {})", objectId, TOA_ALL_OBJECT_IDS);
            seenObjectIds.add(objectId);
        }
        
        return false;
    }
    
    /**
     * Handle object spawning
     */
    private void handleObject(int objectId)
    {
        log.debug("*** TOA *** Found: {}", objectId);
        
        // Add to loaded objects
        if (!loadedObjects.contains(objectId))
        {
            loadedObjects.add(objectId);
        }
        
        // Process objects when we have a player chest (indicating raid completion)
        boolean hasPlayerChest = loadedObjects.contains(TOA_PLAYER_CHEST);
        
        if (hasPlayerChest && !chestsHandled)
        {
            processObjects();
        }
    }
    
    /**
     * Process objects and handle raid completion
     */
    private void processObjects()
    {
        if (chestsHandled)
        {
            log.debug("*** TOA *** Objects already processed, skipping");
            return;
        }
        
        chestsHandled = true;
        
        log.debug("*** TOA *** PROCESSING OBJECTS: {}", loadedObjects);
        
        // TOA-specific logic: check for purple sarcophagus, player chest, and opened vault
        boolean hasPurpleSarcophagus = loadedObjects.contains(TOA_SARCOPHAGUS_PURPLE);
        boolean hasPlayerChest = loadedObjects.contains(TOA_PLAYER_CHEST);
        boolean hasOpenedVault = loadedObjects.contains(TOA_VAULT_CHEST_OPEN);
        
        log.debug("*** TOA ANALYSIS *** Purple sarcophagus: {}, Player chest: {}, Opened vault: {}", 
            hasPurpleSarcophagus, hasPlayerChest, hasOpenedVault);
        
        // Determine if this is a purple and if it's the player's
        boolean isPurpleDropForPlayer = false;
        boolean isPurpleDropForTeammate = false;
        
        if (hasPurpleSarcophagus)
        {
            if (hasPlayerChest)
            {
                // Purple sarcophagus + player chest present = teammate's purple
                isPurpleDropForTeammate = true;
                log.debug("*** TOA ANALYSIS *** TEAMMATE'S PURPLE detected (purple sarc + player chest)");
            }
            else
            {
                // Purple sarcophagus + no player chest = player's purple
                isPurpleDropForPlayer = true;
                log.debug("*** TOA ANALYSIS *** PLAYER'S PURPLE detected (purple sarc + no player chest)");
            }
        }
        else
        {
            log.debug("*** TOA ANALYSIS *** NO PURPLE detected (no purple sarcophagus)");
        }
        
        // Handle completion and unique drops
        log.debug("*** TOA *** Recording raid completion");
        dryRateManager.handleRaidCompletion(RaidType.TOA);
        
        if (isPurpleDropForPlayer)
        {
            log.debug("*** TOA *** Recording PLAYER'S unique drop");
            dryRateManager.handleUniqueDropReceived(RaidType.TOA);
        }
        else if (isPurpleDropForTeammate)
        {
            // Handle teammate drops based on config
            if (config.teamDropResets())
            {
                log.debug("*** TOA *** Recording teammate's unique drop (team drop reset enabled)");
                dryRateManager.handleUniqueDropReceived(RaidType.TOA);
            }
            else
            {
                log.debug("*** TOA *** Teammate's unique drop ignored (team drop reset disabled)");
            }
        }
        
        // Trigger UI update
        if (uiUpdateCallback != null)
        {
            log.debug("*** TOA *** Triggering UI update");
            uiUpdateCallback.updateUI();
        }
        else
        {
            log.warn("*** TOA *** No UI update callback set!");
        }
    }

    @Override
    public void reset()
    {
        log.debug("*** TOA *** Detector reset");
        inRaid = false;
        chestsHandled = false;
        loadedObjects.clear();
        seenObjectIds.clear();
    }
} 