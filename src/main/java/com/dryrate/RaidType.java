package com.dryrate;

/**
 * Enum representing the different raid types we track
 */
public enum RaidType
{
    TOB("Theatre of Blood", "ToB"),
    TOA("Tombs of Amascut", "ToA"),
    COX("Chambers of Xeric", "CoX");

    private final String fullName;
    private final String shortName;

    RaidType(String fullName, String shortName)
    {
        this.fullName = fullName;
        this.shortName = shortName;
    }

    public String getFullName()
    {
        return fullName;
    }

    public String getShortName()
    {
        return shortName;
    }

    @Override
    public String toString()
    {
        return shortName;
    }
} 