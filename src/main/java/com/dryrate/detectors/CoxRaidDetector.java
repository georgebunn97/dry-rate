package com.dryrate.detectors;

import com.dryrate.DryRateManager;
import com.dryrate.RaidType;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Chambers of Xeric raid detector
 * Handles CoX-specific detection logic including light object detection
 */
@Slf4j
@Singleton
public class CoxRaidDetector implements RaidDetector
{
    // CoX region and constants
    private static final int COX_REGION = 12889; // Chambers of Xeric
    
    // CoX light detection constants
    private static final int COX_LIGHT_OBJECT_ID = 28848; // Light object spawned after raid completion
    private static final int COX_VARBIT_LIGHT_TYPE = 5456; // Varbit for loot type: 1=standard, 2=unique, 3=dust, 4=kit
    
    private final Client client;
    private final DryRateManager dryRateManager;
    
    // State tracking
    private boolean inRaid = false;
    private boolean chestsHandled = false;
    private boolean lightObjectDetected = false;
    
    // UI update callback
    private UIUpdateCallback uiUpdateCallback;
    
    @Inject
    public CoxRaidDetector(Client client, DryRateManager dryRateManager)
    {
        this.client = client;
        this.dryRateManager = dryRateManager;
    }
    
    @Override
    public RaidType getRaidType()
    {
        return RaidType.COX;
    }
    
    @Override
    public int getRaidRegion()
    {
        return COX_REGION;
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
        lightObjectDetected = false;
        log.debug("CoX detector reset");
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
        inRaid = (currentRegion == COX_REGION);
        
        if (!wasInRaid && inRaid)
        {
            log.debug("Entered CoX raid");
            chestsHandled = false;
            lightObjectDetected = false;
        }
        else if (wasInRaid && !inRaid)
        {
            log.debug("Left CoX raid");
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
        
        // Special case: CoX light object detection
        if (objectId == COX_LIGHT_OBJECT_ID)
        {
            log.debug("CoX light object detected: {}", objectId);
            lightObjectDetected = true;
            handleLight();
            return true;
        }
        
        return false;
    }
    
    /**
     * Handle game tick events - check light state if we detected the light object
     */
    public void handleGameTick(GameTick event)
    {
        if (!inRaid || !lightObjectDetected || chestsHandled)
        {
            return;
        }
        
        // Check if the light has become active
        int lightType = client.getVarbitValue(COX_VARBIT_LIGHT_TYPE);
        if (lightType > 0)
        {
            log.debug("*** COX LIGHT BECAME ACTIVE *** lightType={}", lightType);
            handleLight();
        }
    }
    
    /**
     * Handle CoX light object using varbit detection
     */
    private void handleLight()
    {
        if (chestsHandled)
        {
            log.debug("CoX light already handled, ignoring");
            return;
        }
        
        // Check light type using varbit
        int lightType = client.getVarbitValue(COX_VARBIT_LIGHT_TYPE);
        
        log.debug("*** COX LIGHT DETECTED *** lightType={}", lightType);
        
        // Only process if the light indicates actual completion (non-zero lightType)
        // lightType 0 = inactive/no completion, 1+ = actual completion states
        if (lightType == 0)
        {
            log.debug("*** COX LIGHT *** Not active yet (lightType=0), waiting for completion");
            return;
        }
        
        boolean isPurple = (lightType == 2); // 2 = unique drop
        
        log.debug("*** PROCESSING COX COMPLETION *** lightType={}, isPurple={}", lightType, isPurple);
        
        // Always count as raid completion
        dryRateManager.handleRaidCompletion(RaidType.COX);
        
        // Handle unique drops - CoX light doesn't distinguish personal vs team
        // We'll treat all unique drops as personal for now
        if (isPurple)
        {
            log.debug("*** COX UNIQUE DROP *** detected");
            dryRateManager.handleUniqueDropReceived(RaidType.COX);
        }
        else
        {
            log.debug("No CoX purple detected - dry streak will increment");
        }
        
        chestsHandled = true;
        
        // Update UI
        if (uiUpdateCallback != null)
        {
            uiUpdateCallback.updateUI();
        }
    }
} 