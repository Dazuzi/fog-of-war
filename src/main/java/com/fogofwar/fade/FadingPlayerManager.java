package com.fogofwar.fade;

import com.fogofwar.FogOfWarConfig;
import com.fogofwar.util.AreaManager;
import com.fogofwar.util.DynamicRenderDistance;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Singleton
public class FadingPlayerManager {
	@Inject
	@SuppressWarnings("unused")
	private Client client;
	@Inject
	@SuppressWarnings("unused")
	private FogOfWarConfig config;
	@Inject
	@SuppressWarnings("unused")
	private OverlayManager overlayManager;
	@Inject
	@SuppressWarnings("unused")
	private EventBus eventBus;
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
	@Getter
	private final Map<Player, FadingPlayer> fadingPlayers = new HashMap<>();
	private final Map<Player, WorldPoint> lastTickPlayerLocations = new HashMap<>();
	private final Map<Player, WorldPoint> twoTicksAgoPlayerLocations = new HashMap<>();
	public void start() {
		overlayManager.add(fadingPlayerOverlay);
		overlayManager.add(fadingPlayerMinimapOverlay);
		eventBus.register(this);
	}
	public void stop() {
		overlayManager.remove(fadingPlayerOverlay);
		overlayManager.remove(fadingPlayerMinimapOverlay);
		eventBus.unregister(this);
		clearAllTracking();
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState() == GameState.LOADING) clearAllTracking();
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onGameTick(GameTick event) {
		if (client.getGameState() != GameState.LOGGED_IN) {
			clearAllTracking();
			return;
		}
		if (areaManager.isPlayerInExcludedArea()) {
			if (!fadingPlayers.isEmpty()) {
				clearAllTracking();
			}
			return;
		}
		if (config.enableFadingPlayers()) {
			handleFadingPlayers();
			updatePlayerTracking();
		} else {
			clearAllTracking();
		}
	}
	private void clearAllTracking() {
		fadingPlayers.clear();
		lastTickPlayerLocations.clear();
		twoTicksAgoPlayerLocations.clear();
	}
	private void handleFadingPlayers() {
		fadingPlayers.entrySet().removeIf(entry -> {
			FadingPlayer fp = entry.getValue();
			fp.setTicksSinceDisappeared(fp.getTicksSinceDisappeared() + 1);
			if (fp.getTicksSinceDisappeared() > config.fadeDuration()) return true;
			WorldPoint localPlayerLocation = client.getLocalPlayer().getWorldLocation();
			if (fp.getTicksSinceDisappeared() > 1 && fp.getLastLocation().distanceTo(localPlayerLocation) <= dynamicRenderDistance.getCurrentRenderDistance()) {
				return true;
			}
			if (config.extrapolateMovement()) {
				WorldPoint nextPos = new WorldPoint(
						fp.getLastLocation().getX() + fp.getVelocity().getX(),
						fp.getLastLocation().getY() + fp.getVelocity().getY(),
						fp.getLastLocation().getPlane()
				);
				fp.setLastLocation(nextPos);
			}
			return false;
		});
	}
	private void updatePlayerTracking() {
		Map<Player, WorldPoint> currentPlayerLocations = new HashMap<>();
		Set<String> currentPlayerNames = new HashSet<>();
		for (Player player : client.getTopLevelWorldView().players()) {
			if (player != null && player != client.getLocalPlayer()) {
				currentPlayerLocations.put(player, player.getWorldLocation());
				if (player.getName() != null) {
					currentPlayerNames.add(player.getName());
				}
			}
		}
		Set<Player> disappearedPlayers = new HashSet<>(lastTickPlayerLocations.keySet());
		disappearedPlayers.removeAll(currentPlayerLocations.keySet());
		WorldPoint currentLocalPlayerLocation = client.getLocalPlayer().getWorldLocation();
		for (Player player : disappearedPlayers) {
			if (fadingPlayers.containsKey(player)) continue;
			WorldPoint lastLocation = lastTickPlayerLocations.get(player);
			if (lastLocation == null) continue;
			WorldPoint twoTicksAgoLocation = twoTicksAgoPlayerLocations.get(player);
			WorldPoint velocity = (twoTicksAgoLocation != null)
					? new WorldPoint(lastLocation.getX() - twoTicksAgoLocation.getX(), lastLocation.getY() - twoTicksAgoLocation.getY(), 0)
					: new WorldPoint(0, 0, 0);
			int renderDistance = dynamicRenderDistance.getCurrentRenderDistance();
			int velocityMagnitude = Math.abs(velocity.getX()) + Math.abs(velocity.getY());
			int distanceFromPlayer = lastLocation.distanceTo(currentLocalPlayerLocation);
			boolean isAtRenderLimit = distanceFromPlayer >= renderDistance - 1;
			boolean isRunningNearLimit = distanceFromPlayer >= renderDistance - 2 && velocityMagnitude >= 2;
			if (config.onlyFadeAtRenderLimit()) {
				if (!isAtRenderLimit && !isRunningNearLimit) {
					continue;
				}
			}
			WorldPoint initialFadeLocation;
			if (config.extrapolateMovement()) {
				int dx = lastLocation.getX() - currentLocalPlayerLocation.getX();
				int dy = lastLocation.getY() - currentLocalPlayerLocation.getY();
				boolean onXEdge = Math.abs(dx) >= renderDistance - 1;
				boolean onYEdge = Math.abs(dy) >= renderDistance - 1;
				WorldPoint predictedNextLocation = new WorldPoint(lastLocation.getX() + velocity.getX(), lastLocation.getY() + velocity.getY(), lastLocation.getPlane());
				boolean isMovingIntoRenderArea = (onXEdge && dx * velocity.getX() < 0) || (onYEdge && dy * velocity.getY() < 0);
				if (isMovingIntoRenderArea) {
					int pushX = onXEdge ? Integer.signum(dx) : 0;
					int pushY = onYEdge ? Integer.signum(dy) : 0;
					initialFadeLocation = new WorldPoint(predictedNextLocation.getX() + pushX, predictedNextLocation.getY() + pushY, predictedNextLocation.getPlane());
				} else {
					initialFadeLocation = predictedNextLocation;
					if (isAtRenderLimit || isRunningNearLimit) {
						while (initialFadeLocation.distanceTo(currentLocalPlayerLocation) <= renderDistance) {
							int pushX = 0;
							int pushY = 0;
							if (Math.abs(dx) > Math.abs(dy)) {
								pushX = Integer.signum(dx);
							} else if (Math.abs(dy) > Math.abs(dx)) {
								pushY = Integer.signum(dy);
							} else {
								pushX = Integer.signum(dx);
								pushY = Integer.signum(dy);
							}
							initialFadeLocation = new WorldPoint(initialFadeLocation.getX() + pushX, initialFadeLocation.getY() + pushY, initialFadeLocation.getPlane());
						}
					}
				}
			} else {
				initialFadeLocation = lastLocation;
			}
			FadingPlayer fp = new FadingPlayer(player, velocity);
			fp.setLastLocation(initialFadeLocation);
			fadingPlayers.put(player, fp);
		}
		twoTicksAgoPlayerLocations.clear();
		twoTicksAgoPlayerLocations.putAll(lastTickPlayerLocations);
		lastTickPlayerLocations.clear();
		lastTickPlayerLocations.putAll(currentPlayerLocations);
		if (!fadingPlayers.isEmpty() && !currentPlayerNames.isEmpty()) {
			fadingPlayers.entrySet().removeIf(entry -> {
				Player fadingPlayer = entry.getKey();
				return fadingPlayer.getName() != null && currentPlayerNames.contains(fadingPlayer.getName());
			});
		}
	}
}