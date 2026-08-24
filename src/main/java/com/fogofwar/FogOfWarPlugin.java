package com.fogofwar;
import com.fogofwar.config.FadingPlayerMode;
import com.fogofwar.config.FogDisplayMode;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.debug.DebugOverlay;
import com.fogofwar.fade.FadingPlayerManager;
import com.fogofwar.fade.FadingPlayerMinimapOverlay;
import com.fogofwar.fade.FadingPlayerOverlay;
import com.fogofwar.lifecycle.LifecycleComponent;
import com.fogofwar.area.AreaExclusionManager;
import com.fogofwar.render.RenderCenterProvider;
import com.fogofwar.render.minimap.MinimapFogOverlay;
import com.fogofwar.render.world.WorldFogOverlay;
import com.fogofwar.render.world.VisibleActorTracker;
import com.fogofwar.state.ClientState;
import com.google.inject.Provides;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import javax.inject.Inject;
import java.awt.Color;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
@PluginDescriptor(
		name = "Fog of War",
		description = "Applies a fog of war effect outside of the player render distance, in both the world and on the minimap.",
		configName = "FogOfWarPlugin"
)
public class FogOfWarPlugin extends Plugin {
	private static final String CONFIG_GROUP = FogOfWarConfig.CONFIG_GROUP;
	@Inject
	private FogOfWarConfig config;
	@Inject
	private ClientThread clientThread;
	@Inject
	private ClientState clientState;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private WorldFogOverlay worldOverlay;
	@Inject
	private MinimapFogOverlay minimapOverlay;
	@Inject
	private FadingPlayerManager fadingPlayerManager;
	@Inject
	private FadingPlayerOverlay fadingPlayerOverlay;
	@Inject
	private FadingPlayerMinimapOverlay fadingPlayerMinimapOverlay;
	@Inject
	private AreaExclusionManager areaExclusionManager;
	@Inject
	private RenderCenterProvider renderCenterProvider;
	@Inject
	private VisibleActorTracker visibleActorTracker;
	@Inject
	private DebugOverlay debugOverlay;
	private List<ToggleSpec> overlayToggles = List.of();
	private List<LifecycleSpec> lifecycleComponents = List.of();
	private final AtomicBoolean componentUpdatePending = new AtomicBoolean();
	private volatile boolean started;
	private boolean sailingUpdatesActive;
	private boolean wildernessUpdatesActive;
	@Override
	protected void startUp() {
		started = true;
		initComponents();
		areaExclusionManager.setOnTransition(this::updateComponents);
		updateComponentsOnClientThread();
	}
	@Override
	protected void shutDown() {
		areaExclusionManager.setOnTransition(null);
		started = false;
		sailingUpdatesActive = wildernessUpdatesActive = false;
		for (ToggleSpec overlayToggle : overlayToggles) overlayToggle.disable();
		areaExclusionManager.stop();
		for (LifecycleSpec component : lifecycleComponents) component.stop();
	}
	private void initComponents() {
		overlayToggles = List.of(
				new ToggleSpec(worldOverlay, state -> state.worldActive, worldOverlay::clearCaches),
				new ToggleSpec(minimapOverlay, state -> state.minimapActive, minimapOverlay::clearCaches),
				new ToggleSpec(debugOverlay, state -> state.debugActive, debugOverlay::clearCaches),
				new ToggleSpec(fadingPlayerOverlay, state -> state.fadingWorldActive),
				new ToggleSpec(fadingPlayerMinimapOverlay, state -> state.fadingMinimapActive, fadingPlayerMinimapOverlay::clearCaches));
		lifecycleComponents = List.of(
				new LifecycleSpec(renderCenterProvider, state -> state.overlayActive || state.debugActive),
				new LifecycleSpec(fadingPlayerManager, state -> state.fadingActive),
				new LifecycleSpec(visibleActorTracker, state -> state.visibleActorTrackingActive));
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onConfigChanged(ConfigChanged event) {
		if (!CONFIG_GROUP.equals(event.getGroup())) return;
		updateComponentsOnClientThread();
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState() == GameState.LOGGED_IN) updateComponents();
		else updateComponents(false);
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onVarbitChanged(VarbitChanged event) {
		if (!started) return;
		int id = event.getVarbitId();
		if (id == VarbitID.SAILING_BOARDED_BOAT) {
			if (sailingUpdatesActive) updateComponents(event.getValue() == 1);
			return;
		}
		if (wildernessUpdatesActive && id == VarbitID.INSIDE_WILDERNESS) updateComponents();
	}
	private void updateComponents() {
		if (!started) return;
		updateComponents(clientState.isLoggedIn() && (config.disableWhileSailing() || config.showLandAreaWhileSailing()) && clientState.isSailing());
	}
	private void updateComponents(boolean sailing) {
		if (!started) return;
		sailingUpdatesActive = config.disableWhileSailing() || config.showLandAreaWhileSailing();
		wildernessUpdatesActive = config.onlyInWilderness();
		ComponentState state = createComponentState(sailing);
		if (state.areaExclusionActive) {
			boolean excluded = areaExclusionManager.isPlayerInExcludedArea();
			areaExclusionManager.start();
			if (excluded != areaExclusionManager.isPlayerInExcludedArea()) state = createComponentState(sailing);
		} else areaExclusionManager.stop();
		for (ToggleSpec overlayToggle : overlayToggles) overlayToggle.update(state);
		for (LifecycleSpec component : lifecycleComponents) component.update(state);
		if (!state.visibleActorTrackingActive) worldOverlay.clearActorCaches();
	}
	private void updateComponentsOnClientThread() {
		if (!started || !componentUpdatePending.compareAndSet(false, true)) return;
		clientThread.invokeLater(() -> {
			componentUpdatePending.set(false);
			updateComponents();
		});
	}
	private ComponentState createComponentState(boolean sailing) {
		if (!clientState.isLoggedIn()) return ComponentState.INACTIVE;
		FogDisplayMode worldMode = config.worldDisplayMode();
		FogDisplayMode minimapMode = config.minimapDisplayMode();
		FadingPlayerMode fadingPlayerMode = config.playerFadeMarkerMode();
		boolean showLandAreaWhileSailing = sailing && (worldMode.isEnabled() || minimapMode.isEnabled()) && config.showLandAreaWhileSailing();
		Color worldFogColour = worldMode.showsFog() ? config.worldFogColour() : null;
		Color worldBorderColour = worldMode.showsBorder() ? config.worldBorderColour() : null;
		Color minimapFogColour = minimapMode.showsFog() ? config.minimapFogColour() : null;
		Color minimapBorderColour = minimapMode.showsBorder() ? config.minimapBorderColour() : null;
		boolean worldConfigured = hasVisibleFogOrBorder(worldMode, worldFogColour, worldBorderColour, showLandAreaWhileSailing);
		boolean minimapConfigured = hasVisibleFogOrBorder(minimapMode, minimapFogColour, minimapBorderColour, showLandAreaWhileSailing);
		boolean fadingWorldMode = fadingPlayerMode.showsWorld();
		boolean fadingMinimapMode = fadingPlayerMode.showsMinimap();
		Color fadeColour = fadingWorldMode || fadingMinimapMode ? config.fadeMarkerColour() : null;
		boolean fadingWorldConfigured = fadingWorldMode && hasVisibleFadingWorld(fadingPlayerMode, fadeColour, config.showFadeMarkerNames());
		boolean fadingMinimapConfigured = fadingMinimapMode && hasVisibleFadingMinimap(fadingPlayerMode, fadeColour);
		boolean anyConfigured = worldConfigured || minimapConfigured || fadingWorldConfigured || fadingMinimapConfigured;
		boolean debugConfigured = config.debugOverlayEnabled();
		if (!anyConfigured && !debugConfigured) return ComponentState.INACTIVE;
		boolean globallyEnabled = isGloballyEnabled(sailing);
		boolean areaEnabled = globallyEnabled && (!anyConfigured || !areaExclusionManager.isPlayerInExcludedArea());
		boolean worldActive = areaEnabled && worldConfigured;
		boolean minimapActive = areaEnabled && minimapConfigured;
		boolean fadingWorldActive = areaEnabled && fadingWorldConfigured;
		boolean fadingMinimapActive = areaEnabled && fadingMinimapConfigured;
		boolean fadingActive = fadingWorldActive || fadingMinimapActive;
		boolean overlayActive = worldActive || minimapActive || fadingActive;
		boolean visibleActorTrackingActive = worldActive && worldMode.showsFog() && (worldFogColour.getAlpha() > 0 || showLandAreaWhileSailing) && config.actorCutoutLimit().isEnabled();
		boolean debugActive = areaEnabled && debugConfigured;
		return new ComponentState(worldActive, minimapActive, debugActive, fadingWorldActive, fadingMinimapActive, fadingActive, overlayActive, visibleActorTrackingActive, globallyEnabled && anyConfigured);
	}
	static boolean hasVisibleFogOrBorder(FogDisplayMode mode, Color fog, Color border, boolean showLandAreaWhileSailing) {
		return mode.showsFog() && (fog.getAlpha() > 0 || showLandAreaWhileSailing) || mode.showsBorder() && (border.getAlpha() > 0 || showLandAreaWhileSailing);
	}
	static boolean hasVisibleFadingWorld(FadingPlayerMode mode, Color colour, boolean showNames) { return mode.showsWorld() && (colour.getAlpha() > 0 || showNames); }
	static boolean hasVisibleFadingMinimap(FadingPlayerMode mode, Color colour) { return mode.showsMinimap() && colour.getAlpha() > 0; }
	private boolean isGloballyEnabled(boolean sailing) {
		if (config.onlyInWilderness() && clientState.isNotInWilderness()) return false;
		return !config.disableWhileSailing() || !sailing;
	}
	private final class ToggleSpec {
		private final Overlay overlay;
		private final Predicate<ComponentState> activeFn;
		private final Runnable onDisable;
		private boolean enabled;
		private ToggleSpec(Overlay overlay, Predicate<ComponentState> activeFn) { this(overlay, activeFn, null); }
		private ToggleSpec(Overlay overlay, Predicate<ComponentState> activeFn, Runnable onDisable) {
			this.overlay = overlay;
			this.activeFn = activeFn;
			this.onDisable = onDisable;
		}
		private void update(ComponentState state) { set(activeFn.test(state)); }
		private void disable() { set(false); }
		private void set(boolean enabled) {
			if (this.enabled == enabled) return;
			if (enabled) overlayManager.add(overlay);
			else {
				overlayManager.remove(overlay);
				if (onDisable != null) onDisable.run();
			}
			this.enabled = enabled;
		}
	}
	private static final class LifecycleSpec {
		private final LifecycleComponent component;
		private final Predicate<ComponentState> activeFn;
		private LifecycleSpec(LifecycleComponent component, Predicate<ComponentState> activeFn) {
			this.component = component;
			this.activeFn = activeFn;
		}
		private void update(ComponentState state) {
			if (activeFn.test(state)) component.start();
			else component.stop();
		}
		private void stop() { component.stop(); }
	}
	private static final class ComponentState {
		private static final ComponentState INACTIVE = new ComponentState(false, false, false, false, false, false, false, false, false);
		private final boolean worldActive;
		private final boolean minimapActive;
		private final boolean debugActive;
		private final boolean fadingWorldActive;
		private final boolean fadingMinimapActive;
		private final boolean fadingActive;
		private final boolean overlayActive;
		private final boolean visibleActorTrackingActive;
		private final boolean areaExclusionActive;
		private ComponentState(boolean worldActive, boolean minimapActive, boolean debugActive, boolean fadingWorldActive, boolean fadingMinimapActive, boolean fadingActive, boolean overlayActive, boolean visibleActorTrackingActive, boolean areaExclusionActive) {
			this.worldActive = worldActive;
			this.minimapActive = minimapActive;
			this.debugActive = debugActive;
			this.fadingWorldActive = fadingWorldActive;
			this.fadingMinimapActive = fadingMinimapActive;
			this.fadingActive = fadingActive;
			this.overlayActive = overlayActive;
			this.visibleActorTrackingActive = visibleActorTrackingActive;
			this.areaExclusionActive = areaExclusionActive;
		}
	}
	@Provides
	@SuppressWarnings("unused")
	FogOfWarConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(FogOfWarConfig.class);
	}
}
