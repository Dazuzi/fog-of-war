package com.fogofwar.render.minimap;
import com.fogofwar.config.FogDisplayMode;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.state.AreaExclusionManager;
import com.fogofwar.state.ClientState;
import com.fogofwar.state.RenderCenter;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
public class MinimapFogOverlay extends Overlay {
	private final Client client;
	private final FogOfWarConfig config;
	private final ClientState clientState;
	private final AreaExclusionManager areaExclusionManager;
	private final MinimapClipProvider clipProvider;
	private final MinimapRenderBoundary renderBoundary;
	private final MinimapFogMask fogMask;
	@Inject
	public MinimapFogOverlay(Client client, FogOfWarConfig config, ClientState clientState, AreaExclusionManager areaExclusionManager, MinimapClipProvider clipProvider) {
		this.client = client;
		this.config = config;
		this.clientState = clientState;
		this.areaExclusionManager = areaExclusionManager;
		this.clipProvider = clipProvider;
		this.renderBoundary = new MinimapRenderBoundary(client);
		this.fogMask = new MinimapFogMask(config);
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(Overlay.PRIORITY_LOW);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}
	public void clearCaches() {
		clipProvider.clearCaches();
		renderBoundary.clearCaches();
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		if (clientState.isSuppressed(config, areaExclusionManager)) return null;
		FogDisplayMode mode = config.minimapDisplayMode();
		boolean showFog = mode.showsFog();
		boolean showBorder = mode.showsBorder();
		if (!showFog && !showBorder) return null;
		Widget minimap = MinimapWidgetProvider.getMinimapWidget(client);
		if (minimap == null || minimap.isHidden()) return null;
		RenderCenter rc = RenderCenter.resolve(client);
		if (rc == null) return null;
		Shape minimapClipShape = clipProvider.getClipShape(minimap);
		Shape oldClip = graphics.getClip();
		graphics.setClip(minimapClipShape);
		try {
			int landRadius = config.landRenderDistance();
			if (rc.isOnWorldEntity()) renderSailingFrame(graphics, showFog, showBorder, rc, minimap, minimapClipShape, landRadius);
			else renderLandFrame(graphics, showFog, showBorder, rc, minimap, minimapClipShape, landRadius);
			return null;
		} finally { graphics.setClip(oldClip); }
	}
	private void renderLandFrame(Graphics2D graphics, boolean showFog, boolean showBorder, RenderCenter rc, Widget minimap, Shape minimapClipShape, int landRadius) {
		GeneralPath landPath = renderBoundary.createLandRenderAreaPath(rc, landRadius, minimap.getBounds());
		if (landPath == null) {
			if (showFog) fogMask.renderFullFog(graphics, minimapClipShape);
			return;
		}
		if (showFog) fogMask.renderFog(graphics, minimapClipShape, landPath);
		if (showBorder) fogMask.renderBorder(graphics, minimapClipShape, landPath);
	}
	private void renderSailingFrame(Graphics2D graphics, boolean showFog, boolean showBorder, RenderCenter rc, Widget minimap, Shape minimapClipShape, int landRadius) {
		int seaRadius = config.sailingRenderDistance();
		GeneralPath seaPath = renderBoundary.createSeaRenderAreaPath(rc, seaRadius, landRadius, minimap.getBounds());
		if (seaPath == null) {
			if (showFog) fogMask.renderFullFog(graphics, minimapClipShape);
			return;
		}
		GeneralPath landPath = config.showMinimapLandAreaWhileSailing() ? renderBoundary.createLandRenderAreaPath(rc, landRadius, minimap.getBounds()) : null;
		if (showFog) {
			fogMask.renderFog(graphics, minimapClipShape, seaPath);
			if (landPath != null) fogMask.renderSailingSeaFog(graphics, seaPath, landPath);
		}
		if (showBorder) {
			fogMask.renderBorder(graphics, minimapClipShape, seaPath);
			if (landPath != null) fogMask.renderSailingSeaBorder(graphics, minimapClipShape, landPath);
		}
	}
}
