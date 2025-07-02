package com.dryrate;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.Map;

/**
 * Manager class that handles all dry rate tracking logic
 */
@Slf4j
@Singleton
public class DryRateManager
{
    private static final String CONFIG_GROUP = "dryrate";
    private static final String DATA_KEY = "data";

    private final DryRateConfig config;
    private final ConfigManager configManager;
    private final Map<RaidType, DryRateData> raidData;
    private final Gson gson;

    @Inject
    public DryRateManager(DryRateConfig config, ConfigManager configManager)
    {
        this.config = config;
        this.configManager = configManager;
        this.raidData = new EnumMap<>(RaidType.class);
        this.gson = new Gson(); // Create our own Gson instance
        
        // Initialize data for each raid type
        for (RaidType raidType : RaidType.values())
        {
            raidData.put(raidType, new DryRateData());
        }
    }

    /**
     * Load data from configuration
     */
    public void loadData()
    {
        try
        {
            String dataJson = configManager.getConfiguration(CONFIG_GROUP, DATA_KEY);
            log.debug("*** LOADING DATA *** Raw JSON from config: {}", dataJson);
            
            if (dataJson != null && !dataJson.isEmpty())
            {
                // Use String keys to avoid enum serialization issues
                Type type = new TypeToken<Map<String, DryRateData>>(){}.getType();
                Map<String, DryRateData> loadedData = gson.fromJson(dataJson, type);
                
                if (loadedData != null)
                {
                    log.info("Dry rate data loaded successfully - {} raid types", loadedData.size());
                    
                    // Convert string keys back to enum keys
                    for (Map.Entry<String, DryRateData> entry : loadedData.entrySet())
                    {
                        try
                        {
                            RaidType raidType = RaidType.valueOf(entry.getKey());
                            DryRateData data = entry.getValue();
                            raidData.put(raidType, data);
                            
                            log.debug("*** {} DATA *** Streak: {}, Completions: {}, Uniques: {}", 
                                raidType, data.getCurrentDryStreak(), data.getTotalCompletions(), data.getTotalUniques());
                        }
                        catch (IllegalArgumentException e)
                        {
                            log.warn("*** LOAD WARNING *** Unknown raid type: {}", entry.getKey());
                        }
                    }
                }
                else
                {
                    log.warn("*** LOAD WARNING *** Parsed data was null");
                }
            }
            else
            {
                log.info("No existing dry rate data found, starting fresh");
            }
            log.debug("*** LOAD COMPLETE *** Current raid data state:");
            for (Map.Entry<RaidType, DryRateData> entry : raidData.entrySet())
            {
                DryRateData data = entry.getValue();
                log.debug("*** {} CURRENT *** Streak: {}, Completions: {}, Uniques: {}", 
                    entry.getKey(), data.getCurrentDryStreak(), data.getTotalCompletions(), data.getTotalUniques());
            }
        }
        catch (Exception e)
        {
            log.error("*** LOAD ERROR *** Error loading dry rate data", e);
        }
    }

    /**
     * Save data to configuration
     */
    public void saveData()
    {
        try
        {
            log.debug("*** SAVING DATA *** Current state before save:");
            for (Map.Entry<RaidType, DryRateData> entry : raidData.entrySet())
            {
                DryRateData data = entry.getValue();
                log.debug("*** {} SAVE *** Streak: {}, Completions: {}, Uniques: {}", 
                    entry.getKey(), data.getCurrentDryStreak(), data.getTotalCompletions(), data.getTotalUniques());
            }
            
            // Convert enum keys to strings to avoid serialization issues
            Map<String, DryRateData> stringKeyMap = new java.util.HashMap<>();
            for (Map.Entry<RaidType, DryRateData> entry : raidData.entrySet())
            {
                stringKeyMap.put(entry.getKey().name(), entry.getValue());
            }
            
            String dataJson = gson.toJson(stringKeyMap);
            log.debug("*** SAVING DATA *** JSON to save: {}", dataJson);
            
            configManager.setConfiguration(CONFIG_GROUP, DATA_KEY, dataJson);
            log.debug("*** SAVE COMPLETE *** Data saved successfully to config group: {}, key: {}", CONFIG_GROUP, DATA_KEY);
        }
        catch (Exception e)
        {
            log.error("*** SAVE ERROR *** Error saving dry rate data", e);
        }
    }

