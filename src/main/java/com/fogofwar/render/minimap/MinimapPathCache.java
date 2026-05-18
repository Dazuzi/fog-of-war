package com.fogofwar.render.minimap;
import java.awt.geom.GeneralPath;
final class MinimapPathCache {
	final GeneralPath path = new GeneralPath();
	final GeneralPath lastPath = new GeneralPath();
	boolean hasLastPath;
	void clear() { hasLastPath = false; }
}
