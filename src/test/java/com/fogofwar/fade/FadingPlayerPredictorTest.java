package com.fogofwar.fade;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
public class FadingPlayerPredictorTest {
	@Test
	public void velocityUsesLastTwoLocations() {
		assertEquals(new WorldPoint(2, -1, 0), FadingPlayerPredictor.getVelocity(new WorldPoint(12, 19, 0), new WorldPoint(10, 20, 0)));
		WorldPoint zero = FadingPlayerPredictor.getVelocity(new WorldPoint(12, 19, 0), null);
		assertEquals(new WorldPoint(0, 0, 0), zero);
		assertSame(zero, FadingPlayerPredictor.getVelocity(new WorldPoint(20, 30, 0), null));
	}
	@Test
	public void nearRenderLimitHonorsEdgeAndRunning() {
		WorldPoint local = new WorldPoint(100, 100, 0);
		assertTrue(FadingPlayerPredictor.isNearRenderLimit(new WorldPoint(114, 100, 0), local, new WorldPoint(0, 0, 0), 15));
		assertFalse(FadingPlayerPredictor.isNearRenderLimit(new WorldPoint(112, 100, 0), local, new WorldPoint(1, 0, 0), 15));
		assertTrue(FadingPlayerPredictor.isNearRenderLimit(new WorldPoint(113, 100, 0), local, new WorldPoint(2, 0, 0), 15));
	}
	@Test
	public void initialLocationMatchesExtrapolationRules() {
		WorldPoint local = new WorldPoint(100, 100, 0);
		assertEquals(new WorldPoint(116, 100, 0), FadingPlayerPredictor.getInitialFadeLocation(new WorldPoint(114, 100, 0), local, new WorldPoint(1, 0, 0), true, 15, true));
		assertEquals(new WorldPoint(114, 100, 0), FadingPlayerPredictor.getInitialFadeLocation(new WorldPoint(114, 100, 0), local, new WorldPoint(-1, 0, 0), true, 15, true));
		assertEquals(new WorldPoint(114, 100, 0), FadingPlayerPredictor.getInitialFadeLocation(new WorldPoint(114, 100, 0), local, new WorldPoint(1, 0, 0), false, 15, true));
	}
}
