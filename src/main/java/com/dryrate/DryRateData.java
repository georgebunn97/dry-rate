package com.dryrate;

import java.util.ArrayList;
import java.util.List;

/**
 * Data class to store dry streak information for a specific raid type
 */
public class DryRateData
{
    private int currentDryStreak;
    private int totalCompletions;
    private int totalUniques;
    private List<Integer> previousDryStreaks;
    private long lastDropTime;

    public DryRateData()
    {
        this.currentDryStreak = 0;
        this.totalCompletions = 0;
        this.totalUniques = 0;
        this.previousDryStreaks = new ArrayList<>();
        this.lastDropTime = 0;
    }

    // Getters
    public int getCurrentDryStreak()
    {
        return currentDryStreak;
    }

    public int getTotalCompletions()
    {
        return totalCompletions;
    }

    public int getTotalUniques()
    {
        return totalUniques;
    }

    public List<Integer> getPreviousDryStreaks()
    {
        return new ArrayList<>(previousDryStreaks);
    }

    public long getLastDropTime()
    {
        return lastDropTime;
    }

    // Setters
    public void setCurrentDryStreak(int currentDryStreak)
    {
        this.currentDryStreak = currentDryStreak;
    }

    public void setTotalCompletions(int totalCompletions)
    {
        this.totalCompletions = totalCompletions;
    }

    public void setTotalUniques(int totalUniques)
    {
        this.totalUniques = totalUniques;
    }

    public void setPreviousDryStreaks(List<Integer> previousDryStreaks)
    {
        this.previousDryStreaks = new ArrayList<>(previousDryStreaks);
    }

    public void setLastDropTime(long lastDropTime)
    {
        this.lastDropTime = lastDropTime;
    }

    // Helper methods for dry streak tracking
    public void incrementDryStreak()
    {
        this.currentDryStreak++;
        this.totalCompletions++;
    }

    public void resetDryStreak()
    {
        // Add to history if we had a streak > 0
        if (currentDryStreak > 0)
        {
            previousDryStreaks.add(currentDryStreak);
        }
        
        // Reset streak to 0 and increment uniques
        this.currentDryStreak = 0;
        this.totalUniques++;
        this.lastDropTime = System.currentTimeMillis();
    }

    public int getLongestDryStreak()
    {
        int longest = currentDryStreak;
        for (int streak : previousDryStreaks)
        {
            if (streak > longest)
            {
                longest = streak;
            }
        }
        return longest;
    }

    public double getAverageDryStreak()
    {
        if (previousDryStreaks.isEmpty())
        {
            return 0.0;
        }
        
        int total = 0;
        for (int streak : previousDryStreaks)
        {
            total += streak;
        }
        return (double) total / previousDryStreaks.size();
    }
} 