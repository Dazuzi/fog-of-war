package com.fogofwar.config;
import net.runelite.client.config.*;
import java.awt.*;
@ConfigGroup(FogOfWarConfigMigration.CONFIG_GROUP)
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
			name = "Only in Wilderness",
			description = "Hide all plugin overlays outside the Wilderness.",
			position = 0
	)
	default boolean onlyInWilderness() { return false; }
	@ConfigItem(
			keyName = "disableWhileSailing",
			name = "Disable while Sailing",
			description = "Hide world, minimap, and fading overlays while Sailing.",
			position = 1
	)
	default boolean disableWhileSailing() { return false; }
	@ConfigItem(
			keyName = "showLandAreaWhileSailing",
			name = "Show land area while Sailing",
			description = "Show the smaller land actor render area while Sailing.",
			position = 2
	)
	default boolean showLandAreaWhileSailing() { return false; }
	@ConfigSection(
			name = "World",
			description = "Settings for the world overlay",
			position = 3
	)
	String worldSection = "worldSection";
	@ConfigItem(
			keyName = "worldDisplayMode",
			name = "World display",
			description = "Choose what to draw in the world view.",
			section = worldSection,
			position = 0
	)
	default FogDisplayMode worldDisplayMode() { return FogDisplayMode.FOG; }
	@Alpha
	@ConfigItem(
			keyName = "worldFogColour",
			name = "Fog colour",
			description = "Colour of the world fog.",
			section = worldSection,
			position = 1
	)
	default Color worldFogColour() { return new Color(0, 0, 0, 125); }
	@Alpha
	@ConfigItem(
			keyName = "worldBorderColour",
			name = "Border colour",
			description = "Colour of the world render-distance border.",
			section = worldSection,
			position = 2
	)
	default Color worldBorderColour() { return new Color(0, 0, 0, 125); }
	@Range(min = 1, max = 16)
	@ConfigItem(
			keyName = "worldBorderThickness",
			name = "Border thickness",
			description = "Width of the world render-distance border.",
			section = worldSection,
			position = 3
	)
	default int worldBorderThickness() { return 1; }
	@ConfigItem(
			keyName = "actorCutoutLimit",
			name = "Actor cutouts",
			description = "Keep visible players and NPCs uncovered by world fog. Higher values cost more performance.",
			section = worldSection,
			position = 4
	)
	default ActorCutoutLimit actorCutoutLimit() { return ActorCutoutLimit.LIMIT_64; }
	@ConfigSection(
			name = "Minimap",
			description = "Settings for the minimap overlay",
			position = 4
	)
	String minimapSection = "minimapSection";
	@ConfigItem(
			keyName = "minimapDisplayMode",
			name = "Minimap display",
			description = "Choose what to draw on the minimap.",
			section = minimapSection,
			position = 0
	)
	default FogDisplayMode minimapDisplayMode() { return FogDisplayMode.FOG; }
	@Alpha
	@ConfigItem(
			keyName = "minimapFogColour",
			name = "Fog colour",
			description = "Colour of the minimap fog.",
			section = minimapSection,
			position = 1
	)
	default Color minimapFogColour() { return new Color(0, 0, 0, 125); }
	@Alpha
	@ConfigItem(
			keyName = "minimapBorderColour",
			name = "Border colour",
			description = "Colour of the minimap render-distance border.",
			section = minimapSection,
			position = 2
	)
	default Color minimapBorderColour() { return new Color(0, 0, 0, 125); }
	@Range(min = 1, max = 16)
	@ConfigItem(
			keyName = "minimapBorderThickness",
			name = "Border thickness",
			description = "Width of the minimap render-distance border.",
			section = minimapSection,
			position = 3
	)
	default int minimapBorderThickness() { return 1; }
	@ConfigSection(
			name = "Fading Players",
			description = "Settings for marking players that leave render distance",
			position = 5
	)
	String fadingPlayerSection = "fadingPlayerSection";
	@ConfigItem(
			keyName = "playerFadeMarkerMode",
			name = "Player fade markers",
			description = "Show fading markers where players leave render distance.",
			section = fadingPlayerSection,
			position = 0
	)
	default FadingPlayerMode playerFadeMarkerMode() { return FadingPlayerMode.OFF; }
	@Alpha
	@ConfigItem(
			keyName = "fadeMarkerColour",
			name = "Marker colour",
			description = "Colour of player fade markers.",
			section = fadingPlayerSection,
			position = 1
	)
	default Color fadeMarkerColour() { return new Color(255, 0, 0, 150); }
	@Range(min = 1, max = 16)
	@ConfigItem(
			keyName = "fadeDurationTicks",
			name = "Fade duration",
			description = "Ticks before fade markers disappear.",
			section = fadingPlayerSection,
			position = 2
	)
	default int fadeDurationTicks() { return 2; }
	@ConfigItem(
			keyName = "onlyFadeAtRenderEdge",
			name = "Render near edge only",
			description = "Only mark players that disappear near the render-distance edge.",
			section = fadingPlayerSection,
			position = 3
	)
	default boolean onlyFadeAtRenderEdge() { return true; }
	@ConfigItem(
			keyName = "predictMovement",
			name = "Predict movement",
			description = "Move fade markers using the player's last known movement.",
			section = fadingPlayerSection,
			position = 4
	)
	default boolean predictMovement() { return true; }
	@ConfigItem(
			keyName = "showFadeMarkerNames",
			name = "Show player names",
			description = "Show names on world fade markers.",
			section = fadingPlayerSection,
			position = 5
	)
	default boolean showFadeMarkerNames() { return true; }
	@ConfigSection(
			name = "Tweaks",
			description = "Advanced settings for experimental features.",
			position = 6,
			closedByDefault = true
	)
	String tweaksSection = "tweaksSection";
	@Range(min = 1, max = 64)
	@ConfigItem(
			keyName = "landRenderDistance",
			name = "Land actor distance",
			description = "Land actor render distance used by the overlay. Does not change the game's render distance.",
			section = tweaksSection,
			position = 0
	)
	default int landRenderDistance() { return 15; }
	@Range(min = 1, max = 64)
	@ConfigItem(
			keyName = "sailingRenderDistance",
			name = "Sea actor distance",
			description = "Sea actor render distance used by the overlay. Does not change the game's render distance.",
			section = tweaksSection,
			position = 1
	)
	default int sailingRenderDistance() { return 30; }
	@ConfigItem(
			keyName = "debugOverlayEnabled",
			name = "Debug overlay",
			description = "Show current plane.",
			section = tweaksSection,
			position = 2
	)
	default boolean debugOverlayEnabled() { return false; }
}
