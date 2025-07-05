package com.fogofwar.util;

import com.fogofwar.FogOfWarConfig;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class DynamicRenderDistance {
    private final Client client;
    private final FogOfWarConfig config;
    private final EventBus eventBus;
    @Getter
    private int currentRenderDistance;
    @Inject
    public DynamicRenderDistance(Client client, FogOfWarConfig config, EventBus eventBus) {
        this.client = client;
        this.config = config;
        this.eventBus = eventBus;
        this.currentRenderDistance = config.renderDistanceRadius();
    }
    public void start() {
        eventBus.register(this);
    }
    public void stop() {
        eventBus.unregister(this);
    }
    @Subscribe
    @SuppressWarnings("unused")
    public void onGameTick(GameTick event) {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null) {
            this.currentRenderDistance = config.renderDistanceRadius();
            return;
        }
        if (!config.enableDynamicRenderDistance()) {
            this.currentRenderDistance = config.renderDistanceRadius();
            return;
        }
        WorldView worldView = client.getTopLevelWorldView();
        List<Player> players = new ArrayList<>();
        for (Player p : worldView.players()) {
            if (p != null) {
                players.add(p);
            }
        }
        if (players.size() >= config.dynamicRenderDistancePlayerThreshold()) {
            int maxDistTiles = 0;
            WorldPoint playerWorldPoint = client.getLocalPlayer().getWorldLocation();
            int playerTileX = playerWorldPoint.getX();
            int playerTileY = playerWorldPoint.getY();
            for (Player p : players) {
                WorldPoint wp = p.getWorldLocation();
                if (wp == null) continue;
                int distTiles = Math.max(Math.abs(wp.getX() - playerTileX), Math.abs(wp.getY() - playerTileY));
                if (distTiles > maxDistTiles) {
                    maxDistTiles = distTiles;
                    if (maxDistTiles >= config.renderDistanceRadius()) {
                        maxDistTiles = config.renderDistanceRadius();
                        break;
                    }
                }
            }
            this.currentRenderDistance = maxDistTiles;
        } else {
            this.currentRenderDistance = config.renderDistanceRadius();
        }
    }
}