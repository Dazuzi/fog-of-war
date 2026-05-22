package com.fogofwar.render.minimap;
import java.awt.geom.GeneralPath;
final class MinimapPathCache {
	private GeneralPath working = new GeneralPath();
	private GeneralPath lastValid = new GeneralPath();
	GeneralPath working() { return working; }
	GeneralPath lastValid() { return lastValid; }
	boolean hasLastValid() { return lastValid.getCurrentPoint() != null; }
	GeneralPath saveValid() {
		GeneralPath valid = working;
		working = lastValid;
		lastValid = valid;
		working.reset();
		return lastValid;
	}
	void clear() {
		working.reset();
		lastValid.reset();
	}
}
