package com.fogofwar.render.minimap;
import com.fogofwar.config.FogDisplayMode;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.state.AreaExclusionManager;
import com.fogofwar.state.ClientState;
import com.fogofwar.state.RenderCenter;
import com.fogofwar.state.RenderDistanceManager;
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
	private final RenderDistanceManager renderDistanceManager;
	private final AreaExclusionManager areaExclusionManager;
	private final MinimapClipProvider clipProvider;
	private final MinimapRenderBoundary renderBoundary;
	private final MinimapFogMask fogMask;
	@Inject
	public MinimapFogOverlay(Client client, FogOfWarConfig config, ClientState clientState, RenderDistanceManager renderDistanceManager, AreaExclusionManager areaExclusionManager) {
		this.client = client;
		this.config = config;
		this.clientState = clientState;
		this.renderDistanceManager = renderDistanceManager;
		this.areaExclusionManager = areaExclusionManager;
		this.clipProvider = new MinimapClipProvider(client);
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
		int landRadius = renderDistanceManager.getCurrentRenderDistance();
		int radius = rc.isOnWorldEntity() ? config.sailingRenderDistance() : landRadius;
		GeneralPath fogPath = renderBoundary.createRenderAreaPath(rc, radius, landRadius, minimap.getBounds());
		if (fogPath == null) {
			graphics.setClip(oldClip);
			return null;
		}
		boolean showSailingLandRenderDistance = rc.isOnWorldEntity() && config.showMinimapLandRenderDistanceWhileSailing() && landRadius < radius;
		GeneralPath sailingLandPath = null;
		if (showSailingLandRenderDistance) sailingLandPath = renderBoundary.createSailingLandRenderAreaPath(rc, landRadius, minimap.getBounds());
		if (showFog) fogMask.renderFog(graphics, minimapClipShape, fogPath);
		if (showFog && sailingLandPath != null) fogMask.renderSailingExtendedFog(graphics, fogPath, sailingLandPath);
		if (showBorder) {
			fogMask.renderBorder(graphics, minimapClipShape, fogPath);
			if (sailingLandPath != null) fogMask.renderSailingLandBorder(graphics, minimapClipShape, sailingLandPath);
		}
		graphics.setClip(oldClip);
		return null;
	}
}
