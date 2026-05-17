package com.fogofwar.util;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
@Value
public class RenderCenter {
	WorldView worldView;
	WorldPoint worldPoint;
	LocalPoint localPoint;
	boolean onWorldEntity;
	int playerWorldViewId;
	public static RenderCenter resolve(Client client) {
		Player p = client.getLocalPlayer();
		if (p == null) return null;
		WorldView pwv = p.getWorldView();
		WorldView topWv = client.getTopLevelWorldView();
		if (topWv == null) return null;
		if (pwv == null || pwv.isTopLevel()) {
			WorldPoint wp = p.getWorldLocation();
			LocalPoint lp = p.getLocalLocation();
			if (wp == null || lp == null) return null;
			return new RenderCenter(topWv, wp, lp, false, pwv != null ? pwv.getId() : topWv.getId());
		}
		WorldEntity we = topWv.worldEntities().byIndex(pwv.getId());
		if (we == null) return null;
		LocalPoint boatLp = we.getLocalLocation();
		if (boatLp == null) return null;
		WorldPoint boatWp = WorldPoint.fromLocal(topWv, boatLp.getX(), boatLp.getY(), topWv.getPlane());
		return new RenderCenter(topWv, boatWp, boatLp, true, pwv.getId());
	}
}
