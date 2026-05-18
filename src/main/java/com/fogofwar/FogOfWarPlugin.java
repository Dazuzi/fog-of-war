package com.fogofwar;
import com.fogofwar.config.FadingPlayerMode;
import com.fogofwar.config.FogDisplayMode;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.config.FogOfWarConfigMigration;
import com.fogofwar.debug.DebugOverlay;
import com.fogofwar.fade.FadingPlayerManager;
import com.fogofwar.fade.FadingPlayerMinimapOverlay;
import com.fogofwar.fade.FadingPlayerOverlay;
import com.fogofwar.lifecycle.OverlayToggle;
import com.fogofwar.render.minimap.MinimapFogOverlay;
import com.fogofwar.render.world.WorldFogOverlay;
import com.fogofwar.actor.VisibleActorTracker;
import com.fogofwar.state.AreaExclusionManager;
import com.fogofwar.state.ClientState;
import com.fogofwar.state.RenderDistanceManager;
import com.google.inject.Provides;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import javax.inject.Inject;
import java.util.List;
@PluginDescriptor(
		name = "Fog of War",
		description = "Applies a fog of war effect outside of the player render distance, in both the world and on the minimap.",
		configName = "FogOfWarPlugin"
)
public class FogOfWarPlugin extends Plugin {
	private static final String CONFIG_GROUP = FogOfWarConfigMigration.CONFIG_GROUP;
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
	private WorldFogOverlay worldOverlay;
	@Inject
	@SuppressWarnings("unused")
	private MinimapFogOverlay minimapOverlay;
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
	private RenderDistanceManager renderDistanceManager;
	@Inject
	@SuppressWarnings("unused")
	private AreaExclusionManager areaExclusionManager;
	@Inject
	@SuppressWarnings("unused")
	private VisibleActorTracker visibleActorTracker;
	@Inject
	@SuppressWarnings("unused")
	private DebugOverlay debugOverlay;
	private OverlayToggle worldOverlayToggle;
	private OverlayToggle minimapOverlayToggle;
	private OverlayToggle debugOverlayToggle;
	private OverlayToggle fadingPlayerOverlayToggle;
	private OverlayToggle fadingPlayerMinimapOverlayToggle;
	private List<OverlayToggle> overlayToggles = List.of();
	@Override
	protected void startUp() {
		initOverlayToggles();
		updateComponents();
	}
	@Override
	protected void shutDown() {
		for (OverlayToggle overlayToggle : overlayToggles) overlayToggle.set(false);
		worldOverlay.clearCaches();
		minimapOverlay.clearCaches();
		debugOverlay.stop();
		fadingPlayerManager.stop();
		renderDistanceManager.stop();
		areaExclusionManager.stop();
		visibleActorTracker.stop();
	}
	private void initOverlayToggles() {
		worldOverlayToggle = new OverlayToggle(overlayManager, worldOverlay);
		minimapOverlayToggle = new OverlayToggle(overlayManager, minimapOverlay);
		debugOverlayToggle = new OverlayToggle(overlayManager, debugOverlay);
		fadingPlayerOverlayToggle = new OverlayToggle(overlayManager, fadingPlayerOverlay);
		fadingPlayerMinimapOverlayToggle = new OverlayToggle(overlayManager, fadingPlayerMinimapOverlay);
		overlayToggles = List.of(worldOverlayToggle, minimapOverlayToggle, debugOverlayToggle, fadingPlayerOverlayToggle, fadingPlayerMinimapOverlayToggle);
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
		FogDisplayMode worldMode = config.worldDisplayMode();
		FogDisplayMode minimapMode = config.minimapDisplayMode();
		FadingPlayerMode fadingPlayerMode = config.playerFadeMarkerMode();
		boolean worldActive = areaEnabled && worldMode.isEnabled();
		boolean minimapActive = areaEnabled && minimapMode.isEnabled();
		boolean fadingWorldActive = areaEnabled && fadingPlayerMode.showsWorld();
		boolean fadingMinimapActive = areaEnabled && fadingPlayerMode.showsMinimap();
		boolean fadingActive = fadingWorldActive || fadingMinimapActive;
		boolean renderDistanceActive = worldActive || minimapActive || fadingActive;
		worldOverlayToggle.set(worldActive);
		minimapOverlayToggle.set(minimapActive);
		debugOverlayToggle.set(config.debugOverlayEnabled());
		if (config.debugOverlayEnabled()) debugOverlay.start();
		else debugOverlay.stop();
		fadingPlayerOverlayToggle.set(fadingWorldActive);
		fadingPlayerMinimapOverlayToggle.set(fadingMinimapActive);
		if (renderDistanceActive) areaExclusionManager.start();
		else areaExclusionManager.stop();
		if (fadingActive) fadingPlayerManager.start();
		else fadingPlayerManager.stop();
		if (config.dynamicRenderDistanceEnabled() && renderDistanceActive) renderDistanceManager.start();
		else renderDistanceManager.stop();
		if (worldActive && worldMode.showsFog() && config.actorCutoutLimit().isEnabled()) visibleActorTracker.start();
		else visibleActorTracker.stop();
	}
	private boolean isCurrentAreaEnabled() { return !config.onlyInWilderness() || !clientState.isNotInWilderness(); }
	@Provides
	@SuppressWarnings("unused")
	FogOfWarConfig provideConfig(ConfigManager configManager) {
		FogOfWarConfigMigration.migrate(configManager);
		return configManager.getConfig(FogOfWarConfig.class);
	}
}
