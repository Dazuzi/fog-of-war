package com.fogofwar.render.world;
import com.fogofwar.render.RenderAreaType;
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
	GeneralPath createRenderAreaBoundary(RenderAreaType type, WorldView worldView, LocalPoint centerLp, int plane, int radius, Rectangle viewport) {
		return buildRenderAreaBoundary(worldView, centerLp, plane, radius, viewport, getPath(type));
	}
	private GeneralPath getPath(RenderAreaType type) { return type == RenderAreaType.SEA ? seaRenderAreaBoundary : landRenderAreaBoundary; }
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
		path = buildPathFromPoints(path, viewport, centerPoint, false);
		if (isValidPath(path, centerPoint)) return path;
		path = buildPathFromPoints(path, viewport, centerPoint, true);
		if (isValidPath(path, centerPoint)) return path;
		return createFullViewportCoveragePath(path, viewport);
	}
	private boolean isValidPath(GeneralPath path, Point centerPoint) { return path != null && path.contains(centerPoint.getX(), centerPoint.getY()); }
	private GeneralPath buildPathFromPoints(GeneralPath path, Rectangle viewport, Point centerPoint, boolean reverseArc) {
		int n = boundaryPoints.size();
		int firstVisible = -1;
		int visibleCount = 0;
		for (int i = 0; i < n; i++) {
			if (boundaryPoints.get(i) != null) {
				if (firstVisible == -1) firstVisible = i;
				visibleCount++;
			}
		}
		if (visibleCount == 0) return createFullViewportCoveragePath(path, viewport);
		path.reset();
		if (visibleCount == n) return buildCompletePath(path);
		Point first = boundaryPoints.get(firstVisible);
		path.moveTo(first.getX(), first.getY());
		for (int i = 0; i < n; i++) {
			int currentIndex = (firstVisible + i) % n;
			int nextIndex = (firstVisible + i + 1) % n;
			Point p1 = boundaryPoints.get(currentIndex);
			Point p2 = boundaryPoints.get(nextIndex);
			if (p1 == null) continue;
			if (p2 != null) path.lineTo(p2.getX(), p2.getY());
			else {
				int nextVisibleIndex = findNextVisibleIndex(currentIndex, n);
				if (nextVisibleIndex != -1) addArcToPath(path, p1, boundaryPoints.get(nextVisibleIndex), viewport, centerPoint, reverseArc);
			}
		}
		path.closePath();
		return path;
	}
	private GeneralPath buildCompletePath(GeneralPath path) {
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
	private int findNextVisibleIndex(int currentIndex, int n) {
		for (int i = 2; i < n; i++) {
			int index = (currentIndex + i) % n;
			if (boundaryPoints.get(index) != null) return index;
		}
		return -1;
	}
	private void addArcToPath(GeneralPath path, Point p1, Point p2, Rectangle viewport, Point centerPoint, boolean reverse) {
		double centerX = centerPoint.getX();
		double centerY = centerPoint.getY();
		double radius = Math.max(viewport.width, viewport.height) * 2.0;
		double startAngle = Math.toDegrees(Math.atan2(p1.getY() - centerY, p1.getX() - centerX));
		double endAngle = Math.toDegrees(Math.atan2(p2.getY() - centerY, p2.getX() - centerX));
		double sweep = endAngle - startAngle;
		if (sweep <= -180) { sweep += 360; } else if (sweep > 180) { sweep -= 360; }
		if (reverse) sweep += sweep > 0 ? -360 : 360;
		int numSteps = (int) (Math.abs(sweep) / 10) + 1;
		for (int i = 1; i <= numSteps; i++) {
			double angle = Math.toRadians(startAngle + sweep * i / numSteps);
			path.lineTo((float) (centerX + radius * Math.cos(angle)), (float) (centerY + radius * Math.sin(angle)));
		}
	}
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
