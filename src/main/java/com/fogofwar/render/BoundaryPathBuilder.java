package com.fogofwar.render;
import java.awt.geom.GeneralPath;
public final class BoundaryPathBuilder {
	public interface Strategy {
		GeneralPath coverage(GeneralPath path);
		boolean isValid(GeneralPath path);
		GeneralPath fallback(GeneralPath path);
	}
	private BoundaryPathBuilder() {}
	public static GeneralPath build(GeneralPath path, int[] x, int[] y, boolean[] visible, int count, double arcCenterX, double arcCenterY, double arcRadius, Strategy strategy) {
		GeneralPath result = buildPath(path, x, y, visible, count, arcCenterX, arcCenterY, arcRadius, strategy, false);
		if (strategy.isValid(result)) return result;
		result = buildPath(path, x, y, visible, count, arcCenterX, arcCenterY, arcRadius, strategy, true);
		if (strategy.isValid(result)) return result;
		return strategy.fallback(result);
	}
	private static GeneralPath buildPath(GeneralPath path, int[] x, int[] y, boolean[] visible, int count, double arcCenterX, double arcCenterY, double arcRadius, Strategy strategy, boolean reverseArc) {
		int firstVisible = -1;
		int visibleCount = 0;
		for (int i = 0; i < count; i++) {
			if (visible[i]) {
				if (firstVisible == -1) firstVisible = i;
				visibleCount++;
			}
		}
		if (visibleCount == 0) return strategy.coverage(path);
		path.reset();
		if (visibleCount == count) return buildCompletePath(path, x, y, count);
		path.moveTo(x[firstVisible], y[firstVisible]);
		for (int i = 0; i < count; i++) {
			int currentIndex = (firstVisible + i) % count;
			int nextIndex = (firstVisible + i + 1) % count;
			if (!visible[currentIndex]) continue;
			if (visible[nextIndex]) path.lineTo(x[nextIndex], y[nextIndex]);
			else {
				int nextVisibleIndex = findNextVisibleIndex(visible, currentIndex, count);
				if (nextVisibleIndex != -1) addArcToPath(path, x[currentIndex], y[currentIndex], x[nextVisibleIndex], y[nextVisibleIndex], arcCenterX, arcCenterY, arcRadius, reverseArc);
			}
		}
		path.closePath();
		return path;
	}
	private static GeneralPath buildCompletePath(GeneralPath path, int[] x, int[] y, int count) {
		path.moveTo(x[0], y[0]);
		for (int i = 1; i < count; i++) path.lineTo(x[i], y[i]);
		path.closePath();
		return path;
	}
	private static int findNextVisibleIndex(boolean[] visible, int currentIndex, int count) {
		for (int i = 2; i < count; i++) {
			int index = (currentIndex + i) % count;
			if (visible[index]) return index;
		}
		return -1;
	}
	private static void addArcToPath(GeneralPath path, int x1, int y1, int x2, int y2, double centerX, double centerY, double radius, boolean reverse) {
		double startAngle = Math.toDegrees(Math.atan2(y1 - centerY, x1 - centerX));
		double endAngle = Math.toDegrees(Math.atan2(y2 - centerY, x2 - centerX));
		double sweep = endAngle - startAngle;
		if (sweep <= -180) { sweep += 360; } else if (sweep > 180) { sweep -= 360; }
		if (reverse) sweep += sweep > 0 ? -360 : 360;
		int numSteps = (int) (Math.abs(sweep) / 10) + 1;
		for (int i = 1; i <= numSteps; i++) {
			double angle = Math.toRadians(startAngle + sweep * i / numSteps);
			path.lineTo((float) (centerX + radius * Math.cos(angle)), (float) (centerY + radius * Math.sin(angle)));
		}
	}
}
