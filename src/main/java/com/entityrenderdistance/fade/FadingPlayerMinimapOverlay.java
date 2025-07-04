package com.entityrenderdistance.fade;

import com.entityrenderdistance.EntityRenderDistanceConfig;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

public class FadingPlayerMinimapOverlay extends Overlay {
	private static final int DOT_SIZE = 4;
	private static final int SIDE_PANELS_ID = 4607;
	private static final int FIXED_PARENT_ID = 548;
	private static final int FIXED_CHILD_ID = 21;
	private static final int STRETCH_PARENT_ID = 161;
	private static final int STRETCH_CHILD_ID = 30;
	private static final int PRE_EOC_PARENT_ID = 164;
	private static final int PRE_EOC_CHILD_ID = 30;
	private final Client client;
	private final EntityRenderDistanceConfig config;
	private final FadingPlayerManager manager;
	@Inject
	protected FadingPlayerMinimapOverlay(Client client, EntityRenderDistanceConfig config, FadingPlayerManager manager) {
		this.client = client;
		this.config = config;
		this.manager = manager;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(Overlay.PRIORITY_HIGH);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		if (!config.enableFadingPlayers() || !config.showFadingOnMinimap() || isClientNotReady()) return null;
		if (config.onlyInWilderness() && isInWilderness()) return null;
		Widget minimapWidget = getMinimapWidget();
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
	private boolean isInWilderness() { return client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) == 1; }
	private boolean isClientNotReady() { return client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null; }
	private Widget getMinimapWidget() {
		if (client.isResized()) {
			if (client.getVarbitValue(SIDE_PANELS_ID) == 1) { return client.getWidget(PRE_EOC_PARENT_ID, PRE_EOC_CHILD_ID); }
			return client.getWidget(STRETCH_PARENT_ID, STRETCH_CHILD_ID);
		}
		return client.getWidget(FIXED_PARENT_ID, FIXED_CHILD_ID);
	}
}