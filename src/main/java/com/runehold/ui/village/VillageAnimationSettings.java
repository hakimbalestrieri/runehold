package com.runehold.ui.village;

import com.runehold.RuneholdAnimationQuality;

public final class VillageAnimationSettings
{
	private static final int MIN_DENSITY = 0;
	private static final int MAX_DENSITY = 4;

	private final boolean enabled;
	private final int characterDensity;
	private final RuneholdAnimationQuality quality;
	private final boolean reduceMotion;

	public VillageAnimationSettings(
		boolean enabled,
		int characterDensity,
		RuneholdAnimationQuality quality,
		boolean reduceMotion)
	{
		this.enabled = enabled;
		this.characterDensity = clamp(characterDensity, MIN_DENSITY, MAX_DENSITY);
		this.quality = quality == null ? RuneholdAnimationQuality.STANDARD : quality;
		this.reduceMotion = reduceMotion;
	}

	public static VillageAnimationSettings defaults()
	{
		return new VillageAnimationSettings(true, 2, RuneholdAnimationQuality.STANDARD, false);
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public int getCharacterDensity()
	{
		return characterDensity;
	}

	public RuneholdAnimationQuality getQuality()
	{
		return quality;
	}

	public boolean isReduceMotion()
	{
		return reduceMotion;
	}

	public int frameMillis()
	{
		return reduceMotion ? Math.max(quality.getFrameMillis(), 500) : quality.getFrameMillis();
	}

	private static int clamp(int value, int minimum, int maximum)
	{
		return Math.max(minimum, Math.min(maximum, value));
	}
}
