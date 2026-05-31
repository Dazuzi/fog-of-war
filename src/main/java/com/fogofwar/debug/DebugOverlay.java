package com.fogofwar.debug;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.coord.WorldEntityCoords;
import com.fogofwar.render.RenderCenterProvider;
import com.fogofwar.state.ClientState;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldEntityConfig;
import net.runelite.api.WorldView;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;
public class DebugOverlay extends OverlayPanel {
	private final Client client;
	private final FogOfWarConfig config;
	private final ClientState clientState;
	private final RenderCenterProvider renderCenterProvider;
	@Inject
	public DebugOverlay(Client client, FogOfWarConfig config, ClientState clientState, RenderCenterProvider renderCenterProvider) {
		this.client = client;
		this.config = config;
		this.clientState = clientState;
		this.renderCenterProvider = renderCenterProvider;
		setPosition(OverlayPosition.TOP_LEFT);
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		if (!config.debugOverlayEnabled() || clientState.isClientNotReady()) return null;
		panelComponent.getChildren().clear();
		WorldView wv = getWorldView();
		WorldEntityConfig entityConfig = getWorldEntityConfig(wv);
		addLine("Current plane:", wv != null ? wv.getPlane() : "?");
		addLine("World view ID:", wv != null ? wv.getId() : "?");
		addLine("Entity ID:", entityConfig != null ? entityConfig.getId() : "?");
		addLine("Entity category:", entityConfig != null ? entityConfig.getCategory() : "?");
		return super.render(graphics);
	}
	private WorldView getWorldView() {
		Player localPlayer = client.getLocalPlayer();
		WorldView wv = localPlayer != null ? localPlayer.getWorldView() : null;
		if (wv == null) wv = renderCenterProvider.getTopLevelWorldView();
		return wv;
	}
	private WorldEntityConfig getWorldEntityConfig(WorldView worldView) {
		WorldEntity worldEntity = WorldEntityCoords.getWorldEntity(worldView, renderCenterProvider.getTopLevelWorldView());
		return worldEntity != null ? worldEntity.getConfig() : null;
	}
	private void addLine(String left, Object right) {
		panelComponent.getChildren().add(LineComponent.builder().left(left).right(String.valueOf(right)).build());
	}
}
