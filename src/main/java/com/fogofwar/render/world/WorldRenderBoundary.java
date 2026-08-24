package com.fogofwar.render.world;
import com.fogofwar.render.BoundaryPathBuilder;
import com.fogofwar.render.RenderCenter;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.Perspective;
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
	private final CanvasProjection projection = new CanvasProjection();
	private Rectangle currentViewport;
	private int currentCenterX, currentCenterY;
	private RenderCenter preparedCenter;
	private int preparedBoundsX, preparedBoundsY, preparedBoundsW, preparedBoundsH;
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
		if (preparedCenter != rc || preparedBoundsX != viewport.x || preparedBoundsY != viewport.y || preparedBoundsW != viewport.width || preparedBoundsH != viewport.height) {
			projection.capture(client, viewport);
			if (projection.project(centerLp, plane)) {
				currentCenterX = projection.x;
				currentCenterY = projection.y;
			} else {
				currentCenterX = viewport.x + viewport.width / 2;
				currentCenterY = viewport.y + viewport.height - 1;
			}
			preparedCenter = rc;
			preparedBoundsX = viewport.x;
			preparedBoundsY = viewport.y;
			preparedBoundsW = viewport.width;
			preparedBoundsH = viewport.height;
		}
		currentViewport = viewport;
		if (cache.matches(projection, worldView, centerLp, radius, plane, viewport, currentCenterX, currentCenterY)) return cache.lastPath();
		LocalPoint[] samples = cache.samples(worldView, centerLp, radius);
		ensureBoundaryCapacity(samples.length);
		for (int i = 0; i < samples.length; i++) {
			boundaryVisible[i] = projection.project(samples[i], plane);
			if (boundaryVisible[i]) {
				boundaryX[i] = projection.x;
				boundaryY[i] = projection.y;
			}
		}
		double arcRadius = Math.max(viewport.width, viewport.height) * 2.0;
		BoundaryPathBuilder.build(cache.working(), boundaryX, boundaryY, boundaryVisible, samples.length, currentCenterX, currentCenterY, arcRadius, this);
		return cache.save(projection, worldView, centerLp, radius, plane, viewport, currentCenterX, currentCenterY);
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
		private final CanvasProjection projection = new CanvasProjection();
		private LocalPoint[] samples;
		private WorldView sampleWorldView;
		private WorldView worldView;
		private boolean valid;
		private int worldViewId, centerLocalX, centerLocalY, radius, plane, boundsX, boundsY, boundsW, boundsH, centerPointX, centerPointY;
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
		private boolean matches(CanvasProjection projection, WorldView worldView, LocalPoint centerLp, int radius, int plane, Rectangle bounds, int centerPointX, int centerPointY) {
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
					&& this.projection.matches(projection);
		}
		private GeneralPath save(CanvasProjection projection, WorldView worldView, LocalPoint centerLp, int radius, int plane, Rectangle bounds, int centerPointX, int centerPointY) {
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
			this.projection.copyFrom(projection);
			return lastPath;
		}
	}
	private static final class CanvasProjection {
		private static final int EXTENDED_OFFSET = (Constants.EXTENDED_SCENE_SIZE - Constants.SCENE_SIZE) / 2;
		private static final int MIN_LOCAL = -EXTENDED_OFFSET << Perspective.LOCAL_COORD_BITS;
		private static final int MAX_LOCAL = Constants.SCENE_SIZE + EXTENDED_OFFSET << Perspective.LOCAL_COORD_BITS;
		private Client client;
		private boolean gpu;
		private int camX, camY, camZ, camPitch, camYaw, scale, vpX, vpY, vpW, vpH, x, y;
		private float camFpX, camFpY, camFpZ, camFpPitch, camFpYaw;
		private int pitchSin, pitchCos, yawSin, yawCos;
		private float pitchSinFp, pitchCosFp, yawSinFp, yawCosFp;
		private boolean prepared;
		private void capture(Client client, Rectangle viewport) {
			this.client = client;
			gpu = client.isGpu();
			scale = client.getScale();
			vpX = viewport.x;
			vpY = viewport.y;
			vpW = viewport.width;
			vpH = viewport.height;
			if (gpu) {
				camFpX = client.getCameraFpX();
				camFpY = client.getCameraFpY();
				camFpZ = client.getCameraFpZ();
				camFpPitch = client.getCameraFpPitch();
				camFpYaw = client.getCameraFpYaw();
			} else {
				camX = client.getCameraX();
				camY = client.getCameraY();
				camZ = client.getCameraZ();
				camPitch = client.getCameraPitch();
				camYaw = client.getCameraYaw();
			}
			prepared = false;
		}
		private boolean project(LocalPoint point, int plane) {
			int localX = point.getX();
			int localY = point.getY();
			if (localX < MIN_LOCAL || localY < MIN_LOCAL || localX > MAX_LOCAL || localY > MAX_LOCAL) return false;
			prepare();
			int height = Perspective.getTileHeight(client, point, plane);
			return gpu ? projectGpu(localX, localY, height) : projectCpu(localX, localY, height);
		}
		private boolean projectCpu(int localX, int localY, int z) {
			localX -= camX;
			localY -= camY;
			z -= camZ;
			int x1 = localX * yawCos + localY * yawSin >> 16;
			int y1 = localY * yawCos - localX * yawSin >> 16;
			int y2 = z * pitchCos - y1 * pitchSin >> 16;
			int z1 = y1 * pitchCos + z * pitchSin >> 16;
			if (z1 < 50) return false;
			x = vpW / 2 + x1 * scale / z1 + vpX;
			y = vpH / 2 + y2 * scale / z1 + vpY;
			return true;
		}
		private boolean projectGpu(int localX, int localY, int z) {
			float fx = localX - camFpX;
			float fy = localY - camFpY;
			float fz = z - camFpZ;
			float x1 = fx * yawCosFp + fy * yawSinFp;
			float y1 = fy * yawCosFp - fx * yawSinFp;
			float y2 = fz * pitchCosFp - y1 * pitchSinFp;
			float z1 = y1 * pitchCosFp + fz * pitchSinFp;
			if (z1 < 50f) return false;
			x = Math.round(vpW / 2f + x1 * scale / z1) + vpX;
			y = Math.round(vpH / 2f + y2 * scale / z1) + vpY;
			return true;
		}
		private void prepare() {
			if (prepared) return;
			if (gpu) {
				pitchSinFp = (float) Math.sin(camFpPitch);
				pitchCosFp = (float) Math.cos(camFpPitch);
				yawSinFp = (float) Math.sin(camFpYaw);
				yawCosFp = (float) Math.cos(camFpYaw);
			} else {
				pitchSin = Perspective.SINE14[camPitch];
				pitchCos = Perspective.COSINE14[camPitch];
				yawSin = Perspective.SINE14[camYaw];
				yawCos = Perspective.COSINE14[camYaw];
			}
			prepared = true;
		}
		private boolean matches(CanvasProjection other) {
			if (gpu != other.gpu || scale != other.scale || vpX != other.vpX || vpY != other.vpY || vpW != other.vpW || vpH != other.vpH) return false;
			if (gpu) return Float.compare(camFpX, other.camFpX) == 0 && Float.compare(camFpY, other.camFpY) == 0 && Float.compare(camFpZ, other.camFpZ) == 0 && Float.compare(camFpPitch, other.camFpPitch) == 0 && Float.compare(camFpYaw, other.camFpYaw) == 0;
			return camX == other.camX && camY == other.camY && camZ == other.camZ && camPitch == other.camPitch && camYaw == other.camYaw;
		}
		private void copyFrom(CanvasProjection other) {
			gpu = other.gpu;
			camX = other.camX;
			camY = other.camY;
			camZ = other.camZ;
			camPitch = other.camPitch;
			camYaw = other.camYaw;
			camFpX = other.camFpX;
			camFpY = other.camFpY;
			camFpZ = other.camFpZ;
			camFpPitch = other.camFpPitch;
			camFpYaw = other.camFpYaw;
			scale = other.scale;
			vpX = other.vpX;
			vpY = other.vpY;
			vpW = other.vpW;
			vpH = other.vpH;
		}
	}
}
