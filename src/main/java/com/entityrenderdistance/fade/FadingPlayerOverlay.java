package com.entityrenderdistance.fade;
import com.entityrenderdistance.EntityRenderDistanceConfig;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Varbits;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
public class FadingPlayerOverlay extends Overlay {
	private final Client client;
	private final EntityRenderDistanceConfig config;
	private final FadingPlayerManager manager;
	@Inject
	protected FadingPlayerOverlay(Client client, EntityRenderDistanceConfig config, FadingPlayerManager manager) {
		this.client = client;
		this.config = config;
		this.manager = manager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		if (!config.enableFadingPlayers() || !config.showFadingInWorld() || isClientNotReady()) return null;
		if (config.onlyInWilderness() && !isInWilderness()) return null;
		for (FadingPlayer fadingPlayer : manager.getFadingPlayers().values()) {
			renderFadingPlayer(graphics, fadingPlayer);
		}
		return null;
	}
	private void renderFadingPlayer(Graphics2D graphics, FadingPlayer fadingPlayer) {
		WorldPoint wp = fadingPlayer.getLastLocation();
		LocalPoint lp = LocalPoint.fromWorld(client, wp);
		if (lp == null) return;
		Polygon poly = Perspective.getCanvasTilePoly(client, lp);
		if (poly == null) return;
		float fadeDuration = Math.max(1, config.fadeDuration());
		float remainingTicks = fadeDuration - fadingPlayer.getTicksSinceDisappeared();
		float opacity = remainingTicks / fadeDuration;
		if (opacity <= 0) return;
		Color color = new Color(
				config.fadeColor().getRed() / 255f,
				config.fadeColor().getGreen() / 255f,
				config.fadeColor().getBlue() / 255f,
				(config.fadeColor().getAlpha() / 255f) * opacity
		);
		graphics.setColor(color);
		graphics.fill(poly);
		if (config.showFadeNames()) {
			String name = fadingPlayer.getPlayer().getName();
			if (name != null) {
				Point textLocation = Perspective.getCanvasTextLocation(client, graphics, lp, name, 0);
				if (textLocation != null) {
					OverlayUtil.renderTextLocation(graphics, textLocation, name, color);
				}
			}
		}
	}
	private boolean isInWilderness() { return client.getVarbitValue(Varbits.IN_WILDERNESS) == 1; }
	private boolean isClientNotReady() { return client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null; }
}