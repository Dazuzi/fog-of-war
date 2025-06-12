package com.entityrenderdistance;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EntityRenderDistanceOverlay extends Overlay
{
    private final Client client;
    private final EntityRenderDistanceConfig config;

    // Render distance in tiles (draws a box covering 2*RADIUS + 1 tiles on each side)
    private static final int RADIUS = 16;
    // One tile in local coordinates is 128 units; half a tile = 64 units.
    private static final int HALF_TILE = 64;

    @Inject
    public EntityRenderDistanceOverlay(Client client, EntityRenderDistanceConfig config)
    {
        this.client = client;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        // setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        // Check if the world overlay is enabled, and if the client is ready.
        if (!config.enableOverlayBox() || EntityRenderDistanceUtils.isClientNotReady(client))
        {
            return null;
        }

        Player localPlayer = client.getLocalPlayer();
        WorldPoint worldLocation = localPlayer.getWorldLocation();
        int plane = worldLocation.getPlane();
        int centerX = worldLocation.getX();
        int centerY = worldLocation.getY();

        int minX = centerX - RADIUS;
        int maxX = centerX + RADIUS;
        int minY = centerY - RADIUS;
        int maxY = centerY + RADIUS;

        // Use the utility method for wilderness detection.
        if (config.onlyInWilderness() && EntityRenderDistanceUtils.noTileInWilderness(minX, maxX, minY, maxY, plane, worldLocation))
        {
            return null;
        }

        List<java.awt.Point> canvasPoints = new ArrayList<>();

        for (int x = minX; x <= maxX; x++)
        {
            WorldPoint wp = new WorldPoint(x, maxY, plane);
            LocalPoint lp = LocalPoint.fromWorld(client, wp);
            if (lp != null)
            {
                LocalPoint adjusted = new LocalPoint(lp.getX(), lp.getY() + HALF_TILE);
                Point rPoint = Perspective.localToCanvas(client, adjusted, 0);
                if (rPoint != null)
                    canvasPoints.add(new java.awt.Point(rPoint.getX(), rPoint.getY()));
            }
        }

        for (int y = maxY - 1; y >= minY; y--)
        {
            WorldPoint wp = new WorldPoint(maxX, y, plane);
            LocalPoint lp = LocalPoint.fromWorld(client, wp);
            if (lp != null)
            {
                LocalPoint adjusted = new LocalPoint(lp.getX() + HALF_TILE, lp.getY());
                Point rPoint = Perspective.localToCanvas(client, adjusted, 0);
                if (rPoint != null)
                    canvasPoints.add(new java.awt.Point(rPoint.getX(), rPoint.getY()));
            }
        }

        for (int x = maxX - 1; x >= minX; x--)
        {
            WorldPoint wp = new WorldPoint(x, minY, plane);
            LocalPoint lp = LocalPoint.fromWorld(client, wp);
            if (lp != null)
            {
                LocalPoint adjusted = new LocalPoint(lp.getX(), lp.getY() - HALF_TILE);
                Point rPoint = Perspective.localToCanvas(client, adjusted, 0);
                if (rPoint != null)
                    canvasPoints.add(new java.awt.Point(rPoint.getX(), rPoint.getY()));
            }
        }

        for (int y = minY + 1; y < maxY; y++)
        {
            WorldPoint wp = new WorldPoint(minX, y, plane);
            LocalPoint lp = LocalPoint.fromWorld(client, wp);
            if (lp != null)
            {
                LocalPoint adjusted = new LocalPoint(lp.getX() - HALF_TILE, lp.getY());
                Point rPoint = Perspective.localToCanvas(client, adjusted, 0);
                if (rPoint != null)
                    canvasPoints.add(new java.awt.Point(rPoint.getX(), rPoint.getY()));
            }
        }

        if (canvasPoints.size() < 3)
        {
            return null;
        }

        Polygon boundary = new Polygon();
        for (java.awt.Point p : canvasPoints)
        {
            boundary.addPoint(p.x, p.y);
        }

        graphics.setColor(config.overlayBorderColour());
        graphics.draw(boundary);

        return null;
    }
}
