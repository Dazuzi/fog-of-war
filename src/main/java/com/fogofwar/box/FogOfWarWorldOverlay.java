package com.fogofwar.box;
import com.fogofwar.FogOfWarConfig;
import com.fogofwar.util.AreaManager;
import com.fogofwar.util.ClientState;
import com.fogofwar.util.DynamicRenderDistance;
import com.fogofwar.util.VisibleActorTracker;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;
public class FogOfWarWorldOverlay extends Overlay {
	private static final int LOCAL_TILE_SIZE = 128;
	private static final int HALF_TILE_SIZE = 64;
	private final Client client;
	private final FogOfWarConfig config;
	private final ClientState clientState;
	private final DynamicRenderDistance dynamicRenderDistance;
	private final AreaManager areaManager;
	private final VisibleActorTracker visibleActorTracker;
	private final Rectangle viewport = new Rectangle();
	private final List<Point> boundaryPoints = new ArrayList<>(256);
	private final GeneralPath renderAreaBoundary = new GeneralPath();
	private final GeneralPath fogPath = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
	private BasicStroke borderStroke;
	private int borderStrokeWidth = -1;
	@Inject
	public FogOfWarWorldOverlay(Client client, FogOfWarConfig config, ClientState clientState, DynamicRenderDistance dynamicRenderDistance, AreaManager areaManager, VisibleActorTracker visibleActorTracker) {
		this.client = client;
		this.config = config;
		this.clientState = clientState;
		this.dynamicRenderDistance = dynamicRenderDistance;
		this.areaManager = areaManager;
		this.visibleActorTracker = visibleActorTracker;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(Overlay.PRIORITY_LOW);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		if (clientState.isSuppressed(config, areaManager)) return null;
		boolean showFog = config.showWorldFog();
		boolean showBorder = config.showWorldBorder();
		if (!showFog && !showBorder) return null;
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null) return null;
		WorldPoint playerLocation = localPlayer.getWorldLocation();
		if (playerLocation == null) return null;
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null) return null;
		int radius = dynamicRenderDistance.getCurrentRenderDistance();
		GeneralPath boundary = createRenderAreaBoundary(worldView, playerLocation, radius);
		setViewportBounds();
		if (boundary == null) {
			if (showFog) {
				graphics.setColor(config.worldFogColour());
				graphics.fill(viewport);
			}
			return null;
		}
		if (showFog) renderWorldFog(graphics, worldView, boundary);
		if (showBorder) renderWorldBorder(graphics, boundary);
		return null;
	}
	private void setViewportBounds() {
		viewport.setBounds(client.getViewportXOffset(), client.getViewportYOffset(), client.getViewportWidth(), client.getViewportHeight());
	}
	private void renderWorldFog(Graphics2D graphics, WorldView worldView, GeneralPath boundary) {
		if (boundary.contains(viewport)) return;
		createFogPath(boundary);
		graphics.setColor(config.worldFogColour());
		if (!config.excludeEntities()) {
			graphics.fill(fogPath);
			return;
		}
		Area fogArea = null;
		for (Actor actor : visibleActorTracker.getVisibleActors()) fogArea = subtractVisibleActorFromFog(fogArea, boundary, worldView, actor);
		graphics.fill(fogArea != null ? fogArea : fogPath);
	}
	private Area subtractVisibleActorFromFog(Area fogArea, GeneralPath boundary, WorldView worldView, Actor actor) {
		if (actor == null) return fogArea;
		if (actor.getWorldView() != worldView) return fogArea;
		Shape hull = actor.getConvexHull();
		if (hull == null) return fogArea;
		Rectangle bounds = hull.getBounds();
		if (!viewport.intersects(bounds) || boundary.contains(bounds)) return fogArea;
		if (fogArea == null) fogArea = new Area(fogPath);
		fogArea.subtract(new Area(hull));
		return fogArea;
	}
	private void createFogPath(GeneralPath boundary) {
		fogPath.reset();
		fogPath.moveTo(viewport.x, viewport.y);
		fogPath.lineTo(viewport.x + viewport.width, viewport.y);
		fogPath.lineTo(viewport.x + viewport.width, viewport.y + viewport.height);
		fogPath.lineTo(viewport.x, viewport.y + viewport.height);
		fogPath.closePath();
		fogPath.append(boundary, false);
	}
	private void renderWorldBorder(Graphics2D graphics, GeneralPath boundary) {
		Stroke oldStroke = graphics.getStroke();
		graphics.setColor(config.worldBorderColour());
		graphics.setStroke(getBorderStroke());
		graphics.draw(boundary);
		graphics.setStroke(oldStroke);
	}
	private BasicStroke getBorderStroke() {
		int width = config.worldBorderThickness();
		if (borderStroke == null || borderStrokeWidth != width) {
			borderStroke = new BasicStroke(width);
			borderStrokeWidth = width;
		}
		return borderStroke;
	}
	private GeneralPath createRenderAreaBoundary(WorldView worldView, WorldPoint centerWp, int radius) {
		LocalPoint centerLp = LocalPoint.fromWorld(worldView, centerWp);
		if (centerLp == null) return null;
		boundaryPoints.clear();
		int localRadius = radius * LOCAL_TILE_SIZE + HALF_TILE_SIZE;
		int plane = centerWp.getPlane();
		int sampleCount = Math.max(1, radius / 4) * 4;
		int step = localRadius * 2 / sampleCount;
		int minX = centerLp.getX() - localRadius;
		int maxX = centerLp.getX() + localRadius;
		int minY = centerLp.getY() - localRadius;
		int maxY = centerLp.getY() + localRadius;
		for (int i = 0; i < sampleCount; i++) addPoint(worldView, boundaryPoints, minX + i * step, maxY, plane);
		for (int i = 0; i < sampleCount; i++) addPoint(worldView, boundaryPoints, maxX, maxY - i * step, plane);
		for (int i = 0; i < sampleCount; i++) addPoint(worldView, boundaryPoints, maxX - i * step, minY, plane);
		for (int i = 0; i < sampleCount; i++) addPoint(worldView, boundaryPoints, minX, minY + i * step, plane);
		return createPathFromPoints(boundaryPoints);
	}
	private void addPoint(WorldView worldView, List<Point> points, int localX, int localY, int plane) {
		LocalPoint lp = new LocalPoint(localX, localY, worldView);
		Point canvasPoint = Perspective.localToCanvas(client, lp, plane);
		if (canvasPoint != null) points.add(canvasPoint);
	}
	private GeneralPath createPathFromPoints(List<Point> points) {
		if (points.size() < 3) return null;
		renderAreaBoundary.reset();
		Point first = points.get(0);
		renderAreaBoundary.moveTo(first.getX(), first.getY());
		for (int i = 1; i < points.size(); i++) {
			Point point = points.get(i);
			renderAreaBoundary.lineTo(point.getX(), point.getY());
		}
		renderAreaBoundary.closePath();
		return renderAreaBoundary;
	}
}
