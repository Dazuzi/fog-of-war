package com.fogofwar.actor;
import com.fogofwar.lifecycle.LifecycleComponent;
import lombok.Getter;
import net.runelite.api.Actor;
import net.runelite.api.Renderable;
import net.runelite.api.events.BeforeRender;
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
public class VisibleActorTracker extends LifecycleComponent implements RenderCallback {
	private final RenderCallbackManager renderCallbackManager;
	@Getter
	private final Set<Actor> visibleActors = Collections.newSetFromMap(new IdentityHashMap<>(256));
	@Inject
	public VisibleActorTracker(EventBus eventBus, RenderCallbackManager renderCallbackManager) {
		super(eventBus);
		this.renderCallbackManager = renderCallbackManager;
	}
	@Override
	protected void onStart() { renderCallbackManager.register(this); }
	@Override
	protected void onStop(boolean wasStarted) {
		if (wasStarted) renderCallbackManager.unregister(this);
		visibleActors.clear();
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onBeforeRender(BeforeRender event) { visibleActors.clear(); }
	@Override
	public boolean addEntity(Renderable renderable, boolean ui) {
		if (!ui && renderable instanceof Actor) visibleActors.add((Actor) renderable);
		return true;
	}
}
