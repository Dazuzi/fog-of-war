package com.fogofwar.debug;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.coord.WorldEntityCoords;
import com.fogofwar.render.RenderCenterProvider;
import com.fogofwar.state.ClientState;
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
	private final FogOfWarConfig config;
	private final ClientState clientState;
	private final RenderCenterProvider renderCenterProvider;
	private LineComponent planeLine;
	private LineComponent worldViewLine;
	private LineComponent entityLine;
	private LineComponent categoryLine;
	private int plane = Integer.MIN_VALUE;
	private int worldViewId = Integer.MIN_VALUE;
	private int entityId = Integer.MIN_VALUE;
	private int entityCategory = Integer.MIN_VALUE;
	@Inject
	public DebugOverlay(FogOfWarConfig config, ClientState clientState, RenderCenterProvider renderCenterProvider) {
		this.config = config;
		this.clientState = clientState;
		this.renderCenterProvider = renderCenterProvider;
		setPosition(OverlayPosition.TOP_LEFT);
	}
	public void clearCaches() {
		panelComponent.getChildren().clear();
		planeLine = worldViewLine = entityLine = categoryLine = null;
		plane = worldViewId = entityId = entityCategory = Integer.MIN_VALUE;
		setClearChildren(true);
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		if (!config.debugOverlayEnabled()) return null;
		Player localPlayer = clientState.getLocalPlayerIfReady();
		if (localPlayer == null) return null;
		if (planeLine == null) initLines();
		WorldView wv = localPlayer.getWorldView();
		WorldView topWorldView = wv != null && wv.isTopLevel() ? wv : renderCenterProvider.getTopLevelWorldView(localPlayer);
		if (wv == null) wv = topWorldView;
		WorldEntityConfig entityConfig = getWorldEntityConfig(wv, topWorldView);
		plane = update(planeLine, plane, wv != null ? wv.getPlane() : Integer.MIN_VALUE);
		worldViewId = update(worldViewLine, worldViewId, wv != null ? wv.getId() : Integer.MIN_VALUE);
		entityId = update(entityLine, entityId, entityConfig != null ? entityConfig.getId() : Integer.MIN_VALUE);
		entityCategory = update(categoryLine, entityCategory, entityConfig != null ? entityConfig.getCategory() : Integer.MIN_VALUE);
		return super.render(graphics);
	}
	private void initLines() {
		planeLine = line("Current plane:");
		worldViewLine = line("World view ID:");
		entityLine = line("Entity ID:");
		categoryLine = line("Entity category:");
		setClearChildren(false);
		panelComponent.getChildren().add(planeLine);
		panelComponent.getChildren().add(worldViewLine);
		panelComponent.getChildren().add(entityLine);
		panelComponent.getChildren().add(categoryLine);
	}
	private WorldEntityConfig getWorldEntityConfig(WorldView worldView, WorldView topWorldView) {
		WorldEntity worldEntity = WorldEntityCoords.getWorldEntity(worldView, topWorldView);
		return worldEntity != null ? worldEntity.getConfig() : null;
	}
	private static LineComponent line(String left) { return LineComponent.builder().left(left).right("?").build(); }
	private static int update(LineComponent line, int previous, int value) {
		if (previous != value) line.setRight(value == Integer.MIN_VALUE ? "?" : Integer.toString(value));
		return value;
	}
}
