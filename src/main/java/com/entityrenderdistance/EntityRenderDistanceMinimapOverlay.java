package com.entityrenderdistance;

import net.runelite.api.Client;
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
import java.util.ArrayList;
import java.util.List;

public class EntityRenderDistanceMinimapOverlay extends Overlay
{
    private final Client client;
    private final EntityRenderDistanceConfig config;

    // Updated to use 16 tiles to match the world overlay.
    private static final int TILE_RADIUS = 16;

    @Inject
    public EntityRenderDistanceMinimapOverlay(Client client, EntityRenderDistanceConfig config)
    {
        this.client = client;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.HIGH);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        // Check if the minimap overlay is enabled, and if the client is ready.
        if (!config.enableMinimapBox() || EntityRenderDistanceUtils.isClientNotReady(client))
        {
            return null;
        }

        Widget minimapWidget = getMinimapWidget();
        if (minimapWidget == null || minimapWidget.isHidden())
        {
            return null;
        }

        Rectangle bounds = minimapWidget.getBounds();
        int widgetCenterX = bounds.x + bounds.width / 2;
        int widgetCenterY = bounds.y + bounds.height / 2;

        double zoom = client.getMinimapZoom();
        if (zoom <= 0)
        {
            zoom = 3.0;
        }
        // Do not render box if zoom exceeds 5.00.
        if (zoom > 5.00)
        {
            return null;
        }

        double scale = (bounds.width / 48.0) * (zoom / 3.0);

        WorldPoint centerWP = client.getLocalPlayer().getWorldLocation();
        int plane = centerWP.getPlane();

        int minX = centerWP.getX() - TILE_RADIUS;
        int maxX = centerWP.getX() + TILE_RADIUS;
        int minY = centerWP.getY() - TILE_RADIUS;
        int maxY = centerWP.getY() + TILE_RADIUS;

        // Use the utility method for wilderness-checking
        if (config.onlyInWilderness() && EntityRenderDistanceUtils.noTileInWilderness(minX, maxX, minY, maxY, plane, centerWP))
        {
            return null;
        }

        LocalPoint playerLocal = client.getLocalPlayer().getLocalLocation();
        java.awt.Point centerCanvas;
        {
            Point apiPt = Perspective.localToMinimap(client, playerLocal);
            centerCanvas = (apiPt != null)
                    ? new java.awt.Point(apiPt.getX(), apiPt.getY())
                    : new java.awt.Point(widgetCenterX, widgetCenterY);
        }

        // Sample points along each edge.
        List<java.awt.Point> topPoints = new ArrayList<>();
        List<java.awt.Point> rightPoints = new ArrayList<>();
        List<java.awt.Point> bottomPoints = new ArrayList<>();
        List<java.awt.Point> leftPoints = new ArrayList<>();

        for (int x = minX; x <= maxX; x++)
        {
            WorldPoint wp = new WorldPoint(x, maxY, plane);
            java.awt.Point p = convertWorldPointToCanvas(wp, centerWP, centerCanvas, bounds, scale, zoom);
            if (p != null)
                topPoints.add(p);
        }
        for (int y = maxY - 1; y >= minY; y--)
        {
            WorldPoint wp = new WorldPoint(maxX, y, plane);
            java.awt.Point p = convertWorldPointToCanvas(wp, centerWP, centerCanvas, bounds, scale, zoom);
            if (p != null)
                rightPoints.add(p);
        }
        for (int x = maxX - 1; x >= minX; x--)
        {
            WorldPoint wp = new WorldPoint(x, minY, plane);
            java.awt.Point p = convertWorldPointToCanvas(wp, centerWP, centerCanvas, bounds, scale, zoom);
            if (p != null)
                bottomPoints.add(p);
        }
        for (int y = minY + 1; y < maxY; y++)
        {
            WorldPoint wp = new WorldPoint(minX, y, plane);
            java.awt.Point p = convertWorldPointToCanvas(wp, centerWP, centerCanvas, bounds, scale, zoom);
            if (p != null)
                leftPoints.add(p);
        }

        Shape oldClip = graphics.getClip();
        graphics.setClip(bounds);
        graphics.setColor(config.minimapBorderColour());

