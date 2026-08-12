package com.runehold.ui.village;

import com.runehold.domain.layout.GridPoint;
import java.awt.Point;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IsometricProjectionTest
{
	@Test
	public void projectsAndUnprojectsTileCentersDeterministically()
	{
		IsometricProjection projection = new IsometricProjection(384, 70, 28, 14);

		for (GridPoint point : new GridPoint[]{
			new GridPoint(0, 0),
			new GridPoint(7, 7),
			new GridPoint(17, 17),
			new GridPoint(3, 9)})
		{
			Point screen = projection.toScreen(point);
			assertEquals(point, projection.toGrid(screen.x, screen.y));
		}
	}
}
