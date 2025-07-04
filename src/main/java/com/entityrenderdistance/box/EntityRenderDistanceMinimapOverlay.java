package com.entityrenderdistance.box;

import com.entityrenderdistance.EntityRenderDistanceConfig;
import com.entityrenderdistance.util.ClientState;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

public class EntityRenderDistanceMinimapOverlay extends Overlay {
	private final Client client;
	private final EntityRenderDistanceConfig config;
	private final ClientState clientState;
	private static final int SIDE_PANELS_ID = 4607;
	private static final int FIXED_PARENT_ID = 548;
	private static final int FIXED_CHILD_ID = 21;
	private static final int STRETCH_PARENT_ID = 161;
	private static final int STRETCH_CHILD_ID = 30;
	private static final int PRE_EOC_PARENT_ID = 164;
	private static final int PRE_EOC_CHILD_ID = 30;
	@Inject
	public EntityRenderDistanceMinimapOverlay(Client client, EntityRenderDistanceConfig config, ClientState clientState) {
		this.client = client;
		this.config = config;
		this.clientState = clientState;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(Overlay.PRIORITY_LOW);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		if (clientState.isClientNotReady()) return null;
		if (config.onlyInWilderness() && clientState.isNotInWilderness()) return null;
		Widget minimap = getMinimapWidget();
		if (minimap == null || minimap.isHidden()) return null;
		var oldClip = graphics.getClip();
		graphics.setClip(minimap.getBounds());
		if (config.showMinimapFog()) { renderMinimapFog(graphics, minimap); }
		if (config.showMinimapBorder()) { renderMinimapBox(graphics); }
		graphics.setClip(oldClip);
		return null;
	}
	private void renderMinimapBox(Graphics2D graphics) {
		int radius = config.renderDistanceRadius();
		WorldPoint centerWp = client.getLocalPlayer().getWorldLocation();
		GeneralPath borderPath = createMinimapBorderPath(centerWp, radius);
		if (borderPath != null) {
			graphics.setColor(config.minimapBorderColour());
			graphics.setStroke(new BasicStroke(config.minimapBorderThickness()));
			graphics.draw(borderPath);
		}
	}
	private GeneralPath createMinimapBorderPath(WorldPoint center, int radius) {
		List<Point> borderPoints = new ArrayList<>();
		for (int x = -radius; x <= radius; x++) {
			WorldPoint wp = new WorldPoint(center.getX() + x, center.getY() + radius, center.getPlane());
			Point p1 = getMinimapPoint(new WorldPoint(wp.getX(), wp.getY() + 1, wp.getPlane()));
			Point p2 = getMinimapPoint(new WorldPoint(wp.getX() + 1, wp.getY() + 1, wp.getPlane()));
			if (p1 != null && p2 != null) {
				if (x == -radius) borderPoints.add(p1);
				borderPoints.add(p2);
			}
		}
		for (int y = radius; y >= -radius; y--) {
			WorldPoint wp = new WorldPoint(center.getX() + radius, center.getY() + y, center.getPlane());
			Point p2 = getMinimapPoint(new WorldPoint(wp.getX() + 1, wp.getY(), wp.getPlane()));
			if (p2 != null) borderPoints.add(p2);
		}
		for (int x = radius; x >= -radius; x--) {
			WorldPoint wp = new WorldPoint(center.getX() + x, center.getY() - radius, center.getPlane());
			Point p2 = getMinimapPoint(new WorldPoint(wp.getX(), wp.getY(), wp.getPlane()));
			if (p2 != null) borderPoints.add(p2);
		}
		for (int y = -radius; y <= radius; y++) {
			if (y == -radius || y == radius) continue;
			WorldPoint wp = new WorldPoint(center.getX() - radius, center.getY() + y, center.getPlane());
			Point p2 = getMinimapPoint(new WorldPoint(wp.getX(), wp.getY() + 1, wp.getPlane()));
			if (p2 != null) borderPoints.add(p2);
		}
		if (borderPoints.isEmpty()) return null;
		GeneralPath path = new GeneralPath();
		boolean pathStarted = false;
		for (Point point : borderPoints) {
			if (point != null) {
				if (!pathStarted) {
					path.moveTo(point.getX(), point.getY());
					pathStarted = true;
				} else {
					path.lineTo(point.getX(), point.getY());
				}
			}
		}
		if (pathStarted) path.closePath();
		return pathStarted ? path : null;
	}
	private void renderMinimapFog(Graphics2D graphics, Widget minimapWidget) {
		Rectangle minimapBounds = minimapWidget.getBounds();
		int centerX = minimapBounds.x + minimapBounds.width  / 2;
		int centerY = minimapBounds.y + minimapBounds.height / 2;
		int minimapRadius = Math.min(minimapBounds.width, minimapBounds.height) / 2;
		Area minimapCircle = new Area(new java.awt.geom.Ellipse2D.Double(
				centerX - minimapRadius,
				centerY - minimapRadius,
				minimapRadius * 2,
				minimapRadius * 2
		));
		int radius = config.renderDistanceRadius();
		WorldPoint centerWp = client.getLocalPlayer().getWorldLocation();
		GeneralPath renderAreaPath = createMinimapRenderAreaBoundary(centerWp, radius);
		if (renderAreaPath != null) {
			Area renderArea = new Area(renderAreaPath);
			renderArea.intersect(minimapCircle);
			minimapCircle.subtract(renderArea);
		}
		graphics.setColor(config.minimapFogColour());
		graphics.fill(minimapCircle);
	}
	private GeneralPath createMinimapRenderAreaBoundary(WorldPoint center, int radius) {
		List<Point> boundaryPoints = new ArrayList<>();
		int sampleRate = Math.max(1, radius / 8);
		for (int x = -radius; x <= radius; x += sampleRate) {
			WorldPoint wp = new WorldPoint(center.getX() + x, center.getY() + radius + 1, center.getPlane());
			Point p = getMinimapPoint(wp);
			if (p != null) boundaryPoints.add(p);
		}
		WorldPoint topRightCorner = new WorldPoint(center.getX() + radius + 1, center.getY() + radius + 1, center.getPlane());
		Point p = getMinimapPoint(topRightCorner);
		if (p != null) boundaryPoints.add(p);
		for (int y = radius; y >= -radius; y -= sampleRate) {
			WorldPoint wp = new WorldPoint(center.getX() + radius + 1, center.getY() + y, center.getPlane());
			p = getMinimapPoint(wp);
			if (p != null) boundaryPoints.add(p);
		}
		WorldPoint bottomRightCorner = new WorldPoint(center.getX() + radius + 1, center.getY() - radius - 1, center.getPlane());
		p = getMinimapPoint(bottomRightCorner);
		if (p != null) boundaryPoints.add(p);
		for (int x = radius; x >= -radius; x -= sampleRate) {
			WorldPoint wp = new WorldPoint(center.getX() + x, center.getY() - radius - 1, center.getPlane());
			p = getMinimapPoint(wp);
			if (p != null) boundaryPoints.add(p);
		}
		WorldPoint bottomLeftCorner = new WorldPoint(center.getX() - radius - 1, center.getY() - radius - 1, center.getPlane());
		p = getMinimapPoint(bottomLeftCorner);
		if (p != null) boundaryPoints.add(p);
		for (int y = -radius; y <= radius; y += sampleRate) {
			if (y == -radius || y == radius) continue; // Skip corners
			WorldPoint wp = new WorldPoint(center.getX() - radius - 1, center.getY() + y, center.getPlane());
			p = getMinimapPoint(wp);
			if (p != null) boundaryPoints.add(p);
		}
		WorldPoint topLeftCorner = new WorldPoint(center.getX() - radius - 1, center.getY() + radius + 1, center.getPlane());
		p = getMinimapPoint(topLeftCorner);
		if (p != null) boundaryPoints.add(p);
		if (boundaryPoints.isEmpty()) return null;
		GeneralPath path = new GeneralPath();
		boolean pathStarted = false;
		for (Point point : boundaryPoints) {
			if (point != null) {
				if (!pathStarted) {
					path.moveTo(point.getX(), point.getY());
					pathStarted = true;
				} else {
					path.lineTo(point.getX(), point.getY());
				}
			}
		}
		if (pathStarted) path.closePath();
		return pathStarted ? path : null;
	}
	private Point getMinimapPoint(WorldPoint worldPoint) {
		LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), worldPoint);
		if (lp == null) return null;
		return Perspective.localToMinimap(client, lp);
	}
	private Widget getMinimapWidget() {
		if (client.isResized()) {
			if (client.getVarbitValue(SIDE_PANELS_ID) == 1) { return client.getWidget(PRE_EOC_PARENT_ID, PRE_EOC_CHILD_ID); }
			return client.getWidget(STRETCH_PARENT_ID, STRETCH_CHILD_ID);
		}
		return client.getWidget(FIXED_PARENT_ID, FIXED_CHILD_ID);
	}
}