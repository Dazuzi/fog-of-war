package com.fogofwar;
public enum FadingPlayerMode {
	OFF,
	WORLD,
	MINIMAP,
	BOTH;
	public boolean showsWorld() { return this == WORLD || this == BOTH; }
	public boolean showsMinimap() { return this == MINIMAP || this == BOTH; }
}
