package com.fogofwar.render.minimap;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.render.FogMaskRenderer;
import com.fogofwar.render.FogPathBuilder;
import com.fogofwar.render.StrokeCache;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
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
	void renderFullFog(Graphics2D graphics, Shape minimapClipShape) {
		graphics.setColor(config.minimapFogColour());
		graphics.fill(minimapClipShape);
	}
	void renderBorder(Graphics2D graphics, Shape minimapClipShape, GeneralPath path) {
		if (path.contains(minimapClipShape.getBounds2D())) return;
		FogMaskRenderer.drawBorder(graphics, path, config.minimapBorderColour(), borderStroke, config.minimapBorderThickness());
	}
	void renderSailingSeaFog(Graphics2D graphics, GeneralPath boundary, GeneralPath innerBoundary) {
		Area area = FogMaskRenderer.createDifferenceArea(boundary, innerBoundary);
		if (area.isEmpty()) return;
		graphics.setColor(FogMaskRenderer.sailingSea(config.minimapFogColour()));
		graphics.fill(area);
	}
	void renderSailingSeaBorder(Graphics2D graphics, Shape minimapClipShape, GeneralPath path) {
		if (path.contains(minimapClipShape.getBounds2D())) return;
		FogMaskRenderer.drawBorder(graphics, path, FogMaskRenderer.sailingSea(config.minimapBorderColour()), borderStroke, config.minimapBorderThickness());
	}
}
