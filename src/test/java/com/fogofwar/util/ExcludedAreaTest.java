package com.fogofwar.util;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
public class ExcludedAreaTest {
	@Test
	public void containsPointInsideConfiguredPlane() {
		ExcludedArea area = new ExcludedArea(10, 20, 30, 40, 1, 2);
		assertTrue(area.contains(new WorldPoint(20, 30, 1)));
	}
	@Test
	public void rejectsPointOutsideBounds() {
		ExcludedArea area = new ExcludedArea(10, 20, 30, 40, 1, 2);
		assertFalse(area.contains(new WorldPoint(31, 30, 1)));
	}
	@Test
	public void rejectsPointOnUnconfiguredPlane() {
		ExcludedArea area = new ExcludedArea(10, 20, 30, 40, 1, 2);
		assertFalse(area.contains(new WorldPoint(20, 30, 0)));
	}
}