package com.fogofwar;
import net.runelite.client.config.*;
import java.awt.*;
@ConfigGroup("fogofwar")
public interface FogOfWarConfig extends Config {
	@ConfigItem(
			keyName = "settingsVersion",
			name = "Settings version",
			description = "",
			hidden = true
	)
	@SuppressWarnings("unused")
	default int settingsVersion() { return 0; }
	@ConfigItem(
			keyName = "onlyInWilderness",
			name = "Only show in Wilderness",
			description = "Only display overlays while in the Wilderness.",
			position = 0
	)
	default boolean onlyInWilderness() { return false; }
	@ConfigSection(
			name = "World",
			description = "Settings for the world overlay",
			position = 2
	)
	String worldSection = "worldSection";
	@ConfigItem(
			keyName = "worldMode",
			name = "World overlay",
			description = "Controls the render distance overlay in the world view.",
			section = worldSection,
			position = 0
	)
	default FogDisplayMode worldMode() { return FogDisplayMode.FOG; }
	@Alpha
	@ConfigItem(
			keyName = "worldFogColour",
			name = "Fog colour",
			description = "The colour of the world's fog of war effect.",
			section = worldSection,
			position = 1
	)
	default Color worldFogColour() { return new Color(0, 0, 0, 125); }
	@Alpha
	@ConfigItem(
			keyName = "worldBorderColour",
			name = "Border colour",
			description = "The colour of the world render distance border.",
			section = worldSection,
			position = 2
	)
	default Color worldBorderColour() { return new Color(0, 0, 0, 125); }
	@Range(min = 1, max = 16)
	@ConfigItem(
			keyName = "worldBorderThickness",
			name = "Border thickness",
			description = "The thickness of the world render distance border.",
			section = worldSection,
			position = 3
	)
	default int worldBorderThickness() { return 1; }
	@ConfigItem(
			keyName = "excludeEntities",
			name = "Exclude entities from fog",
			description = "Prevents the fog from drawing on top of players and NPCs. Major negative performance impact in crowded areas.",
			section = worldSection,
			position = 4
	)
	default boolean excludeEntities() { return false; }
	@ConfigSection(
			name = "Minimap",
			description = "Settings for the minimap overlay",
			position = 3
	)
	String minimapSection = "minimapSection";
	@ConfigItem(
			keyName = "minimapMode",
			name = "Minimap overlay",
			description = "Controls the render distance overlay on the minimap.",
			section = minimapSection,
			position = 0
	)
	default FogDisplayMode minimapMode() { return FogDisplayMode.FOG; }
	@Alpha
	@ConfigItem(
			keyName = "minimapFogColour",
			name = "Fog colour",
			description = "The colour of the minimap's fog of war effect.",
			section = minimapSection,
			position = 1
	)
	default Color minimapFogColour() { return new Color(0, 0, 0, 125); }
	@Alpha
	@ConfigItem(
			keyName = "minimapBorderColour",
			name = "Border colour",
			description = "The colour of the minimap render distance border.",
			section = minimapSection,
			position = 2
	)
	default Color minimapBorderColour() { return new Color(0, 0, 0, 125); }
	@Range(min = 1, max = 16)
	@ConfigItem(
			keyName = "minimapBorderThickness",
			name = "Border thickness",
			description = "The thickness of the minimap render distance border.",
			section = minimapSection,
			position = 3
	)
	default int minimapBorderThickness() { return 1; }
	@ConfigSection(
			name = "Fading Players",
			description = "Settings for marking players that leave render distance",
			position = 4
	)
	String fadingPlayerSection = "fadingPlayerSection";
	@ConfigItem(
			keyName = "fadingPlayerMode",
			name = "Fading player tiles",
			description = "Controls fading markers for players that leave render distance.",
			section = fadingPlayerSection,
			position = 0
	)
	default FadingPlayerMode fadingPlayerMode() { return FadingPlayerMode.OFF; }
	@Alpha
	@ConfigItem(
			keyName = "fadeColor",
			name = "Fade marker colour",
			description = "The colour of the fading tile marker.",
			section = fadingPlayerSection,
			position = 1
	)
	default Color fadeColor() { return new Color(255, 0, 0, 150); }
	@Range(min = 1, max = 16)
	@ConfigItem(
			keyName = "fadeDuration",
			name = "Fade duration (ticks)",
			description = "How many game ticks it takes for a marker to completely fade.",
			section = fadingPlayerSection,
			position = 2
	)
	default int fadeDuration() { return 2; }
	@ConfigItem(
			keyName = "onlyFadeAtRenderLimit",
			name = "Only fade at the render limit",
			description = "Only create a fading marker if the player disappears at the edge of the render distance.",
			section = fadingPlayerSection,
			position = 3
	)
	default boolean onlyFadeAtRenderLimit() { return true; }
	@ConfigItem(
			keyName = "extrapolateMovement",
			name = "Extrapolate movement",
			description = "Fading player markers will continue to move based on their last known velocity.",
			section = fadingPlayerSection,
			position = 4
	)
	default boolean extrapolateMovement() { return true; }
	@ConfigItem(
			keyName = "showFadeNames",
			name = "Label tiles after players",
			description = "Show the player's name on the fading marker in the world view.",
			section = fadingPlayerSection,
			position = 5
	)
	default boolean showFadeNames() { return true; }
	@ConfigSection(
			name = "Tweaks",
			description = "Advanced settings for experimental features.",
			position = 5,
			closedByDefault = true
	)
	String tweaksSection = "tweaksSection";
	@Range(min = 1, max = 64)
	@ConfigItem(
			keyName = "renderDistanceRadius",
			name = "Maximum render distance",
			description = "Sets the maximum possible render distance the plugin will use (normally 15).<br>Note: this does NOT change the game's render distance. Adjust this only if Jagex changes the render distance in a future game update.",
			section = tweaksSection,
			position = 0
	)
	default int renderDistanceRadius() { return 15; }
	@Range(min = 1, max = 64)
	@ConfigItem(
			keyName = "boatRenderDistanceRadius",
			name = "Boat render distance",
			description = "Render distance used while sailing, centred on the boat (normally 30).<br>The game uses a separate render distance for sailing entities; this overlay uses this value whenever the player is on a boat/WorldEntity.",
			section = tweaksSection,
			position = 1
	)
	default int boatRenderDistanceRadius() { return 30; }
	@ConfigItem(
			keyName = "enableDynamicRenderDistance",
			name = "Enable dynamic render distance",
			description = "(Experimental) Automatically scales the render distance based on player count in high-population areas.",
			section = tweaksSection,
			position = 2
	)
	default boolean enableDynamicRenderDistance() { return false; }
	@Range(min = 1, max = 500)
	@ConfigItem(
			keyName = "dynamicRenderDistancePlayerThreshold",
			name = "Player count trigger",
			description = "(Experimental) The number of players needed to trigger the dynamic render distance check.",
			section = tweaksSection,
			position = 3
	)
	default int dynamicRenderDistancePlayerThreshold() { return 150; }
	@ConfigItem(
			keyName = "showDebugOverlay",
			name = "Show debug overlay",
			description = "Shows plane, player count, and render distance.",
			section = tweaksSection,
			position = 4
	)
	default boolean showDebugOverlay() { return false; }
}
