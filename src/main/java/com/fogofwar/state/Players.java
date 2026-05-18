package com.fogofwar.state;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
public final class Players {
	private Players() {}
	public static int count(WorldView wv) {
		if (wv == null) return 0;
		int n = 0;
		for (Player p : wv.players()) if (p != null) n++;
		return n;
	}
}
