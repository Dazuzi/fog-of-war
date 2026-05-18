package com.fogofwar.config;
public enum FadingPlayerMode {
	OFF("Off"),
	WORLD("World"),
	MINIMAP("Minimap"),
	BOTH("World + minimap");
	private final String name;
	FadingPlayerMode(String name) { this.name = name; }
	public boolean showsWorld() { return this == WORLD || this == BOTH; }
	public boolean showsMinimap() { return this == MINIMAP || this == BOTH; }
	@Override
	public String toString() { return name; }
}
