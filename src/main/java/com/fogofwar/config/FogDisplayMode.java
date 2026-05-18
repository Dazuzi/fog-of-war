package com.fogofwar.config;
public enum FogDisplayMode {
	OFF(false, false, "Off"),
	FOG(true, false, "Fog"),
	BORDER(false, true, "Border"),
	BOTH(true, true, "Fog + border");
	private final boolean fog;
	private final boolean border;
	private final String name;
	FogDisplayMode(boolean fog, boolean border, String name) {
		this.fog = fog;
		this.border = border;
		this.name = name;
	}
	public boolean showsFog() { return fog; }
	public boolean showsBorder() { return border; }
	public boolean isEnabled() { return fog || border; }
	@Override
	public String toString() { return name; }
}
