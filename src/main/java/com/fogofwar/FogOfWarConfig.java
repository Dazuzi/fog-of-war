package com.fogofwar;

import net.runelite.client.config.*;

import java.awt.*;
@ConfigGroup("entityrenderdistance")
public interface FogOfWarConfig extends Config {
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
			keyName = "showWorldFog",
			name = "Fog of war",
			description = "Shades the area outside of the render distance with a fog effect.",
			section = worldSection,
			position = 0
	)
	default boolean showWorldFog() { return true; }
	@Alpha
	@ConfigItem(
			keyName = "worldFogColour",
			name = "Fog colour",
			description = "The colour of the world's fog of war effect.",
			section = worldSection,
			position = 1
	)
	default Color worldFogColour() { return new Color(0, 0, 0, 75); }
	@ConfigItem(
			keyName = "showWorldBorder",
			name = "Show border",
			description = "Shows the render distance border in the world view.",
			section = worldSection,
			position = 2
	)
	default boolean showWorldBorder() { return false; }
	@Alpha
	@ConfigItem(
			keyName = "worldBorderColour",
			name = "Border colour",
			description = "The colour of the world render distance border.",
			section = worldSection,
			position = 3
	)
	default Color worldBorderColour() { return new Color(0, 0, 0, 100); }
	@Range(min = 1, max = 16)
	@ConfigItem(
			keyName = "worldBorderThickness",
			name = "Border thickness",
			description = "The thickness of the world render distance border.",
			section = worldSection,
			position = 4
	)
	default int worldBorderThickness() { return 1; }
	@ConfigSection(
			name = "Minimap",
			description = "Settings for the minimap overlay",
			position = 3
	)
	String minimapSection = "minimapSection";
	@ConfigItem(
			keyName = "showMinimapFog",
			name = "Fog of war",
			description = "Shades the area outside the render distance on the minimap with a fog effect.",
			section = minimapSection,
			position = 0
	)
	default boolean showMinimapFog() { return true; }
	@Alpha
	@ConfigItem(
			keyName = "minimapFogColour",
			name = "Fog colour",
			description = "The colour of the minimap's fog of war effect.",
			section = minimapSection,
			position = 1
	)
	default Color minimapFogColour() { return new Color(0, 0, 0, 75); }
	@ConfigItem(
			keyName = "showMinimapBorder",
			name = "Show border",
			description = "Shows the render distance border on the minimap.",
			section = minimapSection,
			position = 2
	)
	default boolean showMinimapBorder() { return false; }
	@Alpha
	@ConfigItem(
			keyName = "minimapBorderColour",
			name = "Border colour",
			description = "The colour of the minimap render distance border.",
			section = minimapSection,
			position = 3
	)
	default Color minimapBorderColour() { return new Color(0, 0, 0, 100); }
	@Range(min = 1, max = 16)
	@ConfigItem(
			keyName = "minimapBorderThickness",
			name = "Border thickness",
			description = "The thickness of the minimap render distance border.",
			section = minimapSection,
			position = 4
	)
	default int minimapBorderThickness() { return 1; }
	@ConfigSection(
			name = "Fading Players",
			description = "Settings for marking players that leave render distance",
			position = 4
	)
	String fadingPlayerSection = "fadingPlayerSection";
	@ConfigItem(
			keyName = "enableFadingPlayers",
			name = "Enable fading player tiles",
			description = "Mark players that leave render distance with a fading marker.",
			section = fadingPlayerSection,
			position = 0
	)
	default boolean enableFadingPlayers() { return false; }
	@ConfigItem(
			keyName = "onlyFadeAtRenderLimit",
			name = "Only fade at the limit",
			description = "Only create a fading marker if the player disappears at the edge of the render distance.",
			section = fadingPlayerSection,
			position = 1
	)
	default boolean onlyFadeAtRenderLimit() { return true; }
	@ConfigItem(
			keyName = "extrapolateMovement",
			name = "Extrapolate movement",
			description = "Fading player markers will continue to move based on their last known velocity.",
			section = fadingPlayerSection,
			position = 2
	)
	default boolean extrapolateMovement() { return true; }
	@ConfigItem(
			keyName = "showFadeNames",
			name = "Label tiles after players",
			description = "Show the player's name on the fading marker in the world view.",
			section = fadingPlayerSection,
			position = 3
	)
	default boolean showFadeNames() { return true; }
	@ConfigItem(
			keyName = "showFadingInWorld",
			name = "Show in world",
			description = "Show fading player markers in the main game view.",
			section = fadingPlayerSection,
			position = 4
	)
	default boolean showFadingInWorld() { return true; }
	@ConfigItem(
			keyName = "showFadingOnMinimap",
			name = "Show on minimap",
			description = "Show fading player markers on the minimap.",
			section = fadingPlayerSection,
			position = 5
	)
	default boolean showFadingOnMinimap() { return true; }
	@Range(min = 1, max = 16)
	@ConfigItem(
			keyName = "fadeDuration",
			name = "Fade duration (ticks)",
			description = "How many game ticks it takes for a marker to completely fade.",
			section = fadingPlayerSection,
			position = 6
	)
	default int fadeDuration() { return 2; }
	@Alpha
	@ConfigItem(
			keyName = "fadeColor",
			name = "Fade marker colour",
			description = "The colour of the fading tile marker.",
			section = fadingPlayerSection,
			position = 7
	)
	default Color fadeColor() { return new Color(255, 0, 0, 150); }
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
	@ConfigItem(
			keyName = "enableDynamicRenderDistance",
			name = "Enable dynamic render distance",
			description = "Automatically scales the render distance based on player count in high-population areas. (Experimental)",
			section = tweaksSection,
			position = 1
	)
	default boolean enableDynamicRenderDistance() { return false; }
	@Range(min = 1, max = 500)
	@ConfigItem(
			keyName = "dynamicRenderDistancePlayerThreshold",
			name = "Player count trigger",
			description = "The number of players needed to trigger the dynamic render distance check. (Experimental)",
			section = tweaksSection,
			position = 2
	)
	default int dynamicRenderDistancePlayerThreshold() { return 100; }
}