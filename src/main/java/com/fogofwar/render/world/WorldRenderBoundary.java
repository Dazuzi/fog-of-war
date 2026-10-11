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
final class WorldRenderBoundary implements BoundaryPathBuilder.Strategy {
	private final Client client;
	private int[] boundaryX;
	private int[] boundaryY;
	private boolean[] boundaryVisible;
	private PathCache seaRenderAreaBoundary;
	private PathCache landRenderAreaBoundary;
	private Rectangle currentViewport;
	private int currentCenterX, currentCenterY;
	WorldRenderBoundary(Client client) { this.client = client; }
	GeneralPath createSeaRenderAreaBoundary(RenderCenter rc, int radius, Rectangle viewport) {
		if (seaRenderAreaBoundary == null) seaRenderAreaBoundary = new PathCache();
		return buildRenderAreaBoundary(rc, radius, viewport, seaRenderAreaBoundary);
	}
	GeneralPath createLandRenderAreaBoundary(RenderCenter rc, int radius, Rectangle viewport) {
		if (landRenderAreaBoundary == null) landRenderAreaBoundary = new PathCache();
		return buildRenderAreaBoundary(rc, radius, viewport, landRenderAreaBoundary);
	}
	private GeneralPath buildRenderAreaBoundary(RenderCenter rc, int radius, Rectangle viewport, PathCache cache) {
		WorldView worldView = rc.getWorldView();
		LocalPoint centerLp = rc.snappedCenter();
		int plane = rc.getWorldPoint().getPlane();
		if (centerLp == null) return null;
		Point centerPoint = Perspective.localToCanvas(client, centerLp, plane);
		currentCenterX = centerPoint != null ? centerPoint.getX() : viewport.x + viewport.width / 2;
		currentCenterY = centerPoint != null ? centerPoint.getY() : viewport.y + viewport.height - 1;
		currentViewport = viewport;
		if (cache.matches(client, worldView, centerLp, radius, plane, viewport, currentCenterX, currentCenterY)) return cache.lastPath();
		LocalPoint[] samples = cache.samples(worldView, centerLp, radius);
		ensureBoundaryCapacity(samples.length);
		for (int i = 0; i < samples.length; i++) {
			Point point = Perspective.localToCanvas(client, samples[i], plane);
			boundaryVisible[i] = point != null;
			if (point != null) {
				boundaryX[i] = point.getX();
				boundaryY[i] = point.getY();
			}
		}
		double arcRadius = Math.max(viewport.width, viewport.height) * 2.0;
		BoundaryPathBuilder.build(cache.working(), boundaryX, boundaryY, boundaryVisible, samples.length, currentCenterX, currentCenterY, arcRadius, this);
		return cache.save(client, worldView, centerLp, radius, plane, viewport, currentCenterX, currentCenterY);
	}
	private void ensureBoundaryCapacity(int capacity) {
		if (boundaryX != null && boundaryX.length >= capacity) return;
		boundaryX = new int[capacity];
		boundaryY = new int[capacity];
		boundaryVisible = new boolean[capacity];
	}
	@Override
	public GeneralPath coverage(GeneralPath path) { return fallback(path); }
	@Override
	public boolean isValid(GeneralPath path) { return path != null && path.contains(currentCenterX, currentCenterY); }
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
		private LocalPoint[] samples;
		private WorldView sampleWorldView;
		private WorldView worldView;
		private boolean valid, gpu;
		private int worldViewId, centerLocalX, centerLocalY, radius, plane, boundsX, boundsY, boundsW, boundsH, centerPointX, centerPointY;
		private int camX, camY, camZ, camPitch, camYaw, scale;
		private float camFpX, camFpY, camFpZ, camFpPitch, camFpYaw;
		private int sampleCenterX, sampleCenterY, sampleRadius;
		private GeneralPath working() { return working; }
		private GeneralPath lastPath() { return lastPath; }
		private LocalPoint[] samples(WorldView worldView, LocalPoint center, int radius) {
			if (sampleWorldView == worldView && sampleCenterX == center.getX() && sampleCenterY == center.getY() && sampleRadius == radius) return samples;
			int localRadius = radius * Perspective.LOCAL_TILE_SIZE + Perspective.LOCAL_HALF_TILE_SIZE;
			int sideSamples = radius * 2 + 1;
			int step = localRadius * 2 / sideSamples;
			int minX = center.getX() - localRadius;
			int maxX = center.getX() + localRadius;
			int minY = center.getY() - localRadius;
			int maxY = center.getY() + localRadius;
			samples = new LocalPoint[sideSamples * 4];
			int index = 0;
			for (int i = 0; i < sideSamples; i++) samples[index++] = new LocalPoint(minX + i * step, maxY, worldView);
			for (int i = 0; i < sideSamples; i++) samples[index++] = new LocalPoint(maxX, maxY - i * step, worldView);
			for (int i = 0; i < sideSamples; i++) samples[index++] = new LocalPoint(maxX - i * step, minY, worldView);
			for (int i = 0; i < sideSamples; i++) samples[index++] = new LocalPoint(minX, minY + i * step, worldView);
			sampleWorldView = worldView;
			sampleCenterX = center.getX();
			sampleCenterY = center.getY();
			sampleRadius = radius;
			return samples;
		}
		private boolean matches(Client client, WorldView worldView, LocalPoint centerLp, int radius, int plane, Rectangle bounds, int centerPointX, int centerPointY) {
			return valid
					&& this.worldView == worldView
					&& this.worldViewId == worldView.getId()
					&& centerLocalX == centerLp.getX()
					&& centerLocalY == centerLp.getY()
					&& this.radius == radius
					&& this.plane == plane
					&& boundsX == bounds.x
					&& boundsY == bounds.y
					&& boundsW == bounds.width
					&& boundsH == bounds.height
					&& this.centerPointX == centerPointX
					&& this.centerPointY == centerPointY
					&& gpu == client.isGpu()
					&& camX == client.getCameraX()
					&& camY == client.getCameraY()
					&& camZ == client.getCameraZ()
					&& camPitch == client.getCameraPitch()
					&& camYaw == client.getCameraYaw()
					&& scale == client.getScale()
					&& Float.compare(camFpX, client.getCameraFpX()) == 0
					&& Float.compare(camFpY, client.getCameraFpY()) == 0
					&& Float.compare(camFpZ, client.getCameraFpZ()) == 0
					&& Float.compare(camFpPitch, client.getCameraFpPitch()) == 0
					&& Float.compare(camFpYaw, client.getCameraFpYaw()) == 0;
		}
		private GeneralPath save(Client client, WorldView worldView, LocalPoint centerLp, int radius, int plane, Rectangle bounds, int centerPointX, int centerPointY) {
			GeneralPath path = working;
			working = lastPath;
			lastPath = path;
			working.reset();
			valid = true;
			this.worldView = worldView;
			this.worldViewId = worldView.getId();
			this.centerLocalX = centerLp.getX();
			this.centerLocalY = centerLp.getY();
			this.radius = radius;
			this.plane = plane;
			this.boundsX = bounds.x;
			this.boundsY = bounds.y;
			this.boundsW = bounds.width;
			this.boundsH = bounds.height;
			this.centerPointX = centerPointX;
			this.centerPointY = centerPointY;
			gpu = client.isGpu();
			camX = client.getCameraX();
			camY = client.getCameraY();
			camZ = client.getCameraZ();
			camPitch = client.getCameraPitch();
			camYaw = client.getCameraYaw();
			scale = client.getScale();
			camFpX = client.getCameraFpX();
			camFpY = client.getCameraFpY();
			camFpZ = client.getCameraFpZ();
			camFpPitch = client.getCameraFpPitch();
			camFpYaw = client.getCameraFpYaw();
			return lastPath;
		}
	}
}
