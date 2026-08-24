package com.fogofwar.fade;
import com.fogofwar.config.FogOfWarConfig;
import com.fogofwar.render.RenderCenter;
import com.fogofwar.render.RenderCenterProvider;
import com.fogofwar.state.ClientState;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Collection;
abstract class AbstractFadingPlayerOverlay extends Overlay {
	protected final Client client;
	protected final FogOfWarConfig config;
	private final FadingPlayerManager manager;
	private final ClientState clientState;
	private final RenderCenterProvider renderCenterProvider;
	AbstractFadingPlayerOverlay(Client client, FogOfWarConfig config, FadingPlayerManager manager, ClientState clientState, RenderCenterProvider renderCenterProvider, OverlayLayer layer) {
		this.client = client;
		this.config = config;
		this.manager = manager;
		this.clientState = clientState;
		this.renderCenterProvider = renderCenterProvider;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(Overlay.PRIORITY_HIGH);
		setLayer(layer);
	}
	@Override
	public Dimension render(Graphics2D graphics) {
		if (!showsMarker()) return null;
		Player localPlayer = clientState.getLocalPlayerIfReady();
		if (localPlayer == null) return null;
		Collection<FadingPlayer> fadingPlayers = manager.getFadingPlayers().values();
		if (fadingPlayers.isEmpty()) return null;
		RenderCenter rc = renderCenterProvider.get(localPlayer);
		if (rc == null) return null;
		WorldView wv = rc.getWorldView();
		return renderPlayers(graphics, wv, fadingPlayers);
	}
	Dimension renderPlayers(Graphics2D graphics, WorldView wv, Collection<FadingPlayer> fadingPlayers) {
		Color base = config.fadeMarkerColour();
		int duration = config.fadeDurationTicks();
		for (FadingPlayer fadingPlayer : fadingPlayers) renderPlayer(graphics, wv, fadingPlayer, base, duration);
		return null;
	}
	private void renderPlayer(Graphics2D graphics, WorldView wv, FadingPlayer fadingPlayer, Color base, int duration) {
		LocalPoint lp = fadingPlayer.getLocalPoint(wv);
		if (lp == null) return;
		renderPlayer(graphics, wv, lp, fadingPlayer, base, duration);
	}
	abstract boolean showsMarker();
	abstract void renderPlayer(Graphics2D graphics, WorldView wv, LocalPoint lp, FadingPlayer fadingPlayer, Color base, int duration);
}
