package com.fogofwar.state;
import com.fogofwar.config.FogOfWarConfig;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.VarbitID;
import javax.inject.Inject;
import javax.inject.Singleton;
@Singleton
public class ClientState {
	private final Client client;
	@Inject
	public ClientState(Client client) { this.client = client; }
	public boolean isNotInWilderness() { return client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) != 1; }
	public boolean isClientNotReady() { return client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null; }
	private boolean isSuppressed(FogOfWarConfig config) { return isClientNotReady() || (config.onlyInWilderness() && isNotInWilderness()); }
	public boolean isSuppressed(FogOfWarConfig config, AreaExclusionManager areaExclusionManager) { return areaExclusionManager.isPlayerInExcludedArea() || isSuppressed(config); }
}
