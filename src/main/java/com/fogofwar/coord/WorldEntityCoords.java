package com.fogofwar.coord;
import net.runelite.api.Actor;
import net.runelite.api.Player;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldEntityConfig;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
public final class WorldEntityCoords {
	private static final int SHIP_ENTITY_CATEGORY = 2395;
	private WorldEntityCoords() {}
	public static WorldEntity getWorldEntity(WorldView sourceWorldView, WorldView topWorldView) {
		if (sourceWorldView == null || topWorldView == null || sourceWorldView.isTopLevel()) return null;
		return topWorldView.worldEntities().byIndex(sourceWorldView.getId());
	}
	public static WorldEntity getPlayerWorldEntity(Player player, WorldView topWorldView) {
		if (player == null) return null;
		return getWorldEntity(player.getWorldView(), topWorldView);
	}
	public static WorldPoint toTopLevelWorldPoint(WorldView topWorldView, LocalPoint localPoint) {
		if (topWorldView == null || localPoint == null) return null;
		return WorldPoint.fromLocal(topWorldView, localPoint.getX(), localPoint.getY(), topWorldView.getPlane());
	}
	private static WorldEntity getPlayerShip(Player player, WorldView topWorldView) {
		WorldEntity worldEntity = getPlayerWorldEntity(player, topWorldView);
		return isShip(worldEntity) ? worldEntity : null;
	}
	public static boolean isPlayerOnShip(Player player, WorldView topWorldView) { return getPlayerShip(player, topWorldView) != null; }
	public static boolean isShip(WorldEntity worldEntity) {
		WorldEntityConfig config = worldEntity != null ? worldEntity.getConfig() : null;
		return config != null && config.getCategory() == SHIP_ENTITY_CATEGORY;
	}
	public static ResolvedPoint resolveTopLevel(Actor actor, WorldView topWorldView) { return resolveTopLevel(actor, null, topWorldView, null); }
	public static ResolvedPoint resolveTopLevel(Actor actor, WorldView sourceWorldView, WorldView topWorldView, WorldEntity worldEntity) {
		if (actor == null || topWorldView == null) return null;
		if (sourceWorldView == null) sourceWorldView = actor.getWorldView();
		if (sourceWorldView == null) return null;
		WorldPoint worldPoint = actor.getWorldLocation();
		LocalPoint localPoint = actor.getLocalLocation();
		if (sourceWorldView.isTopLevel()) {
			if (worldPoint == null) return null;
			if (localPoint == null) localPoint = LocalPoint.fromWorld(topWorldView, worldPoint);
			return localPoint != null ? new ResolvedPoint(worldPoint, localPoint) : null;
		}
		if (worldEntity == null) worldEntity = getWorldEntity(sourceWorldView, topWorldView);
		if (worldEntity == null) return null;
		if (localPoint == null && worldPoint != null) localPoint = LocalPoint.fromWorld(sourceWorldView, worldPoint);
		if (localPoint == null) return null;
		LocalPoint topLocalPoint = worldEntity.transformToMainWorld(localPoint);
		WorldPoint topWorldPoint = toTopLevelWorldPoint(topWorldView, topLocalPoint);
		return topWorldPoint != null ? new ResolvedPoint(topWorldPoint, topLocalPoint) : null;
	}
	public static WorldPoint playerToTopLevel(Player player, WorldView sourceWorldView, WorldView topWorldView) {
		ResolvedPoint point = resolveTopLevel(player, sourceWorldView, topWorldView, null);
		return point != null ? point.worldPoint : null;
	}
	public static final class ResolvedPoint {
		public final WorldPoint worldPoint;
		public final LocalPoint localPoint;
		private ResolvedPoint(WorldPoint worldPoint, LocalPoint localPoint) {
			this.worldPoint = worldPoint;
			this.localPoint = localPoint;
		}
	}
}
