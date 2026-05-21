package com.fogofwar.state;
import com.fogofwar.lifecycle.LifecycleComponent;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import javax.inject.Inject;
import javax.inject.Singleton;
@Singleton
public class RenderCenterProvider extends LifecycleComponent {
	private final Client client;
	private RenderCenter current;
	private boolean resolved;
	@Inject
	public RenderCenterProvider(Client client, EventBus eventBus) {
		super(eventBus);
		this.client = client;
	}
	public RenderCenter get() {
		if (!resolved) {
			current = RenderCenter.resolve(client);
			resolved = true;
		}
		return current;
	}
	public void clear() {
		current = null;
		resolved = false;
	}
	@Override
	protected void onStop() { clear(); }
	@Subscribe
	@SuppressWarnings("unused")
	public void onBeforeRender(BeforeRender event) { clear(); }
	@Subscribe
	@SuppressWarnings("unused")
	public void onGameStateChanged(GameStateChanged event) {
		if (event.getGameState() != GameState.LOGGED_IN) clear();
	}
}
