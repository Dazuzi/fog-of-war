package com.fogofwar.render;
import net.runelite.api.Point;
import java.awt.geom.GeneralPath;
import java.util.List;
public final class BoundaryPathBuilder {
	public interface Strategy {
		GeneralPath coverage(GeneralPath path);
		boolean isValid(GeneralPath path);
		GeneralPath fallback(GeneralPath path);
	}
	private BoundaryPathBuilder() {}
	public static GeneralPath build(GeneralPath path, List<Point> points, double arcCenterX, double arcCenterY, double arcRadius, Strategy strategy) {
		GeneralPath result = buildPath(path, points, arcCenterX, arcCenterY, arcRadius, strategy, false);
		if (strategy.isValid(result)) return result;
		result = buildPath(path, points, arcCenterX, arcCenterY, arcRadius, strategy, true);
		if (strategy.isValid(result)) return result;
		return strategy.fallback(result);
	}
	private static GeneralPath buildPath(GeneralPath path, List<Point> points, double arcCenterX, double arcCenterY, double arcRadius, Strategy strategy, boolean reverseArc) {
		int n = points.size();
		int firstVisible = -1;
		int visibleCount = 0;
		for (int i = 0; i < n; i++) {
			if (points.get(i) != null) {
				if (firstVisible == -1) firstVisible = i;
				visibleCount++;
			}
		}
		if (visibleCount == 0) return strategy.coverage(path);
		path.reset();
		if (visibleCount == n) return buildCompletePath(path, points);
		Point first = points.get(firstVisible);
		path.moveTo(first.getX(), first.getY());
		for (int i = 0; i < n; i++) {
			int currentIndex = (firstVisible + i) % n;
			int nextIndex = (firstVisible + i + 1) % n;
			Point p1 = points.get(currentIndex);
			Point p2 = points.get(nextIndex);
			if (p1 == null) continue;
			if (p2 != null) path.lineTo(p2.getX(), p2.getY());
			else {
				int nextVisibleIndex = findNextVisibleIndex(points, currentIndex, n);
				if (nextVisibleIndex != -1) addArcToPath(path, p1, points.get(nextVisibleIndex), arcCenterX, arcCenterY, arcRadius, reverseArc);
			}
		}
		path.closePath();
		return path;
	}
	private static GeneralPath buildCompletePath(GeneralPath path, List<Point> points) {
		Point first = points.get(0);
		path.moveTo(first.getX(), first.getY());
		for (int i = 1; i < points.size(); i++) {
			Point point = points.get(i);
			path.lineTo(point.getX(), point.getY());
		}
		path.closePath();
		return path;
	}
	private static int findNextVisibleIndex(List<Point> points, int currentIndex, int n) {
		for (int i = 2; i < n; i++) {
			int index = (currentIndex + i) % n;
			if (points.get(index) != null) return index;
		}
		return -1;
	}
	private static void addArcToPath(GeneralPath path, Point p1, Point p2, double centerX, double centerY, double radius, boolean reverse) {
		double startAngle = Math.toDegrees(Math.atan2(p1.getY() - centerY, p1.getX() - centerX));
		double endAngle = Math.toDegrees(Math.atan2(p2.getY() - centerY, p2.getX() - centerX));
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
