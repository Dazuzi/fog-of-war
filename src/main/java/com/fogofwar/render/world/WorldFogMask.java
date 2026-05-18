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
	void renderSailingExtendedFog(Graphics2D graphics, Rectangle viewport, WorldView worldView, GeneralPath boundary, GeneralPath landBoundary, LocalPoint centerLp, int plane, int radius, ActorCutoutMask actorCutouts) {
		Area area = new Area(boundary);
		area.subtract(new Area(landBoundary));
		if (area.isEmpty()) return;
		EntityExclusionLimit exclusionLimit = config.actorCutoutLimit();
		if (exclusionLimit.isEnabled()) actorCutouts.subtractExclusions(area, viewport, worldView, landBoundary, centerLp, plane, radius, exclusionLimit.getLimit());
		if (area.isEmpty()) return;
		graphics.setColor(getSailingExtendedFogColour());
		graphics.fill(area);
	}
	void renderSailingLandBorder(Graphics2D graphics, GeneralPath boundary) {
		drawBorder(graphics, boundary, getSailingLandBorderColour());
	}
	private void drawBorder(Graphics2D graphics, GeneralPath boundary, Color color) {
		Stroke oldStroke = graphics.getStroke();
		graphics.setColor(color);
		graphics.setStroke(borderStroke.get(config.worldBorderThickness()));
		graphics.draw(boundary);
		graphics.setStroke(oldStroke);
	}
	private Color getSailingExtendedFogColour() { return FogColour.sailingLand(config.worldFogColour()); }
	private Color getSailingLandBorderColour() { return FogColour.sailingLand(config.worldBorderColour()); }
}
