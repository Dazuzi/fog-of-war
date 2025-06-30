package com.entityrenderdistance;
import com.entityrenderdistance.box.EntityRenderDistanceMinimapOverlay;
import com.entityrenderdistance.box.EntityRenderDistanceWorldOverlay;
import com.entityrenderdistance.fade.FadingPlayerManager;
import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
@PluginDescriptor(
		name = "Entity Render Distance",
		description = "Draws boxes around the player to mark the maximum entity render distance",
		enabledByDefault = true
)
public class EntityRenderDistancePlugin extends Plugin {
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private EntityRenderDistanceWorldOverlay worldOverlay;
	@Inject
	private EntityRenderDistanceMinimapOverlay minimapOverlay;
	@Inject
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
	@Provides
	EntityRenderDistanceConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(EntityRenderDistanceConfig.class);
	}
}