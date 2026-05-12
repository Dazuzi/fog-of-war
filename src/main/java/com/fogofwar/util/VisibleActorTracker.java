package com.fogofwar.util;
import lombok.Getter;
import net.runelite.api.Actor;
import net.runelite.api.Renderable;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.RenderCallback;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
@Singleton
public class VisibleActorTracker implements RenderCallback {
	private final EventBus eventBus;
	private final RenderCallbackManager renderCallbackManager;
	@Getter
	private final Set<Actor> visibleActors = Collections.newSetFromMap(new IdentityHashMap<>(256));
	private boolean started;
	@Inject
	public VisibleActorTracker(EventBus eventBus, RenderCallbackManager renderCallbackManager) {
		this.eventBus = eventBus;
		this.renderCallbackManager = renderCallbackManager;
	}
	public void start() {
		if (started) return;
		eventBus.register(this);
		renderCallbackManager.register(this);
		started = true;
	}
	public void stop() {
		if (!started) return;
		renderCallbackManager.unregister(this);
		eventBus.unregister(this);
		started = false;
		clear();
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onBeforeRender(BeforeRender event) { clear(); }
	@Subscribe
	@SuppressWarnings("unused")
	public void onGameStateChanged(GameStateChanged event) { clear(); }
	@Override
	public boolean addEntity(Renderable renderable, boolean ui) {
		if (!ui && renderable instanceof Actor) visibleActors.add((Actor) renderable);
		return true;
	}
	private void clear() { visibleActors.clear(); }
}
