package com.fogofwar.render;
import org.junit.Test;
import java.awt.Color;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
public class FogRenderTest {
	@Test
	public void sailingSeaColourRetainsVisibleAlphaFloorAndCachesResult() {
		FogRender.SailingSeaCache cache = new FogRender.SailingSeaCache();
		Color transparent = new Color(10, 20, 30, 0);
		Color sailing = cache.get(transparent);
		assertEquals(16, sailing.getAlpha());
		assertSame(sailing, cache.get(transparent));
		assertEquals(50, cache.get(new Color(10, 20, 30, 200)).getAlpha());
	}
}
