package com.fogofwar.render.world;
import net.runelite.api.Actor;
import net.runelite.api.coords.WorldPoint;
final class ActorCutoutCandidate {
	Actor actor;
	ActorHullCache.Entry cached;
	WorldPoint worldPoint;
	int anim, frame, pose, poseFrame, score, bucket, canvasX, canvasY, localX, localY;
	boolean hit, selected;
	void set(Actor actor, ActorHullCache.Entry cached, WorldPoint worldPoint, int anim, int frame, int pose, int poseFrame, boolean hit, int score, int bucket, int canvasX, int canvasY, int localX, int localY) {
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
		this.canvasX = canvasX;
		this.canvasY = canvasY;
		this.localX = localX;
		this.localY = localY;
		this.selected = false;
	}
	void clear() {
		actor = null;
		cached = null;
		worldPoint = null;
		anim = frame = pose = poseFrame = score = bucket = canvasX = canvasY = localX = localY = 0;
		hit = selected = false;
	}
}
