package com.fogofwar.fade;
import com.fogofwar.config.FogOfWarConfig;
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
	private int cachedTick = Integer.MIN_VALUE;
	private int cachedFadeDuration = Integer.MIN_VALUE;
	private int cachedBaseRgb;
	private Color cachedColor;
	FadingPlayer(Player player, WorldPoint velocity, WorldPoint initialLocation) {
		this.player = player;
		this.velocity = velocity;
		this.lastLocation = initialLocation;
	}
	float getOpacity(FogOfWarConfig config) {
		float d = Math.max(1, config.fadeDurationTicks());
		return (d - ticksSinceDisappeared) / d;
	}
	Color getColor(FogOfWarConfig config) {
		Color base = config.fadeMarkerColour();
		int duration = config.fadeDurationTicks();
		int baseRgb = base.getRGB();
		if (cachedColor != null && cachedTick == ticksSinceDisappeared && cachedFadeDuration == duration && cachedBaseRgb == baseRgb) return cachedColor;
		float o = getOpacity(config);
		cachedColor = new Color(base.getRed() / 255f, base.getGreen() / 255f, base.getBlue() / 255f, (base.getAlpha() / 255f) * o);
		cachedTick = ticksSinceDisappeared;
		cachedFadeDuration = duration;
		cachedBaseRgb = baseRgb;
		return cachedColor;
	}
}
