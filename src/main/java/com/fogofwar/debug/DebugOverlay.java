package com.fogofwar.debug;
import com.fogofwar.FogOfWarConfig;
import com.fogofwar.util.ClientState;
import com.fogofwar.util.DynamicRenderDistance;
import com.fogofwar.util.Players;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import javax.inject.Inject;
import java.awt.*;
public class DebugOverlay extends OverlayPanel {
	private final Client client;
	private final FogOfWarConfig config;
	private final ClientState clientState;
	private final DynamicRenderDistance dynamicRenderDistance;
	@Inject
	public DebugOverlay(Client client, FogOfWarConfig config, ClientState clientState, DynamicRenderDistance dynamicRenderDistance) {
		this.client = client;
		this.config = config;
		this.clientState = clientState;
		this.dynamicRenderDistance = dynamicRenderDistance;
		setPosition(OverlayPosition.TOP_LEFT);
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		if (!config.showDebugOverlay() || clientState.isClientNotReady()) return null;
		addLine("Current plane:", client.getPlane());
		addLine("Players:", Players.count(client.getTopLevelWorldView()));
		addLine("Render distance:", dynamicRenderDistance.getCurrentRenderDistance());
		return super.render(graphics);
	}
	private void addLine(String left, Object right) {
		panelComponent.getChildren().add(LineComponent.builder().left(left).right(String.valueOf(right)).build());
	}
}
