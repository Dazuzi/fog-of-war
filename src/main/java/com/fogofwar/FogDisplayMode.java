package com.fogofwar;
public enum FogDisplayMode {
	OFF(false, false),
	FOG(true, false),
	BORDER(false, true),
	BOTH(true, true);
	private final boolean fog;
	private final boolean border;
	FogDisplayMode(boolean fog, boolean border) {
		this.fog = fog;
		this.border = border;
	}
	public boolean showsFog() { return fog; }
	public boolean showsBorder() { return border; }
	public boolean isEnabled() { return fog || border; }
}
