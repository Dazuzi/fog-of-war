package com.fogofwar.render;
import com.fogofwar.coord.WorldEntityCoords;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
public final class RenderCenter {
	public static final int MINIMAP_PROJECTION_DISTANCE = 32768;
	@Getter
	private final WorldView worldView;
	@Getter
	private final WorldPoint worldPoint;
	private final LocalPoint snappedCenter;
	@Getter
	private final WorldPoint snappedWorldPoint;
	@Getter
	private final Point canvasCenterPoint;
	@Getter
	private final Point minimapCenterPoint;
	@Getter
	private final boolean onWorldEntity;
	private RenderCenter(Client client, WorldView worldView, WorldPoint worldPoint, LocalPoint localPoint, LocalPoint targetLocalPoint, boolean onWorldEntity) {
		this.worldView = worldView;
		this.worldPoint = worldPoint;
		this.onWorldEntity = onWorldEntity;
		this.snappedCenter = snapCenter(onWorldEntity ? targetLocalPoint : localPoint, worldView);
		this.snappedWorldPoint = WorldPoint.fromLocal(worldView, snappedCenter.getX(), snappedCenter.getY(), worldPoint.getPlane());
		this.canvasCenterPoint = Perspective.localToCanvas(client, snappedCenter, worldPoint.getPlane());
		this.minimapCenterPoint = Perspective.localToMinimap(client, snappedCenter, MINIMAP_PROJECTION_DISTANCE);
	}
	public static RenderCenter resolve(Client client) {
		Player p = client.getLocalPlayer();
		if (p == null) return null;
		WorldView pwv = p.getWorldView();
		WorldView topWv = client.getTopLevelWorldView();
		if (topWv == null) return null;
		if (pwv == null || pwv.isTopLevel()) {
			WorldPoint wp = p.getWorldLocation();
			LocalPoint lp = wp != null ? LocalPoint.fromWorld(topWv, wp) : null;
			if (wp == null || lp == null) return null;
			return new RenderCenter(client, topWv, wp, lp, lp, false);
		}
		WorldEntity we = WorldEntityCoords.getPlayerWorldEntity(p, topWv);
		if (we == null) return null;
		if (!WorldEntityCoords.isShip(we)) {
			WorldEntityCoords.ResolvedPoint point = WorldEntityCoords.resolveTopLevel(p, pwv, topWv, we);
			return point != null ? new RenderCenter(client, topWv, point.worldPoint, point.localPoint, point.localPoint, false) : null;
		}
		LocalPoint boatLp = we.getLocalLocation();
		if (boatLp == null) return null;
		LocalPoint boatTarget = we.getTargetLocation();
		if (boatTarget == null) boatTarget = boatLp;
		WorldPoint boatWp = WorldEntityCoords.toTopLevelWorldPoint(topWv, boatLp);
		return new RenderCenter(client, topWv, boatWp, boatLp, boatTarget, true);
	}
	public LocalPoint snappedCenter() { return snappedCenter; }
	private static LocalPoint snapCenter(LocalPoint lp, WorldView worldView) {
		return new LocalPoint(snapAxis(lp.getX()), snapAxis(lp.getY()), worldView);
	}
	private static int snapAxis(int current) { return (current / Perspective.LOCAL_TILE_SIZE) * Perspective.LOCAL_TILE_SIZE + Perspective.LOCAL_HALF_TILE_SIZE; }
}
