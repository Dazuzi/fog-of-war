package com.fogofwar.debug;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.state.ClientState;
import com.fogofwar.state.RenderDistanceManager;
import com.fogofwar.state.Players;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import javax.inject.Inject;
import java.awt.*;
public class DebugOverlay extends OverlayPanel {
	private final Client client;
	private final FogOfWarConfig config;
	private final ClientState clientState;
	private final RenderDistanceManager renderDistanceManager;
	private final EventBus eventBus;
	private boolean started;
	private int cachedPlayerCount;
	@Inject
	public DebugOverlay(Client client, FogOfWarConfig config, ClientState clientState, RenderDistanceManager renderDistanceManager, EventBus eventBus) {
		this.client = client;
		this.config = config;
		this.clientState = clientState;
		this.renderDistanceManager = renderDistanceManager;
		this.eventBus = eventBus;
		setPosition(OverlayPosition.TOP_LEFT);
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
		cachedPlayerCount = 0;
	}
	@Subscribe
	@SuppressWarnings("unused")
	public void onGameTick(GameTick event) { updatePlayerCount(); }
	@Override
	public Dimension render(Graphics2D graphics) {
		if (!config.debugOverlayEnabled() || clientState.isClientNotReady()) return null;
		WorldView wv = getWorldView();
		addLine("Current plane:", wv != null ? wv.getPlane() : "?");
		addLine("Players:", cachedPlayerCount);
		addLine("Render distance:", renderDistanceManager.getCurrentRenderDistance());
		return super.render(graphics);
	}
	private void updatePlayerCount() { cachedPlayerCount = Players.count(getWorldView()); }
	private WorldView getWorldView() {
		Player localPlayer = client.getLocalPlayer();
		WorldView wv = localPlayer != null ? localPlayer.getWorldView() : null;
		if (wv == null) wv = client.getTopLevelWorldView();
		return wv;
	}
	private void addLine(String left, Object right) {
		panelComponent.getChildren().add(LineComponent.builder().left(left).right(String.valueOf(right)).build());
	}
}
