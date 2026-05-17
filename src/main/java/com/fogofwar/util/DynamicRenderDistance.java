package com.fogofwar.util;
import com.fogofwar.FogOfWarConfig;
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
public class DynamicRenderDistance extends LifecycleComponent {
	private final Client client;
	private final FogOfWarConfig config;
	@Getter
	private int currentRenderDistance;
	@Inject
	public DynamicRenderDistance(Client client, FogOfWarConfig config, EventBus eventBus) {
		super(eventBus);
		this.client = client;
		this.config = config;
		this.currentRenderDistance = config.renderDistanceRadius();
	}
	@Override
	protected void onStop(boolean wasStarted) { currentRenderDistance = config.renderDistanceRadius(); }
	@Subscribe
	@SuppressWarnings("unused")
	public void onGameTick(GameTick event) {
		int maxRadius = config.renderDistanceRadius();
		if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null || !config.enableDynamicRenderDistance()) {
			this.currentRenderDistance = maxRadius;
			return;
		}
		WorldView worldView = client.getTopLevelWorldView();
		int count = Players.count(worldView);
		if (count < config.dynamicRenderDistancePlayerThreshold()) {
			this.currentRenderDistance = maxRadius;
			return;
		}
		WorldPoint playerWp = client.getLocalPlayer().getWorldLocation();
		int px = playerWp.getX(), py = playerWp.getY();
		int maxDist = 0;
		for (Player p : worldView.players()) {
			if (p == null) continue;
			WorldPoint wp = p.getWorldLocation();
			if (wp == null) continue;
			int dist = Math.max(Math.abs(wp.getX() - px), Math.abs(wp.getY() - py));
			if (dist > maxDist) maxDist = Math.min(dist, maxRadius);
		}
		this.currentRenderDistance = Math.max(1, maxDist);
	}
}
