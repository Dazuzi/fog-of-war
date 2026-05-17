package com.fogofwar;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
final class FogOfWarConfigMigration {
	static final int SETTINGS_VERSION = 1;
	static final String CONFIG_GROUP = "fogofwar";
	static final String OLD_CONFIG_GROUP = "entityrenderdistance";
	private static final String PLUGIN_KEY = "fogofwarplugin";
	private static final String OLD_PLUGIN_KEY = "entityrenderdistanceplugin";
	private static final String SETTINGS_VERSION_KEY = "settingsVersion";
	private static final String[][] DIRECT_KEYS = {
			{"onlyInWilderness", "onlyInWilderness"},
			{"worldFogColour", "worldFogColour"},
			{"worldBorderColour", "worldBorderColour"},
			{"worldBorderThickness", "worldBorderThickness"},
			{"minimapFogColour", "minimapFogColour"},
			{"minimapBorderColour", "minimapBorderColour"},
			{"minimapBorderThickness", "minimapBorderThickness"},
			{"onlyFadeAtRenderLimit", "onlyFadeAtRenderLimit"},
			{"extrapolateMovement", "extrapolateMovement"},
			{"showFadeNames", "showFadeNames"},
			{"fadeDuration", "fadeDuration"},
			{"fadeColor", "fadeColor"},
			{"renderDistanceRadius", "renderDistanceRadius"},
			{"boatRenderDistanceRadius", "boatRenderDistanceRadius"},
			{"enableDynamicRenderDistance", "enableDynamicRenderDistance"},
			{"dynamicRenderDistancePlayerThreshold", "dynamicRenderDistancePlayerThreshold"},
			{"showPlaneDisplay", "showDebugOverlay"}
	};
	private static final String[] OLD_KEYS = {
			"onlyInWilderness",
			"showWorldFog",
			"worldFogColour",
			"excludeEntities",
			"showWorldBorder",
			"worldBorderColour",
			"worldBorderThickness",
			"showMinimapFog",
			"minimapFogColour",
			"showMinimapBorder",
			"minimapBorderColour",
			"minimapBorderThickness",
			"enableFadingPlayers",
			"onlyFadeAtRenderLimit",
			"extrapolateMovement",
			"showFadeNames",
			"showFadingInWorld",
			"showFadingOnMinimap",
			"fadeDuration",
			"fadeColor",
			"renderDistanceRadius",
			"boatRenderDistanceRadius",
			"enableDynamicRenderDistance",
			"dynamicRenderDistancePlayerThreshold",
			"showPlaneDisplay"
	};
	private FogOfWarConfigMigration() {}
	static void migrate(ConfigManager configManager) {
		migratePluginEnabled(configManager);
		Integer settingsVersion = configManager.getConfiguration(CONFIG_GROUP, SETTINGS_VERSION_KEY, int.class);
		if (settingsVersion != null && settingsVersion >= SETTINGS_VERSION) {
			migrateEntityExclusionLimit(configManager);
			clearOldKeys(configManager);
			return;
		}
		for (String[] pair : DIRECT_KEYS) copy(configManager, pair[0], pair[1]);
		migrateEntityExclusionLimit(configManager);
		migrateFogDisplayMode(configManager, "showWorldFog", "showWorldBorder", "worldMode");
		migrateFogDisplayMode(configManager, "showMinimapFog", "showMinimapBorder", "minimapMode");
		migrateFadingPlayerMode(configManager);
		clearOldKeys(configManager);
		configManager.setConfiguration(CONFIG_GROUP, SETTINGS_VERSION_KEY, SETTINGS_VERSION);
	}
	private static void clearOldKeys(ConfigManager configManager) {
		for (String key : OLD_KEYS) configManager.unsetConfiguration(OLD_CONFIG_GROUP, key);
		configManager.unsetConfiguration(CONFIG_GROUP, "excludeEntities");
	}
	private static void migratePluginEnabled(ConfigManager configManager) {
		String oldValue = configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, OLD_PLUGIN_KEY);
		if (oldValue == null) return;
		if (configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, PLUGIN_KEY) == null) configManager.setConfiguration(RuneLiteConfig.GROUP_NAME, PLUGIN_KEY, oldValue);
		configManager.unsetConfiguration(RuneLiteConfig.GROUP_NAME, OLD_PLUGIN_KEY);
	}
	static FogDisplayMode toFogDisplayMode(boolean fog, boolean border) {
		if (fog && border) return FogDisplayMode.BOTH;
		if (fog) return FogDisplayMode.FOG;
		if (border) return FogDisplayMode.BORDER;
		return FogDisplayMode.OFF;
	}
	static FadingPlayerMode toFadingPlayerMode(boolean enabled, boolean world, boolean minimap) {
		if (!enabled) return FadingPlayerMode.OFF;
		if (world && minimap) return FadingPlayerMode.BOTH;
		if (world) return FadingPlayerMode.WORLD;
		if (minimap) return FadingPlayerMode.MINIMAP;
		return FadingPlayerMode.OFF;
	}
	static EntityExclusionLimit toEntityExclusionLimit(boolean enabled) { return enabled ? EntityExclusionLimit.ALL : EntityExclusionLimit.NONE; }
	private static void copy(ConfigManager configManager, String oldKey, String newKey) {
		String value = configManager.getConfiguration(OLD_CONFIG_GROUP, oldKey);
		if (value != null) configManager.setConfiguration(CONFIG_GROUP, newKey, value);
	}
	private static void migrateEntityExclusionLimit(ConfigManager configManager) {
		if (configManager.getConfiguration(CONFIG_GROUP, "entityExclusionLimit") != null) return;
		Boolean value = configManager.getConfiguration(CONFIG_GROUP, "excludeEntities", boolean.class);
		if (value == null) value = configManager.getConfiguration(OLD_CONFIG_GROUP, "excludeEntities", boolean.class);
		if (value != null) configManager.setConfiguration(CONFIG_GROUP, "entityExclusionLimit", toEntityExclusionLimit(value));
	}
	private static void migrateFogDisplayMode(ConfigManager configManager, String fogKey, String borderKey, String newKey) {
		boolean fog = getBoolean(configManager, fogKey, true);
		boolean border = getBoolean(configManager, borderKey, false);
		configManager.setConfiguration(CONFIG_GROUP, newKey, toFogDisplayMode(fog, border));
	}
	private static void migrateFadingPlayerMode(ConfigManager configManager) {
		boolean enabled = getBoolean(configManager, "enableFadingPlayers", false);
		boolean world = getBoolean(configManager, "showFadingInWorld", true);
		boolean minimap = getBoolean(configManager, "showFadingOnMinimap", true);
		configManager.setConfiguration(CONFIG_GROUP, "fadingPlayerMode", toFadingPlayerMode(enabled, world, minimap));
	}
	private static boolean getBoolean(ConfigManager configManager, String key, boolean defaultValue) {
		Boolean value = configManager.getConfiguration(OLD_CONFIG_GROUP, key, boolean.class);
		return value != null ? value : defaultValue;
	}
}
