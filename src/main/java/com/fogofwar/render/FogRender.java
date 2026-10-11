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
	public static final class SailingSeaCache {
		private Color colour;
		private Color sailingSea;
		public Color get(Color c) {
			if (!c.equals(colour)) {
				sailingSea = new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(SAILING_SEA_ALPHA_FLOOR, c.getAlpha() / 4));
				colour = c;
			}
			return sailingSea;
		}
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
