package com.fogofwar.util;

import com.google.common.collect.ImmutableSet;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

import java.util.Set;

@Value
public class ExcludedArea {
    int minX;
    int minY;
    int maxX;
    int maxY;
    Set<Integer> planes;

    public ExcludedArea(int minX, int minY, int maxX, int maxY, Integer... planes) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.planes = ImmutableSet.copyOf(planes);
    }

    public boolean contains(WorldPoint point) {
        return this.planes.contains(point.getPlane()) &&
                point.getX() >= minX && point.getX() <= maxX &&
                point.getY() >= minY && point.getY() <= maxY;
    }
}