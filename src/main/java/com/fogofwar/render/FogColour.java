package com.fogofwar.render;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
public final class FogColour {
	private static final int SAILING_SEA_ALPHA_FLOOR = 16;
	private static final Map<Integer, Color> SAILING_SEA_CACHE = new HashMap<>();
	private FogColour() {}
	public static Color sailingSea(Color colour) {
		int rgb = colour.getRGB();
		Color cached = SAILING_SEA_CACHE.get(rgb);
		if (cached != null) return cached;
		Color sailingSea = new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), Math.max(SAILING_SEA_ALPHA_FLOOR, colour.getAlpha() / 4));
		SAILING_SEA_CACHE.put(rgb, sailingSea);
		return sailingSea;
	}
}
