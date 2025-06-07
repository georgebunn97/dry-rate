package com.dryrate;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("dryrate")
public interface DryRateConfig extends Config
{
	@ConfigItem(
		keyName = "showNotifications",
		name = "Show notifications",
		description = "Show notifications when you go dry or get a unique"
	)
	default boolean showNotifications()
	{
		return true;
	}

	@ConfigItem(
		keyName = "trackToB",
		name = "Track Theatre of Blood",
		description = "Track dry streaks for Theatre of Blood"
	)
	default boolean trackToB()
	{
		return true;
	}

	@ConfigItem(
		keyName = "trackToA",
		name = "Track Tombs of Amascut",
		description = "Track dry streaks for Tombs of Amascut"
	)
	default boolean trackToA()
	{
		return true;
	}

	@ConfigItem(
		keyName = "trackCoX",
		name = "Track Chambers of Xeric",
		description = "Track dry streaks for Chambers of Xeric"
	)
	default boolean trackCoX()
	{
		return true;
	}

	@ConfigItem(
		keyName = "resetConfirmation",
		name = "Confirm resets",
		description = "Ask for confirmation before resetting dry streak counters"
	)
	default boolean resetConfirmation()
	{
		return true;
	}
} 