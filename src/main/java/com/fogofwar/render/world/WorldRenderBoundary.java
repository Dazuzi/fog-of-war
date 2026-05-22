package com.fogofwar.render.world;
import com.fogofwar.render.BoundaryPathBuilder;
import com.fogofwar.state.RenderCenter;
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
	private final GeneralPath seaRenderAreaBoundary = new GeneralPath();
	private final GeneralPath landRenderAreaBoundary = new GeneralPath();
	private Rectangle currentViewport;
	private Point currentCenterPoint;
	WorldRenderBoundary(Client client) { this.client = client; }
	GeneralPath createSeaRenderAreaBoundary(RenderCenter rc, int radius, Rectangle viewport) {
		return buildRenderAreaBoundary(rc, radius, viewport, seaRenderAreaBoundary);
	}
	GeneralPath createLandRenderAreaBoundary(RenderCenter rc, int radius, Rectangle viewport) {
		return buildRenderAreaBoundary(rc, radius, viewport, landRenderAreaBoundary);
	}
	private GeneralPath buildRenderAreaBoundary(RenderCenter rc, int radius, Rectangle viewport, GeneralPath path) {
		WorldView worldView = rc.getWorldView();
		LocalPoint centerLp = rc.snappedCenter();
		int plane = rc.getWorldPoint().getPlane();
		if (centerLp == null) return null;
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
		Point centerPoint = rc.getCanvasCenterPoint();
		if (centerPoint == null) centerPoint = new Point(viewport.x + viewport.width / 2, viewport.y + viewport.height - 1);
		currentViewport = viewport;
		currentCenterPoint = centerPoint;
		double arcRadius = Math.max(viewport.width, viewport.height) * 2.0;
		return BoundaryPathBuilder.build(path, boundaryPoints, centerPoint.getX(), centerPoint.getY(), arcRadius, this);
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
}
