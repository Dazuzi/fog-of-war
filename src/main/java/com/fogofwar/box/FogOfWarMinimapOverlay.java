package com.fogofwar.box;
import com.fogofwar.FogOfWarConfig;
import com.fogofwar.util.AreaManager;
import com.fogofwar.util.CachedStroke;
import com.fogofwar.util.ClientState;
import com.fogofwar.util.DynamicRenderDistance;
import com.fogofwar.util.MinimapUtil;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import javax.inject.Inject;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;
public class FogOfWarMinimapOverlay extends Overlay {
	private final Client client;
	private final FogOfWarConfig config;
	private final ClientState clientState;
	private final DynamicRenderDistance dynamicRenderDistance;
	private final AreaManager areaManager;
	private static final int ORB_GROUP_ID = 160;
	private static final int HEALTH_ORB_CHILD_ID = 7;
	private static final int PRAYER_ORB_CHILD_ID = 18;
	private static final int RUN_ORB_CHILD_ID = 26;
	private static final int SPEC_ORB_CHILD_ID = 34;
	private static final int WORLD_MAP_ORB_CHILD_ID = 49;
	private static final int LOCAL_TILE_SIZE = 128;
	private static final int HALF_TILE_OFFSET = 64;
	private static final int[] ORB_CHILD_IDS = {HEALTH_ORB_CHILD_ID, PRAYER_ORB_CHILD_ID, RUN_ORB_CHILD_ID, SPEC_ORB_CHILD_ID, WORLD_MAP_ORB_CHILD_ID};
	private final List<Point> boundaryPoints = new ArrayList<>(128);
	private final GeneralPath renderAreaPath = new GeneralPath();
	private final GeneralPath fullMinimapCoveragePath = new GeneralPath();
	private final CachedStroke borderStroke = new CachedStroke();
	private final Rectangle[] currentOrbBounds = new Rectangle[ORB_CHILD_IDS.length];
	private Rectangle cachedMinimapBounds;
	private Shape cachedClipShape;
	private Area cachedInteriorArea;
	private long cachedOrbsHash;
	private Area cachedFogArea;
	private Area cachedRenderArea;
	private GeneralPath cachedBorderPath;
	private long cachedFogBoundaryHash;
	private Shape cachedFogClipRef;
	@Inject
	public FogOfWarMinimapOverlay(Client client, FogOfWarConfig config, ClientState clientState, DynamicRenderDistance dynamicRenderDistance, AreaManager areaManager) {
		this.client = client;
		this.config = config;
		this.clientState = clientState;
		this.dynamicRenderDistance = dynamicRenderDistance;
		this.areaManager = areaManager;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(Overlay.PRIORITY_LOW);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}
	public void clearCaches() {
		cachedMinimapBounds = null;
		cachedClipShape = null;
		cachedInteriorArea = null;
		cachedFogArea = null;
		cachedRenderArea = null;
		cachedBorderPath = null;
		cachedFogClipRef = null;
		cachedOrbsHash = 0;
		cachedFogBoundaryHash = 0;
		for (int i = 0; i < currentOrbBounds.length; i++) currentOrbBounds[i] = null;
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		if (clientState.isSuppressed(config, areaManager)) return null;
		boolean showFog = config.showMinimapFog();
		boolean showBorder = config.showMinimapBorder();
		if (!showFog && !showBorder) return null;
		Widget minimap = MinimapUtil.getMinimapWidget(client);
		if (minimap == null || minimap.isHidden()) return null;
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null) return null;
		WorldPoint centerWp = client.getLocalPlayer().getWorldLocation();
		if (centerWp == null) return null;
		Shape minimapClipShape = getMinimapClipShape(minimap);
		Shape oldClip = graphics.getClip();
		graphics.setClip(minimapClipShape);
		int radius = dynamicRenderDistance.getCurrentRenderDistance();
		List<Point> points = getBoundaryPointsWithNulls(worldView, centerWp, radius);
		long boundaryHash = hashBoundary(points);
		GeneralPath fogPath = createClippedRenderAreaPath(points, minimap.getBounds());
		if (fogPath == null) {
			graphics.setClip(oldClip);
			return null;
		}
		if (showFog) renderMinimapFog(graphics, minimapClipShape, fogPath, boundaryHash);
		if (showBorder) renderMinimapBorder(graphics, minimapClipShape, fogPath, boundaryHash);
		graphics.setClip(oldClip);
		return null;
	}
	private long hashBoundary(List<Point> pts) {
		long h = 1469598103934665603L;
		for (Point p : pts) {
			long v = (p == null) ? 0xDEADBEEFL : (((long) p.getX()) << 32) | (p.getY() & 0xFFFFFFFFL);
			h ^= v;
			h *= 1099511628211L;
		}
		return h;
	}
	private Shape getMinimapClipShape(Widget minimapWidget) {
		Rectangle bounds = minimapWidget.getBounds();
		long orbsHash = collectOrbBoundsAndHash();
		if (cachedClipShape != null && bounds.equals(cachedMinimapBounds) && orbsHash == cachedOrbsHash) return cachedClipShape;
		Area clipArea = new Area(new Ellipse2D.Double(bounds.getX() - 1, bounds.getY() - 1, bounds.getWidth() + 2, bounds.getHeight() + 2));
		Area interiorArea = new Area(new Ellipse2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight()));
		for (Rectangle ob : currentOrbBounds) {
			if (ob == null) continue;
			Area orbArea = new Area(new Ellipse2D.Double(ob.getX(), ob.getY(), ob.getWidth(), ob.getHeight()));
			clipArea.subtract(orbArea);
			interiorArea.subtract(orbArea);
		}
		cachedClipShape = clipArea;
		cachedInteriorArea = interiorArea;
		cachedMinimapBounds = new Rectangle(bounds);
		cachedOrbsHash = orbsHash;
		return cachedClipShape;
	}
	private long collectOrbBoundsAndHash() {
		long h = 1469598103934665603L;
		for (int i = 0; i < ORB_CHILD_IDS.length; i++) {
			Widget orb = client.getWidget(ORB_GROUP_ID, ORB_CHILD_IDS[i]);
			Rectangle b = (orb != null && !orb.isHidden()) ? orb.getBounds() : null;
			currentOrbBounds[i] = b;
			if (b == null) h ^= 0xDEADBEEFL;
			else h ^= ((long) b.x << 48) ^ ((long) b.y << 32) ^ ((long) b.width << 16) ^ (b.height & 0xFFFFL);
			h *= 1099511628211L;
		}
		return h;
	}
	private void renderMinimapBorder(Graphics2D graphics, Shape minimapClipShape, GeneralPath path, long boundaryHash) {
		if (path.contains(minimapClipShape.getBounds2D())) return;
		ensureAreasCached(minimapClipShape, path, boundaryHash);
		if (cachedFogArea.isEmpty()) return;
		Stroke oldStroke = graphics.getStroke();
		graphics.setColor(config.minimapBorderColour());
		graphics.setStroke(borderStroke.get(config.minimapBorderThickness()));
		graphics.draw(cachedBorderPath);
		graphics.setStroke(oldStroke);
	}
	private void renderMinimapFog(Graphics2D graphics, Shape minimapClipShape, GeneralPath path, long boundaryHash) {
		if (path.contains(minimapClipShape.getBounds2D())) return;
		ensureAreasCached(minimapClipShape, path, boundaryHash);
		graphics.setColor(config.minimapFogColour());
		graphics.fill(cachedFogArea);
	}
	private void ensureAreasCached(Shape clip, GeneralPath path, long hash) {
		if (cachedFogArea != null && hash == cachedFogBoundaryHash && clip == cachedFogClipRef) return;
		Area pathArea = new Area(path);
		Area clipArea = new Area(clip);
		Area fog = (Area) clipArea.clone();
		fog.subtract(pathArea);
		pathArea.intersect(cachedInteriorArea);
		cachedFogArea = fog;
		cachedRenderArea = pathArea;
		cachedBorderPath = extractLineSegments(pathArea);
		cachedFogBoundaryHash = hash;
		cachedFogClipRef = clip;
	}
	private GeneralPath extractLineSegments(Area area) {
		GeneralPath result = new GeneralPath();
		PathIterator iter = area.getPathIterator(null);
		double[] coords = new double[6];
		double curX = 0, curY = 0;
		boolean penDown = false;
		while (!iter.isDone()) {
			int seg = iter.currentSegment(coords);
			switch (seg) {
				case PathIterator.SEG_MOVETO:
					curX = coords[0]; curY = coords[1];
					penDown = false;
					break;
				case PathIterator.SEG_LINETO:
					if (!penDown) { result.moveTo(curX, curY); penDown = true; }
					result.lineTo(coords[0], coords[1]);
					curX = coords[0]; curY = coords[1];
					break;
				case PathIterator.SEG_QUADTO:
					curX = coords[2]; curY = coords[3];
					penDown = false;
					break;
				case PathIterator.SEG_CUBICTO:
					curX = coords[4]; curY = coords[5];
					penDown = false;
					break;
				case PathIterator.SEG_CLOSE:
					penDown = false;
					break;
			}
			iter.next();
		}
		return result;
	}
	private GeneralPath createClippedRenderAreaPath(List<Point> points, Rectangle minimapBounds) {
		int n = points.size();
		int firstVisible = -1;
		int visibleCount = 0;
		for (int i = 0; i < n; i++) {
			if (points.get(i) != null) {
				if (firstVisible == -1) firstVisible = i;
				visibleCount++;
			}
		}
		if (visibleCount == 0) return createFullMinimapCoveragePath(minimapBounds);
		GeneralPath path = renderAreaPath;
		path.reset();
		if (visibleCount == n) {
			Point first = points.get(0);
			path.moveTo(first.getX(), first.getY());
			for (int i = 1; i < n; i++) {
				Point p = points.get(i);
				path.lineTo(p.getX(), p.getY());
			}
			path.closePath();
			return path;
		}
		path.moveTo(points.get(firstVisible).getX(), points.get(firstVisible).getY());
		for (int i = 0; i < n; i++) {
			int currentIndex = (firstVisible + i) % n;
			int nextIndex = (firstVisible + i + 1) % n;
			Point p1 = points.get(currentIndex);
			Point p2 = points.get(nextIndex);
			if (p1 != null) {
				if (p2 != null) {
					path.lineTo(p2.getX(), p2.getY());
				} else {
					int nextVisibleIndex = -1;
					for (int j = 2; j < n; j++) {
						if (points.get((currentIndex + j) % n) != null) {
							nextVisibleIndex = (currentIndex + j) % n;
							break;
						}
					}
					if (nextVisibleIndex != -1) addArcToPath(path, p1, points.get(nextVisibleIndex), minimapBounds);
				}
			}
		}
		path.closePath();
		return path;
	}
	private void addArcToPath(GeneralPath path, Point p1, Point p2, Rectangle minimapBounds) {
		double centerX = minimapBounds.getCenterX();
		double centerY = minimapBounds.getCenterY();
		double radius = minimapBounds.width / 2.0 + 1;
		double startAngle = Math.toDegrees(Math.atan2(p1.getY() - centerY, p1.getX() - centerX));
		double endAngle = Math.toDegrees(Math.atan2(p2.getY() - centerY, p2.getX() - centerX));
		double sweep = endAngle - startAngle;
		if (sweep <= -180) { sweep += 360; } else if (sweep > 180) { sweep -= 360; }
		int numSteps = (int) (Math.abs(sweep) / 10) + 1;
		for (int i = 1; i <= numSteps; i++) {
			double currentAngleRad = Math.toRadians(startAngle + (sweep * i / numSteps));
			path.lineTo((float) (centerX + radius * Math.cos(currentAngleRad)), (float) (centerY + radius * Math.sin(currentAngleRad)));
		}
	}
	private List<Point> getBoundaryPointsWithNulls(WorldView worldView, WorldPoint center, int radius) {
		boundaryPoints.clear();
		LocalPoint centerLp = LocalPoint.fromWorld(worldView, center);
		if (centerLp == null) return boundaryPoints;
		int sampleRate = Math.max(1, radius / 12);
		addMinimapPoint(worldView, center, centerLp, -radius, -radius, -HALF_TILE_OFFSET, -HALF_TILE_OFFSET);
		for (int x = -radius; x <= radius; x += sampleRate) addMinimapPoint(worldView, center, centerLp, x, -radius, 0, -HALF_TILE_OFFSET);
		addMinimapPoint(worldView, center, centerLp, radius, -radius, HALF_TILE_OFFSET, -HALF_TILE_OFFSET);
		for (int y = -radius; y <= radius; y += sampleRate) addMinimapPoint(worldView, center, centerLp, radius, y, HALF_TILE_OFFSET, 0);
		addMinimapPoint(worldView, center, centerLp, radius, radius, HALF_TILE_OFFSET, HALF_TILE_OFFSET);
		for (int x = radius; x >= -radius; x -= sampleRate) addMinimapPoint(worldView, center, centerLp, x, radius, 0, HALF_TILE_OFFSET);
		addMinimapPoint(worldView, center, centerLp, -radius, radius, -HALF_TILE_OFFSET, HALF_TILE_OFFSET);
		for (int y = radius; y >= -radius; y -= sampleRate) addMinimapPoint(worldView, center, centerLp, -radius, y, -HALF_TILE_OFFSET, 0);
		return boundaryPoints;
	}
	private void addMinimapPoint(WorldView worldView, WorldPoint centerWp, LocalPoint centerLp, int tileXOffset, int tileYOffset, int xOffset, int yOffset) {
		if (!WorldPoint.isInScene(worldView, centerWp.getX() + tileXOffset, centerWp.getY() + tileYOffset)) {
			boundaryPoints.add(null);
			return;
		}
		int x = centerLp.getX() + tileXOffset * LOCAL_TILE_SIZE + xOffset;
		int y = centerLp.getY() + tileYOffset * LOCAL_TILE_SIZE + yOffset;
		boundaryPoints.add(Perspective.localToMinimap(client, new LocalPoint(x, y, worldView)));
	}
	private GeneralPath createFullMinimapCoveragePath(Rectangle minimapBounds) {
		fullMinimapCoveragePath.reset();
		double centerX = minimapBounds.getCenterX();
		double centerY = minimapBounds.getCenterY();
		double radius = Math.max(minimapBounds.width, minimapBounds.height);
		int numSegments = 32;
		for (int i = 0; i < numSegments; i++) {
			double angle = 2 * Math.PI * i / numSegments;
			double x = centerX + radius * Math.cos(angle);
			double y = centerY + radius * Math.sin(angle);
			if (i == 0) fullMinimapCoveragePath.moveTo(x, y);
			else fullMinimapCoveragePath.lineTo(x, y);
		}
		fullMinimapCoveragePath.closePath();
		return fullMinimapCoveragePath;
	}
}
