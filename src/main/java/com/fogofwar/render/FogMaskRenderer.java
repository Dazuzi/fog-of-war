package com.fogofwar.render;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Area;
public final class FogMaskRenderer {
	private FogMaskRenderer() {}
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
	public static Color sailingSea(Color color) { return FogColour.sailingSea(color); }
}
