package com.fogofwar.box;
import com.fogofwar.FogOfWarConfig;
import com.fogofwar.util.AreaManager;
import com.fogofwar.util.ClientState;
import com.fogofwar.util.DynamicRenderDistance;
import com.fogofwar.util.MinimapUtil;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
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
public class FogOfWarMinimapOverlay extends Overlay {
	private final Client client;
	private final FogOfWarConfig config;
	private final ClientState clientState;
	private final DynamicRenderDistance dynamicRenderDistance;
	private final AreaManager areaManager;
	private static final int HEALTH_ORB_CHILD_ID = 16;
	private static final int PRAYER_ORB_CHILD_ID = 17;
	private static final int RUN_ORB_CHILD_ID = 18;
	private static final int SPEC_ORB_CHILD_ID = 19;
	private static final int ORBS_GROUP_ID = 160;
	private static final int WORLD_MAP_ORB_CHILD_ID = 48;
	private static final int LOCAL_TILE_SIZE = 128;
	private static final int HALF_TILE_OFFSET = 64;
	private static final int[] ORB_CHILD_IDS = {HEALTH_ORB_CHILD_ID, PRAYER_ORB_CHILD_ID, RUN_ORB_CHILD_ID, SPEC_ORB_CHILD_ID};
	private final List<Point> boundaryPoints = new ArrayList<>(64);
	private final List<Point> visiblePoints = new ArrayList<>(64);
	private final GeneralPath renderAreaPath = new GeneralPath();
	private final GeneralPath fullMinimapCoveragePath = new GeneralPath();
	private BasicStroke borderStroke;
	private int borderStrokeWidth = -1;
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
		if (clientState.isSuppressed(config, areaManager)) return null;
		boolean showFog = config.showMinimapFog();
		boolean showBorder = config.showMinimapBorder();
		if (!showFog && !showBorder) return null;
		Widget minimap = MinimapUtil.getMinimapWidget(client);
		if (minimap == null || minimap.isHidden()) return null;
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null) return null;
		WorldPoint centerWp = client.getLocalPlayer().getWorldLocation();
		if (centerWp == null) return null;
		Shape minimapClipShape = getMinimapClipShape(minimap);
		Shape oldClip = graphics.getClip();
		graphics.setClip(minimapClipShape);
		int radius = dynamicRenderDistance.getCurrentRenderDistance();
		List<Point> boundaryPoints = getBoundaryPointsWithNulls(worldView, centerWp, radius);
		GeneralPath fogPath = createClippedRenderAreaPath(boundaryPoints, minimap.getBounds());
		if (fogPath == null) {
			graphics.setClip(oldClip);
			return null;
		}
		if (showFog) renderMinimapFog(graphics, minimapClipShape, fogPath);
		if (showBorder) renderMinimapBorder(graphics, fogPath);
		graphics.setClip(oldClip);
		return null;
	}
	private Shape getMinimapClipShape(Widget minimapWidget) {
		Rectangle bounds = minimapWidget.getBounds();
		Area clipArea = new Area(new Ellipse2D.Double(bounds.getX() - 1, bounds.getY() - 1, bounds.getWidth() + 2, bounds.getHeight() + 2));
		for (int childId : ORB_CHILD_IDS) {
			Widget orb = client.getWidget(MinimapUtil.MINIMAP_GROUP_ID, childId);
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
	private void renderMinimapBorder(Graphics2D graphics, GeneralPath path) {
		Stroke oldStroke = graphics.getStroke();
		graphics.setColor(config.minimapBorderColour());
		graphics.setStroke(getBorderStroke());
		graphics.draw(path);
		graphics.setStroke(oldStroke);
	}
	private BasicStroke getBorderStroke() {
		int width = config.minimapBorderThickness();
		if (borderStroke == null || borderStrokeWidth != width) {
			borderStroke = new BasicStroke(width);
			borderStrokeWidth = width;
		}
		return borderStroke;
	}
	private void renderMinimapFog(Graphics2D graphics, Shape minimapClipShape, GeneralPath renderAreaPath) {
		Area renderArea = new Area(renderAreaPath);
		if (renderArea.contains(minimapClipShape.getBounds2D())) return;
		Area fogArea = new Area(minimapClipShape);
		fogArea.subtract(renderArea);
		graphics.setColor(config.minimapFogColour());
		graphics.fill(fogArea);
	}
	private GeneralPath createClippedRenderAreaPath(List<Point> boundaryPoints, Rectangle minimapBounds) {
		visiblePoints.clear();
		int firstVisible = -1;
		for (int i = 0; i < boundaryPoints.size(); i++) {
			Point point = boundaryPoints.get(i);
			if (point != null) {
				if (firstVisible == -1) firstVisible = i;
				visiblePoints.add(point);
			}
		}
		if (visiblePoints.isEmpty()) return createFullMinimapCoveragePath(minimapBounds);
		GeneralPath path = renderAreaPath;
		path.reset();
		if (visiblePoints.size() == boundaryPoints.size()) {
			Point first = visiblePoints.get(0);
			path.moveTo(first.getX(), first.getY());
			for (int i = 1; i < visiblePoints.size(); i++) path.lineTo(visiblePoints.get(i).getX(), visiblePoints.get(i).getY());
			path.closePath();
			return path;
		}
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
					if (nextVisibleIndex != -1) addArcToPath(path, p1, boundaryPoints.get(nextVisibleIndex), minimapBounds);
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
			path.lineTo((float) (centerX + radius * Math.cos(currentAngleRad)), (float) (centerY + radius * Math.sin(currentAngleRad)));
		}
	}
	private List<Point> getBoundaryPointsWithNulls(WorldView worldView, WorldPoint center, int radius) {
		boundaryPoints.clear();
		LocalPoint centerLp = LocalPoint.fromWorld(worldView, center);
		if (centerLp == null) return boundaryPoints;
		int sampleRate = Math.max(1, radius / 12);
		addMinimapPoint(worldView, center, centerLp, -radius, -radius, -HALF_TILE_OFFSET, -HALF_TILE_OFFSET);
		for (int x = -radius; x <= radius; x += sampleRate) addMinimapPoint(worldView, center, centerLp, x, -radius, 0, -HALF_TILE_OFFSET);
		addMinimapPoint(worldView, center, centerLp, radius, -radius, HALF_TILE_OFFSET, -HALF_TILE_OFFSET);
		for (int y = -radius; y <= radius; y += sampleRate) addMinimapPoint(worldView, center, centerLp, radius, y, HALF_TILE_OFFSET, 0);
		addMinimapPoint(worldView, center, centerLp, radius, radius, HALF_TILE_OFFSET, HALF_TILE_OFFSET);
		for (int x = radius; x >= -radius; x -= sampleRate) addMinimapPoint(worldView, center, centerLp, x, radius, 0, HALF_TILE_OFFSET);
		addMinimapPoint(worldView, center, centerLp, -radius, radius, -HALF_TILE_OFFSET, HALF_TILE_OFFSET);
		for (int y = radius; y >= -radius; y -= sampleRate) addMinimapPoint(worldView, center, centerLp, -radius, y, -HALF_TILE_OFFSET, 0);
		return boundaryPoints;
	}
	private void addMinimapPoint(WorldView worldView, WorldPoint centerWp, LocalPoint centerLp, int tileXOffset, int tileYOffset, int xOffset, int yOffset) {
		if (!WorldPoint.isInScene(worldView, centerWp.getX() + tileXOffset, centerWp.getY() + tileYOffset)) {
			boundaryPoints.add(null);
			return;
		}
		int x = centerLp.getX() + tileXOffset * LOCAL_TILE_SIZE + xOffset;
		int y = centerLp.getY() + tileYOffset * LOCAL_TILE_SIZE + yOffset;
		boundaryPoints.add(Perspective.localToMinimap(client, new LocalPoint(x, y, worldView)));
	}
	private GeneralPath createFullMinimapCoveragePath(Rectangle minimapBounds) {
		fullMinimapCoveragePath.reset();
		double centerX = minimapBounds.getCenterX();
		double centerY = minimapBounds.getCenterY();
		double radius = Math.max(minimapBounds.width, minimapBounds.height);
		int numSegments = 32;
		for (int i = 0; i < numSegments; i++) {
			double angle = 2 * Math.PI * i / numSegments;
			double x = centerX + radius * Math.cos(angle);
			double y = centerY + radius * Math.sin(angle);
			if (i == 0) fullMinimapCoveragePath.moveTo(x, y);
			else fullMinimapCoveragePath.lineTo(x, y);
		}
		fullMinimapCoveragePath.closePath();
		return fullMinimapCoveragePath;
	}
}