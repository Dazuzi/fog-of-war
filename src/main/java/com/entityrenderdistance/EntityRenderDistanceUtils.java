package com.entityrenderdistance;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;

public class EntityRenderDistanceUtils
{
    /**
     * Returns true if the given world point is in the wilderness.
     * The wilderness area is defined by the rectangle (2941, 3520) to (3392, 3968),
     * with an additional constraint on the x-coordinate based on the y-coordinate.
     */
    public static boolean isInWilderness(WorldPoint wp)
    {
        // Check overall wilderness boundaries.
        if (wp.getX() < 2941 || wp.getX() > 3392 || wp.getY() < 3520 || wp.getY() > 3968)
        {
            return false;
        }

        // Calculate the wilderness level based on the Y-coordinate.
        int level = (wp.getY() - 3520) / 8 + 1;

        // The point is considered to be in the wilderness if its X-coordinate meets the minimum required
        // for the given wilderness level.
        return wp.getX() >= 2941 + (level - 1) * 8;
    }

    /**
     * Returns true if any of the defined bounding tiles (corners or the player's current point)
     * is in the wilderness.
     *
     * @param minX the minimum x coordinate of the box
     * @param maxX the maximum x coordinate of the box
     * @param minY the minimum y coordinate of the box
     * @param maxY the maximum y coordinate of the box
     * @param plane the plane (or level) on which the world point resides
     * @param playerPoint the player's current world location
     */
    public static boolean noTileInWilderness(int minX, int maxX, int minY, int maxY, int plane, WorldPoint playerPoint)
    {
        // Check the player's current point first.
        if (isInWilderness(playerPoint))
        {
            return false;
        }

        // Create the four corner points.
        WorldPoint nw = new WorldPoint(minX, maxY, plane);
        WorldPoint ne = new WorldPoint(maxX, maxY, plane);
        WorldPoint se = new WorldPoint(maxX, minY, plane);
        WorldPoint sw = new WorldPoint(minX, minY, plane);

        // Return true if none of these are in the wilderness.
        return !(isInWilderness(nw) || isInWilderness(ne) || isInWilderness(se) || isInWilderness(sw));
    }

    /**
     * Checks if the client is in a valid state for rendering overlays.
     * Returns true if the game state is LOGGED_IN and the local player is not null.
     */
    public static boolean isClientNotReady(Client client)
    {
        return client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null;
    }
}
