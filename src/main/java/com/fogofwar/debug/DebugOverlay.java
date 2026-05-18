package com.fogofwar.debug;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.state.ClientState;
import com.fogofwar.state.RenderDistanceManager;
import com.fogofwar.state.Players;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
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
	@Inject
	public DebugOverlay(Client client, FogOfWarConfig config, ClientState clientState, RenderDistanceManager renderDistanceManager) {
		this.client = client;
		this.config = config;
		this.clientState = clientState;
		this.renderDistanceManager = renderDistanceManager;
		setPosition(OverlayPosition.TOP_LEFT);
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		if (!config.debugOverlayEnabled() || clientState.isClientNotReady()) return null;
		Player localPlayer = client.getLocalPlayer();
		WorldView wv = localPlayer != null ? localPlayer.getWorldView() : null;
		if (wv == null) wv = client.getTopLevelWorldView();
		addLine("Current plane:", wv != null ? wv.getPlane() : "?");
		addLine("Players:", Players.count(wv));
		addLine("Render distance:", renderDistanceManager.getCurrentRenderDistance());
		return super.render(graphics);
	}
	private void addLine(String left, Object right) {
		panelComponent.getChildren().add(LineComponent.builder().left(left).right(String.valueOf(right)).build());
	}
}
