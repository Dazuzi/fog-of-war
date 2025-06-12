package com.entityrenderdistance;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup("entityrenderdistance")
public interface EntityRenderDistanceConfig extends Config
{
	// The ungrouped wilderness setting appears on its own.
	@ConfigItem(
			keyName = "onlyInWilderness",
			name = "Only show in Wilderness",
			description = "Only display overlays while in, or near, the wilderness.",
			position = 0
	)
	default boolean onlyInWilderness()
	{
		return false;
	}

	// Overlay (world) box settings group.
	@ConfigSection(
			name = "Overlay Box",
			description = "Settings for the world overlay box",
			position = 1
	)
	String overlayBoxSection = "overlayBoxSection";

	@ConfigItem(
			keyName = "enableOverlayBox",
			name = "Enable overlay box",
			description = "Enable the main world overlay box",
			section = overlayBoxSection,
			position = 0
	)
	default boolean enableOverlayBox()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
			keyName = "overlayBorderColour",
			name = "Border colour",
			description = "The border colour of the overlay box drawn around the player",
			section = overlayBoxSection,
			position = 1
	)
	default Color overlayBorderColour()
	{
		return new Color(255, 173, 0, 150);
	}

	// Minimap box settings group.
	@ConfigSection(
			name = "Minimap Box",
			description = "Settings for the minimap box",
			position = 2
	)
	String minimapBoxSection = "minimapBoxSection";

	@ConfigItem(
			keyName = "enableMinimapBox",
			name = "Enable minimap box",
			description = "Enable the minimap overlay box",
			section = minimapBoxSection,
			position = 0
	)
	default boolean enableMinimapBox()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
			keyName = "minimapBorderColour",
			name = "Border colour",
			description = "The border colour of the overlay drawn on the minimap",
			section = minimapBoxSection,
			position = 1
	)
	default Color minimapBorderColour()
	{
		return new Color(255, 173, 0, 150);
	}
}
