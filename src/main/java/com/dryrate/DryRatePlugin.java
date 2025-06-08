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

	// Raid completion patterns - very specific to avoid false positives
	private static final Pattern TOB_COMPLETION = Pattern.compile("^Your completed Theatre of Blood count is: (\\d+)\\.$");
	private static final Pattern TOA_COMPLETION = Pattern.compile("^Your completed Tombs of Amascut count is: (\\d+)\\.$");
	private static final Pattern COX_COMPLETION = Pattern.compile("^Your completed Chambers of Xeric count is: (\\d+)\\.$");

	// Personal unique drop patterns (when you get the drop)
	private static final Pattern TOB_UNIQUE_PERSONAL = Pattern.compile("(?i).*you.*received.*(scythe of vitur|ghrazi rapier|sanguinesti staff|justiciar faceguard|justiciar chestguard|justiciar legguards|avernic defender hilt|sanguine ornament kit|holy ornament kit|sanguine dust).*");
	private static final Pattern TOA_UNIQUE_PERSONAL = Pattern.compile("(?i).*you.*received.*(tumeken's shadow|elidinis' ward|masori mask|masori body|masori chaps|lightbearer|osmumten's fang).*");
	private static final Pattern COX_UNIQUE_PERSONAL = Pattern.compile("(?i).*you.*received.*(twisted bow|elder maul|kodai insignia|dragon hunter crossbow|dinhs bulwark|ancestral hat|ancestral robe top|ancestral robe bottom|dragon claws|twisted buckler|twisted ancestral colour kit|metamorphic dust).*");

	// Team unique drop patterns (when anyone gets the drop)
	private static final Pattern TOB_UNIQUE_TEAM = Pattern.compile("(?i).*(scythe of vitur|ghrazi rapier|sanguinesti staff|justiciar faceguard|justiciar chestguard|justiciar legguards|avernic defender hilt|sanguine ornament kit|holy ornament kit|sanguine dust).*");
	private static final Pattern TOA_UNIQUE_TEAM = Pattern.compile("(?i).*(tumeken's shadow|elidinis' ward|masori mask|masori body|masori chaps|lightbearer|osmumten's fang).*");
	private static final Pattern COX_UNIQUE_TEAM = Pattern.compile("(?i).*(twisted bow|elder maul|kodai insignia|dragon hunter crossbow|dinhs bulwark|ancestral hat|ancestral robe top|ancestral robe bottom|dragon claws|twisted buckler|twisted ancestral colour kit|metamorphic dust).*");

	@Override
	protected void startUp() throws Exception
	{
		log.info("Dry Rate Tracker started!");
		
		// Initialize the dry rate manager
		dryRateManager.loadData();
		
		// Create the panel
		panel = new DryRatePanel(dryRateManager);
		log.info("Panel created successfully");
		
		// Create a towel icon (perfect for "dry" tracker!)
		BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics2D g2d = icon.createGraphics();
		g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
		
		// Draw towel main body (light blue/white towel)
		g2d.setColor(new java.awt.Color(200, 220, 255)); // Light blue towel
		g2d.fillRect(3, 2, 10, 12);
		
		// Add towel texture lines (horizontal stripes)
		g2d.setColor(new java.awt.Color(180, 200, 240)); // Slightly darker blue
		g2d.drawLine(4, 4, 11, 4);   // Top stripe
		g2d.drawLine(4, 6, 11, 6);   // Second stripe
		g2d.drawLine(4, 8, 11, 8);   // Third stripe
		g2d.drawLine(4, 10, 11, 10); // Fourth stripe
		g2d.drawLine(4, 12, 11, 12); // Bottom stripe
		
		// Add towel edges/border for definition
		g2d.setColor(new java.awt.Color(150, 170, 200)); // Darker border
		g2d.drawRect(3, 2, 10, 12);
		
		// Add a small "hanging" effect at the top
		g2d.setColor(new java.awt.Color(100, 100, 100)); // Dark gray hook/hanger
		g2d.fillRect(7, 0, 2, 3);
		
		g2d.dispose();
		
		navButton = NavigationButton.builder()
			.tooltip("Dry Rate Tracker")
			.icon(icon)
			.priority(5)
			.panel(panel)
			.build();
		
		clientToolbar.addNavigation(navButton);
		log.info("Navigation button added to toolbar");
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
		// First check for personal drops (always reset)
		if (TOB_UNIQUE_PERSONAL.matcher(cleanMessage).matches())
		{
			handleUniqueDropReceived(RaidType.TOB, cleanMessage, true);
		}
		else if (TOA_UNIQUE_PERSONAL.matcher(cleanMessage).matches())
		{
			handleUniqueDropReceived(RaidType.TOA, cleanMessage, true);
		}
		else if (COX_UNIQUE_PERSONAL.matcher(cleanMessage).matches())
		{
			handleUniqueDropReceived(RaidType.COX, cleanMessage, true);
		}
		// Then check for team drops (only reset if config enabled)
		else if (config.teamDropResets())
		{
			if (TOB_UNIQUE_TEAM.matcher(cleanMessage).matches())
			{
				handleUniqueDropReceived(RaidType.TOB, cleanMessage, false);
			}
			else if (TOA_UNIQUE_TEAM.matcher(cleanMessage).matches())
			{
				handleUniqueDropReceived(RaidType.TOA, cleanMessage, false);
			}
			else if (COX_UNIQUE_TEAM.matcher(cleanMessage).matches())
			{
				handleUniqueDropReceived(RaidType.COX, cleanMessage, false);
			}
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

	private void handleUniqueDropReceived(RaidType raidType, String message, boolean isPersonalDrop)
	{
		if (isPersonalDrop)
		{
			log.info("Personal unique drop detected: {} - {}", raidType, message);
		}
		else
		{
			log.info("Team unique drop detected: {} - {}", raidType, message);
		}
		
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