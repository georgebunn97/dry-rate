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

    @Inject
    private DryRateConfig config;

    @Inject
    private ConfigManager configManager;

    private final Map<RaidType, DryRateData> raidData;
    private final Gson gson;

    public DryRateManager()
    {
        this.raidData = new EnumMap<>(RaidType.class);
        this.gson = new Gson();
        
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
            log.info("Dry rate data loaded successfully");
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
            log.info("Dry rate data saved successfully");
        }
        catch (Exception e)
        {
            log.error("Error saving dry rate data", e);
        }
    }

    /**
     * Handle a raid completion (increment dry streak if no unique received)
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
            data.incrementDryStreak();
            log.info("Incremented dry streak for {}: {}", raidType, data.getCurrentDryStreak());
            saveData();
        }
    }

    /**
     * Handle receiving a unique drop (reset dry streak)
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
            data.resetDryStreak();
            log.info("Reset dry streak for {} after {} completions", raidType, previousStreak);
            saveData();
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
            log.info("Manually reset dry streak for {}", raidType);
            saveData();
        }
    }

    /**
     * Reset all data for a specific raid type
     */
    public void resetAllData(RaidType raidType)
    {
        raidData.put(raidType, new DryRateData());
        log.info("Reset all data for {}", raidType);
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