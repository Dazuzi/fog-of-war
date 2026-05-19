package com.fogofwar.render.world;
import com.fogofwar.actor.VisibleActorTracker;
import com.fogofwar.config.FogDisplayMode;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.render.RenderAreaType;
import com.fogofwar.state.AreaExclusionManager;
import com.fogofwar.state.ClientState;
import com.fogofwar.state.RenderCenter;
import net.runelite.api.Client;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
public class WorldFogOverlay extends Overlay {
	private final Client client;
	private final FogOfWarConfig config;
	private final ClientState clientState;
	private final AreaExclusionManager areaExclusionManager;
	private final Rectangle viewport = new Rectangle();
	private final WorldRenderBoundary renderBoundary;
	private final WorldFogMask fogMask;
	private final ActorCutoutMask actorCutouts;
	@Inject
	public WorldFogOverlay(Client client, FogOfWarConfig config, ClientState clientState, AreaExclusionManager areaExclusionManager, VisibleActorTracker visibleActorTracker) {
		this.client = client;
		this.config = config;
		this.clientState = clientState;
		this.areaExclusionManager = areaExclusionManager;
		this.renderBoundary = new WorldRenderBoundary(client);
		this.fogMask = new WorldFogMask(config);
		this.actorCutouts = new ActorCutoutMask(client, visibleActorTracker);
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(Overlay.PRIORITY_LOW);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}
	public void clearCaches() { actorCutouts.clearCaches(); }
	@Override
	public Dimension render(Graphics2D graphics) {
		actorCutouts.beginFrame();
		try {
			renderFrame(graphics);
		} finally {
			actorCutouts.endFrame();
		}
		return null;
	}
	private void renderFrame(Graphics2D graphics) {
		if (clientState.isSuppressed(config, areaExclusionManager)) return;
		FogDisplayMode mode = config.worldDisplayMode();
		boolean showFog = mode.showsFog();
		boolean showBorder = mode.showsBorder();
		if (!showFog && !showBorder) return;
		RenderCenter rc = RenderCenter.resolve(client);
		if (rc == null) return;
		WorldView worldView = rc.getWorldView();
		int landRadius = config.landRenderDistance();
		boolean sailing = rc.isOnWorldEntity();
		int plane = rc.getWorldPoint().getPlane();
		setViewportBounds();
		if (sailing) renderSailingFrame(graphics, showFog, showBorder, rc, worldView, plane, landRadius);
		else renderLandFrame(graphics, showFog, showBorder, rc, worldView, plane, landRadius);
	}
	private void renderLandFrame(Graphics2D graphics, boolean showFog, boolean showBorder, RenderCenter rc, WorldView worldView, int plane, int landRadius) {
		LocalPoint landCenter = rc.snappedCenter();
		GeneralPath landBoundary = renderBoundary.createRenderAreaBoundary(RenderAreaType.LAND, worldView, landCenter, plane, landRadius);
		if (landBoundary == null) {
			if (showFog) fogMask.renderFullFog(graphics, viewport);
			return;
		}
		if (showFog) fogMask.renderFog(graphics, viewport, worldView, landBoundary, landCenter, plane, landRadius, actorCutouts);
		if (showBorder) fogMask.renderBorder(graphics, landBoundary);
	}
	private void renderSailingFrame(Graphics2D graphics, boolean showFog, boolean showBorder, RenderCenter rc, WorldView worldView, int plane, int landRadius) {
		int seaRadius = config.sailingRenderDistance();
		LocalPoint center = rc.snappedCenter();
		GeneralPath seaBoundary = renderBoundary.createRenderAreaBoundary(RenderAreaType.SEA, worldView, center, plane, seaRadius);
		if (seaBoundary == null) {
			if (showFog) fogMask.renderFullFog(graphics, viewport);
			return;
		}
		GeneralPath landBoundary = config.showWorldLandAreaWhileSailing() ? renderBoundary.createRenderAreaBoundary(RenderAreaType.LAND, worldView, center, plane, landRadius) : null;
		if (showFog) {
			fogMask.renderFog(graphics, viewport, worldView, seaBoundary, center, plane, seaRadius, actorCutouts);
			if (landBoundary != null) fogMask.renderSailingSeaFog(graphics, viewport, worldView, seaBoundary, landBoundary, center, plane, landRadius, actorCutouts);
		}
		if (showBorder) {
			fogMask.renderBorder(graphics, seaBoundary);
			if (landBoundary != null) fogMask.renderSailingSeaBorder(graphics, landBoundary);
		}
	}
	private void setViewportBounds() {
		viewport.setBounds(client.getViewportXOffset(), client.getViewportYOffset(), client.getViewportWidth(), client.getViewportHeight());
	}
}
