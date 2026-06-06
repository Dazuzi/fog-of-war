package com.fogofwar.render.world;
import com.fogofwar.render.BoundaryPathBuilder;
import com.fogofwar.render.RenderCenter;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;
final class WorldRenderBoundary implements BoundaryPathBuilder.Strategy {
	private final Client client;
	private final List<Point> boundaryPoints = new ArrayList<>(256);
	private final PathCache seaRenderAreaBoundary = new PathCache();
	private final PathCache landRenderAreaBoundary = new PathCache();
	private Rectangle currentViewport;
	private Point currentCenterPoint;
	WorldRenderBoundary(Client client) { this.client = client; }
	void clearCaches() {
		seaRenderAreaBoundary.clear();
		landRenderAreaBoundary.clear();
	}
	GeneralPath createSeaRenderAreaBoundary(RenderCenter rc, int radius, Rectangle viewport) {
		return buildRenderAreaBoundary(rc, radius, viewport, seaRenderAreaBoundary);
	}
	GeneralPath createLandRenderAreaBoundary(RenderCenter rc, int radius, Rectangle viewport) {
		return buildRenderAreaBoundary(rc, radius, viewport, landRenderAreaBoundary);
	}
	private GeneralPath buildRenderAreaBoundary(RenderCenter rc, int radius, Rectangle viewport, PathCache cache) {
		WorldView worldView = rc.getWorldView();
		LocalPoint centerLp = rc.snappedCenter();
		int plane = rc.getWorldPoint().getPlane();
		if (centerLp == null) return null;
		Point centerPoint = rc.getCanvasCenterPoint();
		if (centerPoint == null) centerPoint = new Point(viewport.x + viewport.width / 2, viewport.y + viewport.height - 1);
		currentViewport = viewport;
		currentCenterPoint = centerPoint;
		if (cache.matches(client, worldView.getId(), centerLp, radius, plane, viewport, centerPoint)) return cache.lastPath();
		boundaryPoints.clear();
		int localRadius = radius * Perspective.LOCAL_TILE_SIZE + Perspective.LOCAL_HALF_TILE_SIZE;
		int sampleCount = radius * 2 + 1;
		int step = localRadius * 2 / sampleCount;
		int minX = centerLp.getX() - localRadius;
		int maxX = centerLp.getX() + localRadius;
		int minY = centerLp.getY() - localRadius;
		int maxY = centerLp.getY() + localRadius;
		for (int i = 0; i < sampleCount; i++) addPoint(worldView, minX + i * step, maxY, plane);
		for (int i = 0; i < sampleCount; i++) addPoint(worldView, maxX, maxY - i * step, plane);
		for (int i = 0; i < sampleCount; i++) addPoint(worldView, maxX - i * step, minY, plane);
		for (int i = 0; i < sampleCount; i++) addPoint(worldView, minX, minY + i * step, plane);
		double arcRadius = Math.max(viewport.width, viewport.height) * 2.0;
		BoundaryPathBuilder.build(cache.working(), boundaryPoints, centerPoint.getX(), centerPoint.getY(), arcRadius, this);
		return cache.save(client, worldView.getId(), centerLp, radius, plane, viewport, centerPoint);
	}
	private void addPoint(WorldView worldView, int localX, int localY, int plane) {
		LocalPoint lp = new LocalPoint(localX, localY, worldView);
		boundaryPoints.add(Perspective.localToCanvas(client, lp, plane));
	}
	@Override
	public GeneralPath coverage(GeneralPath path) { return fallback(path); }
	@Override
	public boolean isValid(GeneralPath path) { return path != null && path.contains(currentCenterPoint.getX(), currentCenterPoint.getY()); }
	@Override
	public GeneralPath fallback(GeneralPath path) { return fullViewportCoveragePath(path); }
	private GeneralPath fullViewportCoveragePath(GeneralPath path) {
		Rectangle viewport = currentViewport;
		path.reset();
		path.moveTo(viewport.x - 1, viewport.y - 1);
		path.lineTo(viewport.x + viewport.width + 1, viewport.y - 1);
		path.lineTo(viewport.x + viewport.width + 1, viewport.y + viewport.height + 1);
		path.lineTo(viewport.x - 1, viewport.y + viewport.height + 1);
		path.closePath();
		return path;
	}
	private static final class PathCache {
		private GeneralPath working = new GeneralPath();
		private GeneralPath lastPath = new GeneralPath();
		private boolean valid;
		private int worldViewId, centerLocalX, centerLocalY, radius, plane, boundsX, boundsY, boundsW, boundsH, centerPointX, centerPointY;
		private int camX, camY, camZ, camPitch, camYaw, scale;
		private GeneralPath working() { return working; }
		private GeneralPath lastPath() { return lastPath; }
		private boolean matches(Client client, int worldViewId, LocalPoint centerLp, int radius, int plane, Rectangle bounds, Point centerPoint) {
			return valid
					&& this.worldViewId == worldViewId
					&& centerLocalX == centerLp.getX()
					&& centerLocalY == centerLp.getY()
					&& this.radius == radius
					&& this.plane == plane
					&& boundsX == bounds.x
					&& boundsY == bounds.y
					&& boundsW == bounds.width
					&& boundsH == bounds.height
					&& centerPointX == centerPoint.getX()
					&& centerPointY == centerPoint.getY()
					&& camX == client.getCameraX()
					&& camY == client.getCameraY()
					&& camZ == client.getCameraZ()
					&& camPitch == client.getCameraPitch()
					&& camYaw == client.getCameraYaw()
					&& scale == client.getScale();
		}
		private GeneralPath save(Client client, int worldViewId, LocalPoint centerLp, int radius, int plane, Rectangle bounds, Point centerPoint) {
			GeneralPath path = working;
			working = lastPath;
			lastPath = path;
			working.reset();
			valid = true;
			this.worldViewId = worldViewId;
			this.centerLocalX = centerLp.getX();
			this.centerLocalY = centerLp.getY();
			this.radius = radius;
			this.plane = plane;
			this.boundsX = bounds.x;
			this.boundsY = bounds.y;
			this.boundsW = bounds.width;
			this.boundsH = bounds.height;
			this.centerPointX = centerPoint.getX();
			this.centerPointY = centerPoint.getY();
			this.camX = client.getCameraX();
			this.camY = client.getCameraY();
			this.camZ = client.getCameraZ();
			this.camPitch = client.getCameraPitch();
			this.camYaw = client.getCameraYaw();
			this.scale = client.getScale();
			return lastPath;
		}
		private void clear() {
			working.reset();
			lastPath.reset();
			valid = false;
		}
	}
}
