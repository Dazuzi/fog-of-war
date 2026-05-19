package com.fogofwar.render.world;
import com.fogofwar.config.EntityExclusionLimit;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.render.FogColour;
import com.fogofwar.render.FogPathBuilder;
import com.fogofwar.render.StrokeCache;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
final class WorldFogMask {
	private final FogOfWarConfig config;
	private final GeneralPath fogPath = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
	private final StrokeCache borderStroke = new StrokeCache();
	WorldFogMask(FogOfWarConfig config) { this.config = config; }
	void renderFog(Graphics2D graphics, Rectangle viewport, WorldView worldView, GeneralPath boundary, LocalPoint centerLp, int plane, int radius, ActorCutoutMask actorCutouts) {
		if (boundary.contains(viewport)) return;
		FogPathBuilder.fill(fogPath, viewport, 0, boundary);
		graphics.setColor(config.worldFogColour());
		EntityExclusionLimit exclusionLimit = config.actorCutoutLimit();
		if (!exclusionLimit.isEnabled()) {
			graphics.fill(fogPath);
			return;
		}
		Area fogArea = new Area(fogPath);
		actorCutouts.subtractExclusions(fogArea, viewport, worldView, boundary, centerLp, plane, radius, exclusionLimit.getLimit());
		graphics.fill(fogArea);
	}
	void renderFullFog(Graphics2D graphics, Rectangle viewport) {
		graphics.setColor(config.worldFogColour());
		graphics.fill(viewport);
	}
	void renderBorder(Graphics2D graphics, GeneralPath boundary) {
		drawBorder(graphics, boundary, config.worldBorderColour());
	}
	void renderSailingSeaFog(Graphics2D graphics, Rectangle viewport, WorldView worldView, GeneralPath boundary, GeneralPath innerBoundary, LocalPoint centerLp, int plane, int radius, ActorCutoutMask actorCutouts) {
		renderSailingExtendedFog(graphics, viewport, worldView, boundary, innerBoundary, centerLp, plane, radius, actorCutouts, getSailingSeaFogColour());
	}
	private void renderSailingExtendedFog(Graphics2D graphics, Rectangle viewport, WorldView worldView, GeneralPath boundary, GeneralPath innerBoundary, LocalPoint centerLp, int plane, int radius, ActorCutoutMask actorCutouts, Color color) {
		Area area = new Area(boundary);
		area.subtract(new Area(innerBoundary));
		if (area.isEmpty()) return;
		EntityExclusionLimit exclusionLimit = config.actorCutoutLimit();
		if (exclusionLimit.isEnabled()) actorCutouts.subtractExclusions(area, viewport, worldView, innerBoundary, centerLp, plane, radius, exclusionLimit.getLimit());
		if (area.isEmpty()) return;
		graphics.setColor(color);
		graphics.fill(area);
	}
	void renderSailingSeaBorder(Graphics2D graphics, GeneralPath boundary) {
		drawBorder(graphics, boundary, getSailingSeaBorderColour());
	}
	private void drawBorder(Graphics2D graphics, GeneralPath boundary, Color color) {
		Stroke oldStroke = graphics.getStroke();
		graphics.setColor(color);
		graphics.setStroke(borderStroke.get(config.worldBorderThickness()));
		graphics.draw(boundary);
		graphics.setStroke(oldStroke);
	}
	private Color getSailingSeaFogColour() { return FogColour.sailingSea(config.worldFogColour()); }
	private Color getSailingSeaBorderColour() { return FogColour.sailingSea(config.worldBorderColour()); }
}
