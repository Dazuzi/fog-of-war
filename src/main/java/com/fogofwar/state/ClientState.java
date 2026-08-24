package com.fogofwar.state;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.gameval.VarbitID;
import javax.inject.Inject;
import javax.inject.Singleton;
@Singleton
public class ClientState {
	private final Client client;
	@Inject
	public ClientState(Client client) { this.client = client; }
	public boolean isLoggedIn() { return client.getGameState() == GameState.LOGGED_IN; }
	public boolean isNotInWilderness() { return client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) != 1; }
	public boolean isSailing() { return client.getVarbitValue(VarbitID.SAILING_BOARDED_BOAT) == 1; }
	public Player getLocalPlayerIfReady() { return client.getGameState() == GameState.LOGGED_IN ? client.getLocalPlayer() : null; }
}
