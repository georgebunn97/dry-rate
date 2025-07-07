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
 * Theatre of Blood raid detector
 * Handles TOB loot room chest detection and raid completion tracking
 */
@Slf4j
@Singleton
public class TobRaidDetector implements RaidDetector
{
    // TOB loot room region ID
    private static final int TOB_REGION = 12867; // Theatre of Blood loot room
    
    // TOB chest object IDs
    private static final int TOB_PLAYER_PURPLE = 32993;          // Player's purple chest
    private static final int TOB_PLAYER_CHEST_CLOSED = 32992;    // Player's closed chest  
    private static final int TOB_TEAMMATES_PURPLE = 32991;       // Teammates' purple chests
    private static final int TOB_TEAMMATES_CHEST_CLOSED = 32990; // Teammates' closed chests
    
    // Complete list of ALL chest IDs that can spawn
    private static final List<Integer> TOB_ALL_CHEST_IDS = Arrays.asList(
        // Regular chests that spawn in the room
        33086, 33087, 33088, 33089, 33090,
        // Purple and white chests
        TOB_PLAYER_CHEST_CLOSED,      // 32992 - Player's closed chest
        TOB_TEAMMATES_CHEST_CLOSED,   // 32990 - Teammates' closed chests
        TOB_PLAYER_PURPLE,            // 32993 - Player's purple
        TOB_TEAMMATES_PURPLE          // 32991 - Teammates' purple
    );
    
    // Purple chest IDs for detection
    private static final List<Integer> TOB_PURPLE_CHEST_IDS = Arrays.asList(
        TOB_PLAYER_PURPLE,            // 32993
        TOB_TEAMMATES_PURPLE          // 32991
    );
    
    private final Client client;
    private final DryRateManager dryRateManager;
    private final DryRateConfig config;
    
    // State tracking
    private boolean inRaid = false;
    private boolean chestsHandled = false;
    private List<Integer> loadedChests = new ArrayList<>();
    
    // Track seen object IDs to prevent log spam
    private Set<Integer> seenObjectIds = new HashSet<>();
    
    // UI update callback
    private UIUpdateCallback uiUpdateCallback;
    
    @Inject
    public TobRaidDetector(Client client, DryRateManager dryRateManager, DryRateConfig config)
    {
        this.client = client;
        this.dryRateManager = dryRateManager;
        this.config = config;
    }
    
    @Override
    public RaidType getRaidType()
    {
        return RaidType.TOB;
    }
    
    @Override
    public int getRaidRegion()
    {
        return TOB_REGION;
    }
    
    @Override
    public boolean isInRaid()
    {
        return inRaid;
    }
    
    @Override
    public void reset()
    {
        inRaid = false;
        chestsHandled = false;
        loadedChests.clear();
        seenObjectIds.clear();
        log.debug("TOB detector reset");
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
        inRaid = (currentRegion == TOB_REGION);
        
        // Only log when actually entering/leaving TOB
        if (!wasInRaid && inRaid)
        {
            			log.debug("*** TOB *** ENTERING loot room - region {}", currentRegion);
            chestsHandled = false;
            loadedChests.clear();
            seenObjectIds.clear();
        }
        else if (wasInRaid && !inRaid)
        {
            			log.debug("*** TOB *** LEAVING loot room - region {}", currentRegion);
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
        
        // Check if objectId is in expected chest list
        if (TOB_ALL_CHEST_IDS.contains(objectId))
        {
            		log.debug("*** TOB *** Found chest object: {}", objectId);
            
            // Use the object ID directly for processing
            handleChest(objectId);
            return true;
        }
        
        // Only log non-chest objects at debug level to reduce spam
        if (!seenObjectIds.contains(objectId))
        {
            log.debug("TOB: Object {} not a chest, ignoring", objectId);
            seenObjectIds.add(objectId);
        }
        
        return false;
    }
    
    /**
     * Handle chest spawning
     */
    private void handleChest(int chestId)
    {
        		log.debug("*** TOB *** Processing chest: {}", chestId);
        
        // Add to loaded chests
        if (!loadedChests.contains(chestId))
        {
            loadedChests.add(chestId);
            log.debug("TOB: Added chest {} to loadedChests: {}", chestId, loadedChests);
        }
        
        // Process chests when we have at least one
        if (!loadedChests.isEmpty() && !chestsHandled)
        {
            log.debug("TOB: Triggering chest processing");
            processChests();
        }
    }
    
    /**
     * Process chests and handle raid completion
     */
    private void processChests()
    {
        log.debug("TOB: Processing chests: {}", loadedChests);
        
        if (loadedChests.isEmpty())
        {
            log.warn("TOB: Cannot process chests - loadedChests is empty");
            return;
        }

        // Detect purple chests
        boolean isPurple = loadedChests.stream().anyMatch(TOB_PURPLE_CHEST_IDS::contains);
        
        // Detect if it's a personal purple (player's own purple)
        boolean isPersonal = loadedChests.contains(TOB_PLAYER_PURPLE);

        		log.debug("*** TOB *** Analysis - chests: {}, isPurple: {}, isPersonal: {}",
			loadedChests, isPurple, isPersonal);

        // Always count this as a raid completion
        		log.debug("*** TOB *** Recording raid completion");
        dryRateManager.handleRaidCompletion(RaidType.TOB);

        // Handle unique drops
        if (isPurple)
        {
            if (isPersonal)
            {
                // Personal purple - always reset dry streak
                			log.debug("*** TOB *** PERSONAL purple detected - resetting dry streak");
                dryRateManager.handleUniqueDropReceived(RaidType.TOB);
                processCompletion(true);
            }
            else
            {
                // Team purple - handle based on config
                				log.debug("*** TOB *** TEAM purple detected, teamDropResets: {}", config.teamDropResets());
                if (config.teamDropResets())
                {
                    					log.debug("*** TOB *** Team drops reset enabled - resetting dry streak");
                    dryRateManager.handleTeamUniqueDropReceived(RaidType.TOB);
                    processCompletion(true);
                }
                else
                {
                    					log.debug("*** TOB *** Team drops reset disabled - NOT resetting dry streak");
                    processCompletion(false);
                }
            }
        }
        else
        {
            			log.debug("*** TOB *** No purple detected - dry streak will increment");
            processCompletion(false);
        }
        
        // Always trigger UI update after processing
        processCompletion(isPurple);
    }
    
    /**
     * Process raid completion and trigger UI update
     */
    private void processCompletion(boolean hasUnique)
    {
        log.debug("TOB: Completion processing, hasUnique={}, uiCallback present: {}", 
            hasUnique, (uiUpdateCallback != null));
        
        // Set flag to prevent duplicate processing
        chestsHandled = true;
        
        // Trigger UI update callback
        if (uiUpdateCallback != null)
        {
            		log.debug("*** TOB *** Triggering UI update");
            uiUpdateCallback.updateUI();
        }
        else
        {
            log.warn("TOB: No UI update callback set!");
        }
    }
} 