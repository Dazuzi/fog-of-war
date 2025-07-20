package com.fogofwar.box;

import com.fogofwar.FogOfWarConfig;
import com.fogofwar.util.AreaManager;
import com.fogofwar.util.ClientState;
import com.fogofwar.util.DynamicRenderDistance;
import net.runelite.api.*;
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
    private final AreaManager areaManager;
    private final List<net.runelite.api.Point> boundaryPoints = new ArrayList<>();
    private final GeneralPath path = new GeneralPath();
    @Inject
    public FogOfWarWorldOverlay(Client client, FogOfWarConfig config, ClientState clientState, DynamicRenderDistance dynamicRenderDistance, AreaManager areaManager) {
        this.client = client;
        this.config = config;
        this.clientState = clientState;
        this.dynamicRenderDistance = dynamicRenderDistance;
        this.areaManager = areaManager;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(Overlay.PRIORITY_LOW);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }
    @Override
    public Dimension render(Graphics2D graphics) {
        if (areaManager.isPlayerInExcludedArea() || clientState.isClientNotReady() || (config.onlyInWilderness() && clientState.isNotInWilderness())) {
            return null;
        }
        int radius = dynamicRenderDistance.getCurrentRenderDistance();
        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
        GeneralPath renderAreaBoundary = createRenderAreaBoundary(playerLocation, radius);
        if (renderAreaBoundary == null) {
            if (config.showWorldFog()) {
                Rectangle viewport = new Rectangle(client.getViewportXOffset(), client.getViewportYOffset(), client.getViewportWidth(), client.getViewportHeight());
                graphics.setColor(config.worldFogColour());
                graphics.fill(viewport);
            }
            return null;
        }
        if (config.showWorldFog()) {
            renderWorldFog(graphics, renderAreaBoundary);
        }
        if (config.showWorldBorder()) {
            renderWorldBorder(graphics, renderAreaBoundary);
        }
        return null;
    }
    private void subtractEntitiesFromFog(Area fogArea) {
        WorldView worldView = client.getTopLevelWorldView();
        for (Player player : worldView.players()) {
            if (player == null || player.equals(client.getLocalPlayer())) continue;
            Shape convexHull = player.getConvexHull();
            if (convexHull != null) {
                fogArea.subtract(new Area(convexHull));
            }
        }
        for (NPC npc : worldView.npcs()) {
            if (npc == null) continue;
            Shape convexHull = npc.getConvexHull();
            if (convexHull != null) {
                fogArea.subtract(new Area(convexHull));
            }
        }
    }
    private void renderWorldFog(Graphics2D graphics, GeneralPath renderAreaBoundary) {
        Rectangle viewport = new Rectangle(
                client.getViewportXOffset(),
                client.getViewportYOffset(),
                client.getViewportWidth(),
                client.getViewportHeight()
        );
        Area screenArea = new Area(viewport);
        screenArea.subtract(new Area(renderAreaBoundary));
        if (config.excludeEntities()) {
            subtractEntitiesFromFog(screenArea);
        }
        graphics.setColor(config.worldFogColour());
        graphics.fill(screenArea);
    }
    private void renderWorldBorder(Graphics2D graphics, GeneralPath renderAreaBoundary) {
        graphics.setColor(config.worldBorderColour());
        graphics.setStroke(new BasicStroke(config.worldBorderThickness()));
        graphics.draw(renderAreaBoundary);
    }
    private GeneralPath createRenderAreaBoundary(WorldPoint centerWp, int radius) {
        LocalPoint centerLp = LocalPoint.fromWorld(client.getTopLevelWorldView(), centerWp);
        if (centerLp == null) return null;
        boundaryPoints.clear();
        int localRadius = radius * 128 + 64;
        int plane = centerWp.getPlane();
        int sampleCount = Math.max(1, radius / 4) * 4;
        int step = (localRadius * 2) / sampleCount;
        for (int i = 0; i < sampleCount; i++) { addPoint(boundaryPoints, centerLp.getX() - localRadius + (i * step), centerLp.getY() + localRadius, plane); }
        for (int i = 0; i < sampleCount; i++) { addPoint(boundaryPoints, centerLp.getX() + localRadius, centerLp.getY() + localRadius - (i * step), plane); }
        for (int i = 0; i < sampleCount; i++) { addPoint(boundaryPoints, centerLp.getX() + localRadius - (i * step), centerLp.getY() - localRadius, plane); }
        for (int i = 0; i < sampleCount; i++) { addPoint(boundaryPoints, centerLp.getX() - localRadius, centerLp.getY() - localRadius + (i * step), plane); }
        return createPathFromPoints(boundaryPoints);
    }
    private void addPoint(List<net.runelite.api.Point> points, int localX, int localY, int plane) {
        LocalPoint lp = new LocalPoint(localX, localY, client.getTopLevelWorldView());
        net.runelite.api.Point canvasPoint = Perspective.localToCanvas(client, lp, plane);
        if (canvasPoint != null) {
            points.add(canvasPoint);
        }
    }
    private GeneralPath createPathFromPoints(List<net.runelite.api.Point> points) {
        if (points.isEmpty()) return null;
        path.reset();
        path.moveTo(points.get(0).getX(), points.get(0).getY());
        for (int i = 1; i < points.size(); i++) {
            path.lineTo(points.get(i).getX(), points.get(i).getY());
        }
        path.closePath();
        return path;
    }
}