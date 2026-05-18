package com.fogofwar.render.world;
import net.runelite.api.Actor;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
final class ActorCutoutCandidate {
	Actor actor;
	ActorHullCache.Entry cached;
	WorldPoint worldPoint;
	int anim, frame, pose, poseFrame, score, bucket, canvasX, canvasY, wx, wy;
	boolean hit, selected;
	void set(Actor actor, ActorHullCache.Entry cached, WorldPoint worldPoint, int anim, int frame, int pose, int poseFrame, boolean hit, int score, int bucket, Point canvasPoint) {
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
