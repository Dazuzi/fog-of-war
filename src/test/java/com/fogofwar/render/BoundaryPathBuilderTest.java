package com.fogofwar.render;
import org.junit.Test;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
public class BoundaryPathBuilderTest {
	@Test
	public void buildsCompletePrimitiveBoundary() {
		GeneralPath path = new GeneralPath();
		GeneralPath result = BoundaryPathBuilder.build(path, new int[]{0, 10, 10, 0}, new int[]{0, 0, 10, 10}, new boolean[]{true, true, true, true}, 4, 5, 5, 20, new ValidStrategy());
		assertSame(path, result);
		assertEquals(new Rectangle(0, 0, 10, 10), path.getBounds());
		assertTrue(path.contains(5, 5));
	}
	@Test
	public void retriesThenFallsBackWhenBoundaryIsInvalid() {
		GeneralPath fallback = new GeneralPath();
		BoundaryPathBuilder.Strategy strategy = new BoundaryPathBuilder.Strategy() {
			@Override
			public GeneralPath coverage(GeneralPath path) { return path; }
			@Override
			public boolean isValid(GeneralPath path) { return false; }
			@Override
			public GeneralPath fallback(GeneralPath path) { return fallback; }
		};
		assertSame(fallback, BoundaryPathBuilder.build(new GeneralPath(), new int[]{0, 10, 10}, new int[]{0, 0, 10}, new boolean[]{true, false, true}, 3, 5, 5, 20, strategy));
	}
	private static final class ValidStrategy implements BoundaryPathBuilder.Strategy {
		@Override
		public GeneralPath coverage(GeneralPath path) { return path; }
		@Override
		public boolean isValid(GeneralPath path) { return true; }
		@Override
		public GeneralPath fallback(GeneralPath path) { return path; }
	}
}
