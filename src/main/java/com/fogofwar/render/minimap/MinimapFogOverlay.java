package com.fogofwar.render.minimap;
import com.fogofwar.config.FogDisplayMode;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.render.FogRender;
import com.fogofwar.render.RenderCenter;
import com.fogofwar.render.RenderCenterProvider;
import com.fogofwar.state.ClientState;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
public final class MinimapFogOverlay extends Overlay {
	private final FogOfWarConfig config;
	private final ClientState clientState;
	private final RenderCenterProvider renderCenterProvider;
	private final MinimapClipProvider clipProvider;
	private final Client client;
	private MinimapRenderBoundary renderBoundary;
	private MinimapFogMask fogMask;
	@Inject
	public MinimapFogOverlay(Client client, FogOfWarConfig config, ClientState clientState, RenderCenterProvider renderCenterProvider, MinimapClipProvider clipProvider) {
		this.config = config;
		this.clientState = clientState;
		this.renderCenterProvider = renderCenterProvider;
		this.clipProvider = clipProvider;
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(Overlay.PRIORITY_LOW);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}
	public void clearCaches() {
		clipProvider.clearCaches();
		renderBoundary = null;
		fogMask = null;
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		Player localPlayer = clientState.getLocalPlayerIfReady();
		if (localPlayer == null) return null;
		FogDisplayMode mode = config.minimapDisplayMode();
		boolean fogMode = mode.showsFog();
		boolean borderMode = mode.showsBorder();
		if (!fogMode && !borderMode) return null;
		Color fogColour = fogMode ? config.minimapFogColour() : null;
		Color borderColour = borderMode ? config.minimapBorderColour() : null;
		boolean showFog = fogMode && fogColour.getAlpha() > 0;
		boolean showBorder = borderMode && borderColour.getAlpha() > 0;
		boolean mayShowSailingArea = config.showLandAreaWhileSailing();
		if (!showFog && !showBorder && !mayShowSailingArea) return null;
		Widget minimap = clipProvider.getMinimapWidget();
		if (minimap == null || minimap.isHidden()) return null;
		RenderCenter rc = renderCenterProvider.get(localPlayer);
		if (rc == null) return null;
		boolean showSailingArea = rc.isOnWorldEntity() && mayShowSailingArea;
		boolean showSailingFog = showSailingArea && fogMode;
		boolean showSailingBorder = showSailingArea && borderMode;
		if (!showFog && !showBorder && !showSailingFog && !showSailingBorder) return null;
		if (renderBoundary == null) renderBoundary = new MinimapRenderBoundary(client);
		if (fogMask == null) fogMask = new MinimapFogMask();
		Rectangle minimapBounds = clipProvider.getMinimapBounds(minimap);
		Shape minimapClipShape = clipProvider.getClipShape(minimap);
		Shape oldClip = graphics.getClip();
		graphics.clip(minimapClipShape);
		try {
			int landRadius = config.landRenderDistance();
			int borderThickness = showBorder || showSailingBorder ? config.minimapBorderThickness() : 0;
			Color sailingFogColour = showSailingFog ? FogRender.sailingSea(fogColour) : null;
			Color sailingBorderColour = showSailingBorder ? FogRender.sailingSea(borderColour) : null;
			if (rc.isOnWorldEntity()) renderSailingFrame(graphics, showFog, showBorder, showSailingFog, showSailingBorder, rc, minimap, minimapBounds, minimapClipShape, landRadius, fogColour, borderColour, sailingFogColour, sailingBorderColour, borderThickness);
			else renderLandFrame(graphics, showFog, showBorder, rc, minimap, minimapBounds, minimapClipShape, landRadius, fogColour, borderColour, borderThickness);
			return null;
		} finally { graphics.setClip(oldClip); }
	}
	private void renderLandFrame(Graphics2D graphics, boolean showFog, boolean showBorder, RenderCenter rc, Widget minimap, Rectangle minimapBounds, Shape minimapClipShape, int landRadius, Color fogColour, Color borderColour, int borderThickness) {
		GeneralPath landPath = renderBoundary.createLandRenderAreaPath(rc, landRadius, minimap, minimapBounds);
		if (landPath == null) {
			if (showFog) fogMask.renderFullFog(graphics, minimapClipShape, fogColour);
			return;
		}
		if (showFog) fogMask.renderFog(graphics, minimapClipShape, landPath, fogColour);
		if (showBorder) fogMask.renderBorder(graphics, minimapClipShape, landPath, borderColour, borderThickness);
	}
	private void renderSailingFrame(Graphics2D graphics, boolean showFog, boolean showBorder, boolean showSailingFog, boolean showSailingBorder, RenderCenter rc, Widget minimap, Rectangle minimapBounds, Shape minimapClipShape, int landRadius, Color fogColour, Color borderColour, Color sailingFogColour, Color sailingBorderColour, int borderThickness) {
		int seaRadius = config.sailingRenderDistance();
		GeneralPath seaPath = renderBoundary.createSeaRenderAreaPath(rc, seaRadius, minimap, minimapBounds);
		if (seaPath == null) {
			if (showFog) fogMask.renderFullFog(graphics, minimapClipShape, fogColour);
			return;
		}
		GeneralPath landPath = showSailingFog || showSailingBorder ? renderBoundary.createLandRenderAreaPath(rc, landRadius, minimap, minimapBounds) : null;
		if (showFog) fogMask.renderFog(graphics, minimapClipShape, seaPath, fogColour);
		if (showSailingFog && landPath != null) fogMask.renderSailingSeaFog(graphics, seaPath, landPath, sailingFogColour);
		if (showBorder) fogMask.renderBorder(graphics, minimapClipShape, seaPath, borderColour, borderThickness);
		if (showSailingBorder && landPath != null) fogMask.renderSailingSeaBorder(graphics, minimapClipShape, landPath, sailingBorderColour, borderThickness);
	}
}
