package com.fogofwar.render.minimap;
import com.fogofwar.render.FogRender;
import com.fogofwar.render.FogRender.StrokeCache;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
final class MinimapFogMask {
	private GeneralPath fogFillPath;
	private StrokeCache borderStroke;
	void renderFog(Graphics2D graphics, Shape minimapClipShape, GeneralPath path, Color colour) {
		Rectangle2D bounds = minimapClipShape.getBounds2D();
		if (path.contains(bounds)) return;
		Rectangle b = bounds.getBounds();
		if (fogFillPath == null) fogFillPath = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
		FogRender.fill(fogFillPath, b, 1, path);
		graphics.setColor(colour);
		graphics.fill(fogFillPath);
	}
	void renderFullFog(Graphics2D graphics, Shape minimapClipShape, Color colour) {
		graphics.setColor(colour);
		graphics.fill(minimapClipShape);
	}
	void renderBorder(Graphics2D graphics, Shape minimapClipShape, GeneralPath path, Color colour, int thickness) {
		if (path.contains(minimapClipShape.getBounds2D())) return;
		if (borderStroke == null) borderStroke = new StrokeCache();
		FogRender.drawBorder(graphics, path, colour, borderStroke, thickness);
	}
	void renderSailingSeaFog(Graphics2D graphics, GeneralPath boundary, GeneralPath innerBoundary, Color colour) {
		Area area = FogRender.createDifferenceArea(boundary, innerBoundary);
		if (area.isEmpty()) return;
		graphics.setColor(colour);
		graphics.fill(area);
	}
	void renderSailingSeaBorder(Graphics2D graphics, Shape minimapClipShape, GeneralPath path, Color colour, int thickness) {
		if (path.contains(minimapClipShape.getBounds2D())) return;
		if (borderStroke == null) borderStroke = new StrokeCache();
		FogRender.drawBorder(graphics, path, colour, borderStroke, thickness);
	}
}
