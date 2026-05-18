package com.fogofwar.state;
import com.fogofwar.lifecycle.LifecycleComponent;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
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
	protected void onStop(boolean wasStarted) { playerInExcludedArea = false; }
	@Subscribe
	@SuppressWarnings("unused")
	public void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState() == GameState.LOGGED_IN) checkArea();
		else if (event.getGameState() == GameState.LOADING) playerInExcludedArea = false;
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onGameTick(GameTick event) { checkArea(); }
	private void checkArea() {
		if (client.getLocalPlayer() == null) {
			playerInExcludedArea = false;
			return;
		}
		LocalPoint localPoint = client.getLocalPlayer().getLocalLocation();
		if (localPoint == null) {
			playerInExcludedArea = false;
			return;
		}
		WorldPoint playerPoint = WorldPoint.fromLocalInstance(client, localPoint);
		if (playerPoint == null) {
			playerInExcludedArea = false;
			return;
		}
		for (ExcludedArea area : EXCLUDED_AREAS) {
			if (area.contains(playerPoint)) {
				playerInExcludedArea = true;
				return;
			}
		}
		playerInExcludedArea = false;
	}
}
