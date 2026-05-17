package com.fogofwar.util;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import javax.inject.Inject;
import javax.inject.Singleton;
@Slf4j
@Singleton
public class SailingDiagnostics {
	private int lastWvId = Integer.MIN_VALUE;
	private long lastSampleMs;
	private int boundaryNullStreak;
	@Inject
	public SailingDiagnostics() {}
	public void observe(Client client, RenderCenter rc) {
		int wvId = rc.getPlayerWorldViewId();
		if (wvId != lastWvId) {
			logTransition(client, rc, wvId);
			lastWvId = wvId;
			boundaryNullStreak = 0;
		}
		if (!rc.isOnWorldEntity()) return;
		long now = System.currentTimeMillis();
		if (now - lastSampleMs < 2000) return;
		lastSampleMs = now;
		logSample(client, rc);
	}
	public void boundaryNull(RenderCenter rc) {
		boundaryNullStreak++;
		if (boundaryNullStreak == 1 || boundaryNullStreak % 60 == 0) {
			log.debug("[fogofwar] boundary null (streak={}) onWE={} wvId={} plane={} centerWP={} centerLP=({},{}) sceneLP=({},{})",
					boundaryNullStreak, rc.isOnWorldEntity(), rc.getPlayerWorldViewId(),
					rc.getWorldPoint().getPlane(),
					formatWp(rc.getWorldPoint()),
					rc.getLocalPoint().getX(), rc.getLocalPoint().getY(),
					rc.getLocalPoint().getSceneX(), rc.getLocalPoint().getSceneY());
		}
	}
	private void logTransition(Client client, RenderCenter rc, int newWvId) {
		WorldView topWv = client.getTopLevelWorldView();
		Player p = client.getLocalPlayer();
		LocalPoint playerLp = p != null ? p.getLocalLocation() : null;
		WorldPoint playerWp = p != null ? p.getWorldLocation() : null;
		WorldEntity we = (topWv != null && rc.isOnWorldEntity()) ? topWv.worldEntities().byIndex(newWvId) : null;
		LocalPoint weLp = we != null ? we.getLocalLocation() : null;
		int weOrient = we != null ? we.getOrientation() : 0;
		log.info("[fogofwar] WV transition prev={} new={} onWE={} topWvId={} topPlane={} topBase=({},{}) topSize=({},{}) topIsInstance={} playerWP={} playerLP=({},{}) playerSceneLP=({},{}) playerWvIsInstance={} boatLP={} boatOrient={} boatWP={} renderCenterWP={} renderCenterLP=({},{})",
				lastWvId, newWvId, rc.isOnWorldEntity(),
				topWv != null ? topWv.getId() : -2,
				topWv != null ? topWv.getPlane() : -2,
				topWv != null ? topWv.getBaseX() : -2, topWv != null ? topWv.getBaseY() : -2,
				topWv != null ? topWv.getSizeX() : -2, topWv != null ? topWv.getSizeY() : -2,
				topWv != null && topWv.isInstance(),
				formatWp(playerWp),
				playerLp != null ? playerLp.getX() : -2, playerLp != null ? playerLp.getY() : -2,
				playerLp != null ? playerLp.getSceneX() : -2, playerLp != null ? playerLp.getSceneY() : -2,
				p != null && p.getWorldView() != null && p.getWorldView().isInstance(),
				weLp != null ? "(" + weLp.getX() + "," + weLp.getY() + ")" : "null",
				weOrient,
				we != null ? formatWp(WorldPoint.fromLocal(topWv, weLp.getX(), weLp.getY(), topWv.getPlane())) : "null",
				formatWp(rc.getWorldPoint()),
				rc.getLocalPoint().getX(), rc.getLocalPoint().getY());
	}
	private void logSample(Client client, RenderCenter rc) {
		WorldView topWv = client.getTopLevelWorldView();
		WorldEntity we = topWv != null ? topWv.worldEntities().byIndex(rc.getPlayerWorldViewId()) : null;
		LocalPoint weLp = we != null ? we.getLocalLocation() : null;
		int weOrient = we != null ? we.getOrientation() : 0;
		Player p = client.getLocalPlayer();
		LocalPoint playerLp = p != null ? p.getLocalLocation() : null;
		WorldPoint playerWp = p != null ? p.getWorldLocation() : null;
		log.debug("[fogofwar] sailing tick boatLP={} boatOrient={} cam=({},{},{}) pitch={} yaw={} playerWP={} playerSceneLP=({},{}) renderCenterWP={} renderCenterLP=({},{})",
				weLp != null ? "(" + weLp.getX() + "," + weLp.getY() + ")" : "null",
				weOrient,
				client.getCameraX(), client.getCameraY(), client.getCameraZ(),
				client.getCameraPitch(), client.getCameraYaw(),
				formatWp(playerWp),
				playerLp != null ? playerLp.getSceneX() : -2, playerLp != null ? playerLp.getSceneY() : -2,
				formatWp(rc.getWorldPoint()),
				rc.getLocalPoint().getX(), rc.getLocalPoint().getY());
	}
	private static String formatWp(WorldPoint wp) {
		return wp != null ? "(" + wp.getX() + "," + wp.getY() + "," + wp.getPlane() + ")" : "null";
	}
}
