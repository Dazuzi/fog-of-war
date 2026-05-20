package com.fogofwar.render.minimap;
import com.fogofwar.render.BoundaryPathBuilder;
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
final class MinimapRenderBoundary implements BoundaryPathBuilder.Strategy {
	private static final int MINIMAP_PROJECTION_DISTANCE = 32768;
	private static final int MINIMAP_RENDER_AREA_PADDING = 1;
	private final Client client;
	private final List<Point> boundaryPoints = new ArrayList<>(128);
	private final MinimapPathCache seaRenderAreaPath = new MinimapPathCache();
	private final MinimapPathCache landRenderAreaPath = new MinimapPathCache();
	private Rectangle currentMinimapBounds;
	private Point currentCenterPoint;
	private MinimapPathCache currentCache;
	MinimapRenderBoundary(Client client) { this.client = client; }
	void clearCaches() {
		seaRenderAreaPath.clear();
		landRenderAreaPath.clear();
	}
	GeneralPath createSeaRenderAreaPath(RenderCenter rc, int radius, Rectangle minimapBounds) {
		return buildRenderAreaPath(rc, rc.snappedCenter(), radius, minimapBounds, seaRenderAreaPath);
	}
	GeneralPath createLandRenderAreaPath(RenderCenter rc, int radius, Rectangle minimapBounds) {
		return buildRenderAreaPath(rc, rc.snappedCenter(), radius, minimapBounds, landRenderAreaPath);
	}
	private GeneralPath buildRenderAreaPath(RenderCenter rc, LocalPoint centerLp, int radius, Rectangle minimapBounds, MinimapPathCache cache) {
		WorldPoint centerWp = WorldPoint.fromLocal(rc.getWorldView(), centerLp.getX(), centerLp.getY(), rc.getWorldPoint().getPlane());
		currentMinimapBounds = minimapBounds;
		currentCenterPoint = Perspective.localToMinimap(client, centerLp, MINIMAP_PROJECTION_DISTANCE);
		currentCache = cache;
		collectBoundaryPoints(rc.getWorldView(), centerWp, centerLp, radius);
		double arcRadius = Math.max(minimapBounds.width, minimapBounds.height) / 2.0 + 1;
		GeneralPath path = BoundaryPathBuilder.build(cache.path, boundaryPoints, minimapBounds.getCenterX(), minimapBounds.getCenterY(), arcRadius, this);
		return path != cache.lastPath && isValid(path) ? saveValidPath(path, cache) : path;
	}
	@Override
	public GeneralPath coverage(GeneralPath path) { return fullMinimapCoveragePath(path); }
	@Override
	public boolean isValid(GeneralPath path) { return path == null || currentCenterPoint == null || path.contains(currentCenterPoint.getX(), currentCenterPoint.getY()); }
	@Override
	public GeneralPath fallback(GeneralPath path) { return currentCache.hasLastPath() ? currentCache.lastPath : path; }
	private GeneralPath saveValidPath(GeneralPath path, MinimapPathCache cache) {
		if (path != null) {
			cache.lastPath.reset();
			cache.lastPath.append(path, false);
		}
		return path;
	}
	private Point padRenderAreaPoint(Point point) {
		double cx = currentCenterPoint != null ? currentCenterPoint.getX() : currentMinimapBounds.getCenterX();
		double cy = currentCenterPoint != null ? currentCenterPoint.getY() : currentMinimapBounds.getCenterY();
		double dx = point.getX() - cx;
		double dy = point.getY() - cy;
		double distance = Math.hypot(dx, dy);
		if (distance == 0) return point;
		return new Point((int) Math.round(point.getX() + dx * MINIMAP_RENDER_AREA_PADDING / distance), (int) Math.round(point.getY() + dy * MINIMAP_RENDER_AREA_PADDING / distance));
	}
	private void collectBoundaryPoints(WorldView worldView, WorldPoint center, LocalPoint centerLp, int radius) {
		boundaryPoints.clear();
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
	}
	private void addMinimapPoint(WorldView worldView, WorldPoint centerWp, LocalPoint centerLp, int tileXOffset, int tileYOffset, int xOffset, int yOffset) {
		if (!WorldPoint.isInScene(worldView, centerWp.getX() + tileXOffset, centerWp.getY() + tileYOffset)) {
			boundaryPoints.add(null);
			return;
		}
		int x = centerLp.getX() + tileXOffset * Perspective.LOCAL_TILE_SIZE + xOffset;
		int y = centerLp.getY() + tileYOffset * Perspective.LOCAL_TILE_SIZE + yOffset;
		Point projected = Perspective.localToMinimap(client, new LocalPoint(x, y, worldView), MINIMAP_PROJECTION_DISTANCE);
		boundaryPoints.add(projected == null ? null : padRenderAreaPoint(projected));
	}
	private GeneralPath fullMinimapCoveragePath(GeneralPath path) {
		Rectangle minimapBounds = currentMinimapBounds;
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
