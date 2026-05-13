package com.fogofwar;
import com.fogofwar.box.FogOfWarMinimapOverlay;
import com.fogofwar.box.FogOfWarWorldOverlay;
import com.fogofwar.debug.DebugOverlay;
import com.fogofwar.fade.FadingPlayerManager;
import com.fogofwar.fade.FadingPlayerMinimapOverlay;
import com.fogofwar.fade.FadingPlayerOverlay;
import com.fogofwar.util.AreaManager;
import com.fogofwar.util.ClientState;
import com.fogofwar.util.DynamicRenderDistance;
import com.fogofwar.util.VisibleActorTracker;
import com.google.inject.Provides;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import javax.inject.Inject;
@PluginDescriptor(
		name = "Fog of War",
		description = "Applies a fog of war effect outside of the player render distance, in both the world and on the minimap.",
		configName = "EntityRenderDistancePlugin"
)
public class FogOfWarPlugin extends Plugin {
	private static final String CONFIG_GROUP = "entityrenderdistance";
	@Inject
	@SuppressWarnings("unused")
	private FogOfWarConfig config;
	@Inject
	@SuppressWarnings("unused")
	private ClientState clientState;
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
	private DebugOverlay debugOverlay;
	private boolean worldOverlayEnabled;
	private boolean minimapOverlayEnabled;
	private boolean debugOverlayEnabled;
	private boolean fadingPlayerOverlayEnabled;
	private boolean fadingPlayerMinimapOverlayEnabled;
	@Override
	protected void startUp() { updateComponents(); }
	@Override
	protected void shutDown() {
		worldOverlayEnabled = setOverlayEnabled(worldOverlay, worldOverlayEnabled, false);
		minimapOverlayEnabled = setOverlayEnabled(minimapOverlay, minimapOverlayEnabled, false);
		debugOverlayEnabled = setOverlayEnabled(debugOverlay, debugOverlayEnabled, false);
		fadingPlayerOverlayEnabled = setOverlayEnabled(fadingPlayerOverlay, fadingPlayerOverlayEnabled, false);
		fadingPlayerMinimapOverlayEnabled = setOverlayEnabled(fadingPlayerMinimapOverlay, fadingPlayerMinimapOverlayEnabled, false);
		fadingPlayerManager.stop();
		dynamicRenderDistance.stop();
		areaManager.stop();
		visibleActorTracker.stop();
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onConfigChanged(ConfigChanged event) {
		if (!CONFIG_GROUP.equals(event.getGroup())) return;
		updateComponents();
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onGameStateChanged(GameStateChanged event) { updateComponents(); }
	@Subscribe
	@SuppressWarnings("unused")
	public void onVarbitChanged(VarbitChanged event) {
		if (!config.onlyInWilderness() || event.getVarbitId() != VarbitID.INSIDE_WILDERNESS) return;
		updateComponents();
	}
	private void updateComponents() {
		boolean areaEnabled = isCurrentAreaEnabled();
		boolean worldActive = areaEnabled && (config.showWorldFog() || config.showWorldBorder());
		boolean minimapActive = areaEnabled && (config.showMinimapFog() || config.showMinimapBorder());
		boolean fadingWorldActive = areaEnabled && config.enableFadingPlayers() && config.showFadingInWorld();
		boolean fadingMinimapActive = areaEnabled && config.enableFadingPlayers() && config.showFadingOnMinimap();
		boolean fadingActive = fadingWorldActive || fadingMinimapActive;
		boolean renderDistanceActive = worldActive || minimapActive || fadingActive;
		worldOverlayEnabled = setOverlayEnabled(worldOverlay, worldOverlayEnabled, worldActive);
		minimapOverlayEnabled = setOverlayEnabled(minimapOverlay, minimapOverlayEnabled, minimapActive);
		debugOverlayEnabled = setOverlayEnabled(debugOverlay, debugOverlayEnabled, config.showDebugOverlay());
		fadingPlayerOverlayEnabled = setOverlayEnabled(fadingPlayerOverlay, fadingPlayerOverlayEnabled, fadingWorldActive);
		fadingPlayerMinimapOverlayEnabled = setOverlayEnabled(fadingPlayerMinimapOverlay, fadingPlayerMinimapOverlayEnabled, fadingMinimapActive);
		if (worldActive || minimapActive || fadingActive) areaManager.start();
		else areaManager.stop();
		if (fadingActive) fadingPlayerManager.start();
		else fadingPlayerManager.stop();
		if (config.enableDynamicRenderDistance() && renderDistanceActive) dynamicRenderDistance.start();
		else dynamicRenderDistance.stop();
		if (worldActive && config.showWorldFog() && config.excludeEntities()) visibleActorTracker.start();
		else visibleActorTracker.stop();
	}
	private boolean isCurrentAreaEnabled() { return !config.onlyInWilderness() || !clientState.isNotInWilderness(); }
	private boolean setOverlayEnabled(Overlay overlay, boolean current, boolean enabled) {
		if (current == enabled) return current;
		if (enabled) overlayManager.add(overlay);
		else overlayManager.remove(overlay);
		return enabled;
	}
	@Provides
	@SuppressWarnings("unused")
	FogOfWarConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(FogOfWarConfig.class);
	}
}
