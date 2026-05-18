package com.fogofwar.render;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
public final class FogColour {
	private static final int SAILING_LAND_ALPHA_FLOOR = 32;
	private static final Map<Integer, Color> SAILING_LAND_CACHE = new HashMap<>();
	private FogColour() {}
	public static Color sailingLand(Color colour) {
		int rgb = colour.getRGB();
		Color cached = SAILING_LAND_CACHE.get(rgb);
		if (cached != null) return cached;
		Color sailingLand = new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), Math.max(SAILING_LAND_ALPHA_FLOOR, colour.getAlpha() / 2));
		SAILING_LAND_CACHE.put(rgb, sailingLand);
		return sailingLand;
	}
}
