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
    public DryRateManager(DryRateConfig config, ConfigManager configManager, Gson gson)
    {
        this.config = config;
        this.configManager = configManager;
        this.raidData = new EnumMap<>(RaidType.class);
        this.gson = gson;
        
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
            if (dataJson != null && !dataJson.isEmpty())
            {
                Type type = new TypeToken<Map<RaidType, DryRateData>>(){}.getType();
                Map<RaidType, DryRateData> loadedData = gson.fromJson(dataJson, type);
                
                if (loadedData != null)
                {
                    raidData.putAll(loadedData);
                }
            }
            log.debug("Dry rate data loaded successfully");
        }
        catch (Exception e)
        {
            log.error("Error loading dry rate data", e);
        }
    }

    /**
     * Save data to configuration
     */
    public void saveData()
    {
        try
        {
            String dataJson = gson.toJson(raidData);
            configManager.setConfiguration(CONFIG_GROUP, DATA_KEY, dataJson);
            log.debug("Dry rate data saved successfully");
        }
        catch (Exception e)
        {
            log.error("Error saving dry rate data", e);
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