package com.fogofwar.lifecycle;
import net.runelite.client.eventbus.EventBus;
public abstract class LifecycleComponent {
	private final EventBus eventBus;
	private boolean started;
	protected LifecycleComponent(EventBus eventBus) { this.eventBus = eventBus; }
	public final void start() {
		if (started) return;
		eventBus.register(this);
		started = true;
		onStart();
	}
	public final void stop() {
		if (!started) return;
		eventBus.unregister(this);
		started = false;
		onStop();
	}
	protected void onStart() {}
	protected void onStop() {}
}
