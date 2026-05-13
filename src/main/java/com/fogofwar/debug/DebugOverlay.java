package com.fogofwar.debug;
import com.fogofwar.FogOfWarConfig;
import com.fogofwar.util.ClientState;
import com.fogofwar.util.DynamicRenderDistance;
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
		WorldView worldView = client.getTopLevelWorldView();
		int playerCount = countPlayers(worldView);
		addLine("Current plane:", client.getLocalPlayer().getWorldLocation().getPlane());
		addLine("Players:", playerCount);
		addLine("Render distance:", dynamicRenderDistance.getCurrentRenderDistance());
		return super.render(graphics);
	}
	private int countPlayers(WorldView worldView) {
		if (worldView == null) return 0;
		int count = 0;
		for (Player player : worldView.players()) if (player != null) count++;
		return count;
	}
	private void addLine(String left, Object right) {
		panelComponent.getChildren().add(LineComponent.builder().left(left).right(String.valueOf(right)).build());
	}
}
