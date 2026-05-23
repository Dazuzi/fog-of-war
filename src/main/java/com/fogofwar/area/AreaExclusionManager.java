package com.fogofwar.area;
import com.fogofwar.lifecycle.LifecycleComponent;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
@Singleton
public class AreaExclusionManager extends LifecycleComponent {
	private static final List<ExcludedArea> EXCLUDED_AREAS = List.of(
			new ExcludedArea(2367, 5053, 2432, 5119, 0),			// TzHaar Fight Cave
			new ExcludedArea(2256, 5328, 2286, 5359, 0),			// Inferno
			new ExcludedArea(3500, 5100, 4000, 5440, 0, 1),		// Tombs of Amascut
			new ExcludedArea(3136, 4216, 3366, 4474, 0, 1, 2),	// Theatre of Blood
			new ExcludedArea(2215, 5935, 2325, 6035, 0, 1, 2),	// Hallowed Sepulchre Floor 1
			new ExcludedArea(2475, 5935, 2585, 6035, 0, 1, 2),	// Hallowed Sepulchre Floor 2
			new ExcludedArea(2225, 5795, 2575, 5915, 0, 1, 2),	// Hallowed Sepulchre Floors 3-5
			new ExcludedArea(3150, 5690, 3380, 5770, 0, 1, 2),	// Chambers of Xeric
			new ExcludedArea(3250, 5120, 3370, 5700, 0, 1, 2)	// Chambers of Xeric
	);
	private final Client client;
	@Getter
	private boolean playerInExcludedArea = false;
	@Setter
	private Runnable onTransition;
	@Inject
	public AreaExclusionManager(Client client, EventBus eventBus) {
		super(eventBus);
		this.client = client;
	}
	@Override
	protected void onStart() {
		if (client.getGameState() == GameState.LOGGED_IN) checkArea();
	}
	@Override
	protected void onStop() { setPlayerInExcludedArea(false); }
	@Subscribe
	@SuppressWarnings("unused")
	public void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState() == GameState.LOGGED_IN) checkArea();
		else if (event.getGameState() == GameState.LOADING) setPlayerInExcludedArea(false);
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onGameTick(GameTick event) { checkArea(); }
	private void checkArea() {
		WorldPoint playerPoint = currentPlayerWorldPoint();
		setPlayerInExcludedArea(playerPoint != null && isExcludedArea(playerPoint));
	}
	private boolean isExcludedArea(WorldPoint playerPoint) {
		for (ExcludedArea area : EXCLUDED_AREAS) { if (area.contains(playerPoint)) return true; }
		return false;
	}
	private void setPlayerInExcludedArea(boolean value) {
		if (playerInExcludedArea == value) return;
		playerInExcludedArea = value;
		if (onTransition != null) onTransition.run();
	}
	private WorldPoint currentPlayerWorldPoint() {
		Player player = client.getLocalPlayer();
		if (player == null) return null;
		LocalPoint localPoint = player.getLocalLocation();
		if (localPoint == null) return null;
		return WorldPoint.fromLocalInstance(client, localPoint);
	}
	static final class ExcludedArea {
		private final int minX;
		private final int minY;
		private final int maxX;
		private final int maxY;
		private final int planeMask;
		ExcludedArea(int minX, int minY, int maxX, int maxY, int... planes) {
			this.minX = minX;
			this.minY = minY;
			this.maxX = maxX;
			this.maxY = maxY;
			this.planeMask = planeMask(planes);
		}
		boolean contains(WorldPoint point) {
			int plane = point.getPlane();
			return plane >= 0 && plane < Integer.SIZE && (planeMask & (1 << plane)) != 0 &&
					point.getX() >= minX && point.getX() <= maxX &&
					point.getY() >= minY && point.getY() <= maxY;
		}
		private static int planeMask(int... planes) {
			int mask = 0;
			for (int plane : planes) { if (plane >= 0 && plane < Integer.SIZE) mask |= 1 << plane; }
			return mask;
		}
	}
}
