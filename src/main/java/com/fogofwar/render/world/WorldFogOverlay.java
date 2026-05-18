package com.fogofwar.render.world;
import com.fogofwar.actor.VisibleActorTracker;
import com.fogofwar.config.FogDisplayMode;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.state.AreaExclusionManager;
import com.fogofwar.state.ClientState;
import com.fogofwar.state.RenderCenter;
import com.fogofwar.state.RenderDistanceManager;
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
	private final RenderDistanceManager dynamicRenderDistance;
	private final AreaExclusionManager areaManager;
	private final Rectangle viewport = new Rectangle();
	private final WorldRenderBoundary renderBoundary;
	private final WorldFogMask fogMask;
	private final ActorCutoutMask actorCutouts;
	@Inject
	public WorldFogOverlay(Client client, FogOfWarConfig config, ClientState clientState, RenderDistanceManager dynamicRenderDistance, AreaExclusionManager areaManager, VisibleActorTracker visibleActorTracker) {
		this.client = client;
		this.config = config;
		this.clientState = clientState;
		this.dynamicRenderDistance = dynamicRenderDistance;
		this.areaManager = areaManager;
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
		if (clientState.isSuppressed(config, areaManager)) return null;
		FogDisplayMode mode = config.worldDisplayMode();
		boolean showFog = mode.showsFog();
		boolean showBorder = mode.showsBorder();
		if (!showFog && !showBorder) return null;
		RenderCenter rc = RenderCenter.resolve(client);
		if (rc == null) return null;
		WorldView worldView = rc.getWorldView();
		int landRadius = dynamicRenderDistance.getCurrentRenderDistance();
		int radius = rc.isOnWorldEntity() ? config.sailingRenderDistance() : landRadius;
		int plane = rc.getWorldPoint().getPlane();
		LocalPoint centerLp = renderBoundary.getRenderCenter(rc, radius, landRadius);
		GeneralPath boundary = renderBoundary.createRenderAreaBoundary(worldView, centerLp, plane, radius);
		boolean showSailingLandRenderDistance = rc.isOnWorldEntity() && config.showWorldLandRenderDistanceWhileSailing() && landRadius < radius;
		LocalPoint sailingLandCenterLp = showSailingLandRenderDistance ? renderBoundary.getRenderCenter(rc, landRadius, landRadius) : null;
		GeneralPath sailingLandBoundary = null;
		if (showSailingLandRenderDistance) sailingLandBoundary = renderBoundary.createSailingLandRenderAreaBoundary(worldView, sailingLandCenterLp, plane, landRadius);
		setViewportBounds();
		if (boundary == null) {
			if (showFog) fogMask.renderFullFog(graphics, viewport);
			return null;
		}
		if (showFog) {
			fogMask.renderFog(graphics, viewport, worldView, boundary, centerLp, plane, radius, actorCutouts);
			if (sailingLandBoundary != null) fogMask.renderSailingExtendedFog(graphics, viewport, worldView, boundary, sailingLandBoundary, sailingLandCenterLp, plane, landRadius, actorCutouts);
		}
		if (showBorder) {
			fogMask.renderBorder(graphics, boundary);
			if (sailingLandBoundary != null) fogMask.renderSailingLandBorder(graphics, sailingLandBoundary);
		}
		return null;
	}
	private void setViewportBounds() {
		viewport.setBounds(client.getViewportXOffset(), client.getViewportYOffset(), client.getViewportWidth(), client.getViewportHeight());
	}
}
