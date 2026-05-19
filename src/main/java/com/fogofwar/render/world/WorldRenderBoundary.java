package com.fogofwar.render.world;
import com.fogofwar.render.BoundaryPathBuilder;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;
final class WorldRenderBoundary {
	private final Client client;
	private final List<Point> boundaryPoints = new ArrayList<>(256);
	private final GeneralPath seaRenderAreaBoundary = new GeneralPath();
	private final GeneralPath landRenderAreaBoundary = new GeneralPath();
	WorldRenderBoundary(Client client) { this.client = client; }
	GeneralPath createSeaRenderAreaBoundary(WorldView worldView, LocalPoint centerLp, int plane, int radius, Rectangle viewport) {
		return buildRenderAreaBoundary(worldView, centerLp, plane, radius, viewport, seaRenderAreaBoundary);
	}
	GeneralPath createLandRenderAreaBoundary(WorldView worldView, LocalPoint centerLp, int plane, int radius, Rectangle viewport) {
		return buildRenderAreaBoundary(worldView, centerLp, plane, radius, viewport, landRenderAreaBoundary);
	}
	private GeneralPath buildRenderAreaBoundary(WorldView worldView, LocalPoint centerLp, int plane, int radius, Rectangle viewport, GeneralPath path) {
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
		Point centerPoint = Perspective.localToCanvas(client, centerLp, plane);
		if (centerPoint == null) centerPoint = new Point(viewport.x + viewport.width / 2, viewport.y + viewport.height - 1);
		return createPathFromPoints(path, viewport, centerPoint);
	}
	private void addPoint(WorldView worldView, int localX, int localY, int plane) {
		LocalPoint lp = new LocalPoint(localX, localY, worldView);
		boundaryPoints.add(Perspective.localToCanvas(client, lp, plane));
	}
	private GeneralPath createPathFromPoints(GeneralPath path, Rectangle viewport, Point centerPoint) {
		double arcRadius = Math.max(viewport.width, viewport.height) * 2.0;
		return BoundaryPathBuilder.build(path, boundaryPoints, centerPoint.getX(), centerPoint.getY(), arcRadius, point -> point, p -> createFullViewportCoveragePath(p, viewport), p -> isValidPath(p, centerPoint), p -> createFullViewportCoveragePath(p, viewport));
	}
	private boolean isValidPath(GeneralPath path, Point centerPoint) { return path != null && path.contains(centerPoint.getX(), centerPoint.getY()); }
	private GeneralPath createFullViewportCoveragePath(GeneralPath path, Rectangle viewport) {
		path.reset();
		path.moveTo(viewport.x - 1, viewport.y - 1);
		path.lineTo(viewport.x + viewport.width + 1, viewport.y - 1);
		path.lineTo(viewport.x + viewport.width + 1, viewport.y + viewport.height + 1);
		path.lineTo(viewport.x - 1, viewport.y + viewport.height + 1);
		path.closePath();
		return path;
	}
}
