package com.fogofwar.render.world;
import com.fogofwar.coord.WorldEntityCoords;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
final class ActorCutoutMask {
	private static final int ACTOR_CUTOUT_BUCKET_SIZE = 48;
	private static final int PRIORITY_SCORE = Integer.MIN_VALUE / 2;
	private static final List<ActorCutoutCandidate> EMPTY_CANDIDATES = Collections.emptyList();
	private static final Comparator<ActorCutoutCandidate> ACTOR_CUTOUT_CANDIDATE_ORDER = (a, b) -> {
		int c = Integer.compare(a.score, b.score);
		if (c != 0) return c;
		c = Boolean.compare(!a.hit, !b.hit);
		if (c != 0) return c;
		c = Integer.compare(b.canvasY, a.canvasY);
		if (c != 0) return c;
		c = Integer.compare(a.canvasX, b.canvasX);
		if (c != 0) return c;
		c = Integer.compare(a.worldX, b.worldX);
		if (c != 0) return c;
		return Integer.compare(a.worldY, b.worldY);
	};
	private final Client client;
	private final VisibleActorTracker visibleActorTracker;
	private final ActorHullCache hullCache = new ActorHullCache();
	private List<ActorCutoutCandidate> exclusionCandidates = EMPTY_CANDIDATES;
	private boolean[] usedBuckets = new boolean[0];
	private Rectangle viewport;
	private Player localPlayer;
	private int exclusionCandidateCount;
	private int lastCamX, lastCamY, lastCamZ, lastCamPitch, lastCamYaw, lastScale, lastVpX, lastVpY, lastVpW, lastVpH;
	private float lastCamFpX, lastCamFpY, lastCamFpZ, lastCamFpPitch, lastCamFpYaw;
	private boolean lastGpu;
	private boolean prepared;
	private boolean retainHullCache;
	ActorCutoutMask(Client client, VisibleActorTracker visibleActorTracker) {
		this.client = client;
		this.visibleActorTracker = visibleActorTracker;
	}
	void beginFrame(Player localPlayer) {
		this.localPlayer = localPlayer;
		prepared = false;
		retainHullCache = false;
	}
	void endFrame() {
		if (prepared && retainHullCache) hullCache.retainSeen();
		localPlayer = null;
	}
	void subtractExclusions(Area fogArea, Rectangle viewport, WorldView worldView, GeneralPath boundary, LocalPoint centerLp, int plane, int radius, int limit) {
		this.viewport = viewport;
		Set<Actor> visibleActors = visibleActorTracker.getVisibleActors();
		int expectedActors = Math.max(16, Math.min(256, visibleActors.size() + 1));
		prepareFrame(expectedActors);
		retainHullCache = true;
		if (exclusionCandidates == EMPTY_CANDIDATES) exclusionCandidates = new ArrayList<>(expectedActors);
		boolean all = limit == Integer.MAX_VALUE;
		try {
			collectExclusionCandidates(visibleActors, worldView, boundary, centerLp, plane, radius);
			if (exclusionCandidateCount == 0) return;
			if (all || exclusionCandidateCount <= limit) subtractAllExclusionAreas(fogArea, boundary);
			else {
				int bucketColumns = Math.max(1, (viewport.width + ACTOR_CUTOUT_BUCKET_SIZE - 1) / ACTOR_CUTOUT_BUCKET_SIZE);
				rankExclusionCandidates(centerLp, radius * Perspective.LOCAL_TILE_SIZE + Perspective.LOCAL_HALF_TILE_SIZE, bucketColumns);
				exclusionCandidates.subList(0, exclusionCandidateCount).sort(ACTOR_CUTOUT_CANDIDATE_ORDER);
				subtractSelectedExclusionAreas(fogArea, boundary, limit, bucketColumns);
			}
		} finally {
			clearExclusionCandidates();
		}
	}
	private void prepareFrame(int expectedActors) {
		if (prepared) return;
		updateCameraState();
		hullCache.beginFrame(expectedActors);
		prepared = true;
	}
	private void updateCameraState() {
		lastGpu = client.isGpu();
		if (lastGpu) {
			lastCamFpX = client.getCameraFpX();
			lastCamFpY = client.getCameraFpY();
			lastCamFpZ = client.getCameraFpZ();
			lastCamFpPitch = client.getCameraFpPitch();
			lastCamFpYaw = client.getCameraFpYaw();
		} else {
			lastCamX = client.getCameraX();
			lastCamY = client.getCameraY();
			lastCamZ = client.getCameraZ();
			lastCamPitch = client.getCameraPitch();
			lastCamYaw = client.getCameraYaw();
		}
		lastScale = client.getScale();
		lastVpX = viewport.x;
		lastVpY = viewport.y;
		lastVpW = viewport.width;
		lastVpH = viewport.height;
	}
	private void subtractAllExclusionAreas(Area fogArea, GeneralPath boundary) {
		for (int i = 0; i < exclusionCandidateCount; i++) {
			Area entryArea = getCandidateArea(exclusionCandidates.get(i), boundary);
			if (entryArea == null) continue;
			fogArea.subtract(entryArea);
		}
	}
	private void collectExclusionCandidates(Set<Actor> visibleActors, WorldView worldView, GeneralPath boundary, LocalPoint centerLp, int plane, int radius) {
		exclusionCandidateCount = 0;
		int localRadius = radius * Perspective.LOCAL_TILE_SIZE + Perspective.LOCAL_HALF_TILE_SIZE;
		if (localPlayer != null) collectExclusionCandidate(localPlayer, worldView, boundary, centerLp, plane, localRadius);
		for (Actor actor : visibleActors) {
			if (actor == localPlayer) continue;
			collectExclusionCandidate(actor, worldView, boundary, centerLp, plane, localRadius);
		}
	}
	private void collectExclusionCandidate(Actor actor, WorldView worldView, GeneralPath boundary, LocalPoint centerLp, int plane, int localRadius) {
		if (actor == null) return;
		boolean priority = actor == localPlayer;
		WorldView actorWv = actor.getWorldView();
		if (actorWv == null) return;
		WorldPoint actorWorldPoint = actor.getWorldLocation();
		LocalPoint lp = actor.getLocalLocation();
		int worldX, worldY, worldPlane;
		if (actorWv.isTopLevel()) {
			if (actorWorldPoint == null) return;
			if (lp == null) lp = LocalPoint.fromWorld(worldView, actorWorldPoint);
			if (lp == null) return;
			worldX = actorWorldPoint.getX();
			worldY = actorWorldPoint.getY();
			worldPlane = actorWorldPoint.getPlane();
		} else {
			WorldEntity worldEntity = WorldEntityCoords.getWorldEntity(actorWv, worldView);
			if (worldEntity == null) return;
			if (lp == null && actorWorldPoint != null) lp = LocalPoint.fromWorld(actorWv, actorWorldPoint);
			if (lp == null) return;
			lp = worldEntity.transformToMainWorld(lp);
			if (lp == null) return;
			worldX = (lp.getX() >> Perspective.LOCAL_COORD_BITS) + worldView.getBaseX();
			worldY = (lp.getY() >> Perspective.LOCAL_COORD_BITS) + worldView.getBaseY();
			worldPlane = worldView.getPlane();
		}
		if (worldPlane != plane) return;
		boolean onSubWorld = !actorWv.isTopLevel();
		ActorHullCache.Entry cached = hullCache.get(actor);
		hullCache.markSeen(actor);
		int anim = actor.getAnimation(), frame = actor.getAnimationFrame();
		int pose = actor.getPoseAnimation(), poseFrame = actor.getPoseAnimationFrame();
		int orientation = actor.getCurrentOrientation();
		boolean hit = cached != null
				&& cached.wx == worldX && cached.wy == worldY && cached.plane == worldPlane
				&& cached.localX == lp.getX() && cached.localY == lp.getY()
				&& cached.anim == anim && cached.frame == frame
				&& cached.pose == pose && cached.poseFrame == poseFrame
				&& cached.orientation == orientation
				&& cached.gpu == lastGpu
				&& (lastGpu
						? Float.compare(cached.camFpX, lastCamFpX) == 0 && Float.compare(cached.camFpY, lastCamFpY) == 0 && Float.compare(cached.camFpZ, lastCamFpZ) == 0 && Float.compare(cached.camFpPitch, lastCamFpPitch) == 0 && Float.compare(cached.camFpYaw, lastCamFpYaw) == 0
						: cached.camX == lastCamX && cached.camY == lastCamY && cached.camZ == lastCamZ && cached.camPitch == lastCamPitch && cached.camYaw == lastCamYaw)
				&& cached.scale == lastScale
				&& cached.vpX == lastVpX && cached.vpY == lastVpY && cached.vpW == lastVpW && cached.vpH == lastVpH;
		if (hit) {
			if (!viewport.intersects(cached.bounds)) return;
			if (!priority) {
				boolean inside = boundary.contains(cached.bounds);
				if (inside) return;
			}
		}
		int localX, localY, canvasX, canvasY, edgeDistance;
		if (hit) {
			localX = cached.localX;
			localY = cached.localY;
			canvasX = cached.canvasX;
			canvasY = cached.canvasY;
		} else {
			localX = lp.getX();
			localY = lp.getY();
			edgeDistance = getEdgeDistance(localX, localY, centerLp, localRadius);
			int footprintRadius = Math.max(Perspective.LOCAL_TILE_SIZE, actor.getFootprintSize() * Perspective.LOCAL_TILE_SIZE / 2);
			if (!priority && !onSubWorld && edgeDistance < -footprintRadius) return;
			Point canvasPoint = Perspective.localToCanvas(client, lp, plane);
			if (!priority && (canvasPoint == null || !viewport.contains(canvasPoint.getX(), canvasPoint.getY()))) return;
			canvasX = canvasPoint != null ? canvasPoint.getX() : viewport.x + viewport.width / 2;
			canvasY = canvasPoint != null ? canvasPoint.getY() : viewport.y + viewport.height / 2;
		}
		addExclusionCandidate(actor, cached, worldX, worldY, worldPlane, anim, frame, pose, poseFrame, orientation, hit, canvasX, canvasY, localX, localY);
	}
	private void rankExclusionCandidates(LocalPoint centerLp, int localRadius, int bucketColumns) {
		for (int i = 0; i < exclusionCandidateCount; i++) {
			ActorCutoutCandidate candidate = exclusionCandidates.get(i);
			if (viewport.contains(candidate.canvasX, candidate.canvasY)) candidate.bucket = getExclusionBucket(candidate.canvasX, candidate.canvasY, bucketColumns);
			candidate.score = getCandidateScore(candidate.actor, candidate.hit, getEdgeDistance(candidate.localX, candidate.localY, centerLp, localRadius));
		}
	}
	private int getEdgeDistance(int localX, int localY, LocalPoint centerLp, int localRadius) {
		int dx = Math.abs(localX - centerLp.getX());
		int dy = Math.abs(localY - centerLp.getY());
		return Math.max(dx, dy) - localRadius;
	}
	private int getCandidateScore(Actor actor, boolean hit, int edgeDistance) {
		if (actor == localPlayer) return PRIORITY_SCORE;
		int score = Math.abs(edgeDistance);
		if (edgeDistance < 0) score += Perspective.LOCAL_TILE_SIZE;
		if (!(actor instanceof Player)) score += Perspective.LOCAL_TILE_SIZE / 2;
		if (hit) score -= Perspective.LOCAL_TILE_SIZE * 8;
		return score;
	}
	private int getExclusionBucket(int canvasX, int canvasY, int bucketColumns) {
		int x = (canvasX - viewport.x) / ACTOR_CUTOUT_BUCKET_SIZE;
		int y = (canvasY - viewport.y) / ACTOR_CUTOUT_BUCKET_SIZE;
		return y * bucketColumns + x;
	}
	private void addExclusionCandidate(Actor actor, ActorHullCache.Entry cached, int worldX, int worldY, int plane, int anim, int frame, int pose, int poseFrame, int orientation, boolean hit, int canvasX, int canvasY, int localX, int localY) {
		if (exclusionCandidateCount == exclusionCandidates.size()) exclusionCandidates.add(new ActorCutoutCandidate());
		exclusionCandidates.get(exclusionCandidateCount++).set(actor, cached, worldX, worldY, plane, anim, frame, pose, poseFrame, orientation, hit, canvasX, canvasY, localX, localY);
	}
	private void subtractSelectedExclusionAreas(Area fogArea, GeneralPath boundary, int limit, int bucketColumns) {
		int bucketRows = Math.max(1, (viewport.height + ACTOR_CUTOUT_BUCKET_SIZE - 1) / ACTOR_CUTOUT_BUCKET_SIZE);
		int bucketCount = bucketColumns * bucketRows;
		if (usedBuckets.length < bucketCount) usedBuckets = new boolean[bucketCount];
		else Arrays.fill(usedBuckets, 0, bucketCount, false);
		int selected = subtractPriorityExclusionAreas(fogArea, boundary, limit);
		for (int pass = 0; pass < 2 && selected < limit; pass++) {
			for (int i = 0; i < exclusionCandidateCount && selected < limit; i++) {
				ActorCutoutCandidate candidate = exclusionCandidates.get(i);
				if (candidate.selected) continue;
				if (pass == 0 && candidate.bucket < 0) continue;
				if (pass == 0 && usedBuckets[candidate.bucket]) continue;
				Area entryArea = getCandidateArea(candidate, boundary);
				if (entryArea == null) continue;
				candidate.selected = true;
				if (candidate.bucket >= 0) usedBuckets[candidate.bucket] = true;
				fogArea.subtract(entryArea);
				selected++;
			}
		}
	}
	private int subtractPriorityExclusionAreas(Area fogArea, GeneralPath boundary, int limit) {
		int selected = 0;
		for (int i = 0; i < exclusionCandidateCount && selected < limit; i++) {
			ActorCutoutCandidate candidate = exclusionCandidates.get(i);
			if (candidate.actor != localPlayer) continue;
			Area entryArea = getCandidateArea(candidate, boundary);
			if (entryArea != null) {
				candidate.selected = true;
				fogArea.subtract(entryArea);
				selected++;
			}
			break;
		}
		return selected;
	}
	private void clearExclusionCandidates() {
		for (int i = 0; i < exclusionCandidateCount; i++) exclusionCandidates.get(i).clear();
		exclusionCandidateCount = 0;
	}
	private Area getCandidateArea(ActorCutoutCandidate candidate, GeneralPath boundary) {
		Shape hull = getCandidateHull(candidate, boundary);
		if (hull == null) return null;
		ActorHullCache.Entry cached = candidate.cached;
		if (cached == null) return null;
		if (cached.area == null) cached.area = new Area(hull);
		return cached.area;
	}
	private Shape getCandidateHull(ActorCutoutCandidate candidate, GeneralPath boundary) {
		ActorHullCache.Entry cached = candidate.cached;
		if (candidate.hit) return cached.hull;
		Shape hull = candidate.actor.getConvexHull();
		if (hull == null) {
			if (cached != null) hullCache.remove(candidate.actor);
			return null;
		}
		Rectangle bounds = hull.getBounds();
		cache(candidate, hull, bounds);
		if (!viewport.intersects(bounds)) return null;
		if (candidate.actor != localPlayer) {
			boolean inside = boundary.contains(bounds);
			if (inside) return null;
		}
		return hull;
	}
	private void cache(ActorCutoutCandidate candidate, Shape hull, Rectangle bounds) {
		ActorHullCache.Entry cached = hullCache.getOrCreate(candidate.actor);
		candidate.cached = cached;
		cached.hull = hull;
		cached.bounds = bounds;
		cached.area = null;
		cached.wx = candidate.worldX;
		cached.wy = candidate.worldY;
		cached.plane = candidate.plane;
		cached.localX = candidate.localX;
		cached.localY = candidate.localY;
		cached.canvasX = candidate.canvasX;
		cached.canvasY = candidate.canvasY;
		cached.anim = candidate.anim;
		cached.frame = candidate.frame;
		cached.pose = candidate.pose;
		cached.poseFrame = candidate.poseFrame;
		cached.orientation = candidate.orientation;
		cached.camX = lastCamX;
		cached.camY = lastCamY;
		cached.camZ = lastCamZ;
		cached.camPitch = lastCamPitch;
		cached.camYaw = lastCamYaw;
		cached.camFpX = lastCamFpX;
		cached.camFpY = lastCamFpY;
		cached.camFpZ = lastCamFpZ;
		cached.camFpPitch = lastCamFpPitch;
		cached.camFpYaw = lastCamFpYaw;
		cached.gpu = lastGpu;
		cached.scale = lastScale;
		cached.vpX = lastVpX;
		cached.vpY = lastVpY;
		cached.vpW = lastVpW;
		cached.vpH = lastVpH;
	}
	private static final class ActorCutoutCandidate {
		private Actor actor;
		private ActorHullCache.Entry cached;
		private int worldX, worldY, plane, anim, frame, pose, poseFrame, orientation, score, bucket, canvasX, canvasY, localX, localY;
		private boolean hit, selected;
		private void set(Actor actor, ActorHullCache.Entry cached, int worldX, int worldY, int plane, int anim, int frame, int pose, int poseFrame, int orientation, boolean hit, int canvasX, int canvasY, int localX, int localY) {
			this.actor = actor;
			this.cached = cached;
			this.worldX = worldX;
			this.worldY = worldY;
			this.plane = plane;
			this.anim = anim;
			this.frame = frame;
			this.pose = pose;
			this.poseFrame = poseFrame;
			this.orientation = orientation;
			this.hit = hit;
			this.score = 0;
			this.bucket = -1;
			this.canvasX = canvasX;
			this.canvasY = canvasY;
			this.localX = localX;
			this.localY = localY;
			this.selected = false;
		}
		private void clear() {
			actor = null;
			cached = null;
			worldX = worldY = plane = anim = frame = pose = poseFrame = orientation = score = bucket = canvasX = canvasY = localX = localY = 0;
			hit = selected = false;
		}
	}
}
