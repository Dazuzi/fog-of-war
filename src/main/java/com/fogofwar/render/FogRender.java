package com.fogofwar.render;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
public final class FogRender {
	private static final int SAILING_SEA_ALPHA_FLOOR = 16;
	private static final int SAILING_SEA_CACHE_SIZE = 16;
	private static final int[] SAILING_SEA_KEYS = new int[SAILING_SEA_CACHE_SIZE];
	private static final Color[] SAILING_SEA_VALUES = new Color[SAILING_SEA_CACHE_SIZE];
	private static int sailingSeaNext;
	private FogRender() {}
	public static void fill(GeneralPath path, Rectangle bounds, int padding, GeneralPath inner) {
		path.reset();
		path.moveTo(bounds.x - padding, bounds.y - padding);
		path.lineTo(bounds.x + bounds.width + padding, bounds.y - padding);
		path.lineTo(bounds.x + bounds.width + padding, bounds.y + bounds.height + padding);
		path.lineTo(bounds.x - padding, bounds.y + bounds.height + padding);
		path.closePath();
		path.append(inner, false);
	}
	public static void drawBorder(Graphics2D graphics, Shape path, Color color, StrokeCache borderStroke, int thickness) {
		Stroke oldStroke = graphics.getStroke();
		graphics.setColor(color);
		graphics.setStroke(borderStroke.get(thickness));
		graphics.draw(path);
		graphics.setStroke(oldStroke);
	}
	public static Area createDifferenceArea(Shape boundary, Shape innerBoundary) {
		Area area = new Area(boundary);
		area.subtract(new Area(innerBoundary));
		return area;
	}
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
	public static final class StrokeCache {
		private BasicStroke stroke;
		private int width = -1;
		public BasicStroke get(int w) {
			if (stroke == null || width != w) {
				stroke = new BasicStroke(w);
				width = w;
			}
			return stroke;
		}
	}
}
