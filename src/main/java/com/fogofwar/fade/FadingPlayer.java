package com.fogofwar.fade;
import com.fogofwar.FogOfWarConfig;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import java.awt.Color;
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
	float getOpacity(FogOfWarConfig config) {
		float d = Math.max(1, config.fadeDuration());
		return (d - ticksSinceDisappeared) / d;
	}
	Color getColor(FogOfWarConfig config) {
		Color base = config.fadeColor();
		float o = getOpacity(config);
		return new Color(base.getRed() / 255f, base.getGreen() / 255f, base.getBlue() / 255f, (base.getAlpha() / 255f) * o);
	}
}