        // Rendering modes:
        // Mode A: When zoom <= 3.50, draw a closed polygon connecting all sampled points.
        if (zoom <= 3.50)
        {
            List<java.awt.Point> allPoints = new ArrayList<>();
            allPoints.addAll(topPoints);
            allPoints.addAll(rightPoints);
            allPoints.addAll(bottomPoints);
            allPoints.addAll(leftPoints);
            if (allPoints.size() >= 2)
            {
                for (int i = 0; i < allPoints.size() - 1; i++)
                {
                    java.awt.Point start = allPoints.get(i);
                    java.awt.Point end = allPoints.get(i + 1);
                    graphics.drawLine(start.x, start.y, end.x, end.y);
                }
                java.awt.Point first = allPoints.get(0);
                java.awt.Point last = allPoints.get(allPoints.size() - 1);
                graphics.drawLine(last.x, last.y, first.x, first.y);
            }
        }
        // Mode B: For zoom between 3.50 and 5.00: draw each side as a single line
        // (using the first and last sample point for that edge).
        else // (3.50 < zoom <= 5.00)
        {
            if (topPoints.size() >= 2)
            {
                java.awt.Point start = topPoints.get(0);
                java.awt.Point end = topPoints.get(topPoints.size() - 1);
                graphics.drawLine(start.x, start.y, end.x, end.y);
            }
            if (rightPoints.size() >= 2)
            {
                java.awt.Point start = rightPoints.get(0);
                java.awt.Point end = rightPoints.get(rightPoints.size() - 1);
                graphics.drawLine(start.x, start.y, end.x, end.y);
            }
            if (bottomPoints.size() >= 2)
            {
                java.awt.Point start = bottomPoints.get(0);
                java.awt.Point end = bottomPoints.get(bottomPoints.size() - 1);
                graphics.drawLine(start.x, start.y, end.x, end.y);
            }
            if (leftPoints.size() >= 2)
            {
                java.awt.Point start = leftPoints.get(0);
                java.awt.Point end = leftPoints.get(leftPoints.size() - 1);
                graphics.drawLine(start.x, start.y, end.x, end.y);
            }
        }

        graphics.setClip(oldClip);
        return null;
    }

    /**
     * Converts a given world point to a canvas point on the minimap.
     * We assume players are mostly using zooms above 3.50, so if the built-in conversion is available,
     * use that directly. Only if zoom is 3.50 or below, do we apply a fallback estimation.
     */
    private java.awt.Point convertWorldPointToCanvas(WorldPoint wp, WorldPoint centerWP,
                                                     java.awt.Point centerCanvas, Rectangle bounds,
                                                     double scale, double zoom)
    {
        LocalPoint lp = LocalPoint.fromWorld(client, wp);
        if (lp != null)
        {
            Point apiPt = Perspective.localToMinimap(client, lp);
            if (apiPt != null)
            {
                // For zoom above 3.50, simply use built-in conversion.
                return new java.awt.Point(apiPt.getX(), apiPt.getY());
            }
        }

        // Fallback estimation for zoom 3.50 or below.
        if (zoom <= 3.50)
        {
            int dx = wp.getX() - centerWP.getX();
            int dy = wp.getY() - centerWP.getY();
            int estimatedX = centerCanvas.x + (int)(dx * scale);
            int estimatedY = centerCanvas.y - (int)(dy * scale);
            estimatedX = clamp(estimatedX, bounds.x, bounds.x + bounds.width);
            estimatedY = clamp(estimatedY, bounds.y, bounds.y + bounds.height);
            return new java.awt.Point(estimatedX, estimatedY);
        }

        // For zooms above 3.50, if built-in breakdown occurs (unlikely), we simply return null.
        return null;
    }

    private int clamp(int value, int min, int max)
    {
        return Math.max(min, Math.min(value, max));
    }

    private Widget getMinimapWidget()
    {
        if (client.isResized())
        {
            if (client.getVarbitValue(Varbits.SIDE_PANELS) == 1)
            {
                return client.getWidget(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_MINIMAP_DRAW_AREA);
            }
            else
            {
                return client.getWidget(ComponentID.RESIZABLE_VIEWPORT_MINIMAP_DRAW_AREA);
            }
        }
        return client.getWidget(ComponentID.FIXED_VIEWPORT_MINIMAP_DRAW_AREA);
    }
}
