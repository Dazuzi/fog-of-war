package com.fogofwar.render.world;
import com.fogofwar.config.ActorCutoutLimit;
import com.fogofwar.config.FogDisplayMode;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.render.FogRender;
import com.fogofwar.render.RenderCenter;
import com.fogofwar.render.RenderCenterProvider;
import com.fogofwar.state.ClientState;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
public final class WorldFogOverlay extends Overlay {
	private final Client client;
	private final FogOfWarConfig config;
	private final ClientState clientState;
	private final RenderCenterProvider renderCenterProvider;
	private final VisibleActorTracker visibleActorTracker;
	private final Rectangle viewport = new Rectangle();
	private WorldRenderBoundary renderBoundary;
	private WorldFogMask fogMask;
	private ActorCutoutMask actorCutouts;
	@Inject
	public WorldFogOverlay(Client client, FogOfWarConfig config, ClientState clientState, RenderCenterProvider renderCenterProvider, VisibleActorTracker visibleActorTracker) {
		this.client = client;
		this.config = config;
		this.clientState = clientState;
		this.renderCenterProvider = renderCenterProvider;
		this.visibleActorTracker = visibleActorTracker;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(Overlay.PRIORITY_LOW);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}
	public void clearCaches() {
		renderBoundary = null;
		fogMask = null;
		actorCutouts = null;
	}
	public void clearActorCaches() { actorCutouts = null; }
	@Override
	public Dimension render(Graphics2D graphics) {
		Player localPlayer = clientState.getLocalPlayerIfReady();
		if (localPlayer == null) return null;
		FogDisplayMode mode = config.worldDisplayMode();
		boolean fogMode = mode.showsFog();
		boolean borderMode = mode.showsBorder();
		if (!fogMode && !borderMode) return null;
		Color fogColour = fogMode ? config.worldFogColour() : null;
		Color borderColour = borderMode ? config.worldBorderColour() : null;
		boolean showFog = fogMode && fogColour.getAlpha() > 0;
		boolean showBorder = borderMode && borderColour.getAlpha() > 0;
		boolean mayShowSailingArea = config.showLandAreaWhileSailing();
		if (!showFog && !showBorder && !mayShowSailingArea) return null;
		setViewportBounds();
		if (viewport.isEmpty()) return null;
		RenderCenter rc = renderCenterProvider.get(localPlayer);
		if (rc == null) return null;
		boolean showSailingArea = rc.isOnWorldEntity() && mayShowSailingArea;
		boolean showSailingFog = showSailingArea && fogMode;
		boolean showSailingBorder = showSailingArea && borderMode;
		if (!showFog && !showBorder && !showSailingFog && !showSailingBorder) return null;
		if (renderBoundary == null) renderBoundary = new WorldRenderBoundary(client);
		if (fogMask == null) fogMask = new WorldFogMask();
		ActorCutoutLimit cutoutLimit = showFog || showSailingFog ? config.actorCutoutLimit() : ActorCutoutLimit.NONE;
		boolean useCutouts = cutoutLimit.isEnabled();
		if (useCutouts) {
			if (actorCutouts == null) actorCutouts = new ActorCutoutMask(client, visibleActorTracker);
			actorCutouts.beginFrame(localPlayer);
		} else clearActorCaches();
		try {
			int borderThickness = showBorder || showSailingBorder ? config.worldBorderThickness() : 0;
			Color sailingFogColour = showSailingFog ? FogRender.sailingSea(fogColour) : null;
			Color sailingBorderColour = showSailingBorder ? FogRender.sailingSea(borderColour) : null;
			renderFrame(graphics, showFog, showBorder, showSailingFog, showSailingBorder, rc, fogColour, borderColour, sailingFogColour, sailingBorderColour, borderThickness, cutoutLimit);
		} finally {
			if (useCutouts) actorCutouts.endFrame();
		}
		return null;
	}
	private void renderFrame(Graphics2D graphics, boolean showFog, boolean showBorder, boolean showSailingFog, boolean showSailingBorder, RenderCenter rc, Color fogColour, Color borderColour, Color sailingFogColour, Color sailingBorderColour, int borderThickness, ActorCutoutLimit cutoutLimit) {
		WorldView worldView = rc.getWorldView();
		int landRadius = config.landRenderDistance();
		boolean sailing = rc.isOnWorldEntity();
		int plane = rc.getWorldPoint().getPlane();
		if (sailing) renderSailingFrame(graphics, showFog, showBorder, showSailingFog, showSailingBorder, rc, worldView, plane, landRadius, fogColour, borderColour, sailingFogColour, sailingBorderColour, borderThickness, cutoutLimit);
		else renderLandFrame(graphics, showFog, showBorder, rc, worldView, plane, landRadius, fogColour, borderColour, borderThickness, cutoutLimit);
	}
	private void renderLandFrame(Graphics2D graphics, boolean showFog, boolean showBorder, RenderCenter rc, WorldView worldView, int plane, int landRadius, Color fogColour, Color borderColour, int borderThickness, ActorCutoutLimit cutoutLimit) {
		LocalPoint landCenter = rc.snappedCenter();
		GeneralPath landBoundary = renderBoundary.createLandRenderAreaBoundary(rc, landRadius, viewport);
		if (landBoundary == null) {
			if (showFog) fogMask.renderFullFog(graphics, viewport, fogColour);
			return;
		}
		if (showFog) fogMask.renderFog(graphics, viewport, worldView, landBoundary, landCenter, plane, landRadius, fogColour, cutoutLimit, actorCutouts);
		if (showBorder) fogMask.renderBorder(graphics, landBoundary, borderColour, borderThickness);
	}
	private void renderSailingFrame(Graphics2D graphics, boolean showFog, boolean showBorder, boolean showSailingFog, boolean showSailingBorder, RenderCenter rc, WorldView worldView, int plane, int landRadius, Color fogColour, Color borderColour, Color sailingFogColour, Color sailingBorderColour, int borderThickness, ActorCutoutLimit cutoutLimit) {
		int seaRadius = config.sailingRenderDistance();
		LocalPoint center = rc.snappedCenter();
		GeneralPath seaBoundary = renderBoundary.createSeaRenderAreaBoundary(rc, seaRadius, viewport);
		if (seaBoundary == null) {
			if (showFog) fogMask.renderFullFog(graphics, viewport, fogColour);
			return;
		}
		GeneralPath landBoundary = showSailingFog || showSailingBorder ? renderBoundary.createLandRenderAreaBoundary(rc, landRadius, viewport) : null;
		if (showFog) fogMask.renderFog(graphics, viewport, worldView, seaBoundary, center, plane, seaRadius, fogColour, cutoutLimit, actorCutouts);
		if (showSailingFog && landBoundary != null) fogMask.renderSailingSeaFog(graphics, viewport, worldView, seaBoundary, landBoundary, center, plane, landRadius, sailingFogColour, cutoutLimit, actorCutouts);
		if (showBorder) fogMask.renderBorder(graphics, seaBoundary, borderColour, borderThickness);
		if (showSailingBorder && landBoundary != null) fogMask.renderSailingSeaBorder(graphics, landBoundary, sailingBorderColour, borderThickness);
	}
	private void setViewportBounds() {
		viewport.setBounds(client.getViewportXOffset(), client.getViewportYOffset(), client.getViewportWidth(), client.getViewportHeight());
	}
}
