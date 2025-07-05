package com.fogofwar.fade;

import com.fogofwar.FogOfWarConfig;
import com.fogofwar.util.ClientState;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;
public class FadingPlayerOverlay extends Overlay {
	private final Client client;
	private final FogOfWarConfig config;
	private final FadingPlayerManager manager;
	private final ClientState clientState;
	@Inject
	protected FadingPlayerOverlay(Client client, FogOfWarConfig config, FadingPlayerManager manager, ClientState clientState) {
		this.client = client;
		this.config = config;
		this.manager = manager;
		this.clientState = clientState;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(Overlay.PRIORITY_HIGH);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		if (!config.enableFadingPlayers() || !config.showFadingInWorld() || clientState.isClientNotReady()) return null;
		if (config.onlyInWilderness() && clientState.isNotInWilderness()) return null;
		for (FadingPlayer fadingPlayer : manager.getFadingPlayers().values()) {
			renderFadingPlayer(graphics, fadingPlayer);
		}
		return null;
	}
	private void renderFadingPlayer(Graphics2D graphics, FadingPlayer fadingPlayer) {
		WorldPoint wp = fadingPlayer.getLastLocation();
		WorldView wv = client.getTopLevelWorldView();
		LocalPoint lp = LocalPoint.fromWorld(wv, wp);
		if (lp == null) return;
		Polygon poly = Perspective.getCanvasTilePoly(client, lp);
		if (poly == null) return;
		float fadeDuration = Math.max(1, config.fadeDuration());
		float remainingTicks = fadeDuration - fadingPlayer.getTicksSinceDisappeared();
		float opacity = remainingTicks / fadeDuration;
		if (opacity <= 0) return;
		Color base = config.fadeColor();
		Color color = new Color(
				base.getRed() / 255f,
				base.getGreen() / 255f,
				base.getBlue() / 255f,
				(base.getAlpha() / 255f) * opacity
		);
		graphics.setColor(color);
		graphics.fill(poly);
		if (config.showFadeNames()) {
			String name = fadingPlayer.getPlayer().getName();
			if (name != null) {
				Point textLoc = Perspective.getCanvasTextLocation(client, graphics, lp, name, 0);
				if (textLoc != null) OverlayUtil.renderTextLocation(graphics, textLoc, name, color);
			}
		}
	}
}