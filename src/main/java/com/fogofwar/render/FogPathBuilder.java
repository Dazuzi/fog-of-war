package com.fogofwar.render;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
public final class FogPathBuilder {
	private FogPathBuilder() {}
	public static void fill(GeneralPath path, Rectangle bounds, int padding, GeneralPath inner) {
		path.reset();
		path.moveTo(bounds.x - padding, bounds.y - padding);
		path.lineTo(bounds.x + bounds.width + padding, bounds.y - padding);
		path.lineTo(bounds.x + bounds.width + padding, bounds.y + bounds.height + padding);
		path.lineTo(bounds.x - padding, bounds.y + bounds.height + padding);
		path.closePath();
		path.append(inner, false);
	}
}
