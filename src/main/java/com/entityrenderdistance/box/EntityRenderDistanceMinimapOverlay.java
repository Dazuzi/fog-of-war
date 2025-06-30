package com.entityrenderdistance.box;
import com.entityrenderdistance.EntityRenderDistanceConfig;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Varbits;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import javax.inject.Inject;
import java.awt.*;
public class EntityRenderDistanceMinimapOverlay extends Overlay {
	private final Client client;
	private final EntityRenderDistanceConfig config;
	@Inject
	public EntityRenderDistanceMinimapOverlay(Client client, EntityRenderDistanceConfig config) {
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.HIGH);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		if (!config.enableMinimapBox() || isClientNotReady()) return null;
		if (config.onlyInWilderness() && !isInWilderness()) return null;
		Widget minimapWidget = getMinimapWidget();
		if (minimapWidget == null || minimapWidget.isHidden()) return null;
		int radius = config.renderDistanceRadius();
		WorldPoint centerWp = client.getLocalPlayer().getWorldLocation();
		Shape oldClip = graphics.getClip();
		graphics.setClip(minimapWidget.getBounds());
		graphics.setColor(config.minimapBorderColour());
		graphics.setStroke(new BasicStroke(config.minimapBorderThickness()));
		for (int x = -radius; x <= radius; x++) {
			drawMinimapEdgeOfTile(graphics, new WorldPoint(centerWp.getX() + x, centerWp.getY() + radius, centerWp.getPlane()), "NORTH");
		}
		for (int y = radius; y >= -radius; y--) {
			drawMinimapEdgeOfTile(graphics, new WorldPoint(centerWp.getX() + radius, centerWp.getY() + y, centerWp.getPlane()), "EAST");
		}
		for (int x = radius; x >= -radius; x--) {
			drawMinimapEdgeOfTile(graphics, new WorldPoint(centerWp.getX() + x, centerWp.getY() - radius, centerWp.getPlane()), "SOUTH");
		}
		for (int y = -radius; y <= radius; y++) {
			drawMinimapEdgeOfTile(graphics, new WorldPoint(centerWp.getX() - radius, centerWp.getY() + y, centerWp.getPlane()), "WEST");
		}
		graphics.setClip(oldClip);
		return null;
	}
	private void drawMinimapEdgeOfTile(Graphics2D graphics, WorldPoint wp, String edge) {
		Point p1 = null, p2 = null;
		switch (edge) {
			case "NORTH":
				p1 = getMinimapPoint(new WorldPoint(wp.getX(), wp.getY() + 1, wp.getPlane()));
				p2 = getMinimapPoint(new WorldPoint(wp.getX() + 1, wp.getY() + 1, wp.getPlane()));
				break;
			case "EAST":
				p1 = getMinimapPoint(new WorldPoint(wp.getX() + 1, wp.getY() + 1, wp.getPlane()));
				p2 = getMinimapPoint(new WorldPoint(wp.getX() + 1, wp.getY(), wp.getPlane()));
				break;
			case "SOUTH":
				p1 = getMinimapPoint(new WorldPoint(wp.getX() + 1, wp.getY(), wp.getPlane()));
				p2 = getMinimapPoint(new WorldPoint(wp.getX(), wp.getY(), wp.getPlane()));
				break;
			case "WEST":
				p1 = getMinimapPoint(new WorldPoint(wp.getX(), wp.getY(), wp.getPlane()));
				p2 = getMinimapPoint(new WorldPoint(wp.getX(), wp.getY() + 1, wp.getPlane()));
				break;
		}
		if (p1 != null && p2 != null) {
			graphics.drawLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
		}
	}
	private Point getMinimapPoint(WorldPoint worldPoint) {
		LocalPoint lp = LocalPoint.fromWorld(client, worldPoint);
		if (lp == null) return null;
		LocalPoint centeredLp = new LocalPoint(lp.getX() - (Perspective.LOCAL_TILE_SIZE / 2), lp.getY() - (Perspective.LOCAL_TILE_SIZE / 2));
		return Perspective.localToMinimap(client, centeredLp);
	}
	private boolean isInWilderness() { return client.getVarbitValue(Varbits.IN_WILDERNESS) == 1; }
	private boolean isClientNotReady() { return client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null; }
	private Widget getMinimapWidget() {
		if (client.isResized()) {
			if (client.getVarbitValue(Varbits.SIDE_PANELS) == 1) {
				return client.getWidget(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_MINIMAP_DRAW_AREA);
			}
			return client.getWidget(ComponentID.RESIZABLE_VIEWPORT_MINIMAP_DRAW_AREA);
		}
		return client.getWidget(ComponentID.FIXED_VIEWPORT_MINIMAP_DRAW_AREA);
	}
}