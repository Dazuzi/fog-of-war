package com.fogofwar.fade;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.state.AreaExclusionManager;
import com.fogofwar.lifecycle.LifecycleComponent;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
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
	private final AreaExclusionManager areaExclusionManager;
	@Getter
	private final Map<Player, FadingPlayer> fadingPlayers = new HashMap<>();
	private Map<Player, WorldPoint> lastTickPlayerLocations = new HashMap<>();
	private Map<Player, WorldPoint> twoTicksAgoPlayerLocations = new HashMap<>();
	private Map<Player, WorldPoint> currentPlayerLocations = new HashMap<>();
	private final Set<String> currentPlayerNames = new HashSet<>();
	private final FadingPlayerPredictor predictor = new FadingPlayerPredictor();
	@Inject
	public FadingPlayerManager(Client client, FogOfWarConfig config, EventBus eventBus, AreaExclusionManager areaExclusionManager) {
		super(eventBus);
		this.client = client;
		this.config = config;
		this.areaExclusionManager = areaExclusionManager;
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
		Player localPlayer = client.getLocalPlayer();
		WorldView worldView = client.getTopLevelWorldView();
		WorldPoint localPlayerLocation = localPlayer != null ? localPlayer.getWorldLocation() : null;
		if (client.getGameState() != GameState.LOGGED_IN || localPlayer == null || worldView == null || localPlayerLocation == null) {
			clearAllTracking();
			return;
		}
		if (areaExclusionManager.isPlayerInExcludedArea()) {
			clearAllTracking();
			return;
		}
		int renderDistance = config.landRenderDistance();
		int fadeDuration = config.fadeDurationTicks();
		boolean extrapolate = config.predictMovement();
		boolean onlyAtLimit = config.onlyFadeAtRenderEdge();
		handleFadingPlayers(fadeDuration, extrapolate, renderDistance, localPlayerLocation);
		updatePlayerTracking(extrapolate, onlyAtLimit, renderDistance, localPlayer, worldView, localPlayerLocation);
	}
	private void clearAllTracking() {
		fadingPlayers.clear();
		lastTickPlayerLocations.clear();
		twoTicksAgoPlayerLocations.clear();
		currentPlayerLocations.clear();
		currentPlayerNames.clear();
	}
	private void handleFadingPlayers(int fadeDuration, boolean extrapolate, int renderDistance, WorldPoint localPlayerLocation) {
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
	private void updatePlayerTracking(boolean extrapolate, boolean onlyAtLimit, int renderDistance, Player localPlayer, WorldView worldView, WorldPoint localPlayerLocation) {
		currentPlayerLocations.clear();
		currentPlayerNames.clear();
		for (Player player : worldView.players()) {
			if (player == null || player == localPlayer) continue;
			WorldPoint playerLocation = player.getWorldLocation();
			if (playerLocation != null) currentPlayerLocations.put(player, playerLocation);
			if (player.getName() != null) currentPlayerNames.add(player.getName());
		}
		for (Map.Entry<Player, WorldPoint> entry : lastTickPlayerLocations.entrySet()) {
			Player player = entry.getKey();
			if (currentPlayerLocations.containsKey(player)) continue;
			if (fadingPlayers.containsKey(player)) continue;
			WorldPoint lastLocation = entry.getValue();
			if (lastLocation == null) continue;
			WorldPoint twoTicksAgoLocation = twoTicksAgoPlayerLocations.get(player);
			WorldPoint velocity = predictor.getVelocity(lastLocation, twoTicksAgoLocation);
			if (!predictor.shouldFade(lastLocation, localPlayerLocation, velocity, onlyAtLimit, renderDistance)) continue;
			WorldPoint initialFadeLocation = predictor.getInitialFadeLocation(lastLocation, localPlayerLocation, velocity, extrapolate, renderDistance);
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
