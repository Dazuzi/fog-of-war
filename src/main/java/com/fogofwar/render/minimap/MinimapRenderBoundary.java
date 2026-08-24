package com.fogofwar.render.minimap;
import com.fogofwar.render.BoundaryPathBuilder;
import com.fogofwar.render.RenderCenter;
import net.runelite.api.CameraFocusableEntity;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
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
	private final MinimapProjection projection = new MinimapProjection();
	private Rectangle currentMinimapBounds;
	private int currentCenterX, currentCenterY;
	private boolean currentCenterVisible;
	private PathCache currentCache;
	private int minWorldX, minWorldY, maxWorldX, maxWorldY;
	private RenderCenter preparedCenter;
	private Widget preparedMinimap;
	private int preparedBoundsX, preparedBoundsY, preparedBoundsW, preparedBoundsH, preparedYaw;
	private double preparedZoom;
	MinimapRenderBoundary(Client client) { this.client = client; }
	GeneralPath createSeaRenderAreaPath(RenderCenter rc, int radius, Widget minimap, Rectangle minimapBounds) {
		if (seaRenderAreaPath == null) seaRenderAreaPath = new PathCache();
		return buildRenderAreaPath(rc, radius, minimap, minimapBounds, seaRenderAreaPath);
	}
	GeneralPath createLandRenderAreaPath(RenderCenter rc, int radius, Widget minimap, Rectangle minimapBounds) {
		if (landRenderAreaPath == null) landRenderAreaPath = new PathCache();
		return buildRenderAreaPath(rc, radius, minimap, minimapBounds, landRenderAreaPath);
	}
	private GeneralPath buildRenderAreaPath(RenderCenter rc, int radius, Widget minimap, Rectangle minimapBounds, PathCache cache) {
		LocalPoint centerLp = rc.snappedCenter();
		WorldPoint centerWp = rc.getSnappedWorldPoint();
		if (centerWp == null) return null;
		currentMinimapBounds = minimapBounds;
		int yaw = client.getCameraYawTarget();
		double zoom = client.getMinimapZoom();
		if (preparedCenter != rc || preparedMinimap != minimap || preparedBoundsX != minimapBounds.x || preparedBoundsY != minimapBounds.y || preparedBoundsW != minimapBounds.width || preparedBoundsH != minimapBounds.height || preparedYaw != yaw || Double.compare(preparedZoom, zoom) != 0) {
			projection.capture(client, minimap, yaw, zoom);
			currentCenterVisible = projection.project(centerLp.getX(), centerLp.getY());
			if (currentCenterVisible) {
				currentCenterX = projection.x;
				currentCenterY = projection.y;
			}
			preparedCenter = rc;
			preparedMinimap = minimap;
			preparedBoundsX = minimapBounds.x;
			preparedBoundsY = minimapBounds.y;
			preparedBoundsW = minimapBounds.width;
			preparedBoundsH = minimapBounds.height;
			preparedYaw = yaw;
			preparedZoom = zoom;
		}
		currentCache = cache;
		int centerPointX = currentCenterVisible ? currentCenterX : Integer.MIN_VALUE;
		int centerPointY = currentCenterVisible ? currentCenterY : Integer.MIN_VALUE;
		if (cache.matches(projection, rc.getWorldView(), centerWp, centerLp, radius, minimapBounds, centerPointX, centerPointY, yaw, zoom)) {
			return cache.lastValid();
		}
		collectBoundaryPoints(rc.getWorldView(), centerWp, centerLp, radius);
		double arcRadius = Math.max(minimapBounds.width, minimapBounds.height) / 2.0 + 1;
		GeneralPath path = BoundaryPathBuilder.build(cache.working(), boundaryX, boundaryY, boundaryVisible, boundaryCount, minimapBounds.getCenterX(), minimapBounds.getCenterY(), arcRadius, this);
		return path == cache.working() && isValid(path) ? cache.saveValid(projection, rc.getWorldView(), centerWp, centerLp, radius, minimapBounds, centerPointX, centerPointY, yaw, zoom) : path;
	}
	@Override
	public GeneralPath coverage(GeneralPath path) { return fullMinimapCoveragePath(path); }
	@Override
	public boolean isValid(GeneralPath path) { return !currentCenterVisible || path.contains(currentCenterX, currentCenterY); }
	@Override
	public GeneralPath fallback(GeneralPath path) { return currentCache.hasLastValid() ? currentCache.lastValid() : path; }
	private void padRenderAreaPoint(int index, int x, int y) {
		double cx = currentCenterVisible ? currentCenterX : currentMinimapBounds.getCenterX();
		double cy = currentCenterVisible ? currentCenterY : currentMinimapBounds.getCenterY();
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
		minWorldX = worldView.getBaseX();
		minWorldY = worldView.getBaseY();
		maxWorldX = minWorldX + worldView.getSizeX();
		maxWorldY = minWorldY + worldView.getSizeY();
		int halfTile = Perspective.LOCAL_HALF_TILE_SIZE;
		addMinimapPoint(center, centerLp, -radius, -radius, -halfTile, -halfTile);
		for (int x = -radius; x <= radius; x += sampleRate) addMinimapPoint(center, centerLp, x, -radius, 0, -halfTile);
		addMinimapPoint(center, centerLp, radius, -radius, halfTile, -halfTile);
		for (int y = -radius; y <= radius; y += sampleRate) addMinimapPoint(center, centerLp, radius, y, halfTile, 0);
		addMinimapPoint(center, centerLp, radius, radius, halfTile, halfTile);
		for (int x = radius; x >= -radius; x -= sampleRate) addMinimapPoint(center, centerLp, x, radius, 0, halfTile);
		addMinimapPoint(center, centerLp, -radius, radius, -halfTile, halfTile);
		for (int y = radius; y >= -radius; y -= sampleRate) addMinimapPoint(center, centerLp, -radius, y, -halfTile, 0);
	}
	private void addMinimapPoint(WorldPoint centerWp, LocalPoint centerLp, int tileXOffset, int tileYOffset, int xOffset, int yOffset) {
		int index = boundaryCount++;
		int worldX = centerWp.getX() + tileXOffset;
		int worldY = centerWp.getY() + tileYOffset;
		if (worldX < minWorldX || worldX >= maxWorldX || worldY < minWorldY || worldY >= maxWorldY) {
			boundaryVisible[index] = false;
			return;
		}
		int x = centerLp.getX() + tileXOffset * Perspective.LOCAL_TILE_SIZE + xOffset;
		int y = centerLp.getY() + tileYOffset * Perspective.LOCAL_TILE_SIZE + yOffset;
		boundaryVisible[index] = projection.project(x, y);
		if (boundaryVisible[index]) padRenderAreaPoint(index, projection.x, projection.y);
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
		private final MinimapProjection projection = new MinimapProjection();
		private WorldView worldView;
		private boolean valid;
		private int worldViewId, centerWorldX, centerWorldY, centerPlane, centerLocalX, centerLocalY, radius, boundsX, boundsY, boundsW, boundsH, centerPointX, centerPointY, yaw;
		private double zoom;
		private GeneralPath working() { return working; }
		private GeneralPath lastValid() { return lastValid; }
		private boolean hasLastValid() { return lastValid.getCurrentPoint() != null; }
		private boolean matches(MinimapProjection projection, WorldView worldView, WorldPoint centerWp, LocalPoint centerLp, int radius, Rectangle bounds, int centerPointX, int centerPointY, int yaw, double zoom) {
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
					&& Double.compare(this.zoom, zoom) == 0
					&& this.projection.matches(projection);
		}
		private GeneralPath saveValid(MinimapProjection projection, WorldView worldView, WorldPoint centerWp, LocalPoint centerLp, int radius, Rectangle bounds, int centerPointX, int centerPointY, int yaw, double zoom) {
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
			this.projection.copyFrom(projection);
			return lastValid;
		}
	}
	private static final class MinimapProjection {
		private int focusX, focusY, originX, originY, sin, cos, x, y;
		private double zoom;
		private boolean valid;
		private void capture(Client client, Widget minimap, int yaw, double minimapZoom) {
			CameraFocusableEntity cameraFocus = client.getCameraFocusEntity();
			valid = cameraFocus != null && minimap != null && !minimap.isHidden();
			if (!valid) return;
			LocalPoint focus = cameraFocus.getCameraFocus();
			if (focus.getWorldView() != WorldView.TOPLEVEL) {
				WorldView topWorldView = client.getTopLevelWorldView();
				WorldView focusWorldView = cameraFocus.getWorldView();
				WorldEntity worldEntity = topWorldView.worldEntities().byIndex(focusWorldView.getId());
				if (worldEntity != null) focus = worldEntity.transformToMainWorld(focus);
			}
			Point location = minimap.getCanvasLocation();
			focusX = focus.getX();
			focusY = focus.getY();
			originX = location.getX() + minimap.getWidth() / 2;
			originY = location.getY() + minimap.getHeight() / 2;
			zoom = minimapZoom / Perspective.LOCAL_TILE_SIZE;
			int angle = yaw & 0x3fff;
			sin = Perspective.SINE14[angle];
			cos = Perspective.COSINE14[angle];
		}
		private boolean project(int localX, int localY) {
			if (!valid) return false;
			int dx = localX - focusX;
			int dy = localY - focusY;
			if (dx * dx + dy * dy >= MinimapRenderBoundary.MINIMAP_PROJECTION_DISTANCE * MinimapRenderBoundary.MINIMAP_PROJECTION_DISTANCE) return false;
			int scaledX = (int) (dx * zoom);
			int scaledY = (int) (dy * zoom);
			x = originX + (cos * scaledX + sin * scaledY >> 16);
			y = originY + (sin * scaledX - cos * scaledY >> 16);
			return true;
		}
		private boolean matches(MinimapProjection other) {
			return valid == other.valid && focusX == other.focusX && focusY == other.focusY && originX == other.originX && originY == other.originY && sin == other.sin && cos == other.cos && Double.compare(zoom, other.zoom) == 0;
		}
		private void copyFrom(MinimapProjection other) {
			focusX = other.focusX;
			focusY = other.focusY;
			originX = other.originX;
			originY = other.originY;
			sin = other.sin;
			cos = other.cos;
			zoom = other.zoom;
			valid = other.valid;
		}
	}
}
