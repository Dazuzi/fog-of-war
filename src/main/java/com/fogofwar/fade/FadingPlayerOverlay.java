package com.fogofwar.fade;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.render.RenderCenterProvider;
import com.fogofwar.state.ClientState;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayUtil;
import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Collection;
public class FadingPlayerOverlay extends AbstractFadingPlayerOverlay {
	private boolean showNames;
	@Inject
	protected FadingPlayerOverlay(Client client, FogOfWarConfig config, FadingPlayerManager manager, ClientState clientState, RenderCenterProvider renderCenterProvider) {
		super(client, config, manager, clientState, renderCenterProvider, OverlayLayer.ABOVE_SCENE);
	}
	@Override
	boolean showsMarker() { return config.playerFadeMarkerMode().showsWorld(); }
	@Override
	Dimension renderPlayers(Graphics2D graphics, WorldView wv, Collection<FadingPlayer> fadingPlayers) {
		showNames = config.showFadeMarkerNames();
		return super.renderPlayers(graphics, wv, fadingPlayers);
	}
	@Override
	void renderPlayer(Graphics2D graphics, WorldView wv, LocalPoint lp, FadingPlayer fadingPlayer, Color base, int duration) {
		Color color = fadingPlayer.getColor(base, duration);
		if (color.getAlpha() == 0 && !showNames) return;
		Polygon poly = Perspective.getCanvasTilePoly(client, lp);
		if (poly == null) return;
		if (color.getAlpha() > 0) {
			graphics.setColor(color);
			graphics.fill(poly);
		}
		if (showNames) {
			String name = fadingPlayer.getPlayer().getName();
			if (name != null) {
				Point textLoc = Perspective.getCanvasTextLocation(client, graphics, lp, name, 0);
				if (textLoc != null) OverlayUtil.renderTextLocation(graphics, textLoc, name, color);
			}
		}
	}
}
