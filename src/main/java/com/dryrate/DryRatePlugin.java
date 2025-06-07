package com.dryrate;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(
	name = "Dry Rate Tracker",
	description = "Track dry streaks for OSRS raids (ToB, ToA, CoX)",
	tags = {"raids", "tracking", "statistics"}
)
public class DryRatePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private DryRateConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private DryRateManager dryRateManager;

	private DryRatePanel panel;
	private NavigationButton navButton;

	// Raid completion patterns - more flexible to handle variations
	private static final Pattern TOB_COMPLETION = Pattern.compile(".*[Tt]heatre of [Bb]lood.*count.*?(\\d+).*");
	private static final Pattern TOA_COMPLETION = Pattern.compile(".*[Tt]ombs of [Aa]mascut.*count.*?(\\d+).*");
	private static final Pattern COX_COMPLETION = Pattern.compile(".*[Cc]hambers of [Xx]eric.*count.*?(\\d+).*");

	// Unique drop patterns for raids - case insensitive and more flexible
	private static final Pattern TOB_UNIQUE = Pattern.compile("(?i).*(scythe of vitur|ghrazi rapier|sanguinesti staff|justiciar faceguard|justiciar chestguard|justiciar legguards|avernic defender hilt).*");
	private static final Pattern TOA_UNIQUE = Pattern.compile("(?i).*(tumeken's shadow|elidinis' ward|masori mask|masori body|masori chaps|lightbearer|osmumten's fang).*");
	private static final Pattern COX_UNIQUE = Pattern.compile("(?i).*(twisted bow|elder maul|kodai insignia|dragon hunter crossbow|dinhs bulwark|ancestral hat|ancestral robe top|ancestral robe bottom|dragon claws|twisted buckler).*");

	@Override
	protected void startUp() throws Exception
	{
		log.info("Dry Rate Tracker started!");
		
		// Initialize the dry rate manager
		dryRateManager.loadData();
		
		// Create the panel
		panel = new DryRatePanel(dryRateManager);
		
		// Load icon for the sidebar
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
		if (icon == null)
		{
			// Fallback if icon doesn't exist
			icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		}
		
		navButton = NavigationButton.builder()
			.tooltip("Dry Rate Tracker")
			.icon(icon)
			.priority(5)
			.panel(panel)
			.build();
		
		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Dry Rate Tracker stopped!");
		
		// Save data before shutting down
		dryRateManager.saveData();
		
		// Remove the panel
		clientToolbar.removeNavigation(navButton);
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage)
	{
		if (chatMessage.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String message = chatMessage.getMessage();
		
		// Strip HTML tags and clean the message
		String cleanMessage = message.replaceAll("<[^>]*>", "").trim();
		
		// Check for raid completions first
		boolean raidCompleted = false;
		if (TOB_COMPLETION.matcher(cleanMessage).matches())
		{
			handleRaidCompletion(RaidType.TOB, cleanMessage);
			raidCompleted = true;
		}
		else if (TOA_COMPLETION.matcher(cleanMessage).matches())
		{
			handleRaidCompletion(RaidType.TOA, cleanMessage);
			raidCompleted = true;
		}
		else if (COX_COMPLETION.matcher(cleanMessage).matches())
		{
			handleRaidCompletion(RaidType.COX, cleanMessage);
			raidCompleted = true;
		}
		
		// Check for unique drops - these should reset the dry streak
		if (TOB_UNIQUE.matcher(cleanMessage).matches())
		{
			handleUniqueDropReceived(RaidType.TOB, cleanMessage);
		}
		else if (TOA_UNIQUE.matcher(cleanMessage).matches())
		{
			handleUniqueDropReceived(RaidType.TOA, cleanMessage);
		}
		else if (COX_UNIQUE.matcher(cleanMessage).matches())
		{
			handleUniqueDropReceived(RaidType.COX, cleanMessage);
		}
	}

	private void handleRaidCompletion(RaidType raidType, String message)
	{
		log.info("Raid completion detected: {} - {}", raidType, message);
		
		// Check if this is a dry completion (no unique received)
		// We'll track this after a short delay to see if a unique drop message follows
		dryRateManager.handleRaidCompletion(raidType);
		
		// Update the panel
		if (panel != null)
		{
			panel.updateDisplay();
		}
	}

	private void handleUniqueDropReceived(RaidType raidType, String message)
	{
		log.info("Unique drop detected: {} - {}", raidType, message);
		
		// Reset the dry streak for this raid type
		dryRateManager.handleUniqueDropReceived(raidType);
		
		// Update the panel
		if (panel != null)
		{
			panel.updateDisplay();
		}
	}

	@Provides
	DryRateConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DryRateConfig.class);
	}
} 