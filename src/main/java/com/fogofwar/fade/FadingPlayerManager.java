package com.fogofwar.fade;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.area.AreaExclusionManager;
import com.fogofwar.coord.WorldEntityCoords;
import com.fogofwar.lifecycle.LifecycleComponent;
import com.fogofwar.render.RenderCenterProvider;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
@Singleton
public class FadingPlayerManager extends LifecycleComponent {
	private static final Map<Player, FadingPlayer> EMPTY_FADING = Collections.emptyMap();
	private static final Set<String> EMPTY_NAMES = Collections.emptySet();
	private final Client client;
	private final FogOfWarConfig config;
	private final AreaExclusionManager areaExclusionManager;
	private final RenderCenterProvider renderCenterProvider;
	@Getter
	private Map<Player, FadingPlayer> fadingPlayers = EMPTY_FADING;
	private Map<Player, TrackedPlayer> lastTickPlayerLocations = Collections.emptyMap();
	private Map<Player, TrackedPlayer> twoTicksAgoPlayerLocations = Collections.emptyMap();
	private Map<Player, TrackedPlayer> currentPlayerLocations = Collections.emptyMap();
	private Set<String> currentPlayerNames = EMPTY_NAMES;
	@Inject
	public FadingPlayerManager(Client client, FogOfWarConfig config, EventBus eventBus, AreaExclusionManager areaExclusionManager, RenderCenterProvider renderCenterProvider) {
		super(eventBus);
		this.client = client;
		this.config = config;
		this.areaExclusionManager = areaExclusionManager;
		this.renderCenterProvider = renderCenterProvider;
	}
	@Override
	protected void onStart() {
		fadingPlayers = EMPTY_FADING;
		lastTickPlayerLocations = new HashMap<>();
		twoTicksAgoPlayerLocations = new HashMap<>();
		currentPlayerLocations = new HashMap<>();
		currentPlayerNames = EMPTY_NAMES;
	}
	@Override
	protected void onStop() {
		fadingPlayers = EMPTY_FADING;
		lastTickPlayerLocations = Collections.emptyMap();
		twoTicksAgoPlayerLocations = Collections.emptyMap();
		currentPlayerLocations = Collections.emptyMap();
		currentPlayerNames = EMPTY_NAMES;
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onGameTick(GameTick event) {
		if (client.getGameState() != GameState.LOGGED_IN) {
			clearAllTracking();
			return;
		}
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null) {
			clearAllTracking();
			return;
		}
		WorldView worldView = renderCenterProvider.getTopLevelWorldView(localPlayer);
		if (worldView == null) {
			clearAllTracking();
			return;
		}
		WorldPoint localPlayerLocation = WorldEntityCoords.playerToTopLevel(localPlayer, null, worldView);
		if (localPlayerLocation == null) {
			clearAllTracking();
			return;
		}
		if (areaExclusionManager.isPlayerInExcludedArea()) {
			clearAllTracking();
			return;
		}
		boolean onWorldEntity = WorldEntityCoords.isPlayerOnShip(localPlayer, worldView);
		if (config.disableWhileSailing() && onWorldEntity) {
			clearAllTracking();
			return;
		}
		int fadeDuration = config.fadeDurationTicks();
		boolean extrapolate = config.predictMovement();
		boolean onlyAtLimit = config.onlyFadeAtRenderEdge();
		handleFadingPlayers(fadeDuration, extrapolate, localPlayerLocation);
		updatePlayerTracking(extrapolate, onlyAtLimit, localPlayer, worldView, localPlayerLocation, onWorldEntity);
	}
	private void clearAllTracking() {
		fadingPlayers = EMPTY_FADING;
		lastTickPlayerLocations.clear();
		twoTicksAgoPlayerLocations.clear();
		currentPlayerLocations.clear();
		currentPlayerNames = EMPTY_NAMES;
	}
	private void handleFadingPlayers(int fadeDuration, boolean extrapolate, WorldPoint localPlayerLocation) {
		if (fadingPlayers == EMPTY_FADING) return;
		fadingPlayers.entrySet().removeIf(entry -> {
			FadingPlayer fp = entry.getValue();
			fp.setTicksSinceDisappeared(fp.getTicksSinceDisappeared() + 1);
			if (fp.getTicksSinceDisappeared() >= fadeDuration) return true;
			WorldPoint markerLocation = fp.getMarkerLocation();
			if (fp.getTicksSinceDisappeared() > 1 && markerLocation.distanceTo(localPlayerLocation) <= fp.getRenderDistance()) return true;
			WorldPoint velocity = fp.getVelocity();
			if (extrapolate && velocity != null && (velocity.getX() != 0 || velocity.getY() != 0)) {
				fp.setMarkerLocation(new WorldPoint(
						markerLocation.getX() + velocity.getX(),
						markerLocation.getY() + velocity.getY(),
						markerLocation.getPlane()));
			}
			return false;
		});
		if (fadingPlayers.isEmpty()) fadingPlayers = EMPTY_FADING;
	}
	private void updatePlayerTracking(boolean extrapolate, boolean onlyAtLimit, Player localPlayer, WorldView worldView, WorldPoint localPlayerLocation, boolean onWorldEntity) {
		currentPlayerLocations.clear();
		boolean collectNames = !fadingPlayers.isEmpty();
		if (collectNames) {
			if (currentPlayerNames == EMPTY_NAMES) currentPlayerNames = new HashSet<>();
			else currentPlayerNames.clear();
		} else currentPlayerNames = EMPTY_NAMES;
		int landRenderDistance = config.landRenderDistance();
		trackPlayers(localPlayer, worldView, worldView, landRenderDistance, collectNames);
		int boatRenderDistance = onWorldEntity ? config.sailingRenderDistance() : landRenderDistance;
		for (WorldEntity worldEntity : worldView.worldEntities()) {
			if (worldEntity == null) continue;
			WorldView entityWorldView = worldEntity.getWorldView();
			if (entityWorldView == null) continue;
			trackPlayers(localPlayer, entityWorldView, worldView, boatRenderDistance, collectNames);
		}
		boolean needsVelocity = onlyAtLimit || extrapolate;
		for (Map.Entry<Player, TrackedPlayer> entry : lastTickPlayerLocations.entrySet()) {
			Player player = entry.getKey();
			if (currentPlayerLocations.containsKey(player)) continue;
			if (fadingPlayers.containsKey(player)) continue;
			TrackedPlayer lastTickPlayer = entry.getValue();
			WorldPoint lastLocation = lastTickPlayer.getLocation();
			if (lastLocation == null) continue;
			int renderDistance = lastTickPlayer.getRenderDistance();
			WorldPoint velocity = null;
			boolean nearRenderLimit = false;
			if (needsVelocity) {
				TrackedPlayer twoTicksAgoPlayer = twoTicksAgoPlayerLocations.get(player);
				WorldPoint twoTicksAgoLocation = twoTicksAgoPlayer != null ? twoTicksAgoPlayer.getLocation() : null;
				velocity = FadingPlayerPredictor.getVelocity(lastLocation, twoTicksAgoLocation);
				nearRenderLimit = FadingPlayerPredictor.isNearRenderLimit(lastLocation, localPlayerLocation, velocity, renderDistance);
			}
			if (onlyAtLimit && !nearRenderLimit) continue;
			WorldPoint initialFadeLocation = extrapolate ? FadingPlayerPredictor.getInitialFadeLocation(lastLocation, localPlayerLocation, velocity, true, renderDistance, nearRenderLimit) : lastLocation;
			if (fadingPlayers == EMPTY_FADING) fadingPlayers = new HashMap<>();
			fadingPlayers.put(player, new FadingPlayer(player, velocity, initialFadeLocation, renderDistance));
		}
		Map<Player, TrackedPlayer> tmp = twoTicksAgoPlayerLocations;
		twoTicksAgoPlayerLocations = lastTickPlayerLocations;
		lastTickPlayerLocations = currentPlayerLocations;
		currentPlayerLocations = tmp;
		if (!collectNames && !fadingPlayers.isEmpty()) collectCurrentPlayerNames(localPlayer, worldView);
		if (fadingPlayers != EMPTY_FADING) fadingPlayers.entrySet().removeIf(entry -> {
			String name = entry.getKey().getName();
			return name != null && currentPlayerNames.contains(name);
		});
		if (fadingPlayers.isEmpty()) fadingPlayers = EMPTY_FADING;
	}
	private void trackPlayers(Player localPlayer, WorldView worldView, WorldView topWorldView, int renderDistance, boolean collectNames) {
		for (Player player : worldView.players()) {
			if (player == null || player == localPlayer) continue;
			WorldPoint playerLocation = WorldEntityCoords.playerToTopLevel(player, worldView, topWorldView);
			if (playerLocation != null) currentPlayerLocations.put(player, new TrackedPlayer(playerLocation, renderDistance));
			if (collectNames) addPlayerName(player);
		}
	}
	private void collectCurrentPlayerNames(Player localPlayer, WorldView worldView) {
		if (currentPlayerNames == EMPTY_NAMES) currentPlayerNames = new HashSet<>();
		else currentPlayerNames.clear();
		collectPlayerNames(localPlayer, worldView);
		for (WorldEntity worldEntity : worldView.worldEntities()) {
			if (worldEntity == null) continue;
			WorldView entityWorldView = worldEntity.getWorldView();
			if (entityWorldView != null) collectPlayerNames(localPlayer, entityWorldView);
		}
	}
	private void collectPlayerNames(Player localPlayer, WorldView worldView) {
		for (Player player : worldView.players()) { if (player != null && player != localPlayer) addPlayerName(player); }
	}
	private void addPlayerName(Player player) {
		String name = player.getName();
		if (name != null) currentPlayerNames.add(name);
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
