package com.fogofwar.fade;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.state.AreaExclusionManager;
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
import java.awt.Graphics2D;
import java.awt.Polygon;
public class FadingPlayerOverlay extends AbstractFadingPlayerOverlay {
	@Inject
	protected FadingPlayerOverlay(Client client, FogOfWarConfig config, FadingPlayerManager manager, ClientState clientState, AreaExclusionManager areaExclusionManager) {
		super(client, config, manager, clientState, areaExclusionManager, OverlayLayer.ABOVE_SCENE);
	}
	@Override
	boolean showsMarker() { return config.playerFadeMarkerMode().showsWorld(); }
	@Override
	void renderPlayer(Graphics2D graphics, WorldView wv, LocalPoint lp, FadingPlayer fadingPlayer) {
		Polygon poly = Perspective.getCanvasTilePoly(client, lp);
		if (poly == null) return;
		Color color = fadingPlayer.getColor(config);
		graphics.setColor(color);
		graphics.fill(poly);
		if (config.showFadeMarkerNames()) {
			String name = fadingPlayer.getPlayer().getName();
			if (name != null) {
				Point textLoc = Perspective.getCanvasTextLocation(client, graphics, lp, name, 0);
				if (textLoc != null) OverlayUtil.renderTextLocation(graphics, textLoc, name, color);
			}
		}
	}
}
