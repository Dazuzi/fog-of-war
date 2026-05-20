package com.fogofwar.render;
import java.awt.Color;
public final class FogColour {
	private static final int SAILING_SEA_ALPHA_FLOOR = 16;
	private static final int SAILING_SEA_CACHE_SIZE = 16;
	private static final int[] SAILING_SEA_KEYS = new int[SAILING_SEA_CACHE_SIZE];
	private static final Color[] SAILING_SEA_VALUES = new Color[SAILING_SEA_CACHE_SIZE];
	private static int sailingSeaNext;
	private FogColour() {}
	public static Color sailingSea(Color colour) {
		int rgb = colour.getRGB();
		for (int i = 0; i < SAILING_SEA_VALUES.length; i++) {
			Color cached = SAILING_SEA_VALUES[i];
			if (cached != null && SAILING_SEA_KEYS[i] == rgb) return cached;
		}
		Color sailingSea = new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), Math.max(SAILING_SEA_ALPHA_FLOOR, colour.getAlpha() / 4));
		SAILING_SEA_KEYS[sailingSeaNext] = rgb;
		SAILING_SEA_VALUES[sailingSeaNext] = sailingSea;
		sailingSeaNext = (sailingSeaNext + 1) % SAILING_SEA_CACHE_SIZE;
		return sailingSea;
	}
}
