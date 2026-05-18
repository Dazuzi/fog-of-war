package com.fogofwar.lifecycle;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
public final class OverlayToggle {
	private final OverlayManager overlayManager;
	private final Overlay overlay;
	private boolean enabled;
	public OverlayToggle(OverlayManager overlayManager, Overlay overlay) {
		this.overlayManager = overlayManager;
		this.overlay = overlay;
	}
	public void set(boolean enabled) {
		if (this.enabled == enabled) return;
		if (enabled) overlayManager.add(overlay);
		else overlayManager.remove(overlay);
		this.enabled = enabled;
	}
}
