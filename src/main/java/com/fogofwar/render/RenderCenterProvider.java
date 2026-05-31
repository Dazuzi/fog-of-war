package com.fogofwar.render;
import com.fogofwar.lifecycle.LifecycleComponent;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.WorldViewLoaded;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import javax.inject.Inject;
import javax.inject.Singleton;
@Singleton
public class RenderCenterProvider extends LifecycleComponent {
	private final Client client;
	private RenderCenter current;
	private boolean resolved;
	private WorldView topWorldView;
	@Inject
	public RenderCenterProvider(Client client, EventBus eventBus) {
		super(eventBus);
		this.client = client;
	}
	public RenderCenter get() {
		if (!resolved) {
			current = RenderCenter.resolve(client, getTopLevelWorldView());
			resolved = true;
		}
		return current;
	}
	public WorldView getTopLevelWorldView() {
		Player player = client.getLocalPlayer();
		WorldView worldView = player != null ? player.getWorldView() : null;
		if (worldView != null && worldView.isTopLevel()) topWorldView = worldView;
		else if (topWorldView == null) topWorldView = client.getTopLevelWorldView();
		return topWorldView;
	}
	public void clear() {
		current = null;
		resolved = false;
	}
	private void clearAll() {
		clear();
		topWorldView = null;
	}
	@Override
	protected void onStop() { clearAll(); }
	@Subscribe
	@SuppressWarnings("unused")
	public void onBeforeRender(BeforeRender event) { clear(); }
	@Subscribe
	@SuppressWarnings("unused")
	public void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState() != GameState.LOGGED_IN) clearAll();
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onWorldViewLoaded(WorldViewLoaded event) {
		if (!event.getWorldView().isTopLevel()) return;
		topWorldView = event.getWorldView();
		clear();
	}
}
