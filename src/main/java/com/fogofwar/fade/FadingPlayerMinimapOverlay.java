package com.fogofwar.fade;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.render.minimap.MinimapClipProvider;
import com.fogofwar.render.minimap.MinimapWidgetProvider;
import com.fogofwar.state.AreaExclusionManager;
import com.fogofwar.state.ClientState;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.OverlayLayer;
import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.Collection;
public class FadingPlayerMinimapOverlay extends AbstractFadingPlayerOverlay {
	private static final int DOT_SIZE = 4;
	private final MinimapClipProvider clipProvider;
	@Inject
	protected FadingPlayerMinimapOverlay(Client client, FogOfWarConfig config, ClientState clientState, FadingPlayerManager manager, AreaExclusionManager areaExclusionManager, MinimapClipProvider clipProvider) {
		super(client, config, manager, clientState, areaExclusionManager, OverlayLayer.ABOVE_WIDGETS);
		this.clipProvider = clipProvider;
	}
	@Override
	boolean showsMarker() { return config.playerFadeMarkerMode().showsMinimap(); }
	@Override
	Dimension renderPlayers(Graphics2D graphics, WorldView wv, Collection<FadingPlayer> fadingPlayers) {
		Widget minimapWidget = MinimapWidgetProvider.getMinimapWidget(client);
		if (minimapWidget == null) return null;
		Shape oldClip = graphics.getClip();
		graphics.setClip(clipProvider.getClipShape(minimapWidget));
		try {
			return super.renderPlayers(graphics, wv, fadingPlayers);
		} finally { graphics.setClip(oldClip); }
	}
	@Override
	void renderPlayer(Graphics2D graphics, WorldView wv, LocalPoint lp, FadingPlayer fadingPlayer) {
		Point mp = Perspective.localToMinimap(client, lp);
		if (mp == null) return;
		Color color = fadingPlayer.getColor(config);
		Color shadedColor = fadingPlayer.getDarkerColor(config);
		int x = mp.getX() - DOT_SIZE / 2;
		int y = mp.getY() - DOT_SIZE / 2 + 1;
		graphics.setColor(shadedColor);
		graphics.fillArc(x, y, DOT_SIZE, DOT_SIZE, 180, 180);
		graphics.setColor(color);
		graphics.fillArc(x, y, DOT_SIZE, DOT_SIZE, 0, 180);
	}
}
