package com.fogofwar.render.minimap;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.util.Arrays;
import javax.inject.Inject;
import javax.inject.Singleton;
@Singleton
public final class MinimapClipProvider {
	private static final int RESIZED_MINIMAP_CLIP_PADDING = 1;
	private static final int FIXED_MINIMAP_CLIP_PADDING = 3;
	private static final int[] ORB_WIDGETS = {InterfaceID.Orbs.HEALTH_BACKING, InterfaceID.Orbs.PRAYER_BACKING, InterfaceID.Orbs.RUNENERGY_BACKING, InterfaceID.Orbs.SPECENERGY_BACKING, InterfaceID.Orbs.ORB_WORLDMAP};
	private final Client client;
	private final Rectangle[] currentOrbBounds = new Rectangle[ORB_WIDGETS.length];
	private Rectangle cachedMinimapBounds;
	private Shape cachedClipShape;
	private long cachedOrbsHash;
	private boolean cachedResized;
	@Inject
	public MinimapClipProvider(Client client) { this.client = client; }
	void clearCaches() {
		cachedMinimapBounds = null;
		cachedClipShape = null;
		cachedOrbsHash = 0;
		Arrays.fill(currentOrbBounds, null);
	}
	public Shape getClipShape(Widget minimapWidget) {
		Rectangle bounds = minimapWidget.getBounds();
		boolean resized = client.isResized();
		long orbsHash = collectOrbBoundsAndHash();
		if (cachedClipShape != null && bounds.equals(cachedMinimapBounds) && orbsHash == cachedOrbsHash && resized == cachedResized) return cachedClipShape;
		Area clipArea = createEllipse(bounds, resized ? RESIZED_MINIMAP_CLIP_PADDING : FIXED_MINIMAP_CLIP_PADDING);
		for (Rectangle ob : currentOrbBounds) {
			if (ob == null) continue;
			clipArea.subtract(createEllipse(ob, 0));
		}
		cachedClipShape = clipArea;
		cachedMinimapBounds = new Rectangle(bounds);
		cachedOrbsHash = orbsHash;
		cachedResized = resized;
		return cachedClipShape;
	}
	private static Area createEllipse(Rectangle bounds, int padding) { return new Area(new Ellipse2D.Double(bounds.getX() - padding, bounds.getY() - padding, bounds.getWidth() + padding * 2, bounds.getHeight() + padding * 2)); }
	private long collectOrbBoundsAndHash() {
		long h = 1469598103934665603L;
		for (int i = 0; i < ORB_WIDGETS.length; i++) {
			Widget orb = client.getWidget(ORB_WIDGETS[i]);
			Rectangle b = (orb != null && !orb.isHidden()) ? orb.getBounds() : null;
			currentOrbBounds[i] = b;
			if (b == null) h ^= 0xDEADBEEFL;
			else h ^= ((b.x & 0xFFFFL) << 48) ^ ((b.y & 0xFFFFL) << 32) ^ ((b.width & 0xFFFFL) << 16) ^ (b.height & 0xFFFFL);
			h *= 1099511628211L;
		}
		return h;
	}
}
