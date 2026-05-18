package com.fogofwar.state;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
public class ExcludedAreaTest {
	@Test
	public void containsInclusiveBoundsOnAllowedPlane() {
		ExcludedArea area = new ExcludedArea(10, 20, 30, 40, 0, 1);
		assertTrue(area.contains(new WorldPoint(10, 20, 0)));
		assertTrue(area.contains(new WorldPoint(30, 40, 1)));
	}
	@Test
	public void rejectsOutsideBoundsOrPlane() {
		ExcludedArea area = new ExcludedArea(10, 20, 30, 40, 0);
		assertFalse(area.contains(new WorldPoint(9, 20, 0)));
		assertFalse(area.contains(new WorldPoint(10, 41, 0)));
		assertFalse(area.contains(new WorldPoint(10, 20, 1)));
	}
}
