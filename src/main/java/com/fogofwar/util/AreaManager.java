package com.fogofwar.util;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class AreaManager {
    private final Client client;
    private final EventBus eventBus;
    private final List<ExcludedArea> excludedAreas = new ArrayList<>();
    @Getter
    private boolean playerInExcludedArea = false;
    @Inject
    public AreaManager(Client client, EventBus eventBus) {
        this.client = client;
        this.eventBus = eventBus;
        excludedAreas.add(new ExcludedArea(2367, 5053, 2432, 5119, 0));     // TzHaar Fight Cave
        excludedAreas.add(new ExcludedArea(2256, 5328, 2286, 5359, 0));     // Inferno
        excludedAreas.add(new ExcludedArea(2215, 5935, 2325, 6035, 1, 2));  // Hallowed Sepulchre Floor 1
        excludedAreas.add(new ExcludedArea(2475, 5935, 2585, 6035, 1, 2));  // Hallowed Sepulchre Floor 2
        excludedAreas.add(new ExcludedArea(2225, 5795, 2575, 5915, 1, 2));  // Hallowed Sepulchre Floors 3-5
        excludedAreas.add(new ExcludedArea(3136, 4216, 3366, 4474, 0));     // Theatre of Blood
    }
    public void start() {
        eventBus.register(this);
        if (client.getGameState() == GameState.LOGGED_IN) {
            checkArea();
        }
    }
    public void stop() {
        eventBus.unregister(this);
    }
    @Subscribe
    @SuppressWarnings("unused")
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGGED_IN) {
            checkArea();
        } else if (event.getGameState() == GameState.LOADING) {
            playerInExcludedArea = false;
        }
    }
    private void checkArea() {
        if (client.getLocalPlayer() == null) {
            playerInExcludedArea = false;
            return;
        }
        WorldPoint playerPoint = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation());
        for (ExcludedArea area : excludedAreas) {
            if (area.contains(playerPoint)) {
                playerInExcludedArea = true;
                return;
            }
        }
        playerInExcludedArea = false;
    }
}