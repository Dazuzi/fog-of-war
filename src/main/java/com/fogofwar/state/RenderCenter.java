package com.fogofwar.state;
import lombok.Getter;
import net.runelite.api.Client;
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
	private final LocalPoint localPoint;
	private final LocalPoint targetLocalPoint;
	@Getter
	private final boolean onWorldEntity;
	private RenderCenter(WorldView worldView, WorldPoint worldPoint, LocalPoint localPoint, LocalPoint targetLocalPoint, boolean onWorldEntity) {
		this.worldView = worldView;
		this.worldPoint = worldPoint;
		this.localPoint = localPoint;
		this.targetLocalPoint = targetLocalPoint;
		this.onWorldEntity = onWorldEntity;
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
			return new RenderCenter(topWv, wp, lp, lp, false);
		}
		WorldEntity we = WorldEntityCoords.getWorldEntity(pwv, topWv);
		if (we == null) return null;
		LocalPoint boatLp = we.getLocalLocation();
		if (boatLp == null) return null;
		LocalPoint boatTarget = we.getTargetLocation();
		if (boatTarget == null) boatTarget = boatLp;
		WorldPoint boatWp = WorldEntityCoords.toTopLevelWorldPoint(topWv, boatLp);
		return new RenderCenter(topWv, boatWp, boatLp, boatTarget, true);
	}
	public LocalPoint snappedCenter() {
		LocalPoint lp = onWorldEntity ? targetLocalPoint : localPoint;
		return new LocalPoint(snapAxis(lp.getX()), snapAxis(lp.getY()), worldView);
	}
	private static int snapAxis(int current) { return (current / Perspective.LOCAL_TILE_SIZE) * Perspective.LOCAL_TILE_SIZE + Perspective.LOCAL_HALF_TILE_SIZE; }
}
