package com.fogofwar.fade;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.state.AreaExclusionManager;
import com.fogofwar.state.ClientState;
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
import java.util.Collection;
public class FadingPlayerOverlay extends Overlay {
	private final Client client;
	private final FogOfWarConfig config;
	private final FadingPlayerManager manager;
	private final ClientState clientState;
	private final AreaExclusionManager areaExclusionManager;
	@Inject
	protected FadingPlayerOverlay(Client client, FogOfWarConfig config, FadingPlayerManager manager, ClientState clientState, AreaExclusionManager areaExclusionManager) {
		this.client = client;
		this.config = config;
		this.manager = manager;
		this.clientState = clientState;
		this.areaExclusionManager = areaExclusionManager;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(Overlay.PRIORITY_HIGH);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		if (!config.playerFadeMarkerMode().showsWorld() || clientState.isSuppressed(config, areaExclusionManager)) return null;
		Collection<FadingPlayer> fadingPlayers = manager.getFadingPlayers().values();
		if (fadingPlayers.isEmpty()) return null;
		WorldView wv = client.getTopLevelWorldView();
		if (wv == null) return null;
		for (FadingPlayer fadingPlayer : fadingPlayers) renderFadingPlayer(graphics, wv, fadingPlayer);
		return null;
	}
	private void renderFadingPlayer(Graphics2D graphics, WorldView wv, FadingPlayer fadingPlayer) {
		WorldPoint wp = fadingPlayer.getLastLocation();
		LocalPoint lp = LocalPoint.fromWorld(wv, wp);
		if (lp == null) return;
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
