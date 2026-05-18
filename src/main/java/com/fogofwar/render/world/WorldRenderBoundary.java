package com.fogofwar.render.world;
import com.fogofwar.state.RenderCenter;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;
final class WorldRenderBoundary {
	static final int LOCAL_TILE_SIZE = 128;
	private static final int HALF_TILE_SIZE = 64;
	private final Client client;
	private final List<Point> boundaryPoints = new ArrayList<>(256);
	private final GeneralPath renderAreaBoundary = new GeneralPath();
	private final GeneralPath sailingLandRenderAreaBoundary = new GeneralPath();
	WorldRenderBoundary(Client client) { this.client = client; }
	LocalPoint getRenderCenter(RenderCenter rc, int radius, int landRadius) {
		LocalPoint lp = rc.isOnWorldEntity() && radius > landRadius ? rc.getTargetLocalPoint() : rc.getLocalPoint();
		if (lp == null) return null;
		return new LocalPoint(snapAxis(lp.getX()), snapAxis(lp.getY()), rc.getWorldView());
	}
	GeneralPath createRenderAreaBoundary(WorldView worldView, LocalPoint centerLp, int plane, int radius) {
		return createRenderAreaBoundary(worldView, centerLp, plane, radius, renderAreaBoundary);
	}
	GeneralPath createSailingLandRenderAreaBoundary(WorldView worldView, LocalPoint centerLp, int plane, int radius) {
		return createRenderAreaBoundary(worldView, centerLp, plane, radius, sailingLandRenderAreaBoundary);
	}
	private GeneralPath createRenderAreaBoundary(WorldView worldView, LocalPoint centerLp, int plane, int radius, GeneralPath path) {
		if (centerLp == null) return null;
		boundaryPoints.clear();
		int localRadius = radius * LOCAL_TILE_SIZE + HALF_TILE_SIZE;
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
		return createPathFromPoints(path);
	}
	private static int snapAxis(int current) { return (current / LOCAL_TILE_SIZE) * LOCAL_TILE_SIZE + HALF_TILE_SIZE; }
	private void addPoint(WorldView worldView, int localX, int localY, int plane) {
		LocalPoint lp = new LocalPoint(localX, localY, worldView);
		Point canvasPoint = Perspective.localToCanvas(client, lp, plane);
		if (canvasPoint != null) boundaryPoints.add(canvasPoint);
	}
	private GeneralPath createPathFromPoints(GeneralPath path) {
		if (boundaryPoints.size() < 3) return null;
		path.reset();
		Point first = boundaryPoints.get(0);
		path.moveTo(first.getX(), first.getY());
		for (int i = 1; i < boundaryPoints.size(); i++) {
			Point point = boundaryPoints.get(i);
			path.lineTo(point.getX(), point.getY());
		}
		path.closePath();
		return path;
	}
}
