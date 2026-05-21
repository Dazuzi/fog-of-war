package com.fogofwar.state;
import net.runelite.api.coords.WorldPoint;
public final class ExcludedArea {
	private final int minX;
	private final int minY;
	private final int maxX;
	private final int maxY;
	private final int planeMask;
	public ExcludedArea(int minX, int minY, int maxX, int maxY, int... planes) {
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
		this.planeMask = planeMask(planes);
	}
	public boolean contains(WorldPoint point) {
		int plane = point.getPlane();
		return plane >= 0 && plane < Integer.SIZE && (planeMask & (1 << plane)) != 0 &&
				point.getX() >= minX && point.getX() <= maxX &&
				point.getY() >= minY && point.getY() <= maxY;
	}
	private static int planeMask(int... planes) {
		int mask = 0;
		for (int plane : planes) { if (plane >= 0 && plane < Integer.SIZE) mask |= 1 << plane; }
		return mask;
	}
}
