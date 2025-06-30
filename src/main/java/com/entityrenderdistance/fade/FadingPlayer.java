package com.entityrenderdistance.fade;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
@Getter
class FadingPlayer {
	private final Player player;
	@Setter
	private WorldPoint lastLocation;
	private final WorldPoint velocity;
	@Setter
	private int ticksSinceDisappeared = 0;
	FadingPlayer(Player player, WorldPoint velocity) {
		this.player = player;
		this.velocity = velocity;
		this.lastLocation = player.getWorldLocation();
	}
}