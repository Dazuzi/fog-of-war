package com.entityrenderdistance;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import java.awt.*;
@ConfigGroup("entityrenderdistance")
public interface EntityRenderDistanceConfig extends Config {
	@ConfigItem(
			keyName = "onlyInWilderness",
			name = "Only show in Wilderness",
			description = "Only display overlays while in the Wilderness.",
			position = 0
	)
	default boolean onlyInWilderness() { return false; }
	@Range(min = 1, max = 64)
	@ConfigItem(
			keyName = "renderDistanceRadius",
			name = "Render distance",
			description = "This does NOT change the game's render distance.<br>It adjusts the overlay's radius to match the current in-game entity render distance (normally 15).<br>Adjust this only if Jagex changes the render distance in a future game update.",
			position = 1
	)
	default int renderDistanceRadius() { return 15; }
	@ConfigSection(
			name = "World Box",
			description = "Settings for the world overlay box",
			position = 2
	)
	String worldBoxSection = "worldBoxSection";
	@ConfigItem(
			keyName = "enableWorldBox",
			name = "Enable world box",
			description = "Enable the main world overlay box",
			section = worldBoxSection,
			position = 0
	)
	default boolean enableWorldBox() { return true; }
	@Alpha
	@ConfigItem(
			keyName = "worldBorderColour",
			name = "Border colour",
			description = "The border colour of the world overlay box.",
			section = worldBoxSection,
			position = 1
	)
	default Color worldBorderColour() { return new Color(255, 173, 0, 150); }
	@Range(min = 1, max = 5)
	@ConfigItem(
			keyName = "worldBorderThickness",
			name = "Border thickness",
			description = "The thickness of the world overlay box border.",
			section = worldBoxSection,
			position = 2
	)
	default int worldBorderThickness() { return 1; }
	@ConfigSection(
			name = "Minimap Box",
			description = "Settings for the minimap box",
			position = 3
	)
	String minimapBoxSection = "minimapBoxSection";
	@ConfigItem(
			keyName = "enableMinimapBox",
			name = "Enable minimap box",
			description = "Enable the minimap overlay box",
			section = minimapBoxSection,
			position = 0
	)
	default boolean enableMinimapBox() { return true; }
	@Alpha
	@ConfigItem(
			keyName = "minimapBorderColour",
			name = "Border colour",
			description = "The border colour of the minimap overlay.",
			section = minimapBoxSection,
			position = 1
	)
	default Color minimapBorderColour() { return new Color(255, 173, 0, 150); }
	@Range(min = 1, max = 5)
	@ConfigItem(
			keyName = "minimapBorderThickness",
			name = "Border thickness",
			description = "The thickness of the minimap overlay border.",
			section = minimapBoxSection,
			position = 2
	)
	default int minimapBorderThickness() { return 1; }
	@ConfigSection(
			name = "Fading Players",
			description = "Settings for marking players that leave render distance",
			position = 4,
			closedByDefault = false
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
	@Range(min = 1, max = 10)
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
}