package com.fogofwar;
import com.fogofwar.box.FogOfWarMinimapOverlay;
import com.fogofwar.box.FogOfWarWorldOverlay;
import com.fogofwar.debug.PlaneDisplayOverlay;
import com.fogofwar.fade.FadingPlayerManager;
import com.fogofwar.fade.FadingPlayerMinimapOverlay;
import com.fogofwar.fade.FadingPlayerOverlay;
import com.fogofwar.util.AreaManager;
import com.fogofwar.util.DynamicRenderDistance;
import com.fogofwar.util.VisibleActorTracker;
import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import javax.inject.Inject;
@PluginDescriptor(
		name = "Fog of War",
		description = "Applies a fog of war effect outside of the player render distance, in both the world and on the minimap.",
		configName = "EntityRenderDistancePlugin"
)
public class FogOfWarPlugin extends Plugin {
	private static final String CONFIG_GROUP = "entityrenderdistance";
	private static final String EXCLUDE_ENTITIES_KEY = "excludeEntities";
	@Inject
	@SuppressWarnings("unused")
	private FogOfWarConfig config;
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
	private FadingPlayerOverlay fadingPlayerOverlay;
	@Inject
	@SuppressWarnings("unused")
	private FadingPlayerMinimapOverlay fadingPlayerMinimapOverlay;
	@Inject
	@SuppressWarnings("unused")
	private DynamicRenderDistance dynamicRenderDistance;
	@Inject
	@SuppressWarnings("unused")
	private AreaManager areaManager;
	@Inject
	@SuppressWarnings("unused")
	private VisibleActorTracker visibleActorTracker;
	@Inject
	@SuppressWarnings("unused")
	private PlaneDisplayOverlay planeDisplayOverlay;
	@Override
	protected void startUp() {
		overlayManager.add(worldOverlay);
		overlayManager.add(minimapOverlay);
		overlayManager.add(planeDisplayOverlay);
		overlayManager.add(fadingPlayerOverlay);
		overlayManager.add(fadingPlayerMinimapOverlay);
		fadingPlayerManager.start();
		dynamicRenderDistance.start();
		areaManager.start();
		updateVisibleActorTracker();
	}
	@Override
	protected void shutDown() {
		overlayManager.remove(worldOverlay);
		overlayManager.remove(minimapOverlay);
		overlayManager.remove(planeDisplayOverlay);
		overlayManager.remove(fadingPlayerOverlay);
		overlayManager.remove(fadingPlayerMinimapOverlay);
		fadingPlayerManager.stop();
		dynamicRenderDistance.stop();
		areaManager.stop();
		visibleActorTracker.stop();
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onConfigChanged(ConfigChanged event) {
		if (!CONFIG_GROUP.equals(event.getGroup()) || !EXCLUDE_ENTITIES_KEY.equals(event.getKey())) return;
		updateVisibleActorTracker();
	}
	private void updateVisibleActorTracker() {
		if (config.excludeEntities()) visibleActorTracker.start();
		else visibleActorTracker.stop();
	}
	@Provides
	@SuppressWarnings("unused")
	FogOfWarConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(FogOfWarConfig.class);
	}
}
