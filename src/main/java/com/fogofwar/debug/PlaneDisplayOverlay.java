package com.fogofwar.debug;

import com.fogofwar.FogOfWarConfig;
import com.fogofwar.util.ClientState;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import javax.inject.Inject;
import java.awt.*;

public class PlaneDisplayOverlay extends OverlayPanel {
	private final Client client;
	private final FogOfWarConfig config;
	private final ClientState clientState;
	@Inject
	public PlaneDisplayOverlay(Client client, FogOfWarConfig config, ClientState clientState) {
		this.client = client;
		this.config = config;
		this.clientState = clientState;
		setPosition(OverlayPosition.TOP_LEFT);
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		if (!config.showPlaneDisplay() || clientState.isClientNotReady()) {
			return null;
		}
		int plane = client.getLocalPlayer().getWorldLocation().getPlane();
		panelComponent.getChildren().add(LineComponent.builder()
				.left("Current plane:")
				.right(String.valueOf(plane))
				.build());
		return super.render(graphics);
	}
}