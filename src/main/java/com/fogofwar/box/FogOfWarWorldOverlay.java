package com.fogofwar.box;
import com.fogofwar.FogDisplayMode;
import com.fogofwar.FogOfWarConfig;
import com.fogofwar.EntityExclusionLimit;
import com.fogofwar.util.AreaManager;
import com.fogofwar.util.CachedStroke;
import com.fogofwar.util.ClientState;
import com.fogofwar.util.DynamicRenderDistance;
import com.fogofwar.util.RenderCenter;
import com.fogofwar.util.VisibleActorTracker;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
public class FogOfWarWorldOverlay extends Overlay {
	private static final int ENTITY_EXCLUSION_BUCKET_SIZE = 48;
	private static final int LOCAL_TILE_SIZE = 128;
	private static final int HALF_TILE_SIZE = 64;
	private static final Comparator<ExclusionCandidate> EXCLUSION_CANDIDATE_ORDER = (a, b) -> {
		int c = Integer.compare(a.score, b.score);
		if (c != 0) return c;
		c = Boolean.compare(!a.hit, !b.hit);
		if (c != 0) return c;
		c = Integer.compare(b.canvasY, a.canvasY);
		if (c != 0) return c;
		c = Integer.compare(a.canvasX, b.canvasX);
		if (c != 0) return c;
		c = Integer.compare(a.wx, b.wx);
		if (c != 0) return c;
		return Integer.compare(a.wy, b.wy);
	};
	private final Client client;
	private final FogOfWarConfig config;
	private final ClientState clientState;
	private final DynamicRenderDistance dynamicRenderDistance;
	private final AreaManager areaManager;
	private final VisibleActorTracker visibleActorTracker;
	private final Rectangle viewport = new Rectangle();
	private final List<Point> boundaryPoints = new ArrayList<>(256);
	private final GeneralPath renderAreaBoundary = new GeneralPath();
	private final GeneralPath fogPath = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
	private final CachedStroke borderStroke = new CachedStroke();
	private static class ExclusionCandidate {
		Actor actor;
		CachedHull cached;
		WorldPoint worldPoint;
		int anim, frame, pose, poseFrame, score, bucket, canvasX, canvasY, wx, wy;
		boolean hit, selected;
		void set(Actor actor, CachedHull cached, WorldPoint worldPoint, int anim, int frame, int pose, int poseFrame, boolean hit, int score, int bucket, Point canvasPoint) {
			this.actor = actor;
			this.cached = cached;
			this.worldPoint = worldPoint;
			this.anim = anim;
			this.frame = frame;
			this.pose = pose;
			this.poseFrame = poseFrame;
			this.hit = hit;
			this.score = score;
			this.bucket = bucket;
			this.canvasX = canvasPoint != null ? canvasPoint.getX() : Integer.MAX_VALUE;
			this.canvasY = canvasPoint != null ? canvasPoint.getY() : Integer.MIN_VALUE;
			this.wx = worldPoint.getX();
			this.wy = worldPoint.getY();
			this.selected = false;
		}
	}
	private static class CachedHull {
		Shape hull;
		Area area;
		Rectangle bounds;
		int wx, wy, plane, anim, frame, pose, poseFrame;
		int camX, camY, camZ, camPitch, camYaw;
	}
	private final Map<Actor, CachedHull> hullCache = new IdentityHashMap<>();
	private final Set<Actor> cacheSeen = Collections.newSetFromMap(new IdentityHashMap<>(256));
	private final List<ExclusionCandidate> exclusionCandidates = new ArrayList<>(256);
	private int exclusionCandidateCount;
	private int lastCamX, lastCamY, lastCamZ, lastCamPitch, lastCamYaw;
	@Inject
	public FogOfWarWorldOverlay(Client client, FogOfWarConfig config, ClientState clientState, DynamicRenderDistance dynamicRenderDistance, AreaManager areaManager, VisibleActorTracker visibleActorTracker) {
		this.client = client;
		this.config = config;
		this.clientState = clientState;
		this.dynamicRenderDistance = dynamicRenderDistance;
		this.areaManager = areaManager;
		this.visibleActorTracker = visibleActorTracker;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(Overlay.PRIORITY_LOW);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}
	public void clearCaches() {
		hullCache.clear();
		cacheSeen.clear();
	}

	// TEMPORARY BENCHMARK
	private long sampleStart = System.nanoTime();
	private long accumulatedTimeNs = 0;
	private int frameCount = 0;

