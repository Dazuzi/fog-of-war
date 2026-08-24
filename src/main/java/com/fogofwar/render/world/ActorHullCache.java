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
	private static final Set<Actor> EMPTY = Collections.emptySet();
	private static final Map<Actor, Entry> EMPTY_ENTRIES = Collections.emptyMap();
	private Map<Actor, Entry> entries = EMPTY_ENTRIES;
	private Set<Actor> seen = EMPTY;
	private int expectedActors;
	void beginFrame(int expectedActors) {
		this.expectedActors = expectedActors;
		if (seen == EMPTY) seen = Collections.newSetFromMap(new IdentityHashMap<>(expectedActors));
		else seen.clear();
	}
	void markSeen(Actor actor) { seen.add(actor); }
	Entry get(Actor actor) { return entries.get(actor); }
	Entry getOrCreate(Actor actor) {
		if (entries == EMPTY_ENTRIES) entries = new IdentityHashMap<>(expectedActors);
		return entries.computeIfAbsent(actor, a -> new Entry());
	}
	void remove(Actor actor) { if (entries != EMPTY_ENTRIES) entries.remove(actor); }
	void retainSeen() { if (entries != EMPTY_ENTRIES) entries.keySet().retainAll(seen); }
	static final class Entry {
		Shape hull;
		Area area;
		Rectangle bounds;
		int wx, wy, plane, anim, frame, pose, poseFrame, orientation;
		int localX, localY, canvasX, canvasY;
		int camX, camY, camZ, camPitch, camYaw, scale, vpX, vpY, vpW, vpH;
		float camFpX, camFpY, camFpZ, camFpPitch, camFpYaw;
		boolean gpu;
	}
}
