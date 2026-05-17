package com.fogofwar;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
public class FogOfWarConfigMigrationTest {
	@Test
	public void mapsFogDisplayModes() {
		assertEquals(FogDisplayMode.OFF, FogOfWarConfigMigration.toFogDisplayMode(false, false));
		assertEquals(FogDisplayMode.FOG, FogOfWarConfigMigration.toFogDisplayMode(true, false));
		assertEquals(FogDisplayMode.BORDER, FogOfWarConfigMigration.toFogDisplayMode(false, true));
		assertEquals(FogDisplayMode.BOTH, FogOfWarConfigMigration.toFogDisplayMode(true, true));
	}
	@Test
	public void mapsDisabledFadingPlayersToOff() {
		assertEquals(FadingPlayerMode.OFF, FogOfWarConfigMigration.toFadingPlayerMode(false, false, false));
		assertEquals(FadingPlayerMode.OFF, FogOfWarConfigMigration.toFadingPlayerMode(false, true, false));
		assertEquals(FadingPlayerMode.OFF, FogOfWarConfigMigration.toFadingPlayerMode(false, false, true));
		assertEquals(FadingPlayerMode.OFF, FogOfWarConfigMigration.toFadingPlayerMode(false, true, true));
	}
	@Test
	public void mapsEnabledFadingPlayerModes() {
		assertEquals(FadingPlayerMode.OFF, FogOfWarConfigMigration.toFadingPlayerMode(true, false, false));
		assertEquals(FadingPlayerMode.WORLD, FogOfWarConfigMigration.toFadingPlayerMode(true, true, false));
		assertEquals(FadingPlayerMode.MINIMAP, FogOfWarConfigMigration.toFadingPlayerMode(true, false, true));
		assertEquals(FadingPlayerMode.BOTH, FogOfWarConfigMigration.toFadingPlayerMode(true, true, true));
	}
}
