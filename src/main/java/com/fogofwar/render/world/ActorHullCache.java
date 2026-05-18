package com.fogofwar.render.world;
import net.runelite.api.Actor;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
final class ActorHullCache {
	private final Map<Actor, Entry> entries = new IdentityHashMap<>();
	private final Set<Actor> seen = Collections.newSetFromMap(new IdentityHashMap<>(256));
	void clear() {
		entries.clear();
		seen.clear();
	}
	void beginFrame() { seen.clear(); }
	void markSeen(Actor actor) { seen.add(actor); }
	Entry get(Actor actor) { return entries.get(actor); }
	Entry getOrCreate(Actor actor) { return entries.computeIfAbsent(actor, a -> new Entry()); }
	void remove(Actor actor) { entries.remove(actor); }
	void retainSeen() { entries.keySet().retainAll(seen); }
	static final class Entry {
		Shape hull;
		Area area;
		Rectangle bounds;
		int wx, wy, plane, anim, frame, pose, poseFrame;
		int camX, camY, camZ, camPitch, camYaw;
	}
}
