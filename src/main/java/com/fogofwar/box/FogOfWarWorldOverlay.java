package com.fogofwar.box;
import com.fogofwar.FogOfWarConfig;
import com.fogofwar.util.AreaManager;
import com.fogofwar.util.CachedStroke;
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
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	private final CachedStroke borderStroke = new CachedStroke();
	private static class CachedHull {
		Shape hull;
		Area area;
		Rectangle bounds;
		int wx, wy, plane, anim, frame, pose, poseFrame;
	}
	private final Map<Actor, CachedHull> hullCache = new IdentityHashMap<>();
	private final Set<Actor> cacheSeen = Collections.newSetFromMap(new IdentityHashMap<>(256));
	private int lastCamX, lastCamY, lastCamZ, lastCamPitch, lastCamYaw;
	private boolean cameraStable;
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
	public void clearCaches() {
		hullCache.clear();
		cacheSeen.clear();
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		updateCameraStable();
		cacheSeen.clear();
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
		Area result = buildExclusion(worldView, boundary);
		if (result == null) {
			graphics.fill(fogPath);
			return;
		}
		Area fogArea = new Area(fogPath);
		fogArea.subtract(result);
		graphics.fill(fogArea);
	}
	private void updateCameraStable() {
		int x = client.getCameraX(), y = client.getCameraY(), z = client.getCameraZ();
		int p = client.getCameraPitch(), w = client.getCameraYaw();
		cameraStable = x == lastCamX && y == lastCamY && z == lastCamZ && p == lastCamPitch && w == lastCamYaw;
		lastCamX = x; lastCamY = y; lastCamZ = z; lastCamPitch = p; lastCamYaw = w;
	}
	private Area buildExclusion(WorldView worldView, GeneralPath boundary) {
		Area result = null;
		for (Actor actor : visibleActorTracker.getVisibleActors()) {
			if (actor == null || actor.getWorldView() != worldView) continue;
			cacheSeen.add(actor);
			CachedHull cached = hullCache.get(actor);
			WorldPoint awp = actor.getWorldLocation();
			int anim = actor.getAnimation(), frame = actor.getAnimationFrame();
			int pose = actor.getPoseAnimation(), poseFrame = actor.getPoseAnimationFrame();
			boolean hit = cached != null && cameraStable && awp != null
					&& cached.wx == awp.getX() && cached.wy == awp.getY() && cached.plane == awp.getPlane()
					&& cached.anim == anim && cached.frame == frame
					&& cached.pose == pose && cached.poseFrame == poseFrame;
			Area entryArea;
			Rectangle bounds;
			if (hit) {
				bounds = cached.bounds;
				if (!viewport.intersects(bounds)) continue;
				if (boundary.contains(bounds)) continue;
				if (cached.area == null) cached.area = new Area(cached.hull);
			} else {
				Shape hull = actor.getConvexHull();
				if (hull == null) {
					if (cached != null) hullCache.remove(actor);
					continue;
				}
				bounds = hull.getBounds();
				if (cached == null) {
					cached = new CachedHull();
					hullCache.put(actor, cached);
				}
				cached.hull = hull;
				cached.bounds = bounds;
				cached.area = null;
				if (awp != null) { cached.wx = awp.getX(); cached.wy = awp.getY(); cached.plane = awp.getPlane(); }
				else { cached.wx = Integer.MIN_VALUE; cached.wy = Integer.MIN_VALUE; cached.plane = Integer.MIN_VALUE; }
				cached.anim = anim; cached.frame = frame;
				cached.pose = pose; cached.poseFrame = poseFrame;
				if (!viewport.intersects(bounds)) continue;
				if (boundary.contains(bounds)) continue;
				cached.area = new Area(hull);
			}
			entryArea = cached.area;
			if (result == null) result = new Area(entryArea);
			else result.add(entryArea);
		}
		hullCache.keySet().retainAll(cacheSeen);
		return result;
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
		graphics.setStroke(borderStroke.get(config.worldBorderThickness()));
		graphics.draw(boundary);
		graphics.setStroke(oldStroke);
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
