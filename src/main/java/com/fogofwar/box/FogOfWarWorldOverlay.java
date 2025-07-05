package com.fogofwar.box;

import com.fogofwar.FogOfWarConfig;
import com.fogofwar.util.ClientState;
import com.fogofwar.util.DynamicRenderDistance;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

public class FogOfWarWorldOverlay extends Overlay {
    private final Client client;
    private final FogOfWarConfig config;
    private final ClientState clientState;
    private final DynamicRenderDistance dynamicRenderDistance;
    @Inject
    public FogOfWarWorldOverlay(Client client, FogOfWarConfig config, ClientState clientState, DynamicRenderDistance dynamicRenderDistance) {
        this.client = client;
        this.config = config;
        this.clientState = clientState;
        this.dynamicRenderDistance = dynamicRenderDistance;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(Overlay.PRIORITY_LOW);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }
    @Override
    public Dimension render(Graphics2D graphics) {
        if (clientState.isClientNotReady()) return null;
        if (config.onlyInWilderness() && clientState.isNotInWilderness()) return null;
        if (config.showWorldFog()) {
            renderWorldFog(graphics);
        }
        if (config.showWorldBorder()) {
            int radius = dynamicRenderDistance.getCurrentRenderDistance();
            WorldPoint centerWp = client.getLocalPlayer().getWorldLocation();
            GeneralPath borderPath = createBorderPath(centerWp, radius);
            if (borderPath != null) {
                renderPath(graphics, borderPath);
            }
        }
        return null;
    }
    private void renderWorldFog(Graphics2D graphics) {
        Rectangle viewport = new Rectangle(
                client.getViewportXOffset(),
                client.getViewportYOffset(),
                client.getViewportWidth(),
                client.getViewportHeight()
        );
        int radius = dynamicRenderDistance.getCurrentRenderDistance();
        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
        GeneralPath renderAreaPath = createRenderAreaBoundary(playerLocation, radius);
        if (renderAreaPath != null) {
            Area screenArea = new Area(viewport);
            Area renderArea = new Area(renderAreaPath);
            screenArea.subtract(renderArea);
            graphics.setColor(config.worldFogColour());
            graphics.fill(screenArea);
        } else {
            graphics.setColor(config.worldFogColour());
            graphics.fill(viewport);
        }
    }
    private GeneralPath createRenderAreaBoundary(WorldPoint center, int radius) {
        List<Point> boundaryPoints = new ArrayList<>();
        int sampleRate = Math.max(1, radius / 16);
        WorldView worldView = client.getTopLevelWorldView();
        addBorderPoint(boundaryPoints, center.getX() - radius, center.getY() + radius, center.getPlane(), -64, 64, worldView);
        for (int x = -radius + sampleRate; x < radius; x += sampleRate) { addBorderPoint(boundaryPoints, center.getX() + x, center.getY() + radius, center.getPlane(), 0, 64, worldView); }
        addBorderPoint(boundaryPoints, center.getX() + radius, center.getY() + radius, center.getPlane(), 64, 64, worldView);
        for (int y = radius - sampleRate; y > -radius; y -= sampleRate) { addBorderPoint(boundaryPoints, center.getX() + radius, center.getY() + y, center.getPlane(), 64, 0, worldView); }
        addBorderPoint(boundaryPoints, center.getX() + radius, center.getY() - radius, center.getPlane(), 64, -64, worldView);
        for (int x = radius - sampleRate; x > -radius; x -= sampleRate) { addBorderPoint(boundaryPoints, center.getX() + x, center.getY() - radius, center.getPlane(), 0, -64, worldView); }
        addBorderPoint(boundaryPoints, center.getX() - radius, center.getY() - radius, center.getPlane(), -64, -64, worldView);
        for (int y = -radius + sampleRate; y < radius; y += sampleRate) { addBorderPoint(boundaryPoints, center.getX() - radius, center.getY() + y, center.getPlane(), -64, 0, worldView); }
        return createPathFromPoints(boundaryPoints);
    }
    private void addBorderPoint(List<Point> points, int worldX, int worldY, int plane, int offsetX, int offsetY, WorldView worldView) {
        WorldPoint wp = new WorldPoint(worldX, worldY, plane);
        LocalPoint lp = LocalPoint.fromWorld(worldView, wp);
        if (lp != null) {
            LocalPoint offsetPoint = lp.plus(offsetX, offsetY);
            Point canvasPoint = Perspective.localToCanvas(client, offsetPoint, plane);
            if (canvasPoint != null) points.add(canvasPoint);
        }
    }
    private GeneralPath createBorderPath(WorldPoint center, int radius) {
        List<Point> borderPoints = new ArrayList<>();
        int sampleRate = Math.max(1, radius / 16);
        WorldView worldView = client.getTopLevelWorldView();
        addBorderPoint(borderPoints, center.getX() - radius, center.getY() + radius, center.getPlane(), -64, 64, worldView);
        for (int x = -radius + sampleRate; x < radius; x += sampleRate) { addBorderPoint(borderPoints, center.getX() + x, center.getY() + radius, center.getPlane(), 0, 64, worldView); }
        addBorderPoint(borderPoints, center.getX() + radius, center.getY() + radius, center.getPlane(), 64, 64, worldView);
        for (int y = radius - sampleRate; y > -radius; y -= sampleRate) { addBorderPoint(borderPoints, center.getX() + radius, center.getY() + y, center.getPlane(), 64, 0, worldView); }
        addBorderPoint(borderPoints, center.getX() + radius, center.getY() - radius, center.getPlane(), 64, -64, worldView);
        for (int x = radius - sampleRate; x > -radius; x -= sampleRate) { addBorderPoint(borderPoints, center.getX() + x, center.getY() - radius, center.getPlane(), 0, -64, worldView); }
        addBorderPoint(borderPoints, center.getX() - radius, center.getY() - radius, center.getPlane(), -64, -64, worldView);
        for (int y = -radius + sampleRate; y < radius; y += sampleRate) { addBorderPoint(borderPoints, center.getX() - radius, center.getY() + y, center.getPlane(), -64, 0, worldView); }
        return createPathFromPoints(borderPoints);
    }
    private GeneralPath createPathFromPoints(List<Point> points) {
        if (points.isEmpty()) return null;
        GeneralPath path = new GeneralPath();
        boolean pathStarted = false;
        for (Point point : points) {
            if (point != null) {
                if (!pathStarted) {
                    path.moveTo(point.getX(), point.getY());
                    pathStarted = true;
                } else {
                    path.lineTo(point.getX(), point.getY());
                }
            }
        }
        if (pathStarted) path.closePath();
        return pathStarted ? path : null;
    }
    private void renderPath(Graphics2D graphics, GeneralPath path) {
        graphics.setColor(config.worldBorderColour());
        graphics.setStroke(new BasicStroke(config.worldBorderThickness()));
        graphics.draw(path);
    }
}