    /**
     * Handle a raid completion (increment dry streak and total count)
     */
    public void handleRaidCompletion(RaidType raidType)
    {
        if (!isRaidTrackingEnabled(raidType))
        {
            return;
        }

        DryRateData data = raidData.get(raidType);
        if (data != null)
        {
            // Increment dry streak directly on each completion
            data.incrementDryStreak();
            log.debug("Raid completion for {}: Dry streak now {}, total completions {}", 
                raidType, data.getCurrentDryStreak(), data.getTotalCompletions());
            saveData();
        }
    }

    /**
     * Handle receiving a unique drop (reset dry streak and increment unique count)
     * This is typically called manually when the user confirms they received a unique drop
     */
    public void handleUniqueDropReceived(RaidType raidType)
    {
        if (!isRaidTrackingEnabled(raidType))
        {
            return;
        }

        DryRateData data = raidData.get(raidType);
        if (data != null)
        {
            int previousStreak = data.getCurrentDryStreak();
            
            // Reset dry streak (this handles history and unique count)
            data.resetDryStreak();
            
            log.debug("Unique drop for {}: Reset streak from {}, total uniques now {}", 
                raidType, previousStreak, data.getTotalUniques());
            saveData();
        }
    }

    /**
     * Handle team member receiving unique drop (only reset if config enabled)
     */
    public void handleTeamUniqueDropReceived(RaidType raidType)
    {
        if (!isRaidTrackingEnabled(raidType))
        {
            return;
        }

        // Only reset dry streak if team drops are configured to reset personal streak
        if (config != null && config.teamDropResets())
        {
            DryRateData data = raidData.get(raidType);
            if (data != null)
            {
                int previousStreak = data.getCurrentDryStreak();
                
                // Reset dry streak but don't increment personal unique count
                if (previousStreak > 0)
                {
                    data.getPreviousDryStreaks().add(previousStreak);
                }
                data.setCurrentDryStreak(0);
                
                log.debug("Team unique drop for {}: Reset streak from {} (team drops reset enabled)", 
                    raidType, previousStreak);
                saveData();
            }
        }
        else
        {
            log.debug("Team unique drop for {} ignored (team drops reset disabled)", raidType);
        }
    }

    /**
     * Get dry rate data for a specific raid type
     */
    public DryRateData getRaidData(RaidType raidType)
    {
        return raidData.get(raidType);
    }

    /**
     * Get all raid data
     */
    public Map<RaidType, DryRateData> getAllRaidData()
    {
        return new EnumMap<>(raidData);
    }

    /**
     * Reset dry streak for a specific raid type
     */
    public void resetDryStreak(RaidType raidType)
    {
        DryRateData data = raidData.get(raidType);
        if (data != null)
        {
            data.setCurrentDryStreak(0);
            log.debug("Manually reset dry streak for {}", raidType);
            saveData();
        }
    }

    /**
     * Reset all data for a specific raid type
     */
    public void resetAllData(RaidType raidType)
    {
        raidData.put(raidType, new DryRateData());
        log.debug("Reset all data for {}", raidType);
        saveData();
    }

    /**
     * Test method to manually test save/load functionality
     */
    public void testSaveLoad()
    {
        log.info("*** TESTING SAVE/LOAD *** Starting test");
        
        // Set some test data
        DryRateData testData = raidData.get(RaidType.TOB);
        testData.incrementDryStreak();
        testData.incrementDryStreak();
        testData.incrementDryStreak();
        
        log.info("*** TEST *** Set ToB dry streak to: {}", testData.getCurrentDryStreak());
        
        // Save the data
        saveData();
        
        // Clear the data
        raidData.put(RaidType.TOB, new DryRateData());
        log.info("*** TEST *** Cleared ToB data, streak now: {}", raidData.get(RaidType.TOB).getCurrentDryStreak());
        
        // Load the data back
        loadData();
        
        log.info("*** TEST *** After reload, ToB streak is: {}", raidData.get(RaidType.TOB).getCurrentDryStreak());
        log.info("*** TESTING SAVE/LOAD *** Test complete");
    }

    /**
     * Check if tracking is enabled for a specific raid type
     */
    private boolean isRaidTrackingEnabled(RaidType raidType)
    {
        if (config == null)
        {
            return true; // Default to enabled if config is not available
        }

        switch (raidType)
        {
            case TOB:
                return config.trackToB();
            case TOA:
                return config.trackToA();
            case COX:
                return config.trackCoX();
            default:
                return true;
        }
    }
} 