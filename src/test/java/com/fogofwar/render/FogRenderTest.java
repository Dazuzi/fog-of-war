package com.fogofwar.render;
import org.junit.Test;
import java.awt.Color;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
public class FogRenderTest {
	@Test
	public void sailingSeaColourRetainsVisibleAlphaFloorAndCachesResult() {
		Color transparent = new Color(10, 20, 30, 0);
		Color sailing = FogRender.sailingSea(transparent);
		assertEquals(16, sailing.getAlpha());
		assertSame(sailing, FogRender.sailingSea(transparent));
		assertEquals(50, FogRender.sailingSea(new Color(10, 20, 30, 200)).getAlpha());
	}
}
