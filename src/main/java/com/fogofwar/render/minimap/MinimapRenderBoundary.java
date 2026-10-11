package com.fogofwar.render.minimap;
import com.fogofwar.render.BoundaryPathBuilder;
import com.fogofwar.render.RenderCenter;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
final class MinimapRenderBoundary implements BoundaryPathBuilder.Strategy {
	private static final int MINIMAP_RENDER_AREA_PADDING = 1;
	private static final int MINIMAP_PROJECTION_DISTANCE = 32768;
	private final Client client;
	private int[] boundaryX;
	private int[] boundaryY;
	private boolean[] boundaryVisible;
	private int boundaryCount;
	private PathCache seaRenderAreaPath;
	private PathCache landRenderAreaPath;
	private Rectangle currentMinimapBounds;
	private Point currentCenterPoint;
	private PathCache currentCache;
	MinimapRenderBoundary(Client client) { this.client = client; }
	GeneralPath createSeaRenderAreaPath(RenderCenter rc, int radius, Rectangle minimapBounds) {
		if (seaRenderAreaPath == null) seaRenderAreaPath = new PathCache();
		return buildRenderAreaPath(rc, radius, minimapBounds, seaRenderAreaPath);
	}
	GeneralPath createLandRenderAreaPath(RenderCenter rc, int radius, Rectangle minimapBounds) {
		if (landRenderAreaPath == null) landRenderAreaPath = new PathCache();
		return buildRenderAreaPath(rc, radius, minimapBounds, landRenderAreaPath);
	}
	private GeneralPath buildRenderAreaPath(RenderCenter rc, int radius, Rectangle minimapBounds, PathCache cache) {
		LocalPoint centerLp = rc.snappedCenter();
		WorldPoint centerWp = rc.getSnappedWorldPoint();
		if (centerWp == null) return null;
		currentMinimapBounds = minimapBounds;
		currentCenterPoint = Perspective.localToMinimap(client, centerLp, MINIMAP_PROJECTION_DISTANCE);
		currentCache = cache;
		int centerPointX = currentCenterPoint != null ? currentCenterPoint.getX() : Integer.MIN_VALUE;
		int centerPointY = currentCenterPoint != null ? currentCenterPoint.getY() : Integer.MIN_VALUE;
		int yaw = client.getCameraYawTarget();
		double zoom = client.getMinimapZoom();
		if (cache.matches(rc.getWorldView(), centerWp, centerLp, radius, minimapBounds, centerPointX, centerPointY, yaw, zoom)) return cache.lastValid();
		collectBoundaryPoints(rc.getWorldView(), centerWp, centerLp, radius);
		double arcRadius = Math.max(minimapBounds.width, minimapBounds.height) / 2.0 + 1;
		GeneralPath path = BoundaryPathBuilder.build(cache.working(), boundaryX, boundaryY, boundaryVisible, boundaryCount, minimapBounds.getCenterX(), minimapBounds.getCenterY(), arcRadius, this);
		return path == cache.working() && isValid(path) ? cache.saveValid(rc.getWorldView(), centerWp, centerLp, radius, minimapBounds, centerPointX, centerPointY, yaw, zoom) : path;
	}
	@Override
	public GeneralPath coverage(GeneralPath path) { return fullMinimapCoveragePath(path); }
	@Override
	public boolean isValid(GeneralPath path) { return currentCenterPoint == null || path.contains(currentCenterPoint.getX(), currentCenterPoint.getY()); }
	@Override
	public GeneralPath fallback(GeneralPath path) { return currentCache.hasLastValid() ? currentCache.lastValid() : path; }
	private void padRenderAreaPoint(int index, int x, int y) {
		double cx = currentCenterPoint != null ? currentCenterPoint.getX() : currentMinimapBounds.getCenterX();
		double cy = currentCenterPoint != null ? currentCenterPoint.getY() : currentMinimapBounds.getCenterY();
		double dx = x - cx;
		double dy = y - cy;
		double distance = Math.hypot(dx, dy);
		if (distance == 0) {
			boundaryX[index] = x;
			boundaryY[index] = y;
			return;
		}
		boundaryX[index] = (int) Math.round(x + dx * MINIMAP_RENDER_AREA_PADDING / distance);
		boundaryY[index] = (int) Math.round(y + dy * MINIMAP_RENDER_AREA_PADDING / distance);
	}
	private void collectBoundaryPoints(WorldView worldView, WorldPoint center, LocalPoint centerLp, int radius) {
		boundaryCount = 0;
		int sampleRate = Math.max(1, radius / 12);
		ensureBoundaryCapacity(4 + 4 * (radius * 2 / sampleRate + 1));
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
		int index = boundaryCount++;
		boundaryVisible[index] = false;
		if (!WorldPoint.isInScene(worldView, centerWp.getX() + tileXOffset, centerWp.getY() + tileYOffset)) return;
		int x = centerLp.getX() + tileXOffset * Perspective.LOCAL_TILE_SIZE + xOffset;
		int y = centerLp.getY() + tileYOffset * Perspective.LOCAL_TILE_SIZE + yOffset;
		Point projected = Perspective.localToMinimap(client, new LocalPoint(x, y, worldView), MINIMAP_PROJECTION_DISTANCE);
		if (projected == null) return;
		boundaryVisible[index] = true;
		padRenderAreaPoint(index, projected.getX(), projected.getY());
	}
	private void ensureBoundaryCapacity(int capacity) {
		if (boundaryX != null && boundaryX.length >= capacity) return;
		boundaryX = new int[capacity];
		boundaryY = new int[capacity];
		boundaryVisible = new boolean[capacity];
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
	private static final class PathCache {
		private GeneralPath working = new GeneralPath();
		private GeneralPath lastValid = new GeneralPath();
		private WorldView worldView;
		private boolean valid;
		private int worldViewId, centerWorldX, centerWorldY, centerPlane, centerLocalX, centerLocalY, radius, boundsX, boundsY, boundsW, boundsH, centerPointX, centerPointY, yaw;
		private double zoom;
		private GeneralPath working() { return working; }
		private GeneralPath lastValid() { return lastValid; }
		private boolean hasLastValid() { return lastValid.getCurrentPoint() != null; }
		private boolean matches(WorldView worldView, WorldPoint centerWp, LocalPoint centerLp, int radius, Rectangle bounds, int centerPointX, int centerPointY, int yaw, double zoom) {
			return valid
					&& this.worldView == worldView
					&& this.worldViewId == worldView.getId()
					&& centerWorldX == centerWp.getX()
					&& centerWorldY == centerWp.getY()
					&& centerPlane == centerWp.getPlane()
					&& centerLocalX == centerLp.getX()
					&& centerLocalY == centerLp.getY()
					&& this.radius == radius
					&& boundsX == bounds.x
					&& boundsY == bounds.y
					&& boundsW == bounds.width
					&& boundsH == bounds.height
					&& this.centerPointX == centerPointX
					&& this.centerPointY == centerPointY
					&& this.yaw == yaw
					&& Double.compare(this.zoom, zoom) == 0;
		}
		private GeneralPath saveValid(WorldView worldView, WorldPoint centerWp, LocalPoint centerLp, int radius, Rectangle bounds, int centerPointX, int centerPointY, int yaw, double zoom) {
			GeneralPath valid = working;
			working = lastValid;
			lastValid = valid;
			working.reset();
			this.valid = true;
			this.worldView = worldView;
			this.worldViewId = worldView.getId();
			this.centerWorldX = centerWp.getX();
			this.centerWorldY = centerWp.getY();
			this.centerPlane = centerWp.getPlane();
			this.centerLocalX = centerLp.getX();
			this.centerLocalY = centerLp.getY();
			this.radius = radius;
			this.boundsX = bounds.x;
			this.boundsY = bounds.y;
			this.boundsW = bounds.width;
			this.boundsH = bounds.height;
			this.centerPointX = centerPointX;
			this.centerPointY = centerPointY;
			this.yaw = yaw;
			this.zoom = zoom;
			return lastValid;
		}
	}
}
