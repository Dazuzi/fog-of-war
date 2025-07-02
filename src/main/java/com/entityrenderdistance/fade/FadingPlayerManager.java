package com.entityrenderdistance.fade;
import com.entityrenderdistance.EntityRenderDistanceConfig;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
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
@Singleton
public class FadingPlayerManager {
	@Inject
	private Client client;
	@Inject
	private EntityRenderDistanceConfig config;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private EventBus eventBus;
	@Inject
	private FadingPlayerOverlay fadingPlayerOverlay;
	@Inject
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
	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState() == GameState.LOADING) {
			clearAllTracking();
		}
	}
	@Subscribe
	public void onGameTick(GameTick event) {
		if (client.getGameState() != GameState.LOGGED_IN) {
			clearAllTracking();
			return;
		}
		if (!config.enableFadingPlayers()) {
			clearAllTracking();
			return;
		}
		handleFadingPlayers();
		updatePlayerTracking();
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
			if (fp.getTicksSinceDisappeared() >= config.fadeDuration()) {
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
	private boolean isOnRenderDistanceBoundary(WorldPoint playerLocation, WorldPoint referenceLocation) {
		int dx = Math.abs(playerLocation.getX() - referenceLocation.getX());
		int dy = Math.abs(playerLocation.getY() - referenceLocation.getY());
		int renderRadius = config.renderDistanceRadius();
		return dx == renderRadius || dy == renderRadius;
	}
	private void updatePlayerTracking() {
		Map<Player, WorldPoint> currentPlayerLocations = new HashMap<>();
		Set<String> currentPlayerNames = new HashSet<>();
		for (Player player : client.getPlayers()) {
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
		boolean localPlayerMoved = lastTickLocalPlayerLocation != null &&
				!lastTickLocalPlayerLocation.equals(currentLocalPlayerLocation);
		for (Player player : disappearedPlayers) {
			if (fadingPlayers.containsKey(player)) continue;
			WorldPoint lastLocation = lastTickPlayerLocations.get(player);
			if (lastLocation == null) continue;
			WorldPoint twoTicksAgoLocation = twoTicksAgoPlayerLocations.get(player);
			WorldPoint velocity = (twoTicksAgoLocation != null)
					? new WorldPoint(lastLocation.getX() - twoTicksAgoLocation.getX(), lastLocation.getY() - twoTicksAgoLocation.getY(), 0)
					: new WorldPoint(0, 0, 0);
			WorldPoint initialFadeLocation;
			if (!localPlayerMoved) {
				WorldPoint referencePlayerLocation = lastTickLocalPlayerLocation != null ? lastTickLocalPlayerLocation : currentLocalPlayerLocation;
				if (isOnRenderDistanceBoundary(lastLocation, referencePlayerLocation)) {
					int dx = lastLocation.getX() - referencePlayerLocation.getX();
					int dy = lastLocation.getY() - referencePlayerLocation.getY();
					int absDx = Math.abs(dx);
					int absDy = Math.abs(dy);
					int pushX = 0;
					int pushY = 0;
					if (absDx > absDy) {
						pushX = Integer.signum(dx);
					} else if (absDy > absDx) {
						pushY = Integer.signum(dy);
					} else {
						pushX = Integer.signum(dx);
						pushY = Integer.signum(dy);
					}
					initialFadeLocation = new WorldPoint(lastLocation.getX() + pushX, lastLocation.getY() + pushY, lastLocation.getPlane());
				} else {
					initialFadeLocation = lastLocation;
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
		lastTickLocalPlayerLocation = client.getLocalPlayer().getWorldLocation();
		if (!fadingPlayers.isEmpty() && !currentPlayerNames.isEmpty()) {
			fadingPlayers.entrySet().removeIf(entry -> {
				Player fadingPlayer = entry.getKey();
				return fadingPlayer.getName() != null && currentPlayerNames.contains(fadingPlayer.getName());
			});
		}
	}
}