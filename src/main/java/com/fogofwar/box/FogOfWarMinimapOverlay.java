package com.fogofwar.box;
import com.fogofwar.FogDisplayMode;
import com.fogofwar.FogOfWarConfig;
import com.fogofwar.util.AreaManager;
import com.fogofwar.util.CachedStroke;
import com.fogofwar.util.ClientState;
import com.fogofwar.util.DynamicRenderDistance;
import com.fogofwar.util.MinimapUtil;
import com.fogofwar.util.RenderCenter;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
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
import java.util.Arrays;
import java.util.List;
public class FogOfWarMinimapOverlay extends Overlay {
	private final Client client;
	private final FogOfWarConfig config;
	private final ClientState clientState;
	private final DynamicRenderDistance dynamicRenderDistance;
	private final AreaManager areaManager;
	private static final int LOCAL_TILE_SIZE = 128;
	private static final int HALF_TILE_OFFSET = 64;
	private static final int MINIMAP_PROJECTION_DISTANCE = 32768;
	private static final int MINIMAP_RENDER_AREA_PADDING = 1;
	private static final int RESIZED_MINIMAP_CLIP_PADDING = 1;
	private static final int FIXED_MINIMAP_CLIP_PADDING = 3;
	private static final int SAILING_LAND_ALPHA_FLOOR = 32;
	private static final int[] ORB_WIDGETS = {InterfaceID.Orbs.HEALTH_BACKING, InterfaceID.Orbs.PRAYER_BACKING, InterfaceID.Orbs.RUNENERGY_BACKING, InterfaceID.Orbs.SPECENERGY_BACKING, InterfaceID.Orbs.ORB_WORLDMAP};
	private final List<Point> boundaryPoints = new ArrayList<>(128);
	private final PathCache renderAreaPath = new PathCache();
	private final PathCache sailingLandRenderAreaPath = new PathCache();
	private final GeneralPath fogFillPath = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
	private final CachedStroke borderStroke = new CachedStroke();
	private final Rectangle[] currentOrbBounds = new Rectangle[ORB_WIDGETS.length];
	private Rectangle cachedMinimapBounds;
	private Shape cachedClipShape;
	private long cachedOrbsHash;
	private boolean cachedResized;
	private static class PathCache {
		final GeneralPath path = new GeneralPath();
		final GeneralPath lastPath = new GeneralPath();
		boolean hasLastPath;
	}
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
	public void clearCaches() {
		cachedMinimapBounds = null;
		cachedClipShape = null;
		cachedOrbsHash = 0;
		renderAreaPath.hasLastPath = false;
		sailingLandRenderAreaPath.hasLastPath = false;
		Arrays.fill(currentOrbBounds, null);
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		if (clientState.isSuppressed(config, areaManager)) return null;
		FogDisplayMode mode = config.minimapDisplayMode();
		boolean showFog = mode.showsFog();
		boolean showBorder = mode.showsBorder();
		if (!showFog && !showBorder) return null;
		Widget minimap = MinimapUtil.getMinimapWidget(client);
		if (minimap == null || minimap.isHidden()) return null;
		RenderCenter rc = RenderCenter.resolve(client);
		if (rc == null) return null;
		Shape minimapClipShape = getMinimapClipShape(minimap);
		Shape oldClip = graphics.getClip();
		graphics.setClip(minimapClipShape);
		int landRadius = dynamicRenderDistance.getCurrentRenderDistance();
		int radius = rc.isOnWorldEntity() ? config.sailingRenderDistance() : landRadius;
		LocalPoint centerLp = getRenderCenter(rc, radius, landRadius);
		WorldPoint centerWp = centerLp != null ? WorldPoint.fromLocal(rc.getWorldView(), centerLp.getX(), centerLp.getY(), rc.getWorldPoint().getPlane()) : rc.getWorldPoint();
		List<Point> points = getBoundaryPointsWithNulls(rc.getWorldView(), centerWp, centerLp, radius);
		Point centerPoint = centerLp != null ? Perspective.localToMinimap(client, centerLp, MINIMAP_PROJECTION_DISTANCE) : null;
		GeneralPath fogPath = createClippedRenderAreaPath(points, minimap.getBounds(), centerPoint, renderAreaPath);
		if (fogPath == null) {
			graphics.setClip(oldClip);
			return null;
		}
		boolean showSailingLandRenderDistance = rc.isOnWorldEntity() && config.showMinimapLandRenderDistanceWhileSailing() && landRadius < radius;
		GeneralPath sailingLandPath = null;
		if (showSailingLandRenderDistance) {
			LocalPoint sailingLandCenterLp = getRenderCenter(rc, landRadius, landRadius);
			WorldPoint sailingLandCenterWp = sailingLandCenterLp != null ? WorldPoint.fromLocal(rc.getWorldView(), sailingLandCenterLp.getX(), sailingLandCenterLp.getY(), rc.getWorldPoint().getPlane()) : rc.getWorldPoint();
			List<Point> sailingLandPoints = getBoundaryPointsWithNulls(rc.getWorldView(), sailingLandCenterWp, sailingLandCenterLp, landRadius);
			Point sailingLandCenterPoint = sailingLandCenterLp != null ? Perspective.localToMinimap(client, sailingLandCenterLp, MINIMAP_PROJECTION_DISTANCE) : null;
			sailingLandPath = createClippedRenderAreaPath(sailingLandPoints, minimap.getBounds(), sailingLandCenterPoint, sailingLandRenderAreaPath);
		}
		if (showFog) renderMinimapFog(graphics, minimapClipShape, fogPath);
		if (showFog && sailingLandPath != null) renderSailingExtendedFog(graphics, fogPath, sailingLandPath);
		if (showBorder) {
			renderMinimapBorder(graphics, minimapClipShape, fogPath);
			if (sailingLandPath != null) renderSailingLandBorder(graphics, minimapClipShape, sailingLandPath);
		}
		graphics.setClip(oldClip);
		return null;
	}
	private Shape getMinimapClipShape(Widget minimapWidget) {
		Rectangle bounds = minimapWidget.getBounds();
		boolean resized = client.isResized();
		long orbsHash = collectOrbBoundsAndHash();
		if (cachedClipShape != null && bounds.equals(cachedMinimapBounds) && orbsHash == cachedOrbsHash && resized == cachedResized) return cachedClipShape;
		Area clipArea = createEllipse(bounds, resized ? RESIZED_MINIMAP_CLIP_PADDING : FIXED_MINIMAP_CLIP_PADDING);
		for (Rectangle ob : currentOrbBounds) {
			if (ob == null) continue;
			clipArea.subtract(createEllipse(ob, 0));
		}
		cachedClipShape = clipArea;
		cachedMinimapBounds = new Rectangle(bounds);
		cachedOrbsHash = orbsHash;
		cachedResized = resized;
		return cachedClipShape;
	}
	private static Area createEllipse(Rectangle bounds, int padding) { return new Area(new Ellipse2D.Double(bounds.getX() - padding, bounds.getY() - padding, bounds.getWidth() + padding * 2, bounds.getHeight() + padding * 2)); }
	private long collectOrbBoundsAndHash() {
		long h = 1469598103934665603L;
		for (int i = 0; i < ORB_WIDGETS.length; i++) {
			Widget orb = client.getWidget(ORB_WIDGETS[i]);
			Rectangle b = (orb != null && !orb.isHidden()) ? orb.getBounds() : null;
			currentOrbBounds[i] = b;
			if (b == null) h ^= 0xDEADBEEFL;
			else h ^= ((long) b.x << 48) ^ ((long) b.y << 32) ^ ((long) b.width << 16) ^ (b.height & 0xFFFFL);
			h *= 1099511628211L;
		}
		return h;
	}
	private void renderMinimapBorder(Graphics2D graphics, Shape minimapClipShape, GeneralPath path) {
		if (path.contains(minimapClipShape.getBounds2D())) return;
		Stroke oldStroke = graphics.getStroke();
		graphics.setColor(config.minimapBorderColour());
		graphics.setStroke(borderStroke.get(config.minimapBorderThickness()));
		graphics.draw(path);
		graphics.setStroke(oldStroke);
	}
	private void renderMinimapFog(Graphics2D graphics, Shape minimapClipShape, GeneralPath path) {
		if (path.contains(minimapClipShape.getBounds2D())) return;
		Rectangle b = minimapClipShape.getBounds();
		fogFillPath.reset();
		fogFillPath.moveTo(b.x - 1, b.y - 1);
		fogFillPath.lineTo(b.x + b.width + 1, b.y - 1);
		fogFillPath.lineTo(b.x + b.width + 1, b.y + b.height + 1);
		fogFillPath.lineTo(b.x - 1, b.y + b.height + 1);
		fogFillPath.closePath();
		fogFillPath.append(path, false);
		graphics.setColor(config.minimapFogColour());
		graphics.fill(fogFillPath);
	}
	private void renderSailingExtendedFog(Graphics2D graphics, GeneralPath boundary, GeneralPath landBoundary) {
		Area area = new Area(boundary);
		area.subtract(new Area(landBoundary));
		if (area.isEmpty()) return;
		graphics.setColor(getSailingMinimapFogColour());
		graphics.fill(area);
	}
	private Color getSailingMinimapFogColour() {
		Color colour = config.minimapFogColour();
		return getSailingLandColour(colour);
	}
	private void renderSailingLandBorder(Graphics2D graphics, Shape minimapClipShape, GeneralPath path) {
		if (path.contains(minimapClipShape.getBounds2D())) return;
		Stroke oldStroke = graphics.getStroke();
		graphics.setColor(getSailingMinimapBorderColour());
		graphics.setStroke(borderStroke.get(config.minimapBorderThickness()));
		graphics.draw(path);
		graphics.setStroke(oldStroke);
	}
	private Color getSailingMinimapBorderColour() {
		Color colour = config.minimapBorderColour();
		return getSailingLandColour(colour);
	}
	private Color getSailingLandColour(Color colour) {
		return new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), Math.max(SAILING_LAND_ALPHA_FLOOR, colour.getAlpha() / 2));
	}
	private GeneralPath createClippedRenderAreaPath(List<Point> points, Rectangle minimapBounds, Point centerPoint, PathCache cache) {
		GeneralPath path = buildClippedRenderAreaPath(points, minimapBounds, centerPoint, false, cache.path);
		if (isValidRenderAreaPath(path, centerPoint)) return saveValidPath(path, cache);
		path = buildClippedRenderAreaPath(points, minimapBounds, centerPoint, true, cache.path);
		if (isValidRenderAreaPath(path, centerPoint)) return saveValidPath(path, cache);
		return cache.hasLastPath ? cache.lastPath : path;
	}
	private boolean isValidRenderAreaPath(GeneralPath path, Point centerPoint) { return path == null || centerPoint == null || path.contains(centerPoint.getX(), centerPoint.getY()); }
	private GeneralPath saveValidPath(GeneralPath path, PathCache cache) {
		if (path != null) {
			cache.lastPath.reset();
			cache.lastPath.append(path, false);
			cache.hasLastPath = true;
		}
		return path;
	}
	private GeneralPath buildClippedRenderAreaPath(List<Point> points, Rectangle minimapBounds, Point centerPoint, boolean reverseArc, GeneralPath path) {
		int n = points.size();
		int firstVisible = -1;
		int visibleCount = 0;
		for (int i = 0; i < n; i++) {
			if (points.get(i) != null) {
				if (firstVisible == -1) firstVisible = i;
				visibleCount++;
			}
		}
		if (visibleCount == 0) return createFullMinimapCoveragePath(minimapBounds, path);
		path.reset();
		if (visibleCount == n) {
			Point first = padRenderAreaPoint(points.get(0), minimapBounds, centerPoint);
			path.moveTo(first.getX(), first.getY());
			for (int i = 1; i < n; i++) {
				Point p = padRenderAreaPoint(points.get(i), minimapBounds, centerPoint);
				path.lineTo(p.getX(), p.getY());
			}
			path.closePath();
			return path;
		}
		Point first = padRenderAreaPoint(points.get(firstVisible), minimapBounds, centerPoint);
		path.moveTo(first.getX(), first.getY());
		for (int i = 0; i < n; i++) {
			int currentIndex = (firstVisible + i) % n;
			int nextIndex = (firstVisible + i + 1) % n;
			Point p1 = points.get(currentIndex);
			Point p2 = points.get(nextIndex);
			if (p1 != null) {
				if (p2 != null) {
					Point p = padRenderAreaPoint(p2, minimapBounds, centerPoint);
					path.lineTo(p.getX(), p.getY());
				} else {
					int nextVisibleIndex = -1;
					for (int j = 2; j < n; j++) {
						if (points.get((currentIndex + j) % n) != null) {
							nextVisibleIndex = (currentIndex + j) % n;
							break;
						}
					}
					if (nextVisibleIndex != -1) addArcToPath(path, padRenderAreaPoint(p1, minimapBounds, centerPoint), padRenderAreaPoint(points.get(nextVisibleIndex), minimapBounds, centerPoint), minimapBounds, reverseArc);
				}
			}
		}
		path.closePath();
		return path;
	}
	private Point padRenderAreaPoint(Point point, Rectangle minimapBounds, Point centerPoint) {
		double centerX = centerPoint != null ? centerPoint.getX() : minimapBounds.getCenterX();
		double centerY = centerPoint != null ? centerPoint.getY() : minimapBounds.getCenterY();
		double dx = point.getX() - centerX;
		double dy = point.getY() - centerY;
		double distance = Math.hypot(dx, dy);
		if (distance == 0) return point;
		return new Point((int) Math.round(point.getX() + dx * MINIMAP_RENDER_AREA_PADDING / distance), (int) Math.round(point.getY() + dy * MINIMAP_RENDER_AREA_PADDING / distance));
	}
	private void addArcToPath(GeneralPath path, Point p1, Point p2, Rectangle minimapBounds, boolean reverse) {
		double centerX = minimapBounds.getCenterX();
		double centerY = minimapBounds.getCenterY();
		double radius = Math.max(minimapBounds.width, minimapBounds.height) / 2.0 + 1;
		double startAngle = Math.toDegrees(Math.atan2(p1.getY() - centerY, p1.getX() - centerX));
		double endAngle = Math.toDegrees(Math.atan2(p2.getY() - centerY, p2.getX() - centerX));
		double sweep = endAngle - startAngle;
		if (sweep <= -180) { sweep += 360; } else if (sweep > 180) { sweep -= 360; }
		if (reverse) sweep += sweep > 0 ? -360 : 360;
		int numSteps = (int) (Math.abs(sweep) / 10) + 1;
		for (int i = 1; i <= numSteps; i++) {
			double currentAngleRad = Math.toRadians(startAngle + (sweep * i / numSteps));
			path.lineTo((float) (centerX + radius * Math.cos(currentAngleRad)), (float) (centerY + radius * Math.sin(currentAngleRad)));
		}
	}
	private LocalPoint getRenderCenter(RenderCenter rc, int radius, int landRadius) {
		LocalPoint lp = rc.isOnWorldEntity() && radius > landRadius ? rc.getTargetLocalPoint() : rc.getLocalPoint();
		if (lp == null) return null;
		return new LocalPoint(snapAxis(lp.getX()), snapAxis(lp.getY()), rc.getWorldView());
	}
	private List<Point> getBoundaryPointsWithNulls(WorldView worldView, WorldPoint center, LocalPoint centerLp, int radius) {
		boundaryPoints.clear();
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
	private static int snapAxis(int current) { return (current / LOCAL_TILE_SIZE) * LOCAL_TILE_SIZE + HALF_TILE_OFFSET; }
	private void addMinimapPoint(WorldView worldView, WorldPoint centerWp, LocalPoint centerLp, int tileXOffset, int tileYOffset, int xOffset, int yOffset) {
		if (!WorldPoint.isInScene(worldView, centerWp.getX() + tileXOffset, centerWp.getY() + tileYOffset)) {
			boundaryPoints.add(null);
			return;
		}
		int x = centerLp.getX() + tileXOffset * LOCAL_TILE_SIZE + xOffset;
		int y = centerLp.getY() + tileYOffset * LOCAL_TILE_SIZE + yOffset;
		boundaryPoints.add(Perspective.localToMinimap(client, new LocalPoint(x, y, worldView), MINIMAP_PROJECTION_DISTANCE));
	}
	private GeneralPath createFullMinimapCoveragePath(Rectangle minimapBounds, GeneralPath path) {
		path.reset();
		double centerX = minimapBounds.getCenterX();
		double centerY = minimapBounds.getCenterY();
		double radius = Math.max(minimapBounds.width, minimapBounds.height);
		int numSegments = 32;
		for (int i = 0; i < numSegments; i++) {
			double angle = 2 * Math.PI * i / numSegments;
			double x = centerX + radius * Math.cos(angle);
			double y = centerY + radius * Math.sin(angle);
			if (i == 0) path.moveTo(x, y);
			else path.lineTo(x, y);
		}
		path.closePath();
		return path;
	}
}
