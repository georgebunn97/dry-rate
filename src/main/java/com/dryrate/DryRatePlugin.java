package com.dryrate;

import com.dryrate.detectors.RaidDetector;
import com.dryrate.detectors.TobRaidDetector;
import com.dryrate.detectors.ToaRaidDetector;
import com.dryrate.detectors.CoxRaidDetector;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.coords.LocalPoint;

import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

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

	// Raid detectors
	@Inject
	private TobRaidDetector tobDetector;
	
	@Inject
	private ToaRaidDetector toaDetector;
	
	@Inject
	private CoxRaidDetector coxDetector;

	private DryRatePanel panel;
	private NavigationButton navButton;

	// Track current state
	private int previousRegionId = -1;
	private RaidDetector currentDetector = null;
	
	// Map regions to detectors for quick lookup
	private Map<Integer, RaidDetector> regionToDetector;

	@Override
	protected void startUp() throws Exception
	{
		log.debug("Dry Rate Tracker started!");
		
		// Initialize the dry rate manager
		dryRateManager.loadData();
		
		// Create the panel
		panel = new DryRatePanel(dryRateManager, config);
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
		
		// Initialize region-to-detector mapping
		initializeDetectorMapping();
		
		// Set up UI update callbacks for all detectors
		setupUICallbacks();
		
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
		
		// Reset all detectors
		if (regionToDetector != null)
		{
			for (RaidDetector detector : regionToDetector.values())
			{
				detector.reset();
			}
		}
		
		// Reset tracking state
		currentDetector = null;
		previousRegionId = -1;
	}

	/**
	 * Initialize the mapping between regions and their corresponding detectors
	 */
	private void initializeDetectorMapping()
	{
		regionToDetector = new HashMap<>();
		regionToDetector.put(tobDetector.getRaidRegion(), tobDetector);
		regionToDetector.put(toaDetector.getRaidRegion(), toaDetector);
		regionToDetector.put(coxDetector.getRaidRegion(), coxDetector);
		
				log.debug("*** DETECTOR MAPPING *** Initialized: ToB={}, ToA={}, CoX={}",
			tobDetector.getRaidRegion(), toaDetector.getRaidRegion(), coxDetector.getRaidRegion());
	}

	/**
	 * Set up UI update callbacks for all detectors
	 */
	private void setupUICallbacks()
	{
		RaidDetector.UIUpdateCallback updateCallback = () -> {
			log.debug("*** UI CALLBACK *** Received UI update request, calling panel.updateDisplay()");
			if (panel != null)
			{
				panel.updateDisplay();
				log.debug("*** UI CALLBACK *** panel.updateDisplay() completed successfully");
			}
			else
			{
				log.error("*** UI CALLBACK *** Panel is NULL - cannot update display!");
			}
		};
		
		tobDetector.setUIUpdateCallback(updateCallback);
		toaDetector.setUIUpdateCallback(updateCallback);
		coxDetector.setUIUpdateCallback(updateCallback);
		
		log.debug("*** UI SETUP *** UI update callbacks configured for all detectors");
		log.debug("*** UI SETUP *** ToB detector: region={}, class={}", 
			tobDetector.getRaidRegion(), tobDetector.getClass().getSimpleName());
		log.debug("*** UI SETUP *** ToA detector: region={}, class={}", 
			toaDetector.getRaidRegion(), toaDetector.getClass().getSimpleName());
		log.debug("*** UI SETUP *** CoX detector: region={}, class={}", 
			coxDetector.getRaidRegion(), coxDetector.getClass().getSimpleName());
	}

	/**
	 * Route game object spawned events to the appropriate detector
	 */
	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		// Ensure raid state is up to date before processing objects
		updateRaidState();
		
		// Additional safety check: ensure detector is set for current region
		int currentRegion = getCurrentRegionId();
		if (currentDetector == null && regionToDetector.containsKey(currentRegion))
		{
			currentDetector = regionToDetector.get(currentRegion);
			log.debug("*** SAFETY FIX *** Set detector for region {} to {}", 
				currentRegion, currentDetector.getRaidType());
		}
		
		int objectId = event.getGameObject().getId();
		
		// Only log important objects in raid regions
		if ((currentRegion == 12867 || currentRegion == 14672) && currentDetector != null)
		{
			// Only log if it's a potential chest/important object (reduce spam)
			if (objectId >= 33086 && objectId <= 33090 || // TOB chests
				objectId >= 44786 && objectId <= 44787 || // TOA chests  
				objectId == 44825 || objectId == 44826)   // TOA sarcophagi
			{
				log.debug("*** {} CHEST *** Object: {}", currentDetector.getRaidType(), objectId);
			}
		}
		
		// Route to current detector if available
		if (currentDetector != null)
		{
			currentDetector.handleGameObjectSpawned(event);
		}
		else if (currentRegion == 12867 || currentRegion == 14672 || currentRegion == 12889)
		{
			// Only log once when entering raid region without detector
			log.warn("*** ERROR *** No detector for raid region: {}", currentRegion);
		}
	}

	/**
	 * Route wall object spawned events to the appropriate detector
	 */
	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned event)
	{
		if (currentDetector != null)
		{
			currentDetector.handleWallObjectSpawned(event);
		}
	}

	/**
	 * Route game tick events to the appropriate detector and update raid state
	 */
	@Subscribe(priority = 7)  // HIGH PRIORITY 
	public void onGameTick(GameTick event)
	{
		updateRaidState();
		
		if (currentDetector != null)
		{
			currentDetector.handleGameTick(event);
		}
	}

	/**
	 * Route varbit changed events to the appropriate detector
	 */
	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (currentDetector != null)
		{
			currentDetector.handleVarbitChanged(event);
		}
	}

	/**
	 * Handle config changes by refreshing the panel
	 */
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("dryrate"))
		{
			log.debug("Config changed: {} = {}", event.getKey(), event.getNewValue());
			// Refresh the panel to reflect config changes
			if (panel != null)
			{
				panel.forceRefresh();
			}
		}
	}

	/**
	 * Handle game state changes to reset when logging out
	 */
	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			log.debug("Player logged out, resetting state");
			if (currentDetector != null)
			{
				currentDetector.reset();
			}
			currentDetector = null;
			previousRegionId = -1;
		}
	}

	/**
	 * Get the current region ID from the player's location
	 */
	private int getCurrentRegionId()
	{
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return -1;
		}

		LocalPoint localPoint = localPlayer.getLocalLocation();
		if (localPoint == null)
		{
			return -1;
		}

		WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPoint);
		if (worldPoint == null)
		{
			return -1;
		}

		int regionId = worldPoint.getRegionID();
		
		// Only log when entering raid regions (not every tick)
		if ((regionId == 12867 || regionId == 14672 || regionId == 12889) && 
			regionId != previousRegionId)
		{
			String raidName = (regionId == 12867) ? "TOB" : (regionId == 14672) ? "TOA" : "COX";
		log.debug("*** ENTERED {} REGION ***", raidName);
		}

		return regionId;
	}

	/**
	 * Update raid state based on current region and manage detector transitions
	 */
	private void updateRaidState()
	{
		int currentRegionId = getCurrentRegionId();
		
		if (currentRegionId == previousRegionId)
		{
			return; // No region change
		}
		
		// Check if we're entering a new raid region
		RaidDetector newDetector = regionToDetector.get(currentRegionId);
		
		// Handle detector transitions
		if (newDetector != currentDetector)
		{
			// Reset previous detector if we had one
			if (currentDetector != null)
			{
				log.debug("*** LEAVING {} ***", currentDetector.getRaidType());
				currentDetector.reset();
			}
			
			// Set new detector
			currentDetector = newDetector;
			
			if (currentDetector != null)
			{
				log.debug("*** ENTERING {} ***", currentDetector.getRaidType());
			}
		}
		
		// Update all detectors with current region
		for (RaidDetector detector : regionToDetector.values())
		{
			detector.updateRaidState(currentRegionId);
		}
		
		previousRegionId = currentRegionId;
	}

	@Provides
	DryRateConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DryRateConfig.class);
	}
} 
