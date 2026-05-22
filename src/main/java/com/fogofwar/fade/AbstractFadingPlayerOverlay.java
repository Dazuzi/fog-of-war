package com.fogofwar.fade;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.state.ClientState;
import net.runelite.api.Client;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Collection;
abstract class AbstractFadingPlayerOverlay extends Overlay {
	protected final Client client;
	protected final FogOfWarConfig config;
	private final FadingPlayerManager manager;
	private final ClientState clientState;
	AbstractFadingPlayerOverlay(Client client, FogOfWarConfig config, FadingPlayerManager manager, ClientState clientState, OverlayLayer layer) {
		this.client = client;
		this.config = config;
		this.manager = manager;
		this.clientState = clientState;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(Overlay.PRIORITY_HIGH);
		setLayer(layer);
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		if (!showsMarker() || clientState.isClientNotReady()) return null;
		Collection<FadingPlayer> fadingPlayers = manager.getFadingPlayers().values();
		if (fadingPlayers.isEmpty()) return null;
		WorldView wv = client.getTopLevelWorldView();
		if (wv == null) return null;
		return renderPlayers(graphics, wv, fadingPlayers);
	}
	Dimension renderPlayers(Graphics2D graphics, WorldView wv, Collection<FadingPlayer> fadingPlayers) {
		for (FadingPlayer fadingPlayer : fadingPlayers) renderPlayer(graphics, wv, fadingPlayer);
		return null;
	}
	private void renderPlayer(Graphics2D graphics, WorldView wv, FadingPlayer fadingPlayer) {
		WorldPoint wp = fadingPlayer.getMarkerLocation();
		LocalPoint lp = LocalPoint.fromWorld(wv, wp);
		if (lp == null) return;
		renderPlayer(graphics, wv, lp, fadingPlayer);
	}
	abstract boolean showsMarker();
	abstract void renderPlayer(Graphics2D graphics, WorldView wv, LocalPoint lp, FadingPlayer fadingPlayer);
}
