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
public class DynamicRenderDistance {
	private final Client client;
	private final FogOfWarConfig config;
	private final EventBus eventBus;
	@Getter
	private int currentRenderDistance;
	private boolean started;
	@Inject
	public DynamicRenderDistance(Client client, FogOfWarConfig config, EventBus eventBus) {
		this.client = client;
		this.config = config;
		this.eventBus = eventBus;
		this.currentRenderDistance = config.renderDistanceRadius();
	}
	public void start() {
		if (started) return;
		eventBus.register(this);
		started = true;
	}
	public void stop() {
		if (started) {
			eventBus.unregister(this);
			started = false;
		}
		currentRenderDistance = config.renderDistanceRadius();
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onGameTick(GameTick event) {
		int maxRadius = config.renderDistanceRadius();
		if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null || !config.enableDynamicRenderDistance()) {
			this.currentRenderDistance = maxRadius;
			return;
		}
		WorldView worldView = client.getTopLevelWorldView();
		WorldPoint playerWp = client.getLocalPlayer().getWorldLocation();
		int px = playerWp.getX(), py = playerWp.getY();
		int count = 0, maxDist = 0;
		for (Player p : worldView.players()) {
			if (p == null) continue;
			count++;
			WorldPoint wp = p.getWorldLocation();
			if (wp == null) continue;
			int dist = Math.max(Math.abs(wp.getX() - px), Math.abs(wp.getY() - py));
			if (dist > maxDist) maxDist = Math.min(dist, maxRadius);
		}
		this.currentRenderDistance = count >= config.dynamicRenderDistancePlayerThreshold() ? Math.max(1, maxDist) : maxRadius;
	}
}
