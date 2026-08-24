package com.fogofwar.fade;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import java.awt.Color;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
public class FadingPlayerTest {
	@Test
	public void cachesColoursUntilVisibleInputsChange() {
		FadingPlayer player = new FadingPlayer(null, null, new WorldPoint(0, 0, 0), 15);
		Color base = new Color(100, 50, 25, 160);
		Color initial = player.getColor(base, 4);
		assertSame(initial, player.getColor(base, 4));
		assertSame(player.getDarkerColor(base, 4), player.getDarkerColor(base, 4));
		player.setTicksSinceDisappeared(1);
		Color faded = player.getColor(base, 4);
		assertNotSame(initial, faded);
		assertEquals(120, faded.getAlpha());
	}
}
