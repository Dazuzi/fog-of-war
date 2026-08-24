package com.fogofwar.render;
import com.fogofwar.coord.WorldEntityCoords;
import lombok.Getter;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
public final class RenderCenter {
	@Getter
	private final WorldView worldView;
	@Getter
	private final WorldPoint worldPoint;
	private final LocalPoint center;
	private LocalPoint snappedCenter;
	private WorldPoint snappedWorldPoint;
	@Getter
	private final boolean onWorldEntity;
	private RenderCenter(WorldView worldView, WorldPoint worldPoint, LocalPoint localPoint, LocalPoint targetLocalPoint, boolean onWorldEntity) {
		this.worldView = worldView;
		this.worldPoint = worldPoint;
		this.onWorldEntity = onWorldEntity;
		this.center = onWorldEntity ? targetLocalPoint : localPoint;
	}
	static RenderCenter resolve(Player p, WorldView topWv) {
		if (p == null) return null;
		WorldView pwv = p.getWorldView();
		if (topWv == null) return null;
		if (pwv == null || pwv.isTopLevel()) {
			WorldPoint wp = p.getWorldLocation();
			LocalPoint lp = p.getLocalLocation();
			if (lp == null || lp.getWorldView() != topWv.getId()) lp = wp != null ? LocalPoint.fromWorld(topWv, wp) : null;
			if (wp == null || lp == null) return null;
			return new RenderCenter(topWv, wp, lp, lp, false);
		}
		WorldEntity we = WorldEntityCoords.getPlayerWorldEntity(p, topWv);
		if (we == null) return null;
		if (!WorldEntityCoords.isShip(we)) {
			WorldEntityCoords.ResolvedPoint point = WorldEntityCoords.resolveTopLevel(p, pwv, topWv, we);
			return point != null ? new RenderCenter(topWv, point.worldPoint, point.localPoint, point.localPoint, false) : null;
		}
		LocalPoint boatLp = we.getLocalLocation();
		if (boatLp == null) return null;
		LocalPoint boatTarget = we.getTargetLocation();
		if (boatTarget == null) boatTarget = boatLp;
		WorldPoint boatWp = WorldEntityCoords.toTopLevelWorldPoint(topWv, boatLp);
		return new RenderCenter(topWv, boatWp, boatLp, boatTarget, true);
	}
	public LocalPoint snappedCenter() {
		if (snappedCenter == null) snappedCenter = snapCenter(center, worldView);
		return snappedCenter;
	}
	public WorldPoint getSnappedWorldPoint() {
		LocalPoint snapped = snappedCenter();
		if (snappedWorldPoint == null) snappedWorldPoint = WorldPoint.fromLocal(worldView, snapped.getX(), snapped.getY(), worldPoint.getPlane());
		return snappedWorldPoint;
	}
	private static LocalPoint snapCenter(LocalPoint lp, WorldView worldView) {
		return new LocalPoint(snapAxis(lp.getX()), snapAxis(lp.getY()), worldView);
	}
	private static int snapAxis(int current) { return (current / Perspective.LOCAL_TILE_SIZE) * Perspective.LOCAL_TILE_SIZE + Perspective.LOCAL_HALF_TILE_SIZE; }
}
