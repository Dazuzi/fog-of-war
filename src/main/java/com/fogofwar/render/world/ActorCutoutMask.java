package com.fogofwar.render.world;
import com.fogofwar.coord.WorldEntityCoords;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
final class ActorCutoutMask {
	private static final int ACTOR_CUTOUT_BUCKET_SIZE = 48;
	private static final int PRIORITY_SCORE = Integer.MIN_VALUE / 2;
	private static final Comparator<ActorCutoutCandidate> ACTOR_CUTOUT_CANDIDATE_ORDER = (a, b) -> {
		int c = Integer.compare(a.score, b.score);
		if (c != 0) return c;
		c = Boolean.compare(!a.hit, !b.hit);
		if (c != 0) return c;
		c = Integer.compare(b.canvasY, a.canvasY);
		if (c != 0) return c;
		c = Integer.compare(a.canvasX, b.canvasX);
		if (c != 0) return c;
		c = Integer.compare(a.worldPoint.getX(), b.worldPoint.getX());
		if (c != 0) return c;
		return Integer.compare(a.worldPoint.getY(), b.worldPoint.getY());
	};
	private final Client client;
	private final VisibleActorTracker visibleActorTracker;
	private final ActorHullCache hullCache = new ActorHullCache();
	private final List<ActorCutoutCandidate> exclusionCandidates = new ArrayList<>(256);
	private boolean[] usedBuckets = new boolean[0];
	private Rectangle viewport;
	private Player localPlayer;
	private int exclusionCandidateCount;
	private int lastCamX, lastCamY, lastCamZ, lastCamPitch, lastCamYaw;
	private boolean retainHullCache;
	ActorCutoutMask(Client client, VisibleActorTracker visibleActorTracker) {
		this.client = client;
		this.visibleActorTracker = visibleActorTracker;
	}
	void clearCaches() { hullCache.clear(); }
	void beginFrame() {
		updateCameraState();
		hullCache.beginFrame();
		retainHullCache = false;
	}
	void endFrame() {
		if (retainHullCache) hullCache.retainSeen();
	}
	void subtractExclusions(Area fogArea, Rectangle viewport, WorldView worldView, GeneralPath boundary, LocalPoint centerLp, int plane, int radius, int limit) {
		this.viewport = viewport;
		this.localPlayer = client.getLocalPlayer();
		retainHullCache = true;
		boolean all = limit == Integer.MAX_VALUE;
		int bucketColumns = all ? 1 : Math.max(1, (viewport.width + ACTOR_CUTOUT_BUCKET_SIZE - 1) / ACTOR_CUTOUT_BUCKET_SIZE);
		try {
			collectExclusionCandidates(worldView, boundary, centerLp, plane, radius, !all, bucketColumns);
			if (exclusionCandidateCount == 0) return;
			if (all) subtractAllExclusionAreas(fogArea, boundary);
			else {
				exclusionCandidates.subList(0, exclusionCandidateCount).sort(ACTOR_CUTOUT_CANDIDATE_ORDER);
				subtractSelectedExclusionAreas(fogArea, boundary, limit, bucketColumns);
			}
		} finally {
			clearExclusionCandidates();
			this.localPlayer = null;
		}
	}
	private void updateCameraState() {
		lastCamX = client.getCameraX();
		lastCamY = client.getCameraY();
		lastCamZ = client.getCameraZ();
		lastCamPitch = client.getCameraPitch();
		lastCamYaw = client.getCameraYaw();
	}
	private void subtractAllExclusionAreas(Area fogArea, GeneralPath boundary) {
		for (int i = 0; i < exclusionCandidateCount; i++) {
			Area entryArea = getCandidateArea(exclusionCandidates.get(i), boundary);
			if (entryArea == null) continue;
			fogArea.subtract(entryArea);
		}
	}
	private void collectExclusionCandidates(WorldView worldView, GeneralPath boundary, LocalPoint centerLp, int plane, int radius, boolean ranked, int bucketColumns) {
		exclusionCandidateCount = 0;
		int localRadius = radius * Perspective.LOCAL_TILE_SIZE + Perspective.LOCAL_HALF_TILE_SIZE;
		if (localPlayer != null) collectExclusionCandidate(localPlayer, worldView, boundary, centerLp, plane, localRadius, ranked, bucketColumns);
		for (Actor actor : visibleActorTracker.getVisibleActors()) {
			if (actor == localPlayer) continue;
			collectExclusionCandidate(actor, worldView, boundary, centerLp, plane, localRadius, ranked, bucketColumns);
		}
	}
	private void collectExclusionCandidate(Actor actor, WorldView worldView, GeneralPath boundary, LocalPoint centerLp, int plane, int localRadius, boolean ranked, int bucketColumns) {
		if (actor == null) return;
		boolean priority = actor == localPlayer;
		WorldEntityCoords.ResolvedPoint location = WorldEntityCoords.resolveTopLevel(actor, worldView);
		if (location == null || location.worldPoint.getPlane() != plane) return;
		ActorHullCache.Entry cached = hullCache.get(actor);
		hullCache.markSeen(actor);
		int anim = actor.getAnimation(), frame = actor.getAnimationFrame();
		int pose = actor.getPoseAnimation(), poseFrame = actor.getPoseAnimationFrame();
		boolean hit = cached != null
				&& cached.wx == location.worldPoint.getX() && cached.wy == location.worldPoint.getY() && cached.plane == location.worldPoint.getPlane()
				&& cached.anim == anim && cached.frame == frame
				&& cached.pose == pose && cached.poseFrame == poseFrame
				&& cached.camX == lastCamX && cached.camY == lastCamY && cached.camZ == lastCamZ
				&& cached.camPitch == lastCamPitch && cached.camYaw == lastCamYaw;
		if (hit) {
			if (!viewport.intersects(cached.bounds)) return;
			if (!priority && boundary.contains(cached.bounds)) return;
		}
		int localX, localY, canvasX, canvasY, edgeDistance;
		if (hit) {
			localX = cached.localX;
			localY = cached.localY;
			canvasX = cached.canvasX;
			canvasY = cached.canvasY;
			edgeDistance = getEdgeDistance(localX, localY, centerLp, localRadius);
		} else {
			LocalPoint lp = location.localPoint;
			localX = lp.getX();
			localY = lp.getY();
			edgeDistance = getEdgeDistance(localX, localY, centerLp, localRadius);
			int footprintRadius = Math.max(Perspective.LOCAL_TILE_SIZE, actor.getFootprintSize() * Perspective.LOCAL_TILE_SIZE / 2);
			if (!priority && edgeDistance < -footprintRadius) return;
			Point canvasPoint = Perspective.localToCanvas(client, lp, plane);
			if (!priority && (canvasPoint == null || !viewport.contains(canvasPoint.getX(), canvasPoint.getY()))) return;
			canvasX = canvasPoint != null ? canvasPoint.getX() : viewport.x + viewport.width / 2;
			canvasY = canvasPoint != null ? canvasPoint.getY() : viewport.y + viewport.height / 2;
		}
		int bucket = -1, score = 0;
		if (ranked) {
			if (viewport.contains(canvasX, canvasY)) bucket = getExclusionBucket(canvasX, canvasY, bucketColumns);
			score = getCandidateScore(actor, hit, edgeDistance);
		}
		addExclusionCandidate(actor, cached, location.worldPoint, anim, frame, pose, poseFrame, hit, score, bucket, canvasX, canvasY, localX, localY);
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
	private void addExclusionCandidate(Actor actor, ActorHullCache.Entry cached, WorldPoint awp, int anim, int frame, int pose, int poseFrame, boolean hit, int score, int bucket, int canvasX, int canvasY, int localX, int localY) {
		if (exclusionCandidateCount == exclusionCandidates.size()) exclusionCandidates.add(new ActorCutoutCandidate());
		exclusionCandidates.get(exclusionCandidateCount++).set(actor, cached, awp, anim, frame, pose, poseFrame, hit, score, bucket, canvasX, canvasY, localX, localY);
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
			if (entryArea == null) continue;
			candidate.selected = true;
			fogArea.subtract(entryArea);
			selected++;
		}
		return selected;
	}
	private void clearExclusionCandidates() {
		for (int i = 0; i < exclusionCandidateCount; i++) exclusionCandidates.get(i).clear();
		exclusionCandidateCount = 0;
	}
	private Area getCandidateArea(ActorCutoutCandidate candidate, GeneralPath boundary) {
		ActorHullCache.Entry cached = candidate.cached;
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
		cached = hullCache.getOrCreate(candidate.actor);
		cached.hull = hull;
		cached.bounds = bounds;
		cached.area = null;
		cached.wx = candidate.worldPoint.getX();
		cached.wy = candidate.worldPoint.getY();
		cached.plane = candidate.worldPoint.getPlane();
		cached.localX = candidate.localX;
		cached.localY = candidate.localY;
		cached.canvasX = candidate.canvasX;
		cached.canvasY = candidate.canvasY;
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
		if (candidate.actor != localPlayer && boundary.contains(bounds)) return null;
		cached.area = new Area(hull);
		return cached.area;
	}
}
