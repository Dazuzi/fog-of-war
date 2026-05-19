package com.fogofwar.state;
import net.runelite.api.Player;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
public final class WorldEntityCoords {
	private WorldEntityCoords() {}
	public static WorldEntity getWorldEntity(WorldView sourceWorldView, WorldView topWorldView) {
		if (sourceWorldView == null || topWorldView == null || sourceWorldView.isTopLevel()) return null;
		return topWorldView.worldEntities().byIndex(sourceWorldView.getId());
	}
	public static WorldPoint toTopLevelWorldPoint(WorldView topWorldView, LocalPoint localPoint) {
		if (topWorldView == null || localPoint == null) return null;
		return WorldPoint.fromLocal(topWorldView, localPoint.getX(), localPoint.getY(), topWorldView.getPlane());
	}
	public static WorldPoint playerToTopLevel(Player player, WorldView sourceWorldView, WorldView topWorldView) {
		if (player == null || topWorldView == null) return null;
		if (sourceWorldView == null) sourceWorldView = player.getWorldView();
		if (sourceWorldView == null) return null;
		if (sourceWorldView.isTopLevel()) return player.getWorldLocation();
		WorldEntity worldEntity = getWorldEntity(sourceWorldView, topWorldView);
		if (worldEntity == null) return null;
		LocalPoint localPoint = player.getLocalLocation();
		if (localPoint == null) return null;
		LocalPoint topLocalPoint = worldEntity.transformToMainWorld(localPoint);
		return toTopLevelWorldPoint(topWorldView, topLocalPoint);
	}
}
