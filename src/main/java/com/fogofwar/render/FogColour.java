package com.fogofwar.render;
import java.awt.Color;
public final class FogColour {
	private static final int SAILING_LAND_ALPHA_FLOOR = 32;
	private FogColour() {}
	public static Color sailingLand(Color colour) { return new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), Math.max(SAILING_LAND_ALPHA_FLOOR, colour.getAlpha() / 2)); }
}
