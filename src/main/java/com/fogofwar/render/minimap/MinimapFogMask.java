package com.fogofwar.render.minimap;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.render.FogColour;
import com.fogofwar.render.FogPathBuilder;
import com.fogofwar.render.StrokeCache;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
final class MinimapFogMask {
	private final FogOfWarConfig config;
	private final GeneralPath fogFillPath = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
	private final StrokeCache borderStroke = new StrokeCache();
	MinimapFogMask(FogOfWarConfig config) { this.config = config; }
	void renderFog(Graphics2D graphics, Shape minimapClipShape, GeneralPath path) {
		if (path.contains(minimapClipShape.getBounds2D())) return;
		Rectangle b = minimapClipShape.getBounds();
		FogPathBuilder.fill(fogFillPath, b, 1, path);
		graphics.setColor(config.minimapFogColour());
		graphics.fill(fogFillPath);
	}
	void renderBorder(Graphics2D graphics, Shape minimapClipShape, GeneralPath path) {
		if (path.contains(minimapClipShape.getBounds2D())) return;
		drawBorder(graphics, path, config.minimapBorderColour());
	}
	void renderSailingExtendedFog(Graphics2D graphics, GeneralPath boundary, GeneralPath landBoundary) {
		Area area = new Area(boundary);
		area.subtract(new Area(landBoundary));
		if (area.isEmpty()) return;
		graphics.setColor(getSailingMinimapFogColour());
		graphics.fill(area);
	}
	void renderSailingLandBorder(Graphics2D graphics, Shape minimapClipShape, GeneralPath path) {
		if (path.contains(minimapClipShape.getBounds2D())) return;
		drawBorder(graphics, path, getSailingMinimapBorderColour());
	}
	private void drawBorder(Graphics2D graphics, GeneralPath path, Color color) {
		Stroke oldStroke = graphics.getStroke();
		graphics.setColor(color);
		graphics.setStroke(borderStroke.get(config.minimapBorderThickness()));
		graphics.draw(path);
		graphics.setStroke(oldStroke);
	}
	private Color getSailingMinimapFogColour() { return FogColour.sailingLand(config.minimapFogColour()); }
	private Color getSailingMinimapBorderColour() { return FogColour.sailingLand(config.minimapBorderColour()); }
}
