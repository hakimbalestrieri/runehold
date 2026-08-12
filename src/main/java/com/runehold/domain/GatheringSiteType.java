package com.runehold.domain;

public enum GatheringSiteType
{
	MINE("Mine"),
	FISHING_SPOT("Fishing Spot"),
	WOODCUTTING_GROVE("Woodcutting Grove"),
	QUARRY("Quarry"),
	FARM("Farm"),
	HERB_PATCH("Herb Patch"),
	CLAY_PIT("Clay Pit"),
	RUNE_ESSENCE_SITE("Rune Essence Site");

	private final String displayName;

	GatheringSiteType(String displayName)
	{
		this.displayName = displayName;
	}

	public String getDisplayName()
	{
		return displayName;
	}
}
