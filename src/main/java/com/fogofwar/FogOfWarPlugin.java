package com.fogofwar;

import com.fogofwar.box.FogOfWarMinimapOverlay;
import com.fogofwar.box.FogOfWarWorldOverlay;
import com.fogofwar.fade.FadingPlayerManager;
import com.fogofwar.util.DynamicRenderDistance;
import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
@PluginDescriptor(
		name = "Fog of War",
		description = "Applies a fog of war effect outside of the entity render distance, in both the world and on the minimap.",
		configName = "entityrenderdistance"
)
public class FogOfWarPlugin extends Plugin {
	@Inject
	@SuppressWarnings("unused")
	private OverlayManager overlayManager;
	@Inject
	@SuppressWarnings("unused")
	private FogOfWarWorldOverlay worldOverlay;
	@Inject
	@SuppressWarnings("unused")
	private FogOfWarMinimapOverlay minimapOverlay;
	@Inject
	@SuppressWarnings("unused")
	private FadingPlayerManager fadingPlayerManager;
	@Inject
	@SuppressWarnings("unused")
	private DynamicRenderDistance dynamicRenderDistance;
	@Override
	protected void startUp() {
		overlayManager.add(worldOverlay);
		overlayManager.add(minimapOverlay);
		fadingPlayerManager.start();
		dynamicRenderDistance.start();
	}
	@Override
	protected void shutDown() {
		overlayManager.remove(worldOverlay);
		overlayManager.remove(minimapOverlay);
		fadingPlayerManager.stop();
		dynamicRenderDistance.stop();
	}
	@Provides
	@SuppressWarnings("unused")
	FogOfWarConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(FogOfWarConfig.class);
	}
}