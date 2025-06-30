package com.entityrenderdistance.fade;
import com.entityrenderdistance.EntityRenderDistanceConfig;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Varbits;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
public class FadingPlayerMinimapOverlay extends Overlay {
	private static final int DOT_SIZE = 4;
	private final Client client;
	private final EntityRenderDistanceConfig config;
	private final FadingPlayerManager manager;
	@Inject
	protected FadingPlayerMinimapOverlay(Client client, EntityRenderDistanceConfig config, FadingPlayerManager manager) {
		this.client = client;
		this.config = config;
		this.manager = manager;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.LOW);
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
		LocalPoint lp = LocalPoint.fromWorld(client, wp);
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
		int y = mp.getY() - DOT_SIZE / 2;
		graphics.setColor(shadedColor);
		graphics.fillArc(x, y, DOT_SIZE, DOT_SIZE, 180, 180);
		graphics.setColor(color);
		graphics.fillArc(x, y, DOT_SIZE, DOT_SIZE, 0, 180);
	}
	private boolean isInWilderness() { return client.getVarbitValue(Varbits.IN_WILDERNESS) == 1; }
	private boolean isClientNotReady() { return client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null; }
	private Widget getMinimapWidget() {
		if (client.isResized()) {
			if (client.getVarbitValue(Varbits.SIDE_PANELS) == 1) {
				return client.getWidget(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_MINIMAP_DRAW_AREA);
			}
			return client.getWidget(ComponentID.RESIZABLE_VIEWPORT_MINIMAP_DRAW_AREA);
		}
		return client.getWidget(ComponentID.FIXED_VIEWPORT_MINIMAP_DRAW_AREA);
	}
}