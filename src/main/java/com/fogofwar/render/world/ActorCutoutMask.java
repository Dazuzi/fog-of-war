package com.fogofwar.render.world;
import com.fogofwar.actor.VisibleActorTracker;
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
import java.util.Comparator;
import java.util.List;
final class ActorCutoutMask {
	private static final int ENTITY_EXCLUSION_BUCKET_SIZE = 48;
	private static final int LOCAL_TILE_SIZE = 128;
	private static final int HALF_TILE_SIZE = 64;
	private static final Comparator<ActorCutoutCandidate> EXCLUSION_CANDIDATE_ORDER = (a, b) -> {
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
	private final VisibleActorTracker visibleActorTracker;
	private final ActorHullCache hullCache = new ActorHullCache();
	private final List<ActorCutoutCandidate> exclusionCandidates = new ArrayList<>(256);
	private Rectangle viewport;
	private int exclusionCandidateCount;
	private int lastCamX, lastCamY, lastCamZ, lastCamPitch, lastCamYaw;
	ActorCutoutMask(Client client, VisibleActorTracker visibleActorTracker) {
		this.client = client;
		this.visibleActorTracker = visibleActorTracker;
	}
	void clearCaches() { hullCache.clear(); }
	void beginFrame() {
		updateCameraState();
		hullCache.beginFrame();
	}
	void subtractExclusions(Area fogArea, Rectangle viewport, WorldView worldView, GeneralPath boundary, LocalPoint centerLp, int plane, int radius, int limit) {
		this.viewport = viewport;
		boolean all = limit == Integer.MAX_VALUE;
		collectExclusionCandidates(worldView, boundary, centerLp, plane, radius, !all);
		if (exclusionCandidateCount == 0) {
			hullCache.retainSeen();
			return;
		}
		if (all) subtractAllExclusionAreas(fogArea, boundary);
		else {
			exclusionCandidates.subList(0, exclusionCandidateCount).sort(EXCLUSION_CANDIDATE_ORDER);
			subtractSelectedExclusionAreas(fogArea, boundary, limit);
		}
		hullCache.retainSeen();
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
			subtractExclusionArea(fogArea, entryArea);
		}
	}
	private void collectExclusionCandidates(WorldView worldView, GeneralPath boundary, LocalPoint centerLp, int plane, int radius, boolean ranked) {
		exclusionCandidateCount = 0;
		int localRadius = radius * LOCAL_TILE_SIZE + HALF_TILE_SIZE;
		int bucketColumns = ranked ? Math.max(1, (viewport.width + ENTITY_EXCLUSION_BUCKET_SIZE - 1) / ENTITY_EXCLUSION_BUCKET_SIZE) : 1;
		for (Actor actor : visibleActorTracker.getVisibleActors()) {
			if (actor == null || actor.getWorldView() != worldView) continue;
			ActorHullCache.Entry cached = hullCache.get(actor);
			WorldPoint awp = actor.getWorldLocation();
			if (awp == null || awp.getPlane() != plane) continue;
			hullCache.markSeen(actor);
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
			int bucket = -1, score = 0;
			if (ranked) {
				bucket = insideViewport ? getExclusionBucket(canvasPoint, bucketColumns) : -1;
				score = Math.abs(edgeDistance);
				if (edgeDistance < 0) score += LOCAL_TILE_SIZE;
				if (!(actor instanceof Player)) score += LOCAL_TILE_SIZE / 2;
				if (hit) score -= LOCAL_TILE_SIZE * 8;
			}
			addExclusionCandidate(actor, cached, awp, anim, frame, pose, poseFrame, hit, score, bucket, canvasPoint);
		}
	}
	private int getExclusionBucket(Point canvasPoint, int bucketColumns) {
		int x = (canvasPoint.getX() - viewport.x) / ENTITY_EXCLUSION_BUCKET_SIZE;
		int y = (canvasPoint.getY() - viewport.y) / ENTITY_EXCLUSION_BUCKET_SIZE;
		return y * bucketColumns + x;
	}
	private void addExclusionCandidate(Actor actor, ActorHullCache.Entry cached, WorldPoint awp, int anim, int frame, int pose, int poseFrame, boolean hit, int score, int bucket, Point canvasPoint) {
		if (exclusionCandidateCount == exclusionCandidates.size()) exclusionCandidates.add(new ActorCutoutCandidate());
		exclusionCandidates.get(exclusionCandidateCount++).set(actor, cached, awp, anim, frame, pose, poseFrame, hit, score, bucket, canvasPoint);
	}
	private void subtractSelectedExclusionAreas(Area fogArea, GeneralPath boundary, int limit) {
		int bucketColumns = Math.max(1, (viewport.width + ENTITY_EXCLUSION_BUCKET_SIZE - 1) / ENTITY_EXCLUSION_BUCKET_SIZE);
		int bucketRows = Math.max(1, (viewport.height + ENTITY_EXCLUSION_BUCKET_SIZE - 1) / ENTITY_EXCLUSION_BUCKET_SIZE);
		boolean[] usedBuckets = new boolean[bucketColumns * bucketRows];
		int selected = 0;
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
				subtractExclusionArea(fogArea, entryArea);
				selected++;
			}
		}
	}
	private void subtractExclusionArea(Area fogArea, Area entryArea) {
		fogArea.subtract(entryArea);
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
}
