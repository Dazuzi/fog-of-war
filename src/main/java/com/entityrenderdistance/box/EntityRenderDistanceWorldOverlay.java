package com.entityrenderdistance.box;
import com.entityrenderdistance.EntityRenderDistanceConfig;
import com.entityrenderdistance.EntityRenderDistancePlugin;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Varbits;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.geometry.Geometry;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import javax.inject.Inject;
import java.awt.*;
import java.awt.geom.GeneralPath;
public class EntityRenderDistanceWorldOverlay extends Overlay {
    private final Client client;
    private final EntityRenderDistanceConfig config;
    @Inject
    public EntityRenderDistanceWorldOverlay(Client client, EntityRenderDistanceConfig config) {
        this.client = client;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
    }
    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.enableWorldBox() || isClientNotReady()) return null;
        if (config.onlyInWilderness() && !isInWilderness()) return null;
        int radius = config.renderDistanceRadius();
        WorldPoint centerWp = client.getLocalPlayer().getWorldLocation();
        GeneralPath borderPath = createBorderPath(centerWp, radius);
        if (borderPath != null) { renderPath(graphics, borderPath); }
        return null;
    }
    private GeneralPath createBorderPath(WorldPoint center, int radius) {
        GeneralPath path = new GeneralPath();
        boolean pathStarted = false;
        for (int x = -radius; x <= radius; x++) {
            WorldPoint wp = new WorldPoint(center.getX() + x, center.getY() + radius, center.getPlane());
            LocalPoint lp = LocalPoint.fromWorld(client, wp);
            if (lp != null) {
                int edgeY = lp.getY() + 64;
                if (!pathStarted) {
                    path.moveTo(lp.getX(), edgeY);
                    pathStarted = true;
                } else {
                    path.lineTo(lp.getX(), edgeY);
                }
            }
        }
        for (int y = radius - 1; y >= -radius; y--) {
            WorldPoint wp = new WorldPoint(center.getX() + radius, center.getY() + y, center.getPlane());
            LocalPoint lp = LocalPoint.fromWorld(client, wp);
            if (lp != null) {
                int edgeX = lp.getX() + 64;
                path.lineTo(edgeX, lp.getY());
            }
        }
        for (int x = radius - 1; x >= -radius; x--) {
            WorldPoint wp = new WorldPoint(center.getX() + x, center.getY() - radius, center.getPlane());
            LocalPoint lp = LocalPoint.fromWorld(client, wp);
            if (lp != null) {
                int edgeY = lp.getY() - 64;
                path.lineTo(lp.getX(), edgeY);
            }
        }
        for (int y = -radius + 1; y < radius; y++) {
            WorldPoint wp = new WorldPoint(center.getX() - radius, center.getY() + y, center.getPlane());
            LocalPoint lp = LocalPoint.fromWorld(client, wp);
            if (lp != null) {
                int edgeX = lp.getX() - 64;
                path.lineTo(edgeX, lp.getY());
            }
        }
        if (pathStarted) path.closePath();
        return pathStarted ? path : null;
    }
    private void renderPath(Graphics2D graphics, GeneralPath path) {
        graphics.setColor(config.worldBorderColour());
        graphics.setStroke(new BasicStroke(config.worldBorderThickness()));
        path = Geometry.filterPath(path, (p1, p2) ->
                Perspective.localToCanvas(client, new LocalPoint((int)p1[0], (int)p1[1]), client.getPlane()) != null &&
                        Perspective.localToCanvas(client, new LocalPoint((int)p2[0], (int)p2[1]), client.getPlane()) != null);
        path = Geometry.transformPath(path, coords -> {
            Point point = Perspective.localToCanvas(client, new LocalPoint((int)coords[0], (int)coords[1]), client.getPlane());
            if (point != null) {
                coords[0] = point.getX();
                coords[1] = point.getY();
            }
        });
        graphics.draw(path);
    }
    private boolean isInWilderness() { return client.getVarbitValue(Varbits.IN_WILDERNESS) == 1; }
    private boolean isClientNotReady() { return client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null; }
}