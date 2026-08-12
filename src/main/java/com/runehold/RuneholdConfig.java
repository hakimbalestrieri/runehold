package com.runehold;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("runehold")
public interface RuneholdConfig extends Config
{
	@ConfigItem(
		keyName = "ambientAnimations",
		name = "Ambient animations",
		description = "Show subtle workers, patrols, flags, water and workshop motion in the Runehold village."
	)
	default boolean ambientAnimations()
	{
		return true;
	}

	@Range(
		min = 0,
		max = 4
	)
	@ConfigItem(
		keyName = "characterDensity",
		name = "Character density",
		description = "Controls how many purely visual village characters may appear."
	)
	default int characterDensity()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "animationQuality",
		name = "Animation quality",
		description = "Controls the village animation refresh rate."
	)
	default RuneholdAnimationQuality animationQuality()
	{
		return RuneholdAnimationQuality.STANDARD;
	}

	@ConfigItem(
		keyName = "reduceMotion",
		name = "Reduce motion",
		description = "Use fewer animation frames and slower, gentler motion."
	)
	default boolean reduceMotion()
	{
		return false;
	}
}
