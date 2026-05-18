package com.fogofwar.config;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
public class FogOfWarConfigMigrationTest {
	@Test
	public void toFogDisplayModeKeepsOldToggleSemantics() {
		assertEquals(FogDisplayMode.BOTH, FogOfWarConfigMigration.toFogDisplayMode(true, true));
		assertEquals(FogDisplayMode.FOG, FogOfWarConfigMigration.toFogDisplayMode(true, false));
		assertEquals(FogDisplayMode.BORDER, FogOfWarConfigMigration.toFogDisplayMode(false, true));
		assertEquals(FogDisplayMode.OFF, FogOfWarConfigMigration.toFogDisplayMode(false, false));
	}
	@Test
	public void toFadingPlayerModeKeepsOldToggleSemantics() {
		assertEquals(FadingPlayerMode.OFF, FogOfWarConfigMigration.toFadingPlayerMode(false, true, true));
		assertEquals(FadingPlayerMode.BOTH, FogOfWarConfigMigration.toFadingPlayerMode(true, true, true));
		assertEquals(FadingPlayerMode.WORLD, FogOfWarConfigMigration.toFadingPlayerMode(true, true, false));
		assertEquals(FadingPlayerMode.MINIMAP, FogOfWarConfigMigration.toFadingPlayerMode(true, false, true));
		assertEquals(FadingPlayerMode.OFF, FogOfWarConfigMigration.toFadingPlayerMode(true, false, false));
	}
	@Test
	public void toEntityExclusionLimitKeepsOldToggleSemantics() {
		assertEquals(EntityExclusionLimit.LIMIT_64, FogOfWarConfigMigration.toEntityExclusionLimit(true));
		assertEquals(EntityExclusionLimit.NONE, FogOfWarConfigMigration.toEntityExclusionLimit(false));
	}
}
