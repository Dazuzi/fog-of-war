package com.fogofwar;
import com.fogofwar.config.FadingPlayerMode;
import com.fogofwar.config.FogDisplayMode;
import org.junit.Test;
import java.awt.Color;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
public class FogOfWarPluginTest {
	private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
	private static final Color VISIBLE = new Color(0, 0, 0, 1);
	@Test
	public void transparentFogAndBordersRemainActiveOnlyForVisibleSailingDetail() {
		assertFalse(FogOfWarPlugin.hasVisibleFogOrBorder(FogDisplayMode.FOG, TRANSPARENT, VISIBLE, false));
		assertFalse(FogOfWarPlugin.hasVisibleFogOrBorder(FogDisplayMode.BORDER, VISIBLE, TRANSPARENT, false));
		assertTrue(FogOfWarPlugin.hasVisibleFogOrBorder(FogDisplayMode.BOTH, VISIBLE, TRANSPARENT, false));
		assertTrue(FogOfWarPlugin.hasVisibleFogOrBorder(FogDisplayMode.FOG, TRANSPARENT, TRANSPARENT, true));
		assertTrue(FogOfWarPlugin.hasVisibleFogOrBorder(FogDisplayMode.BORDER, TRANSPARENT, TRANSPARENT, true));
	}
	@Test
	public void transparentFadeWorkRequiresVisibleWorldNames() {
		assertFalse(FogOfWarPlugin.hasVisibleFadingWorld(FadingPlayerMode.WORLD, TRANSPARENT, false));
		assertTrue(FogOfWarPlugin.hasVisibleFadingWorld(FadingPlayerMode.WORLD, TRANSPARENT, true));
		assertTrue(FogOfWarPlugin.hasVisibleFadingWorld(FadingPlayerMode.WORLD, VISIBLE, false));
		assertFalse(FogOfWarPlugin.hasVisibleFadingMinimap(FadingPlayerMode.MINIMAP, TRANSPARENT));
		assertTrue(FogOfWarPlugin.hasVisibleFadingMinimap(FadingPlayerMode.MINIMAP, VISIBLE));
	}
}
