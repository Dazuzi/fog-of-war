package com.entityrenderdistance.fade;

import com.entityrenderdistance.EntityRenderDistanceConfig;
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
	private EntityRenderDistanceConfig config;
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
	@Getter
	private final Map<Player, FadingPlayer> fadingPlayers = new HashMap<>();
	private final Map<Player, WorldPoint> lastTickPlayerLocations = new HashMap<>();
	private final Map<Player, WorldPoint> twoTicksAgoPlayerLocations = new HashMap<>();
	private WorldPoint lastTickLocalPlayerLocation;
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
	@SuppressWarnings("unused")
	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState() == GameState.LOADING) clearAllTracking();
	}
	@SuppressWarnings("unused")
	@Subscribe
	public void onGameTick(GameTick event) {
		if (client.getGameState() != GameState.LOGGED_IN) {
			clearAllTracking();
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
		lastTickLocalPlayerLocation = null;
	}
	private void handleFadingPlayers() {
		fadingPlayers.entrySet().removeIf(entry -> {
			FadingPlayer fp = entry.getValue();
			fp.setTicksSinceDisappeared(fp.getTicksSinceDisappeared() + 1);
			if (fp.getTicksSinceDisappeared() > config.fadeDuration()) return true;
			WorldPoint localPlayerLocation = client.getLocalPlayer().getWorldLocation();
			if (fp.getTicksSinceDisappeared() > 1 && fp.getLastLocation().distanceTo(localPlayerLocation) <= config.renderDistanceRadius()) {
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
		WorldPoint localPlayerVelocity = (lastTickLocalPlayerLocation != null)
				? new WorldPoint(currentLocalPlayerLocation.getX() - lastTickLocalPlayerLocation.getX(), currentLocalPlayerLocation.getY() - lastTickLocalPlayerLocation.getY(), 0)
				: new WorldPoint(0, 0, 0);
		for (Player player : disappearedPlayers) {
			if (fadingPlayers.containsKey(player)) continue;
			WorldPoint lastLocation = lastTickPlayerLocations.get(player);
			if (lastLocation == null) continue;
			WorldPoint twoTicksAgoLocation = twoTicksAgoPlayerLocations.get(player);
			WorldPoint velocity = (twoTicksAgoLocation != null)
					? new WorldPoint(lastLocation.getX() - twoTicksAgoLocation.getX(), lastLocation.getY() - twoTicksAgoLocation.getY(), 0)
					: new WorldPoint(0, 0, 0);
			WorldPoint initialFadeLocation;
			int velocityMagnitude = Math.abs(velocity.getX()) + Math.abs(velocity.getY());
			boolean wasOnRenderEdge = lastLocation.distanceTo(currentLocalPlayerLocation) >= config.renderDistanceRadius() - 1;
			WorldPoint predictedNextLocation = new WorldPoint(lastLocation.getX() + velocity.getX(), lastLocation.getY() + velocity.getY(), lastLocation.getPlane());
			boolean isPredictedInsideRender = predictedNextLocation.distanceTo(currentLocalPlayerLocation) <= config.renderDistanceRadius();
			boolean isWallHuggingCase = isPredictedInsideRender && wasOnRenderEdge && velocityMagnitude > 0;
			if (isWallHuggingCase) {
				int dx = lastLocation.getX() - currentLocalPlayerLocation.getX();
				int dy = lastLocation.getY() - currentLocalPlayerLocation.getY();
				int pushX = (Math.abs(dx) >= config.renderDistanceRadius() - 1) ? Integer.signum(dx) : 0;
				int pushY = (Math.abs(dy) >= config.renderDistanceRadius() - 1) ? Integer.signum(dy) : 0;
				initialFadeLocation = new WorldPoint(predictedNextLocation.getX() + pushX, predictedNextLocation.getY() + pushY, predictedNextLocation.getPlane());
			} else if (velocityMagnitude > 0) {
				initialFadeLocation = predictedNextLocation;
			} else {
				int localVelocityMagnitude = Math.abs(localPlayerVelocity.getX()) + Math.abs(localPlayerVelocity.getY());
				if (localVelocityMagnitude == 0) {
					int dx = lastLocation.getX() - currentLocalPlayerLocation.getX();
					int dy = lastLocation.getY() - currentLocalPlayerLocation.getY();
					int absDx = Math.abs(dx);
					int absDy = Math.abs(dy);
					int pushX = 0;
					int pushY = 0;
					if (absDx > absDy) {
						pushX = Integer.signum(dx);
					} else if (absDy > absDx) {
						pushY = Integer.signum(dy);
					} else if (absDx > 0) {
						pushX = Integer.signum(dx);
						pushY = Integer.signum(dy);
					}
					initialFadeLocation = new WorldPoint(lastLocation.getX() + pushX, lastLocation.getY() + pushY, lastLocation.getPlane());
				} else {
					initialFadeLocation = lastLocation;
				}
			}
			if (config.onlyFadeAtRenderLimit()) {
				if (initialFadeLocation.distanceTo(currentLocalPlayerLocation) <= config.renderDistanceRadius()) {
					continue;
				}
			}
			FadingPlayer fp = new FadingPlayer(player, velocity);
			fp.setLastLocation(initialFadeLocation);
			fadingPlayers.put(player, fp);
		}
		twoTicksAgoPlayerLocations.clear();
		twoTicksAgoPlayerLocations.putAll(lastTickPlayerLocations);
		lastTickPlayerLocations.clear();
		lastTickPlayerLocations.putAll(currentPlayerLocations);
		lastTickLocalPlayerLocation = client.getLocalPlayer().getWorldLocation();
		if (!fadingPlayers.isEmpty() && !currentPlayerNames.isEmpty()) {
			fadingPlayers.entrySet().removeIf(entry -> {
				Player fadingPlayer = entry.getKey();
				return fadingPlayer.getName() != null && currentPlayerNames.contains(fadingPlayer.getName());
			});
		}
	}
}