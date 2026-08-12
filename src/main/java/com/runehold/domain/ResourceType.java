package com.runehold.domain;

public enum ResourceType
{
	LOGS("Logs"),
	STONE("Stone"),
	ORE("Ore"),
	FISH("Fish"),
	CROPS("Crops"),
	HERBS("Herbs"),
	CLAY("Clay"),
	RUNE_ESSENCE("Rune Essence");

	private final String displayName;

	ResourceType(String displayName)
	{
		this.displayName = displayName;
	}

	public String getDisplayName()
	{
		return displayName;
	}
}
