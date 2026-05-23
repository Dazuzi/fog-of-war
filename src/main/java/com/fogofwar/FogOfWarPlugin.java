package com.fogofwar;
import com.fogofwar.config.FadingPlayerMode;
import com.fogofwar.config.FogDisplayMode;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.config.FogOfWarConfigMigration;
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
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
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
import java.util.List;
import java.util.function.Predicate;
@PluginDescriptor(
		name = "Fog of War",
		description = "Applies a fog of war effect outside of the player render distance, in both the world and on the minimap.",
		configName = "FogOfWarPlugin"
)
public class FogOfWarPlugin extends Plugin {
	private static final String CONFIG_GROUP = FogOfWarConfigMigration.CONFIG_GROUP;
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
	private volatile boolean started;
	private boolean lastSailingState;
	@Override
	protected void startUp() {
		started = true;
		initComponents();
		areaExclusionManager.setOnTransition(this::updateComponentsOnClientThread);
		updateComponentsOnClientThread();
	}
	@Override
	protected void shutDown() {
		areaExclusionManager.setOnTransition(null);
		started = false;
		lastSailingState = false;
		for (ToggleSpec overlayToggle : overlayToggles) overlayToggle.disable();
		worldOverlay.clearCaches();
		minimapOverlay.clearCaches();
		for (LifecycleSpec component : lifecycleComponents) component.stop();
	}
	private void initComponents() {
		overlayToggles = List.of(
				new ToggleSpec(worldOverlay, state -> state.worldActive),
				new ToggleSpec(minimapOverlay, state -> state.minimapActive),
				new ToggleSpec(debugOverlay, state -> state.debugActive),
				new ToggleSpec(fadingPlayerOverlay, state -> state.fadingWorldActive),
				new ToggleSpec(fadingPlayerMinimapOverlay, state -> state.fadingMinimapActive));
		lifecycleComponents = List.of(
				new LifecycleSpec(areaExclusionManager, state -> state.anyConfigured),
				new LifecycleSpec(renderCenterProvider, state -> state.overlayActive),
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
	public void onGameStateChanged(GameStateChanged event) { updateComponents(); }
	@Subscribe
	@SuppressWarnings("unused")
	public void onGameTick(GameTick event) {
		if (!config.disableWhileSailing()) return;
		boolean sailing = clientState.isSailing();
		if (sailing == lastSailingState) return;
		updateComponents(sailing);
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onVarbitChanged(VarbitChanged event) {
		if (!config.onlyInWilderness() || event.getVarbitId() != VarbitID.INSIDE_WILDERNESS) return;
		updateComponents();
	}
	private void updateComponents() {
		if (!started) return;
		updateComponents(clientState.isSailing());
	}
	private void updateComponents(boolean sailing) {
		if (!started) return;
		lastSailingState = sailing;
		ComponentState state = createComponentState(sailing);
		for (ToggleSpec overlayToggle : overlayToggles) overlayToggle.update(state);
		for (LifecycleSpec component : lifecycleComponents) component.update(state);
	}
	private void updateComponentsOnClientThread() { clientThread.invokeLater((Runnable) this::updateComponents); }
	private ComponentState createComponentState(boolean sailing) {
		FogDisplayMode worldMode = config.worldDisplayMode();
		FogDisplayMode minimapMode = config.minimapDisplayMode();
		FadingPlayerMode fadingPlayerMode = config.playerFadeMarkerMode();
		boolean worldConfigured = worldMode.isEnabled();
		boolean minimapConfigured = minimapMode.isEnabled();
		boolean fadingWorldConfigured = fadingPlayerMode.showsWorld();
		boolean fadingMinimapConfigured = fadingPlayerMode.showsMinimap();
		boolean anyConfigured = worldConfigured || minimapConfigured || fadingWorldConfigured || fadingMinimapConfigured;
		boolean areaEnabled = isCurrentAreaEnabled(sailing);
		boolean worldActive = areaEnabled && worldConfigured;
		boolean minimapActive = areaEnabled && minimapConfigured;
		boolean fadingWorldActive = areaEnabled && fadingWorldConfigured;
		boolean fadingMinimapActive = areaEnabled && fadingMinimapConfigured;
		boolean fadingActive = fadingWorldActive || fadingMinimapActive;
		boolean overlayActive = worldActive || minimapActive || fadingActive;
		boolean visibleActorTrackingActive = worldActive && worldMode.showsFog() && config.actorCutoutLimit().isEnabled();
		boolean debugActive = areaEnabled && config.debugOverlayEnabled();
		return new ComponentState(worldActive, minimapActive, debugActive, fadingWorldActive, fadingMinimapActive, fadingActive, overlayActive, visibleActorTrackingActive, anyConfigured);
	}
	private boolean isCurrentAreaEnabled(boolean sailing) {
		if (config.onlyInWilderness() && clientState.isNotInWilderness()) return false;
		if (config.disableWhileSailing() && sailing) return false;
		return !areaExclusionManager.isPlayerInExcludedArea();
	}
	private final class ToggleSpec {
		private final Overlay overlay;
		private final Predicate<ComponentState> activeFn;
		private boolean enabled;
		private ToggleSpec(Overlay overlay, Predicate<ComponentState> activeFn) {
			this.overlay = overlay;
			this.activeFn = activeFn;
		}
		private void update(ComponentState state) { set(activeFn.test(state)); }
		private void disable() { set(false); }
		private void set(boolean enabled) {
			if (this.enabled == enabled) return;
			if (enabled) overlayManager.add(overlay);
			else overlayManager.remove(overlay);
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
		private final boolean worldActive;
		private final boolean minimapActive;
		private final boolean debugActive;
		private final boolean fadingWorldActive;
		private final boolean fadingMinimapActive;
		private final boolean fadingActive;
		private final boolean overlayActive;
		private final boolean visibleActorTrackingActive;
		private final boolean anyConfigured;
		private ComponentState(boolean worldActive, boolean minimapActive, boolean debugActive, boolean fadingWorldActive, boolean fadingMinimapActive, boolean fadingActive, boolean overlayActive, boolean visibleActorTrackingActive, boolean anyConfigured) {
			this.worldActive = worldActive;
			this.minimapActive = minimapActive;
			this.debugActive = debugActive;
			this.fadingWorldActive = fadingWorldActive;
			this.fadingMinimapActive = fadingMinimapActive;
			this.fadingActive = fadingActive;
			this.overlayActive = overlayActive;
			this.visibleActorTrackingActive = visibleActorTrackingActive;
			this.anyConfigured = anyConfigured;
		}
	}
	@Provides
	@SuppressWarnings("unused")
	FogOfWarConfig provideConfig(ConfigManager configManager) {
		FogOfWarConfigMigration.migrate(configManager);
		return configManager.getConfig(FogOfWarConfig.class);
	}
}
