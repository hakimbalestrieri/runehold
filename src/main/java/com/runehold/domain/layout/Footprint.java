package com.runehold.domain.layout;

import java.util.Objects;

public final class Footprint
{
	private final int width;
	private final int height;

	public Footprint(int width, int height)
	{
		if (width < 1 || height < 1)
		{
			throw new IllegalArgumentException("footprint dimensions must be positive");
		}
		this.width = width;
		this.height = height;
	}

	public int getWidth()
	{
		return width;
	}

	public int getHeight()
	{
		return height;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (!(other instanceof Footprint))
		{
			return false;
		}
		Footprint that = (Footprint) other;
		return width == that.width && height == that.height;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(width, height);
	}

	@Override
	public String toString()
	{
		return "Footprint{" + "width=" + width + ", height=" + height + '}';
	}
}
