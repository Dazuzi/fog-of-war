package com.fogofwar.render;
import net.runelite.api.Point;
import java.awt.geom.GeneralPath;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
public final class BoundaryPathBuilder {
	private BoundaryPathBuilder() {}
	public static GeneralPath build(GeneralPath path, List<Point> points, double arcCenterX, double arcCenterY, double arcRadius, Function<Point, Point> transform, Function<GeneralPath, GeneralPath> coveragePath, Predicate<GeneralPath> validator, Function<GeneralPath, GeneralPath> invalidFallback) {
		GeneralPath result = buildPath(path, points, arcCenterX, arcCenterY, arcRadius, transform, coveragePath, false);
		if (validator.test(result)) return result;
		result = buildPath(path, points, arcCenterX, arcCenterY, arcRadius, transform, coveragePath, true);
		if (validator.test(result)) return result;
		return invalidFallback.apply(result);
	}
	private static GeneralPath buildPath(GeneralPath path, List<Point> points, double arcCenterX, double arcCenterY, double arcRadius, Function<Point, Point> transform, Function<GeneralPath, GeneralPath> coveragePath, boolean reverseArc) {
		int n = points.size();
		int firstVisible = -1;
		int visibleCount = 0;
		for (int i = 0; i < n; i++) {
			if (points.get(i) != null) {
				if (firstVisible == -1) firstVisible = i;
				visibleCount++;
			}
		}
		if (visibleCount == 0) return coveragePath.apply(path);
		path.reset();
		if (visibleCount == n) return buildCompletePath(path, points, transform);
		Point first = transform.apply(points.get(firstVisible));
		path.moveTo(first.getX(), first.getY());
		for (int i = 0; i < n; i++) {
			int currentIndex = (firstVisible + i) % n;
			int nextIndex = (firstVisible + i + 1) % n;
			Point p1 = points.get(currentIndex);
			Point p2 = points.get(nextIndex);
			if (p1 == null) continue;
			if (p2 != null) {
				Point p = transform.apply(p2);
				path.lineTo(p.getX(), p.getY());
			} else {
				int nextVisibleIndex = findNextVisibleIndex(points, currentIndex, n);
				if (nextVisibleIndex != -1) addArcToPath(path, transform.apply(p1), transform.apply(points.get(nextVisibleIndex)), arcCenterX, arcCenterY, arcRadius, reverseArc);
			}
		}
		path.closePath();
		return path;
	}
	private static GeneralPath buildCompletePath(GeneralPath path, List<Point> points, Function<Point, Point> transform) {
		path.reset();
		Point first = transform.apply(points.get(0));
		path.moveTo(first.getX(), first.getY());
		for (int i = 1; i < points.size(); i++) {
			Point point = transform.apply(points.get(i));
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
