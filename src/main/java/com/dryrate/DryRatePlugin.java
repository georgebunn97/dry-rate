package com.dryrate;


import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.Client;
import net.runelite.api.GameState;

import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@PluginDescriptor(
	name = "Dry Rate Tracker",
	description = "Automatically track dry streaks and unique drops for OSRS raids (ToB, ToA, CoX)",
	tags = {"raids", "tracking", "statistics", "purple", "unique"}
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

	// Track previous region to detect raid completion
	private int previousRegionId = -1;
	private boolean inRaid = false;
	private RaidType currentRaidType = null;
	
	// Chest detection for raid completions and unique drops
	// Regular loot chests (no unique)
	private static final int TOB_LOOT_CHEST_REGULAR_TEAM = 32990;    // Teammate's regular chest
	private static final int TOB_LOOT_CHEST_REGULAR_PERSONAL = 32992; // Player's regular chest
	// ToA does NOT use these chest IDs - it uses sarcophagus + varbits instead
	// CoX does NOT use chest IDs - it uses light object + varbit instead
	
	// Purple loot chests (unique drops) - CORRECTED IDs
	private static final int TOB_LOOT_CHEST_PURPLE_TEAM = 32991;     // Teammate's purple chest
	private static final int TOB_LOOT_CHEST_PURPLE_PERSONAL = 32993; // Player's purple chest  
	// ToA does NOT use these chest IDs - it uses sarcophagus + varbits instead  
	// CoX does NOT use chest IDs - it uses light object + varbit instead
	
	// Chest tracking state
	private boolean chestsHandled = false;
	private List<Integer> loadedChests = new ArrayList<>();
	
	// ToA sarcophagus detection constants
	private static final int TOA_SARCOPHAGUS_ID = 46221; // Wall object ID for sarcophagus
	private static final int TOA_VARBIT_SARCOPHAGUS = 14373; // Varbit for purple detection
	private static final int[] TOA_VARBIT_CHEST_IDS = {14356, 14357, 14358, 14359, 14360, 14370, 14371, 14372};
	private static final int TOA_VARBIT_CHEST_KEY = 2;
	
	// CoX light detection constants (from CoX light colors plugin)
	private static final int COX_LIGHT_OBJECT_ID = 28848; // Light object spawned after raid completion
	private static final int COX_VARBIT_LIGHT_TYPE = 5456; // Varbit for loot type: 1=standard, 2=unique, 3=dust, 4=kit
	
	// Raid region IDs for detection  
	private static final int TOB_REGION = 12613; // Theatre of Blood - CORRECTED
	private static final int TOA_REGION = 14160; // Tombs of Amascut  
	private static final int COX_REGION = 12889; // Chambers of Xeric
	
	// Note: All chat message detection removed to prevent false positives.
	// Raid completions detected via chest spawning only.


	@Override
	protected void startUp() throws Exception
	{
		log.debug("Dry Rate Tracker started!");
		
		// Initialize the dry rate manager
		dryRateManager.loadData();
		
		// Create the panel
		panel = new DryRatePanel(dryRateManager);
		log.debug("Panel created successfully");
		
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
		log.debug("Navigation button added to toolbar");
		
		// Initialize region tracking
		if (client.getLocalPlayer() != null)
		{
			previousRegionId = getCurrentRegionId();
			updateRaidState();
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("Dry Rate Tracker stopped!");
		
		// Save data before shutting down
		dryRateManager.saveData();
		
		// Remove the panel
		clientToolbar.removeNavigation(navButton);
		
		// Reset tracking state
		inRaid = false;
		currentRaidType = null;
		previousRegionId = -1;
	}

	/**
	 * Primary detection method: Monitor for loot chest objects spawning
	 * Uses ToB QoL style logic to detect both completions and unique drops
	 */
	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		int objectId = event.getGameObject().getId();
		int regionId = getCurrentRegionId();
		
		// Debug: Log all object spawns while in raid regions for troubleshooting
		if (regionId == TOB_REGION)
		{
			log.debug("Object spawned in ToB region {}: objectId={}, position={}", 
				regionId, objectId, event.getGameObject().getWorldLocation());
		}
		else if (regionId == TOA_REGION)
		{
			log.debug("Object spawned in ToA region {}: objectId={}, position={}", 
				regionId, objectId, event.getGameObject().getWorldLocation());
		}
		else if (regionId == COX_REGION)
		{
			log.debug("Object spawned in CoX region {}: objectId={}, position={}", 
				regionId, objectId, event.getGameObject().getWorldLocation());
		}
		
		if (!inRaid || currentRaidType == null)
		{
			return;
		}
		
		// Check if this is a loot chest for the current raid type
		boolean isLootChest = false;
		switch (currentRaidType)
		{
			case TOB:
				isLootChest = objectId == TOB_LOOT_CHEST_REGULAR_TEAM || 
				              objectId == TOB_LOOT_CHEST_REGULAR_PERSONAL || 
				              objectId == TOB_LOOT_CHEST_PURPLE_TEAM || 
				              objectId == TOB_LOOT_CHEST_PURPLE_PERSONAL;
				break;
			case TOA:
				// ToA uses sarcophagus wall object + varbits, not chest objects
				// See onWallObjectSpawned for ToA detection
				isLootChest = false;
				break;
			case COX:
				// CoX uses light object + varbit, not chest objects
				isLootChest = false;
				break;
		}
		
		if (isLootChest)
		{
			log.debug("LOOT CHEST DETECTED for {}: objectId={}, position={}", 
				currentRaidType, objectId, event.getGameObject().getWorldLocation());
			handleChest(objectId);
		}
		
		// Special case: CoX light object detection
		if (currentRaidType == RaidType.COX && objectId == COX_LIGHT_OBJECT_ID)
		{
			log.info("*** COX LIGHT OBJECT SPAWNED *** objectId={}, position={}", 
				objectId, event.getGameObject().getWorldLocation());
			handleCoXLight();
		}
	}
	
	/**
	 * ToA detection: Monitor for sarcophagus wall objects (different from ToB/CoX chests)
	 */
	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned event)
	{
		if (!inRaid || currentRaidType != RaidType.TOA)
		{
			return;
		}
		
		int objectId = event.getWallObject().getId();
		
		// ToA sarcophagus detection
		if (objectId == TOA_SARCOPHAGUS_ID)
		{
			log.info("*** TOA SARCOPHAGUS SPAWNED *** objectId={}, position={}", 
				objectId, event.getWallObject().getWorldLocation());
			handleToASarcophagus();
		}
	}
	
	/**
	 * Handle ToA sarcophagus using varbit detection
	 */
	private void handleToASarcophagus()
	{
		if (chestsHandled)
		{
			log.debug("ToA sarcophagus already handled, ignoring");
			return;
		}
		
		// Check if sarcophagus is purple using varbit
		boolean isPurple = client.getVarbitValue(TOA_VARBIT_SARCOPHAGUS) % 2 != 0;
		
		// Check if it's personal (no team member has key)
		boolean isPersonal = true;
		for (int varbitId : TOA_VARBIT_CHEST_IDS)
		{
			if (client.getVarbitValue(varbitId) == TOA_VARBIT_CHEST_KEY)
			{
				isPersonal = false;
				break;
			}
		}
		
		log.info("*** TOA SARCOPHAGUS ANALYSIS *** isPurple={}, isPersonal={}", isPurple, isPersonal);
		
		// Always count as raid completion
		handleRaidCompletion(RaidType.TOA, "ToA sarcophagus detection");
		
		// Handle unique drops
		if (isPurple)
		{
			if (isPersonal)
			{
				log.info("*** TOA PERSONAL UNIQUE DROP *** detected");
				dryRateManager.handleUniqueDropReceived(RaidType.TOA);
			}
			else if (config.teamDropResets())
			{
				log.info("*** TOA TEAM UNIQUE DROP *** detected, team resets enabled: {}", 
					config.teamDropResets());
				dryRateManager.handleUniqueDropReceived(RaidType.TOA);
			}
			else
			{
				log.info("ToA team unique drop detected but team resets disabled");
			}
		}
		else
		{
			log.info("ToA regular sarcophagus (no unique) - dry streak will increment");
		}
		
		chestsHandled = true;
		
		// Update UI
		if (panel != null)
		{
			panel.updateDisplay();
		}
	}
	
	/**
	 * Handle CoX light object using varbit detection
	 */
	private void handleCoXLight()
	{
		if (chestsHandled)
		{
			log.debug("CoX light already handled, ignoring");
			return;
		}
		
		// Check light type using varbit
		int lightType = client.getVarbitValue(COX_VARBIT_LIGHT_TYPE);
		boolean isPurple = (lightType == 2); // 2 = unique drop
		
		log.info("*** COX LIGHT ANALYSIS *** lightType={}, isPurple={}", lightType, isPurple);
		
		// Always count as raid completion
		handleRaidCompletion(RaidType.COX, "CoX light detection");
		
		// Handle unique drops - CoX light doesn't distinguish personal vs team
		// We'll treat all unique drops as personal for now
		if (isPurple)
		{
			log.info("*** COX UNIQUE DROP *** detected");
			dryRateManager.handleUniqueDropReceived(RaidType.COX);
		}
		else
		{
			log.info("CoX regular light (no unique) - dry streak will increment");
		}
		
		chestsHandled = true;
		
		// Update UI
		if (panel != null)
		{
			panel.updateDisplay();
		}
	}
	
	/**
	 * Secondary detection method: Monitor region changes
	 */
	@Subscribe
	public void onGameTick(GameTick event)
	{
		int currentRegionId = getCurrentRegionId();
		
		if (currentRegionId != previousRegionId)
		{
			log.debug("Region changed from {} to {}", previousRegionId, currentRegionId);
			updateRaidState();
			previousRegionId = currentRegionId;
		}
	}
	
	/**
	 * Tertiary detection method: Game state detection via varbits (if available)
	 */
	@Subscribe  
	public void onVarbitChanged(VarbitChanged event)
	{
		// Monitor raid-specific varbits for completion
		// These varbit IDs would need to be researched for each raid
		// Example implementation:
		/*
		if (event.getVarbitId() == TOB_COMPLETION_VARBIT && event.getValue() > 0) 
		{
			handleRaidCompletion(RaidType.TOB, "Varbit completion detected");
		}
		*/
	}

	/**
	 * Handle chest spawning
	 */
	private void handleChest(int chestId)
	{
		log.debug("handleChest called: chestId={}, chestsHandled={}, currentRaidType={}", 
			chestId, chestsHandled, currentRaidType);
			
		if (chestsHandled)
		{
			log.debug("Chests already handled, ignoring chestId={}", chestId);
			return;
		}

		loadedChests.add(chestId);
		log.debug("Chest loaded: {}, total loaded: {}, loadedChests={}", 
			chestId, loadedChests.size(), loadedChests);

		// For now, process immediately when any chest is detected
		// TODO: Could enhance to wait for full party size
		processChests();
		chestsHandled = true;
		log.debug("Set chestsHandled=true after processing");
	}

	/**
	 * Process the loaded chests to determine completion and unique drops
	 */
	private void processChests()
	{
		log.debug("processChests called: currentRaidType={}, loadedChests={}", 
			currentRaidType, loadedChests);
			
		if (currentRaidType == null || loadedChests.isEmpty())
		{
			log.debug("Cannot process chests: currentRaidType={}, loadedChests.isEmpty()={}", 
				currentRaidType, loadedChests.isEmpty());
			return;
		}

		boolean isPurple = false;
		boolean isPersonal = false;

		// Check if any purple chests were loaded
		switch (currentRaidType)
		{
			case TOB:
				isPurple = loadedChests.contains(TOB_LOOT_CHEST_PURPLE_PERSONAL) || 
				           loadedChests.contains(TOB_LOOT_CHEST_PURPLE_TEAM);
				isPersonal = loadedChests.contains(TOB_LOOT_CHEST_PURPLE_PERSONAL);
				log.debug("ToB chest analysis: isPurple={}, isPersonal={}, personalId={}, teamId={}", 
					isPurple, isPersonal, TOB_LOOT_CHEST_PURPLE_PERSONAL, TOB_LOOT_CHEST_PURPLE_TEAM);
				break;
			case TOA:
				// ToA is handled separately via sarcophagus detection, not chest objects
				// This should not be reached since ToA doesn't add items to loadedChests
				log.warn("processChests called for ToA - this should not happen");
				return;
			case COX:
				// CoX is handled separately via light object detection, not chest objects
				// This should not be reached since CoX doesn't add items to loadedChests
				log.warn("processChests called for CoX - this should not happen");
				return;
		}

		log.debug("*** PROCESSING CHESTS *** {}: isPurple={}, isPersonal={}, teamResets={}", 
			currentRaidType, isPurple, isPersonal, config.teamDropResets());

		// Always count this as a raid completion
		handleRaidCompletion(currentRaidType, "Chest detection");

		// Handle unique drops
		if (isPurple)
		{
			if (isPersonal)
			{
				// Personal purple - always reset dry streak
				log.debug("*** PERSONAL UNIQUE DROP *** detected for {}", currentRaidType);
				dryRateManager.handleUniqueDropReceived(currentRaidType);
			}
			else if (config.teamDropResets())
			{
				// Team purple - only reset if config allows
				log.debug("*** TEAM UNIQUE DROP *** detected for {}, team resets enabled: {}", 
					currentRaidType, config.teamDropResets());
				dryRateManager.handleUniqueDropReceived(currentRaidType);
			}
			else
			{
				log.debug("Team unique drop detected for {} but team resets disabled", currentRaidType);
			}
		}
		else
		{
			log.debug("No purple chests detected - dry streak will increment");
		}
		
		// Update UI
		if (panel != null)
		{
			panel.updateDisplay();
		}
	}

	/**
	 * Reset chest tracking state (called when leaving raids)
	 */
	private void resetChestTracking()
	{
		chestsHandled = false;
		loadedChests.clear();
		log.debug("Reset chest tracking state");
	}

	/**
	 * Get current region ID
	 */
	private int getCurrentRegionId()
	{
		if (client.getLocalPlayer() == null)
		{
			return -1;
		}
		
		return client.getLocalPlayer().getWorldLocation().getRegionID();
	}
	
	/**
	 * Update raid state based on current region
	 */
	private void updateRaidState()
	{
		int regionId = getCurrentRegionId();
		
		// Log ALL region changes to help identify correct region IDs
		if (regionId != previousRegionId)
		{
			log.info("*** REGION CHANGE *** from {} to {} (TOB={}, TOA={}, COX={})", 
				previousRegionId, regionId, TOB_REGION, TOA_REGION, COX_REGION);
		}
		
		boolean wasInRaid = inRaid;
		RaidType previousRaidType = currentRaidType;
		
		// Determine if we're in a raid and which type
		if (regionId == TOB_REGION)
		{
			inRaid = true;
			currentRaidType = RaidType.TOB;
		}
		else if (regionId == TOA_REGION)
		{
			inRaid = true;
			currentRaidType = RaidType.TOA;
		}
		else if (regionId == COX_REGION)
		{
			inRaid = true;
			currentRaidType = RaidType.COX;
		}
		else
		{
			inRaid = false;
			currentRaidType = null;
		}
		
		// Enhanced logging for debugging
		log.debug("Region check: current={}, inRaid={}, raidType={}", 
			regionId, inRaid, currentRaidType);
		
		// Log raid state changes and reset chest tracking when leaving raids
		if (!wasInRaid && inRaid)
		{
			log.info("*** ENTERED {} RAID *** region={}, expected: TOB={}, TOA={}, COX={}", 
				currentRaidType, regionId, TOB_REGION, TOA_REGION, COX_REGION);
			resetChestTracking(); // Reset on entry to be safe
		}
		else if (wasInRaid && !inRaid)
		{
			log.info("*** LEFT {} RAID *** region={}", 
				previousRaidType, regionId);
			resetChestTracking(); // Reset when leaving raid
		}
		else if (inRaid && currentRaidType != previousRaidType)
		{
			log.info("*** CHANGED RAIDS *** from {} to {} region={}", 
				previousRaidType, currentRaidType, regionId);
			resetChestTracking(); // Reset when switching raid types
		}
		
		previousRegionId = regionId;
	}
	


	private void handleRaidCompletion(RaidType raidType, String message)
	{
		log.debug("Raid completion detected: {} - {}", raidType, message);
		
		// Track the completion
		dryRateManager.handleRaidCompletion(raidType);
		
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