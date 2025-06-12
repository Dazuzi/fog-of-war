package com.entityrenderdistance;

import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;

@PluginDescriptor(
		name = "Entity Render Distance",
		description = "Draws boxes around the player to mark the maximum entity render distance",
		enabledByDefault = true
)
public class EntityRenderDistancePlugin extends Plugin
{

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private EntityRenderDistanceOverlay overlay;

	@Inject
	private EntityRenderDistanceMinimapOverlay minimapOverlay;

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay); // world overlay
		overlayManager.add(minimapOverlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(minimapOverlay);
	}

	@Provides
	EntityRenderDistanceConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(EntityRenderDistanceConfig.class);
	}
}
