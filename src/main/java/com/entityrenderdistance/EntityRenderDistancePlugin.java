package com.entityrenderdistance;

import com.entityrenderdistance.box.EntityRenderDistanceMinimapOverlay;
import com.entityrenderdistance.box.EntityRenderDistanceWorldOverlay;
import com.entityrenderdistance.fade.FadingPlayerManager;
import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
@PluginDescriptor(
		name = "Entity Render Distance",
		description = "Marks the entity render distance using a fog of war, a customisable border, and predictive fading player markers."
)
public class EntityRenderDistancePlugin extends Plugin {
	@Inject
	@SuppressWarnings("unused")
	private OverlayManager overlayManager;
	@Inject
	@SuppressWarnings("unused")
	private EntityRenderDistanceWorldOverlay worldOverlay;
	@Inject
	@SuppressWarnings("unused")
	private EntityRenderDistanceMinimapOverlay minimapOverlay;
	@Inject
	@SuppressWarnings("unused")
	private FadingPlayerManager fadingPlayerManager;
	@Override
	protected void startUp() {
		overlayManager.add(worldOverlay);
		overlayManager.add(minimapOverlay);
		fadingPlayerManager.start();
	}
	@Override
	protected void shutDown() {
		overlayManager.remove(worldOverlay);
		overlayManager.remove(minimapOverlay);
		fadingPlayerManager.stop();
	}
	@SuppressWarnings("unused")
	@Provides
	EntityRenderDistanceConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(EntityRenderDistanceConfig.class);
	}
}