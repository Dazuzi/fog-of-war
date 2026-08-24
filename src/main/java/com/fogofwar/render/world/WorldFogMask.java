package com.fogofwar.render.world;
import com.fogofwar.config.ActorCutoutLimit;
import com.fogofwar.render.FogRender;
import com.fogofwar.render.FogRender.StrokeCache;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
final class WorldFogMask {
	private GeneralPath fogPath;
	private StrokeCache borderStroke;
	void renderFog(Graphics2D graphics, Rectangle viewport, WorldView worldView, GeneralPath boundary, LocalPoint centerLp, int plane, int radius, Color colour, ActorCutoutLimit cutoutLimit, ActorCutoutMask actorCutouts) {
		if (boundary.contains(viewport)) return;
		if (fogPath == null) fogPath = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
		FogRender.fill(fogPath, viewport, 0, boundary);
		graphics.setColor(colour);
		if (!cutoutLimit.isEnabled()) {
			graphics.fill(fogPath);
			return;
		}
		Area fogArea = new Area(fogPath);
		actorCutouts.subtractExclusions(fogArea, viewport, worldView, boundary, centerLp, plane, radius, cutoutLimit.getLimit());
		graphics.fill(fogArea);
	}
	void renderFullFog(Graphics2D graphics, Rectangle viewport, Color colour) {
		graphics.setColor(colour);
		graphics.fill(viewport);
	}
	void renderBorder(Graphics2D graphics, GeneralPath boundary, Color colour, int thickness) {
		if (borderStroke == null) borderStroke = new StrokeCache();
		FogRender.drawBorder(graphics, boundary, colour, borderStroke, thickness);
	}
	void renderSailingSeaFog(Graphics2D graphics, Rectangle viewport, WorldView worldView, GeneralPath boundary, GeneralPath innerBoundary, LocalPoint centerLp, int plane, int radius, Color colour, ActorCutoutLimit cutoutLimit, ActorCutoutMask actorCutouts) {
		Area area = FogRender.createDifferenceArea(boundary, innerBoundary);
		if (area.isEmpty()) return;
		if (cutoutLimit.isEnabled()) actorCutouts.subtractExclusions(area, viewport, worldView, innerBoundary, centerLp, plane, radius, cutoutLimit.getLimit());
		if (area.isEmpty()) return;
		graphics.setColor(colour);
		graphics.fill(area);
	}
	void renderSailingSeaBorder(Graphics2D graphics, GeneralPath boundary, Color colour, int thickness) {
		if (borderStroke == null) borderStroke = new StrokeCache();
		FogRender.drawBorder(graphics, boundary, colour, borderStroke, thickness);
	}
}
