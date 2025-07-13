package com.fogofwar.box;

import com.fogofwar.FogOfWarConfig;
import com.fogofwar.util.AreaManager;
import com.fogofwar.util.ClientState;
import com.fogofwar.util.DynamicRenderDistance;
import com.fogofwar.util.MinimapUtil;
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
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FogOfWarMinimapOverlay extends Overlay {
	private final Client client;
	private final FogOfWarConfig config;
	private final ClientState clientState;
	private final DynamicRenderDistance dynamicRenderDistance;
	private final AreaManager areaManager;
	private static final int MINIMAP_GROUP_ID = 164;
	private static final int HEALTH_ORB_CHILD_ID = 16;
	private static final int PRAYER_ORB_CHILD_ID = 17;
	private static final int RUN_ORB_CHILD_ID = 18;
	private static final int SPEC_ORB_CHILD_ID = 19;
	private static final int ORBS_GROUP_ID = 160;
	private static final int WORLD_MAP_ORB_CHILD_ID = 48;
	private static final int HALF_TILE_OFFSET = 64;

	private final List<Point> boundaryPoints = new ArrayList<>();
	@Inject
	public FogOfWarMinimapOverlay(Client client, FogOfWarConfig config, ClientState clientState, DynamicRenderDistance dynamicRenderDistance, AreaManager areaManager) {
		this.client = client;
		this.config = config;
		this.clientState = clientState;
		this.dynamicRenderDistance = dynamicRenderDistance;
		this.areaManager = areaManager;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(Overlay.PRIORITY_LOW);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		if (areaManager.isPlayerInExcludedArea() || clientState.isClientNotReady() || (config.onlyInWilderness() && clientState.isNotInWilderness())) {
			return null;
		}
		Widget minimap = MinimapUtil.getMinimapWidget(client);
		if (minimap == null || minimap.isHidden()) return null;
		Shape minimapClipShape = getMinimapClipShape(minimap);
		Shape oldClip = graphics.getClip();
		graphics.setClip(minimapClipShape);
		int radius = dynamicRenderDistance.getCurrentRenderDistance();
		WorldPoint centerWp = client.getLocalPlayer().getWorldLocation();
		List<Point> boundaryPoints = getBoundaryPointsWithNulls(centerWp, radius);
		GeneralPath renderAreaPath = createClippedRenderAreaPath(boundaryPoints, minimap.getBounds());
		if (config.showMinimapFog()) {
			renderMinimapFog(graphics, minimapClipShape, renderAreaPath);
		}
		if (config.showMinimapBorder() && renderAreaPath != null) {
			renderMinimapBorder(graphics, renderAreaPath);
		}
		graphics.setClip(oldClip);
		return null;
	}
	private Shape getMinimapClipShape(Widget minimapWidget) {
		Rectangle bounds = minimapWidget.getBounds();
		Area clipArea = new Area(new Ellipse2D.Double(bounds.getX() - 1, bounds.getY() - 1, bounds.getWidth() + 2, bounds.getHeight() + 2));
		int[] orbChildIds = {HEALTH_ORB_CHILD_ID, PRAYER_ORB_CHILD_ID, RUN_ORB_CHILD_ID, SPEC_ORB_CHILD_ID};
		for (int childId : orbChildIds) {
			Widget orb = client.getWidget(MINIMAP_GROUP_ID, childId);
			if (orb != null && !orb.isHidden()) {
				Rectangle orbBounds = orb.getBounds();
				clipArea.subtract(new Area(new Ellipse2D.Double(orbBounds.getX(), orbBounds.getY(), orbBounds.getWidth(), orbBounds.getHeight())));
			}
		}
		Widget worldMapOrb = client.getWidget(ORBS_GROUP_ID, WORLD_MAP_ORB_CHILD_ID);
		if (worldMapOrb != null && !worldMapOrb.isHidden()) {
			Rectangle orbBounds = worldMapOrb.getBounds();
			clipArea.subtract(new Area(new Ellipse2D.Double(orbBounds.getX(), orbBounds.getY(), orbBounds.getWidth(), orbBounds.getHeight())));
		}

		return clipArea;
	}
	private void renderMinimapBorder(Graphics2D graphics, GeneralPath renderAreaPath) {
		graphics.setColor(config.minimapBorderColour());
		graphics.setStroke(new BasicStroke(config.minimapBorderThickness()));
		graphics.draw(renderAreaPath);
	}
	private void renderMinimapFog(Graphics2D graphics, Shape minimapClipShape, GeneralPath renderAreaPath) {
		Area fogArea = new Area(minimapClipShape);
		if (renderAreaPath != null) {
			fogArea.subtract(new Area(renderAreaPath));
		}
		graphics.setColor(config.minimapFogColour());
		graphics.fill(fogArea);
	}
	private GeneralPath createClippedRenderAreaPath(List<Point> boundaryPoints, Rectangle minimapBounds) {
		List<Point> visiblePoints = boundaryPoints.stream().filter(Objects::nonNull).collect(Collectors.toList());
		if (visiblePoints.isEmpty()) return createFullMinimapCoveragePath(minimapBounds);
		if (visiblePoints.size() == boundaryPoints.size()) {
			GeneralPath path = new GeneralPath();
			Point first = visiblePoints.get(0);
			path.moveTo(first.getX(), first.getY());
			for (int i = 1; i < visiblePoints.size(); i++) {
				path.lineTo(visiblePoints.get(i).getX(), visiblePoints.get(i).getY());
			}
			path.closePath();
			return path;
		}
		int firstVisible = -1;
		for (int i = 0; i < boundaryPoints.size(); i++) {
			if (boundaryPoints.get(i) != null) {
				firstVisible = i;
				break;
			}
		}
		if (firstVisible == -1) return null;
		GeneralPath path = new GeneralPath();
		path.moveTo(boundaryPoints.get(firstVisible).getX(), boundaryPoints.get(firstVisible).getY());
		int n = boundaryPoints.size();
		for (int i = 0; i < n; i++) {
			int currentIndex = (firstVisible + i) % n;
			int nextIndex = (firstVisible + i + 1) % n;
			Point p1 = boundaryPoints.get(currentIndex);
			Point p2 = boundaryPoints.get(nextIndex);
			if (p1 != null) {
				if (p2 != null) {
					path.lineTo(p2.getX(), p2.getY());
				} else {
					int nextVisibleIndex = -1;
					for (int j = 2; j < n; j++) {
						if (boundaryPoints.get((currentIndex + j) % n) != null) {
							nextVisibleIndex = (currentIndex + j) % n;
							break;
						}
					}
					if (nextVisibleIndex != -1) {
						Point nextVisiblePoint = boundaryPoints.get(nextVisibleIndex);
						addArcToPath(path, p1, nextVisiblePoint, minimapBounds);
					}
				}
			}
		}
		path.closePath();
		return path;
	}
	private void addArcToPath(GeneralPath path, Point p1, Point p2, Rectangle minimapBounds) {
		double centerX = minimapBounds.getCenterX();
		double centerY = minimapBounds.getCenterY();
		double radius = minimapBounds.width / 2.0 + 1;
		double startAngle = Math.toDegrees(Math.atan2(p1.getY() - centerY, p1.getX() - centerX));
		double endAngle = Math.toDegrees(Math.atan2(p2.getY() - centerY, p2.getX() - centerX));
		double sweep = endAngle - startAngle;
		if (sweep <= -180) { sweep += 360; } else if (sweep > 180) { sweep -= 360; }
		int numSteps = (int) (Math.abs(sweep) / 10) + 1;
		for (int i = 1; i <= numSteps; i++) {
			double currentAngleRad = Math.toRadians(startAngle + (sweep * i / numSteps));
			float x = (float) (centerX + radius * Math.cos(currentAngleRad));
			float y = (float) (centerY + radius * Math.sin(currentAngleRad));
			path.lineTo(x, y);
		}
	}
	private List<Point> getBoundaryPointsWithNulls(WorldPoint center, int radius) {
		boundaryPoints.clear();
		int sampleRate = Math.max(1, radius / 12);
		boundaryPoints.add(getMinimapPointWithOffset(new WorldPoint(center.getX() - radius, center.getY() - radius, center.getPlane()), -HALF_TILE_OFFSET, -HALF_TILE_OFFSET));
		for (int x = -radius; x <= radius; x += sampleRate) { boundaryPoints.add(getMinimapPointWithOffset(new WorldPoint(center.getX() + x, center.getY() - radius, center.getPlane()), 0, -HALF_TILE_OFFSET)); }
		boundaryPoints.add(getMinimapPointWithOffset(new WorldPoint(center.getX() + radius, center.getY() - radius, center.getPlane()), HALF_TILE_OFFSET, -HALF_TILE_OFFSET));
		for (int y = -radius; y <= radius; y += sampleRate) { boundaryPoints.add(getMinimapPointWithOffset(new WorldPoint(center.getX() + radius, center.getY() + y, center.getPlane()), HALF_TILE_OFFSET, 0)); }
		boundaryPoints.add(getMinimapPointWithOffset(new WorldPoint(center.getX() + radius, center.getY() + radius, center.getPlane()), HALF_TILE_OFFSET, HALF_TILE_OFFSET));
		for (int x = radius; x >= -radius; x -= sampleRate) { boundaryPoints.add(getMinimapPointWithOffset(new WorldPoint(center.getX() + x, center.getY() + radius, center.getPlane()), 0, HALF_TILE_OFFSET)); }
		boundaryPoints.add(getMinimapPointWithOffset(new WorldPoint(center.getX() - radius, center.getY() + radius, center.getPlane()), -HALF_TILE_OFFSET, HALF_TILE_OFFSET));
		for (int y = radius; y >= -radius; y -= sampleRate) { boundaryPoints.add(getMinimapPointWithOffset(new WorldPoint(center.getX() - radius, center.getY() + y, center.getPlane()), -HALF_TILE_OFFSET, 0)); }
		return boundaryPoints;
	}
	private Point getMinimapPointWithOffset(WorldPoint worldPoint, int xOffset, int yOffset) {
		LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), worldPoint);
		if (lp == null) return null;
		LocalPoint offsetLp = new LocalPoint(lp.getX() + xOffset, lp.getY() + yOffset, client.getTopLevelWorldView());
		return Perspective.localToMinimap(client, offsetLp);
	}
	private GeneralPath createFullMinimapCoveragePath(Rectangle minimapBounds) {
		GeneralPath fullPath = new GeneralPath();
		double centerX = minimapBounds.getCenterX();
		double centerY = minimapBounds.getCenterY();
		double radius = Math.max(minimapBounds.width, minimapBounds.height);
		int numSegments = 32;
		for (int i = 0; i < numSegments; i++) {
			double angle = 2 * Math.PI * i / numSegments;
			double x = centerX + radius * Math.cos(angle);
			double y = centerY + radius * Math.sin(angle);
			if (i == 0) {
				fullPath.moveTo(x, y);
			} else {
				fullPath.lineTo(x, y);
			}
		}
		fullPath.closePath();
		return fullPath;
	}
}