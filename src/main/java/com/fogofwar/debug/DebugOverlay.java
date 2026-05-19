package com.fogofwar.debug;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.state.ClientState;
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
	@Inject
	public DebugOverlay(Client client, FogOfWarConfig config, ClientState clientState) {
		this.client = client;
		this.config = config;
		this.clientState = clientState;
		setPosition(OverlayPosition.TOP_LEFT);
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		if (!config.debugOverlayEnabled() || clientState.isClientNotReady()) return null;
		panelComponent.getChildren().clear();
		WorldView wv = getWorldView();
		addLine(wv != null ? wv.getPlane() : "?");
		return super.render(graphics);
	}
	private WorldView getWorldView() {
		Player localPlayer = client.getLocalPlayer();
		WorldView wv = localPlayer != null ? localPlayer.getWorldView() : null;
		if (wv == null) wv = client.getTopLevelWorldView();
		return wv;
	}
	private void addLine(Object right) {
		panelComponent.getChildren().add(LineComponent.builder().left("Current plane:").right(String.valueOf(right)).build());
	}
}
