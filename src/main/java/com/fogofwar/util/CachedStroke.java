package com.fogofwar.util;
import java.awt.BasicStroke;
public final class CachedStroke {
	private BasicStroke stroke;
	private int width = -1;
	public BasicStroke get(int w) {
		if (stroke == null || width != w) {
			stroke = new BasicStroke(w);
			width = w;
		}
		return stroke;
	}
}