	@Override
	public Dimension render(Graphics2D graphics) {

		// TEMPORARY BENCHMARK
		long start = System.nanoTime();

		updateCameraState();
		cacheSeen.clear();
		if (clientState.isSuppressed(config, areaManager)) return null;
		FogDisplayMode mode = config.worldMode();
		boolean showFog = mode.showsFog();
		boolean showBorder = mode.showsBorder();
		if (!showFog && !showBorder) return null;
		RenderCenter rc = RenderCenter.resolve(client);
		if (rc == null) return null;
		WorldView worldView = rc.getWorldView();
		int landRadius = dynamicRenderDistance.getCurrentRenderDistance();
		int radius = rc.isOnWorldEntity() ? config.boatRenderDistanceRadius() : landRadius;
		LocalPoint centerLp = getRenderCenter(rc, radius, landRadius);
		GeneralPath boundary = createRenderAreaBoundary(worldView, centerLp, rc.getWorldPoint().getPlane(), radius);
		setViewportBounds();
		if (boundary == null) {
			if (showFog) {
				graphics.setColor(config.worldFogColour());
				graphics.fill(viewport);
			}
			return null;
		}
		if (showFog) renderWorldFog(graphics, worldView, boundary, centerLp, rc.getWorldPoint().getPlane(), radius);
		if (showBorder) renderWorldBorder(graphics, boundary);

		// TEMPORARY BENCHMARK
		long elapsed = System.nanoTime() - start;accumulatedTimeNs += elapsed;frameCount++;long now = System.nanoTime();
		if (now - sampleStart >= 1_000_000_000L) {
			double averageMs = (accumulatedTimeNs / (double) frameCount) / 1_000_000.0;
			System.out.printf("Average execution over %d frames: %.3f ms%n", frameCount, averageMs);
			sampleStart = now;accumulatedTimeNs = 0;frameCount = 0;
		}

		return null;
	}
	private void setViewportBounds() {
		viewport.setBounds(client.getViewportXOffset(), client.getViewportYOffset(), client.getViewportWidth(), client.getViewportHeight());
	}
	private void renderWorldFog(Graphics2D graphics, WorldView worldView, GeneralPath boundary, LocalPoint centerLp, int plane, int radius) {
		if (boundary.contains(viewport)) return;
		createFogPath(boundary);
		graphics.setColor(config.worldFogColour());
		EntityExclusionLimit exclusionLimit = config.entityExclusionLimit();
		if (!exclusionLimit.isEnabled()) {
			graphics.fill(fogPath);
			return;
		}
		Area result = buildExclusion(worldView, boundary, centerLp, plane, radius, exclusionLimit.getLimit());
		if (result == null) {
			graphics.fill(fogPath);
			return;
		}
		Area fogArea = new Area(fogPath);
		fogArea.subtract(result);
		graphics.fill(fogArea);
	}
	private void updateCameraState() {
		int x = client.getCameraX(), y = client.getCameraY(), z = client.getCameraZ();
		int p = client.getCameraPitch(), w = client.getCameraYaw();
		lastCamX = x; lastCamY = y; lastCamZ = z; lastCamPitch = p; lastCamYaw = w;
	}
	private Area buildExclusion(WorldView worldView, GeneralPath boundary, LocalPoint centerLp, int plane, int radius, int limit) {
		collectExclusionCandidates(worldView, boundary, centerLp, plane, radius);
		if (exclusionCandidateCount == 0) {
			hullCache.keySet().retainAll(cacheSeen);
			return null;
		}
		exclusionCandidates.subList(0, exclusionCandidateCount).sort(EXCLUSION_CANDIDATE_ORDER);
		Area result = buildSelectedExclusionArea(boundary, limit);
		hullCache.keySet().retainAll(cacheSeen);
		return result;
	}
	private void collectExclusionCandidates(WorldView worldView, GeneralPath boundary, LocalPoint centerLp, int plane, int radius) {
		exclusionCandidateCount = 0;
		int localRadius = radius * LOCAL_TILE_SIZE + HALF_TILE_SIZE;
		int bucketColumns = Math.max(1, (viewport.width + ENTITY_EXCLUSION_BUCKET_SIZE - 1) / ENTITY_EXCLUSION_BUCKET_SIZE);
		for (Actor actor : visibleActorTracker.getVisibleActors()) {
			if (actor == null || actor.getWorldView() != worldView) continue;
			CachedHull cached = hullCache.get(actor);
			WorldPoint awp = actor.getWorldLocation();
			if (awp == null || awp.getPlane() != plane) continue;
			cacheSeen.add(actor);
			int anim = actor.getAnimation(), frame = actor.getAnimationFrame();
			int pose = actor.getPoseAnimation(), poseFrame = actor.getPoseAnimationFrame();
			boolean hit = cached != null
					&& cached.wx == awp.getX() && cached.wy == awp.getY() && cached.plane == awp.getPlane()
					&& cached.anim == anim && cached.frame == frame
					&& cached.pose == pose && cached.poseFrame == poseFrame
					&& cached.camX == lastCamX && cached.camY == lastCamY && cached.camZ == lastCamZ
					&& cached.camPitch == lastCamPitch && cached.camYaw == lastCamYaw;
			if (hit) {
				if (!viewport.intersects(cached.bounds)) continue;
				if (boundary.contains(cached.bounds)) continue;
			}
			LocalPoint lp = actor.getLocalLocation();
			if (lp == null) lp = LocalPoint.fromWorld(worldView, awp);
			if (lp == null) continue;
			int dx = Math.abs(lp.getX() - centerLp.getX());
			int dy = Math.abs(lp.getY() - centerLp.getY());
			int edgeDistance = Math.max(dx, dy) - localRadius;
			int footprintRadius = Math.max(LOCAL_TILE_SIZE, actor.getFootprintSize() * LOCAL_TILE_SIZE / 2);
			if (!hit && edgeDistance < -footprintRadius) continue;
			Point canvasPoint = Perspective.localToCanvas(client, lp, plane);
			boolean insideViewport = canvasPoint != null && viewport.contains(canvasPoint.getX(), canvasPoint.getY());
			if (!hit && !insideViewport) continue;
			int bucket = insideViewport ? getExclusionBucket(canvasPoint, bucketColumns) : -1;
			int score = Math.abs(edgeDistance);
			if (edgeDistance < 0) score += LOCAL_TILE_SIZE;
			if (!(actor instanceof Player)) score += LOCAL_TILE_SIZE / 2;
			if (hit) score -= LOCAL_TILE_SIZE * 8;
			addExclusionCandidate(actor, cached, awp, anim, frame, pose, poseFrame, hit, score, bucket, canvasPoint);
		}
	}
	private int getExclusionBucket(Point canvasPoint, int bucketColumns) {
		int x = (canvasPoint.getX() - viewport.x) / ENTITY_EXCLUSION_BUCKET_SIZE;
		int y = (canvasPoint.getY() - viewport.y) / ENTITY_EXCLUSION_BUCKET_SIZE;
		return y * bucketColumns + x;
	}
	private void addExclusionCandidate(Actor actor, CachedHull cached, WorldPoint awp, int anim, int frame, int pose, int poseFrame, boolean hit, int score, int bucket, Point canvasPoint) {
		if (exclusionCandidateCount == exclusionCandidates.size()) exclusionCandidates.add(new ExclusionCandidate());
		exclusionCandidates.get(exclusionCandidateCount++).set(actor, cached, awp, anim, frame, pose, poseFrame, hit, score, bucket, canvasPoint);
	}
	private Area buildSelectedExclusionArea(GeneralPath boundary, int limit) {
		Area result = null;
		int bucketColumns = Math.max(1, (viewport.width + ENTITY_EXCLUSION_BUCKET_SIZE - 1) / ENTITY_EXCLUSION_BUCKET_SIZE);
		int bucketRows = Math.max(1, (viewport.height + ENTITY_EXCLUSION_BUCKET_SIZE - 1) / ENTITY_EXCLUSION_BUCKET_SIZE);
		boolean[] usedBuckets = new boolean[bucketColumns * bucketRows];
		int selected = 0;
		for (int pass = 0; pass < 2 && selected < limit; pass++) {
			for (int i = 0; i < exclusionCandidateCount && selected < limit; i++) {
				ExclusionCandidate candidate = exclusionCandidates.get(i);
				if (candidate.selected) continue;
				if (pass == 0 && candidate.bucket < 0) continue;
				if (pass == 0 && usedBuckets[candidate.bucket]) continue;
				Area entryArea = getCandidateArea(candidate, boundary);
				if (entryArea == null) continue;
				candidate.selected = true;
				if (candidate.bucket >= 0) usedBuckets[candidate.bucket] = true;
				if (result == null) result = new Area(entryArea);
				else result.add(entryArea);
				selected++;
			}
		}
		return result;
	}
	private Area getCandidateArea(ExclusionCandidate candidate, GeneralPath boundary) {
		CachedHull cached = candidate.cached;
		if (candidate.hit) {
			if (cached.area == null) cached.area = new Area(cached.hull);
			return cached.area;
		}
		Shape hull = candidate.actor.getConvexHull();
		if (hull == null) {
			if (cached != null) hullCache.remove(candidate.actor);
			return null;
		}
		Rectangle bounds = hull.getBounds();
		if (cached == null) {
			cached = new CachedHull();
			hullCache.put(candidate.actor, cached);
		}
		cached.hull = hull;
		cached.bounds = bounds;
		cached.area = null;
		cached.wx = candidate.worldPoint.getX();
		cached.wy = candidate.worldPoint.getY();
		cached.plane = candidate.worldPoint.getPlane();
		cached.anim = candidate.anim;
		cached.frame = candidate.frame;
		cached.pose = candidate.pose;
		cached.poseFrame = candidate.poseFrame;
		cached.camX = lastCamX;
		cached.camY = lastCamY;
		cached.camZ = lastCamZ;
		cached.camPitch = lastCamPitch;
		cached.camYaw = lastCamYaw;
		if (!viewport.intersects(bounds)) return null;
		if (boundary.contains(bounds)) return null;
		cached.area = new Area(hull);
		return cached.area;
	}
	private void createFogPath(GeneralPath boundary) {
		fogPath.reset();
		fogPath.moveTo(viewport.x, viewport.y);
		fogPath.lineTo(viewport.x + viewport.width, viewport.y);
		fogPath.lineTo(viewport.x + viewport.width, viewport.y + viewport.height);
		fogPath.lineTo(viewport.x, viewport.y + viewport.height);
		fogPath.closePath();
		fogPath.append(boundary, false);
	}
	private void renderWorldBorder(Graphics2D graphics, GeneralPath boundary) {
		Stroke oldStroke = graphics.getStroke();
		graphics.setColor(config.worldBorderColour());
		graphics.setStroke(borderStroke.get(config.worldBorderThickness()));
		graphics.draw(boundary);
		graphics.setStroke(oldStroke);
	}
	private LocalPoint getRenderCenter(RenderCenter rc, int radius, int landRadius) {
		LocalPoint lp = rc.isOnWorldEntity() && radius > landRadius ? rc.getTargetLocalPoint() : rc.getLocalPoint();
		if (lp == null) return null;
		return new LocalPoint(snapAxis(lp.getX()), snapAxis(lp.getY()), rc.getWorldView());
	}
	private GeneralPath createRenderAreaBoundary(WorldView worldView, LocalPoint centerLp, int plane, int radius) {
		if (centerLp == null) return null;
		boundaryPoints.clear();
		int localRadius = radius * LOCAL_TILE_SIZE + HALF_TILE_SIZE;
		int sampleCount = radius * 2 + 1;
		int step = localRadius * 2 / sampleCount;
		int minX = centerLp.getX() - localRadius;
		int maxX = centerLp.getX() + localRadius;
		int minY = centerLp.getY() - localRadius;
		int maxY = centerLp.getY() + localRadius;
		for (int i = 0; i < sampleCount; i++) addPoint(worldView, boundaryPoints, minX + i * step, maxY, plane);
		for (int i = 0; i < sampleCount; i++) addPoint(worldView, boundaryPoints, maxX, maxY - i * step, plane);
		for (int i = 0; i < sampleCount; i++) addPoint(worldView, boundaryPoints, maxX - i * step, minY, plane);
		for (int i = 0; i < sampleCount; i++) addPoint(worldView, boundaryPoints, minX, minY + i * step, plane);
		return createPathFromPoints(boundaryPoints);
	}
	private static int snapAxis(int current) { return (current / LOCAL_TILE_SIZE) * LOCAL_TILE_SIZE + HALF_TILE_SIZE; }
	private void addPoint(WorldView worldView, List<Point> points, int localX, int localY, int plane) {
		LocalPoint lp = new LocalPoint(localX, localY, worldView);
		Point canvasPoint = Perspective.localToCanvas(client, lp, plane);
		if (canvasPoint != null) points.add(canvasPoint);
	}
	private GeneralPath createPathFromPoints(List<Point> points) {
		if (points.size() < 3) return null;
		renderAreaBoundary.reset();
		Point first = points.get(0);
		renderAreaBoundary.moveTo(first.getX(), first.getY());
		for (int i = 1; i < points.size(); i++) {
			Point point = points.get(i);
			renderAreaBoundary.lineTo(point.getX(), point.getY());
		}
		renderAreaBoundary.closePath();
		return renderAreaBoundary;
	}
}
