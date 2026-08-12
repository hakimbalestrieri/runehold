package com.runehold.ui.village;

import java.util.Objects;

/**
 * Measured facts about one building sprite: its native pixel bounds and where its
 * base sits relative to the footprint diamond.
 *
 * <p>Native bounds are read from the loaded image rather than declared by hand, so
 * the renderer cannot disagree with the artwork on disk. A second, hand-maintained
 * dimension table is what previously made buildings render at the wrong size and
 * float off their tiles.
 *
 * <p>The sprite is authored for {@link #NATIVE_TILE_WIDTH}: at that tile width it is
 * drawn at 1:1, and every other zoom scales it by the same factor regardless of where
 * the building sits on the map.
 */
public final class VillageSpriteMetadata
{
	/** Tile width the sprites are authored against; at this width they draw unscaled. */
	public static final int NATIVE_TILE_WIDTH = 36;

	private final int nativeWidth;
	private final int nativeHeight;
	private final int contentCenterX;
	private final int contentBottom;

	VillageSpriteMetadata(
		int nativeWidth,
		int nativeHeight,
		int contentCenterX,
		int contentBottom)
	{
		if (nativeWidth <= 0 || nativeHeight <= 0)
		{
			throw new IllegalArgumentException("invalid sprite bounds");
		}
		this.nativeWidth = nativeWidth;
		this.nativeHeight = nativeHeight;
		this.contentCenterX = contentCenterX;
		this.contentBottom = contentBottom;
	}

	public int getNativeWidth()
	{
		return nativeWidth;
	}

	public int getNativeHeight()
	{
		return nativeHeight;
	}

	public double getAspectRatio()
	{
		return nativeHeight / (double) nativeWidth;
	}

	/**
	 * Drawn width at the given tile width. Depends only on zoom, never on the
	 * building's position, so the same building is the same size everywhere.
	 */
	public int scaledWidth(int tileWidth)
	{
		return Math.max(8, (int) Math.round(nativeWidth * tileWidth / (double) NATIVE_TILE_WIDTH));
	}

	public int scaledHeight(int tileWidth)
	{
		return Math.max(8, (int) Math.round(scaledWidth(tileWidth) * getAspectRatio()));
	}

	/**
	 * How far above the footprint diamond's front corner the sprite's base sits. The
	 * building reads as standing on the plot rather than in front of it.
	 */
	public static int baselineInset(int tileHeight)
	{
		return tileHeight / 4;
	}

	/**
	 * Horizontal distance from the sprite's left edge to the centre of its opaque
	 * content, at the given tile width.
	 *
	 * <p>Anchoring on the canvas centre instead of the content centre is what made an
	 * asymmetrically padded sprite float off its tiles: {@code barracks.png} carries its
	 * artwork nine pixels right of centre with twenty-four empty pixels underneath,
	 * while every other sprite has a uniform four-pixel margin.
	 */
	public int contentCenterX(int tileWidth)
	{
		return (int) Math.round(contentCenterX * scale(tileWidth));
	}

	/** Distance from the sprite's top edge to the bottom of its opaque content. */
	public int contentBottom(int tileWidth)
	{
		return (int) Math.round(contentBottom * scale(tileWidth));
	}

	private double scale(int tileWidth)
	{
		return scaledWidth(tileWidth) / (double) nativeWidth;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (!(other instanceof VillageSpriteMetadata))
		{
			return false;
		}
		VillageSpriteMetadata that = (VillageSpriteMetadata) other;
		return nativeWidth == that.nativeWidth
			&& nativeHeight == that.nativeHeight
			&& contentCenterX == that.contentCenterX
			&& contentBottom == that.contentBottom;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(nativeWidth, nativeHeight, contentCenterX, contentBottom);
	}

	@Override
	public String toString()
	{
		return nativeWidth + "x" + nativeHeight;
	}
}
