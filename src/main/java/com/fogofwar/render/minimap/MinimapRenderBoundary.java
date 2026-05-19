package com.fogofwar.render.minimap;
import com.fogofwar.state.RenderCenter;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;
final class MinimapRenderBoundary {
	private static final int MINIMAP_PROJECTION_DISTANCE = 32768;
	private static final int MINIMAP_RENDER_AREA_PADDING = 1;
	private final Client client;
	private final List<Point> boundaryPoints = new ArrayList<>(128);
	private final MinimapPathCache seaRenderAreaPath = new MinimapPathCache();
	private final MinimapPathCache landRenderAreaPath = new MinimapPathCache();
	MinimapRenderBoundary(Client client) { this.client = client; }
	void clearCaches() {
		seaRenderAreaPath.clear();
		landRenderAreaPath.clear();
	}
	GeneralPath createSeaRenderAreaPath(RenderCenter rc, int radius, int landRadius, Rectangle minimapBounds) {
		LocalPoint centerLp = rc.snappedCenter(radius, landRadius);
		return createRenderAreaPath(rc, centerLp, radius, minimapBounds, seaRenderAreaPath);
	}
	GeneralPath createLandRenderAreaPath(RenderCenter rc, int radius, Rectangle minimapBounds) {
		LocalPoint centerLp = rc.snappedCenter(radius, radius);
		return createRenderAreaPath(rc, centerLp, radius, minimapBounds, landRenderAreaPath);
	}
	private GeneralPath createRenderAreaPath(RenderCenter rc, LocalPoint centerLp, int radius, Rectangle minimapBounds, MinimapPathCache cache) {
		if (centerLp == null) {
			boundaryPoints.clear();
			return createClippedRenderAreaPath(boundaryPoints, minimapBounds, null, cache);
		}
		WorldPoint centerWp = WorldPoint.fromLocal(rc.getWorldView(), centerLp.getX(), centerLp.getY(), rc.getWorldPoint().getPlane());
		List<Point> points = getBoundaryPointsWithNulls(rc.getWorldView(), centerWp, centerLp, radius);
		Point centerPoint = Perspective.localToMinimap(client, centerLp, MINIMAP_PROJECTION_DISTANCE);
		return createClippedRenderAreaPath(points, minimapBounds, centerPoint, cache);
	}
	private GeneralPath createClippedRenderAreaPath(List<Point> points, Rectangle minimapBounds, Point centerPoint, MinimapPathCache cache) {
		GeneralPath path = buildClippedRenderAreaPath(points, minimapBounds, centerPoint, false, cache.path);
		if (isValidRenderAreaPath(path, centerPoint)) return saveValidPath(path, cache);
		path = buildClippedRenderAreaPath(points, minimapBounds, centerPoint, true, cache.path);
		if (isValidRenderAreaPath(path, centerPoint)) return saveValidPath(path, cache);
		return cache.hasLastPath ? cache.lastPath : path;
	}
	private boolean isValidRenderAreaPath(GeneralPath path, Point centerPoint) { return path == null || centerPoint == null || path.contains(centerPoint.getX(), centerPoint.getY()); }
	private GeneralPath saveValidPath(GeneralPath path, MinimapPathCache cache) {
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
	private List<Point> getBoundaryPointsWithNulls(WorldView worldView, WorldPoint center, LocalPoint centerLp, int radius) {
		boundaryPoints.clear();
		if (centerLp == null) return boundaryPoints;
		int sampleRate = Math.max(1, radius / 12);
		int halfTile = Perspective.LOCAL_HALF_TILE_SIZE;
		addMinimapPoint(worldView, center, centerLp, -radius, -radius, -halfTile, -halfTile);
		for (int x = -radius; x <= radius; x += sampleRate) addMinimapPoint(worldView, center, centerLp, x, -radius, 0, -halfTile);
		addMinimapPoint(worldView, center, centerLp, radius, -radius, halfTile, -halfTile);
		for (int y = -radius; y <= radius; y += sampleRate) addMinimapPoint(worldView, center, centerLp, radius, y, halfTile, 0);
		addMinimapPoint(worldView, center, centerLp, radius, radius, halfTile, halfTile);
		for (int x = radius; x >= -radius; x -= sampleRate) addMinimapPoint(worldView, center, centerLp, x, radius, 0, halfTile);
		addMinimapPoint(worldView, center, centerLp, -radius, radius, -halfTile, halfTile);
		for (int y = radius; y >= -radius; y -= sampleRate) addMinimapPoint(worldView, center, centerLp, -radius, y, -halfTile, 0);
		return boundaryPoints;
	}
	private void addMinimapPoint(WorldView worldView, WorldPoint centerWp, LocalPoint centerLp, int tileXOffset, int tileYOffset, int xOffset, int yOffset) {
		if (!WorldPoint.isInScene(worldView, centerWp.getX() + tileXOffset, centerWp.getY() + tileYOffset)) {
			boundaryPoints.add(null);
			return;
		}
		int x = centerLp.getX() + tileXOffset * Perspective.LOCAL_TILE_SIZE + xOffset;
		int y = centerLp.getY() + tileYOffset * Perspective.LOCAL_TILE_SIZE + yOffset;
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
