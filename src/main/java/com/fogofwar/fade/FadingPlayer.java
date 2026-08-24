package com.fogofwar.fade;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import java.awt.Color;
@Getter
class FadingPlayer {
	private final Player player;
	@Setter
	private WorldPoint markerLocation;
	private final WorldPoint velocity;
	private final int renderDistance;
	@Setter
	private int ticksSinceDisappeared = 0;
	private int cachedTick = Integer.MIN_VALUE;
	private int cachedFadeDuration = Integer.MIN_VALUE;
	private int cachedBaseRgb;
	private Color cachedColor;
	private Color cachedDarkerColor;
	private WorldView cachedWorldView;
	private WorldPoint cachedLocalMarker;
	private LocalPoint cachedLocalPoint;
	FadingPlayer(Player player, WorldPoint velocity, WorldPoint initialLocation, int renderDistance) {
		this.player = player;
		this.velocity = velocity;
		this.markerLocation = initialLocation;
		this.renderDistance = renderDistance;
	}
	Color getColor(Color base, int duration) {
		int baseRgb = base.getRGB();
		if (cachedColor != null && cachedTick == ticksSinceDisappeared && cachedFadeDuration == duration && cachedBaseRgb == baseRgb) return cachedColor;
		float d = Math.max(1, duration);
		float o = (d - ticksSinceDisappeared) / d;
		cachedColor = new Color(base.getRed() / 255f, base.getGreen() / 255f, base.getBlue() / 255f, (base.getAlpha() / 255f) * o);
		cachedDarkerColor = null;
		cachedTick = ticksSinceDisappeared;
		cachedFadeDuration = duration;
		cachedBaseRgb = baseRgb;
		return cachedColor;
	}
	Color getDarkerColor(Color base, int duration) {
		getColor(base, duration);
		if (cachedDarkerColor == null) cachedDarkerColor = cachedColor.darker();
		return cachedDarkerColor;
	}
	LocalPoint getLocalPoint(WorldView worldView) {
		if (cachedWorldView != worldView || cachedLocalMarker != markerLocation) {
			cachedWorldView = worldView;
			cachedLocalMarker = markerLocation;
			cachedLocalPoint = LocalPoint.fromWorld(worldView, markerLocation);
		}
		return cachedLocalPoint;
	}
}
