package com.fogofwar.fade;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.area.AreaExclusionManager;
import com.fogofwar.coord.WorldEntityCoords;
import com.fogofwar.lifecycle.LifecycleComponent;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.WorldEntity;
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
	private Map<Player, TrackedPlayer> lastTickPlayerLocations = new HashMap<>();
	private Map<Player, TrackedPlayer> twoTicksAgoPlayerLocations = new HashMap<>();
	private Map<Player, TrackedPlayer> currentPlayerLocations = new HashMap<>();
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
	protected void onStop() { clearAllTracking(); }
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
		WorldPoint localPlayerLocation = localPlayer != null && worldView != null ? WorldEntityCoords.playerToTopLevel(localPlayer, null, worldView) : null;
		if (client.getGameState() != GameState.LOGGED_IN || localPlayer == null || worldView == null || localPlayerLocation == null) {
			clearAllTracking();
			return;
		}
		if (areaExclusionManager.isPlayerInExcludedArea()) {
			clearAllTracking();
			return;
		}
		if (config.disableWhileSailing() && isOnWorldEntity(localPlayer)) {
			clearAllTracking();
			return;
		}
		int fadeDuration = config.fadeDurationTicks();
		boolean extrapolate = config.predictMovement();
		boolean onlyAtLimit = config.onlyFadeAtRenderEdge();
		handleFadingPlayers(fadeDuration, extrapolate, localPlayerLocation);
		updatePlayerTracking(extrapolate, onlyAtLimit, localPlayer, worldView, localPlayerLocation);
	}
	private void clearAllTracking() {
		fadingPlayers.clear();
		lastTickPlayerLocations.clear();
		twoTicksAgoPlayerLocations.clear();
		currentPlayerLocations.clear();
		currentPlayerNames.clear();
	}
	private void handleFadingPlayers(int fadeDuration, boolean extrapolate, WorldPoint localPlayerLocation) {
		fadingPlayers.entrySet().removeIf(entry -> {
			FadingPlayer fp = entry.getValue();
			fp.setTicksSinceDisappeared(fp.getTicksSinceDisappeared() + 1);
			if (fp.getTicksSinceDisappeared() > fadeDuration) return true;
			WorldPoint markerLocation = fp.getMarkerLocation();
			if (fp.getTicksSinceDisappeared() > 1 && markerLocation.distanceTo(localPlayerLocation) <= fp.getRenderDistance()) return true;
			WorldPoint velocity = fp.getVelocity();
			if (extrapolate && velocity != null) {
				fp.setMarkerLocation(new WorldPoint(
						markerLocation.getX() + velocity.getX(),
						markerLocation.getY() + velocity.getY(),
						markerLocation.getPlane()));
			}
			return false;
		});
	}
	private void updatePlayerTracking(boolean extrapolate, boolean onlyAtLimit, Player localPlayer, WorldView worldView, WorldPoint localPlayerLocation) {
		currentPlayerLocations.clear();
		currentPlayerNames.clear();
		trackPlayers(localPlayer, worldView, worldView, config.landRenderDistance());
		int boatRenderDistance = isOnWorldEntity(localPlayer) ? config.sailingRenderDistance() : config.landRenderDistance();
		for (WorldEntity worldEntity : worldView.worldEntities()) {
			if (worldEntity == null) continue;
			WorldView entityWorldView = worldEntity.getWorldView();
			if (entityWorldView == null) continue;
			trackPlayers(localPlayer, entityWorldView, worldView, boatRenderDistance);
		}
		for (Map.Entry<Player, TrackedPlayer> entry : lastTickPlayerLocations.entrySet()) {
			Player player = entry.getKey();
			if (currentPlayerLocations.containsKey(player)) continue;
			if (fadingPlayers.containsKey(player)) continue;
			TrackedPlayer lastTickPlayer = entry.getValue();
			WorldPoint lastLocation = lastTickPlayer.getLocation();
			if (lastLocation == null) continue;
			int renderDistance = lastTickPlayer.getRenderDistance();
			boolean needsVelocity = onlyAtLimit || extrapolate;
			WorldPoint velocity = null;
			boolean nearRenderLimit = false;
			if (needsVelocity) {
				TrackedPlayer twoTicksAgoPlayer = twoTicksAgoPlayerLocations.get(player);
				WorldPoint twoTicksAgoLocation = twoTicksAgoPlayer != null ? twoTicksAgoPlayer.getLocation() : null;
				velocity = predictor.getVelocity(lastLocation, twoTicksAgoLocation);
				nearRenderLimit = predictor.isNearRenderLimit(lastLocation, localPlayerLocation, velocity, renderDistance);
			}
			if (onlyAtLimit && !nearRenderLimit) continue;
			WorldPoint initialFadeLocation = extrapolate ? predictor.getInitialFadeLocation(lastLocation, localPlayerLocation, velocity, true, renderDistance, nearRenderLimit) : lastLocation;
			fadingPlayers.put(player, new FadingPlayer(player, velocity, initialFadeLocation, renderDistance));
		}
		Map<Player, TrackedPlayer> tmp = twoTicksAgoPlayerLocations;
		twoTicksAgoPlayerLocations = lastTickPlayerLocations;
		lastTickPlayerLocations = currentPlayerLocations;
		currentPlayerLocations = tmp;
		fadingPlayers.entrySet().removeIf(entry -> {
			String name = entry.getKey().getName();
			return name != null && currentPlayerNames.contains(name);
		});
	}
	private void trackPlayers(Player localPlayer, WorldView worldView, WorldView topWorldView, int renderDistance) {
		for (Player player : worldView.players()) {
			if (player == null || player == localPlayer) continue;
			WorldPoint playerLocation = WorldEntityCoords.playerToTopLevel(player, worldView, topWorldView);
			if (playerLocation != null) currentPlayerLocations.put(player, new TrackedPlayer(playerLocation, renderDistance));
			if (player.getName() != null) currentPlayerNames.add(player.getName());
		}
	}
	private boolean isOnWorldEntity(Player player) {
		return WorldEntityCoords.isPlayerOnShip(player, client.getTopLevelWorldView());
	}
	@Getter
	private static class TrackedPlayer {
		private final WorldPoint location;
		private final int renderDistance;
		private TrackedPlayer(WorldPoint location, int renderDistance) {
			this.location = location;
			this.renderDistance = renderDistance;
		}
	}
}
