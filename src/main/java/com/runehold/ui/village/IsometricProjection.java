package com.runehold.ui.village;

import com.runehold.domain.layout.GridPoint;
import java.awt.Point;

public final class IsometricProjection
{
	private final int originX;
	private final int originY;
	private final int tileWidth;
	private final int tileHeight;

	public IsometricProjection(int originX, int originY, int tileWidth, int tileHeight)
	{
		if (tileWidth < 2 || tileHeight < 2 || tileWidth % 2 != 0 || tileHeight % 2 != 0)
		{
			throw new IllegalArgumentException("tile dimensions must be positive even values");
		}
		this.originX = originX;
		this.originY = originY;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
	}

	public Point toScreen(GridPoint point)
	{
		if (point == null)
		{
			throw new IllegalArgumentException("grid point is required");
		}
		return toScreen(point.getX(), point.getY());
	}

	public Point toScreen(int gridX, int gridY)
	{
		return new Point(
			originX + (gridX - gridY) * tileWidth / 2,
			originY + (gridX + gridY) * tileHeight / 2);
	}

	public GridPoint toGrid(int screenX, int screenY)
	{
		double diagonalX = (screenX - originX) / (tileWidth / 2.0);
		double diagonalY = (screenY - originY) / (tileHeight / 2.0);
		return new GridPoint(
			(int) Math.round((diagonalX + diagonalY) / 2.0),
			(int) Math.round((diagonalY - diagonalX) / 2.0));
	}

	public int getTileWidth()
	{
		return tileWidth;
	}

	public int getTileHeight()
	{
		return tileHeight;
	}
}
