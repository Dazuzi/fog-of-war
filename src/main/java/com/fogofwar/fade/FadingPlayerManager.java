package com.fogofwar.fade;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.state.AreaExclusionManager;
import com.fogofwar.state.RenderDistanceManager;
import com.fogofwar.lifecycle.LifecycleComponent;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
@Singleton
public class FadingPlayerManager extends LifecycleComponent {
	private final Client client;
	private final FogOfWarConfig config;
	private final RenderDistanceManager dynamicRenderDistance;
	private final AreaExclusionManager areaManager;
	@Getter
	private final Map<Player, FadingPlayer> fadingPlayers = new HashMap<>();
	private Map<Player, WorldPoint> lastTickPlayerLocations = new HashMap<>();
	private Map<Player, WorldPoint> twoTicksAgoPlayerLocations = new HashMap<>();
	private Map<Player, WorldPoint> currentPlayerLocations = new HashMap<>();
	private final Set<String> currentPlayerNames = new HashSet<>();
	private final FadingPlayerPredictor predictor = new FadingPlayerPredictor();
	@Inject
	public FadingPlayerManager(Client client, FogOfWarConfig config, EventBus eventBus, RenderDistanceManager dynamicRenderDistance, AreaExclusionManager areaManager) {
		super(eventBus);
		this.client = client;
		this.config = config;
		this.dynamicRenderDistance = dynamicRenderDistance;
		this.areaManager = areaManager;
	}
	@Override
	protected void onStop(boolean wasStarted) { clearAllTracking(); }
	@Subscribe
	@SuppressWarnings("unused")
	public void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState() == GameState.LOADING) clearAllTracking();
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onGameTick(GameTick event) {
		if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null) {
			clearAllTracking();
			return;
		}
		if (areaManager.isPlayerInExcludedArea()) {
			clearAllTracking();
			return;
		}
		int renderDistance = dynamicRenderDistance.getCurrentRenderDistance();
		int fadeDuration = config.fadeDurationTicks();
		boolean extrapolate = config.predictMovement();
		boolean onlyAtLimit = config.onlyFadeAtRenderEdge();
		handleFadingPlayers(fadeDuration, extrapolate, renderDistance);
		updatePlayerTracking(extrapolate, onlyAtLimit, renderDistance);
	}
	private void clearAllTracking() {
		fadingPlayers.clear();
		lastTickPlayerLocations.clear();
		twoTicksAgoPlayerLocations.clear();
		currentPlayerLocations.clear();
		currentPlayerNames.clear();
	}
	private void handleFadingPlayers(int fadeDuration, boolean extrapolate, int renderDistance) {
		WorldPoint localPlayerLocation = client.getLocalPlayer().getWorldLocation();
		fadingPlayers.entrySet().removeIf(entry -> {
			FadingPlayer fp = entry.getValue();
			fp.setTicksSinceDisappeared(fp.getTicksSinceDisappeared() + 1);
			if (fp.getTicksSinceDisappeared() > fadeDuration) return true;
			if (fp.getTicksSinceDisappeared() > 1 && fp.getLastLocation().distanceTo(localPlayerLocation) <= renderDistance) return true;
			if (extrapolate) {
				fp.setLastLocation(new WorldPoint(
						fp.getLastLocation().getX() + fp.getVelocity().getX(),
						fp.getLastLocation().getY() + fp.getVelocity().getY(),
						fp.getLastLocation().getPlane()));
			}
			return false;
		});
	}
	private void updatePlayerTracking(boolean extrapolate, boolean onlyAtLimit, int renderDistance) {
		currentPlayerLocations.clear();
		currentPlayerNames.clear();
		for (Player player : client.getTopLevelWorldView().players()) {
			if (player != null && player != client.getLocalPlayer()) {
				currentPlayerLocations.put(player, player.getWorldLocation());
				if (player.getName() != null) currentPlayerNames.add(player.getName());
			}
		}
		WorldPoint currentLocalPlayerLocation = client.getLocalPlayer().getWorldLocation();
		for (Map.Entry<Player, WorldPoint> entry : lastTickPlayerLocations.entrySet()) {
			Player player = entry.getKey();
			if (currentPlayerLocations.containsKey(player)) continue;
			if (fadingPlayers.containsKey(player)) continue;
			WorldPoint lastLocation = entry.getValue();
			if (lastLocation == null) continue;
			WorldPoint twoTicksAgoLocation = twoTicksAgoPlayerLocations.get(player);
			WorldPoint velocity = predictor.getVelocity(lastLocation, twoTicksAgoLocation);
			if (!predictor.shouldFade(lastLocation, currentLocalPlayerLocation, velocity, onlyAtLimit, renderDistance)) continue;
			WorldPoint initialFadeLocation = predictor.getInitialFadeLocation(lastLocation, currentLocalPlayerLocation, velocity, extrapolate, renderDistance);
			fadingPlayers.put(player, new FadingPlayer(player, velocity, initialFadeLocation));
		}
		Map<Player, WorldPoint> tmp = twoTicksAgoPlayerLocations;
		twoTicksAgoPlayerLocations = lastTickPlayerLocations;
		lastTickPlayerLocations = currentPlayerLocations;
		currentPlayerLocations = tmp;
		fadingPlayers.entrySet().removeIf(entry -> {
			String name = entry.getKey().getName();
			return name != null && currentPlayerNames.contains(name);
		});
	}
}
