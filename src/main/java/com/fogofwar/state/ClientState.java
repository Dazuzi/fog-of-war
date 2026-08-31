package com.fogofwar.state;
import com.fogofwar.config.FogOfWarConfig;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.gameval.VarbitID;
import javax.inject.Inject;
import javax.inject.Singleton;
@Singleton
public class ClientState {
	private final Client client;
	private final FogOfWarConfig config;
	@Inject
	public ClientState(Client client, FogOfWarConfig config) {
		this.client = client;
		this.config = config;
	}
	public boolean isLoggedIn() { return client.getGameState() == GameState.LOGGED_IN; }
	public boolean isCutsceneSuppressed() { return config.disableDuringCutscenes() && client.getVarbitValue(VarbitID.CUTSCENE_STATUS) == 1; }
	public boolean isNotInWilderness() { return client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) != 1; }
	public boolean isSailing() { return client.getVarbitValue(VarbitID.SAILING_BOARDED_BOAT) == 1; }
	public Player getLocalPlayerIfReady() { return isLoggedIn() && !isCutsceneSuppressed() ? client.getLocalPlayer() : null; }
}
