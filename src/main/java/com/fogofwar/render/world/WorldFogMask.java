package com.fogofwar.render.world;
import com.fogofwar.config.ActorCutoutLimit;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.render.FogColour;
import com.fogofwar.render.FogMaskRenderer;
import com.fogofwar.render.FogPathBuilder;
import com.fogofwar.render.StrokeCache;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
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
		ActorCutoutLimit cutoutLimit = config.actorCutoutLimit();
		if (!cutoutLimit.isEnabled()) {
			graphics.fill(fogPath);
			return;
		}
		Area fogArea = new Area(fogPath);
		actorCutouts.subtractExclusions(fogArea, viewport, worldView, boundary, centerLp, plane, radius, cutoutLimit.getLimit());
		graphics.fill(fogArea);
	}
	void renderFullFog(Graphics2D graphics, Rectangle viewport) {
		graphics.setColor(config.worldFogColour());
		graphics.fill(viewport);
	}
	void renderBorder(Graphics2D graphics, GeneralPath boundary) {
		FogMaskRenderer.drawBorder(graphics, boundary, config.worldBorderColour(), borderStroke, config.worldBorderThickness());
	}
	void renderSailingSeaFog(Graphics2D graphics, Rectangle viewport, WorldView worldView, GeneralPath boundary, GeneralPath innerBoundary, LocalPoint centerLp, int plane, int radius, ActorCutoutMask actorCutouts) {
		Area area = FogMaskRenderer.createDifferenceArea(boundary, innerBoundary);
		if (area.isEmpty()) return;
		ActorCutoutLimit cutoutLimit = config.actorCutoutLimit();
		if (cutoutLimit.isEnabled()) actorCutouts.subtractExclusions(area, viewport, worldView, innerBoundary, centerLp, plane, radius, cutoutLimit.getLimit());
		if (area.isEmpty()) return;
		graphics.setColor(FogColour.sailingSea(config.worldFogColour()));
		graphics.fill(area);
	}
	void renderSailingSeaBorder(Graphics2D graphics, GeneralPath boundary) {
		FogMaskRenderer.drawBorder(graphics, boundary, FogColour.sailingSea(config.worldBorderColour()), borderStroke, config.worldBorderThickness());
	}
}
