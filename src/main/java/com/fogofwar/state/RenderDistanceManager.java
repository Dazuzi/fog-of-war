package com.fogofwar.state;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.lifecycle.LifecycleComponent;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import javax.inject.Inject;
import javax.inject.Singleton;
@Singleton
public class RenderDistanceManager extends LifecycleComponent {
	private final Client client;
	private final FogOfWarConfig config;
	@Getter
	private int currentRenderDistance;
	@Inject
	public RenderDistanceManager(Client client, FogOfWarConfig config, EventBus eventBus) {
		super(eventBus);
		this.client = client;
		this.config = config;
		this.currentRenderDistance = config.landRenderDistance();
	}
	@Override
	protected void onStop(boolean wasStarted) { currentRenderDistance = config.landRenderDistance(); }
	@Subscribe
	@SuppressWarnings("unused")
	public void onGameTick(GameTick event) {
		int maxRadius = config.landRenderDistance();
		Player localPlayer = client.getLocalPlayer();
		if (client.getGameState() != GameState.LOGGED_IN || localPlayer == null || !config.dynamicRenderDistanceEnabled()) {
			this.currentRenderDistance = maxRadius;
			return;
		}
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null) {
			this.currentRenderDistance = maxRadius;
			return;
		}
		int count = Players.count(worldView);
		if (count < config.dynamicRenderDistanceThreshold()) {
			this.currentRenderDistance = maxRadius;
			return;
		}
		WorldPoint playerWp = localPlayer.getWorldLocation();
		if (playerWp == null) {
			this.currentRenderDistance = maxRadius;
			return;
		}
		int px = playerWp.getX(), py = playerWp.getY();
		int maxDist = 0;
		for (Player p : worldView.players()) {
			if (p == null) continue;
			WorldPoint wp = p.getWorldLocation();
			if (wp == null) continue;
			int dist = Math.max(Math.abs(wp.getX() - px), Math.abs(wp.getY() - py));
			if (dist > maxDist) maxDist = dist;
		}
		this.currentRenderDistance = Math.max(1, Math.min(maxDist, maxRadius));
	}
}
