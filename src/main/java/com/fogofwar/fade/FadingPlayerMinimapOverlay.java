package com.fogofwar.fade;

import com.fogofwar.FogOfWarConfig;
import com.fogofwar.util.ClientState;
import com.fogofwar.util.MinimapUtil;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

public class FadingPlayerMinimapOverlay extends Overlay {
	private static final int DOT_SIZE = 4;
	private final Client client;
	private final FogOfWarConfig config;
	private final ClientState clientState;
	private final FadingPlayerManager manager;
	@Inject
	protected FadingPlayerMinimapOverlay(Client client, FogOfWarConfig config, ClientState clientState, FadingPlayerManager manager) {
		this.client = client;
		this.config = config;
		this.clientState = clientState;
		this.manager = manager;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(Overlay.PRIORITY_HIGH);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		if (!config.enableFadingPlayers() || !config.showFadingOnMinimap() || clientState.isClientNotReady()) return null;
		if (config.onlyInWilderness() && clientState.isNotInWilderness()) return null;
		Widget minimapWidget = MinimapUtil.getMinimapWidget(client);
		if (minimapWidget == null) return null;
		Shape oldClip = graphics.getClip();
		graphics.setClip(minimapWidget.getBounds());
		for (FadingPlayer fadingPlayer : manager.getFadingPlayers().values()) {
			renderFadingPlayer(graphics, fadingPlayer);
		}
		graphics.setClip(oldClip);
		return null;
	}
	private void renderFadingPlayer(Graphics2D graphics, FadingPlayer fadingPlayer) {
		WorldPoint wp = fadingPlayer.getLastLocation();
		WorldView wv = client.getTopLevelWorldView();
		LocalPoint lp = LocalPoint.fromWorld(wv, wp);
		if (lp == null) return;
		Point mp = Perspective.localToMinimap(client, lp);
		if (mp == null) return;
		float fadeDuration = Math.max(1, config.fadeDuration());
		float remainingTicks = fadeDuration - fadingPlayer.getTicksSinceDisappeared();
		float opacity = remainingTicks / fadeDuration;
		Color color = new Color(
				config.fadeColor().getRed() / 255f,
				config.fadeColor().getGreen() / 255f,
				config.fadeColor().getBlue() / 255f,
				opacity
		);
		Color shadedColor = color.darker();
		int x = mp.getX() - DOT_SIZE / 2;
		int y = mp.getY() - DOT_SIZE / 2 + 1;
		graphics.setColor(shadedColor);
		graphics.fillArc(x, y, DOT_SIZE, DOT_SIZE, 180, 180);
		graphics.setColor(color);
		graphics.fillArc(x, y, DOT_SIZE, DOT_SIZE, 0, 180);
	}
}