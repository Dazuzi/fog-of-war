package com.fogofwar.config;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneLiteConfig;
import java.util.List;
public final class FogOfWarConfigMigration {
	static final int SETTINGS_VERSION = 1;
	public static final String CONFIG_GROUP = "fogofwar";
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
			{"onlyFadeAtRenderLimit", "onlyFadeAtRenderEdge"},
			{"extrapolateMovement", "predictMovement"},
			{"showFadeNames", "showFadeMarkerNames"},
			{"fadeDuration", "fadeDurationTicks"},
			{"fadeColor", "fadeMarkerColour"},
			{"renderDistanceRadius", "landRenderDistance"},
			{"showPlaneDisplay", "debugOverlayEnabled"}
	};
	private static final String[] REMOVED_KEYS = {
			"dynamicRenderDistanceEnabled",
			"dynamicRenderDistanceThreshold"
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
			"enableDynamicRenderDistance",
			"dynamicRenderDistancePlayerThreshold",
			"showPlaneDisplay"
	};
	private FogOfWarConfigMigration() {}
	public static void migrate(ConfigManager configManager) {
		boolean oldConfig = hasOldConfig(configManager) || hasOldPluginConfig(configManager);
		migratePluginEnabled(configManager);
		Integer settingsVersion = configManager.getConfiguration(CONFIG_GROUP, SETTINGS_VERSION_KEY, int.class);
		if (settingsVersion != null && settingsVersion >= SETTINGS_VERSION) {
			clearRemovedKeys(configManager);
			clearOldKeys(configManager);
			return;
		}
		if (oldConfig) {
			for (String[] pair : DIRECT_KEYS) copy(configManager, pair[0], pair[1]);
			migrateActorCutoutLimit(configManager);
			migrateFogDisplayMode(configManager, "showWorldFog", "showWorldBorder", "worldDisplayMode");
			migrateFogDisplayMode(configManager, "showMinimapFog", "showMinimapBorder", "minimapDisplayMode");
			migrateFadingPlayerMode(configManager);
		}
		clearRemovedKeys(configManager);
		clearOldKeys(configManager);
		configManager.setConfiguration(CONFIG_GROUP, SETTINGS_VERSION_KEY, SETTINGS_VERSION);
	}
	private static void clearRemovedKeys(ConfigManager configManager) {
		for (String key : REMOVED_KEYS) configManager.unsetConfiguration(CONFIG_GROUP, key);
	}
	private static void clearOldKeys(ConfigManager configManager) {
		for (String key : OLD_KEYS) configManager.unsetConfiguration(OLD_CONFIG_GROUP, key);
	}
	private static void migratePluginEnabled(ConfigManager configManager) {
		String oldValue = configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, OLD_PLUGIN_KEY);
		if (oldValue == null) return;
		if (configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, PLUGIN_KEY) == null) configManager.setConfiguration(RuneLiteConfig.GROUP_NAME, PLUGIN_KEY, oldValue);
		configManager.unsetConfiguration(RuneLiteConfig.GROUP_NAME, OLD_PLUGIN_KEY);
	}
	private static boolean hasOldConfig(ConfigManager configManager) {
		List<String> keys = configManager.getConfigurationKeys(OLD_CONFIG_GROUP);
		return keys != null && !keys.isEmpty();
	}
	private static boolean hasOldPluginConfig(ConfigManager configManager) { return configManager.getConfiguration(RuneLiteConfig.GROUP_NAME, OLD_PLUGIN_KEY) != null; }
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
	static ActorCutoutLimit toActorCutoutLimit(boolean enabled) { return enabled ? ActorCutoutLimit.LIMIT_64 : ActorCutoutLimit.NONE; }
	private static void copy(ConfigManager configManager, String oldKey, String newKey) {
		String value = configManager.getConfiguration(OLD_CONFIG_GROUP, oldKey);
		if (value != null && configManager.getConfiguration(CONFIG_GROUP, newKey) == null) configManager.setConfiguration(CONFIG_GROUP, newKey, value);
	}
	private static void migrateActorCutoutLimit(ConfigManager configManager) {
		if (configManager.getConfiguration(CONFIG_GROUP, "actorCutoutLimit") != null) return;
		Boolean value = configManager.getConfiguration(OLD_CONFIG_GROUP, "excludeEntities", boolean.class);
		if (value != null) configManager.setConfiguration(CONFIG_GROUP, "actorCutoutLimit", toActorCutoutLimit(value));
	}
	private static void migrateFogDisplayMode(ConfigManager configManager, String fogKey, String borderKey, String newKey) {
		if (configManager.getConfiguration(OLD_CONFIG_GROUP, fogKey) == null && configManager.getConfiguration(OLD_CONFIG_GROUP, borderKey) == null) return;
		boolean fog = getBoolean(configManager, fogKey, true);
		boolean border = getBoolean(configManager, borderKey, false);
		if (configManager.getConfiguration(CONFIG_GROUP, newKey) == null) configManager.setConfiguration(CONFIG_GROUP, newKey, toFogDisplayMode(fog, border));
	}
	private static void migrateFadingPlayerMode(ConfigManager configManager) {
		if (configManager.getConfiguration(CONFIG_GROUP, "playerFadeMarkerMode") != null) return;
		if (configManager.getConfiguration(OLD_CONFIG_GROUP, "enableFadingPlayers") == null
				&& configManager.getConfiguration(OLD_CONFIG_GROUP, "showFadingInWorld") == null
				&& configManager.getConfiguration(OLD_CONFIG_GROUP, "showFadingOnMinimap") == null) return;
		boolean enabled = getBoolean(configManager, "enableFadingPlayers", false);
		boolean world = getBoolean(configManager, "showFadingInWorld", true);
		boolean minimap = getBoolean(configManager, "showFadingOnMinimap", true);
		configManager.setConfiguration(CONFIG_GROUP, "playerFadeMarkerMode", toFadingPlayerMode(enabled, world, minimap));
	}
	private static boolean getBoolean(ConfigManager configManager, String key, boolean defaultValue) {
		Boolean value = configManager.getConfiguration(OLD_CONFIG_GROUP, key, boolean.class);
		return value != null ? value : defaultValue;
	}
}
