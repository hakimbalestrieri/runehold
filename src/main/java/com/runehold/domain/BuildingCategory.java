package com.runehold.domain;

public enum BuildingCategory
{
	CORE("Core"),
	PRODUCTION("Production"),
	GATHERING("Gathering"),
	MILITARY("Military"),
	UTILITY("Utility"),
	DECORATION("Decoration");

	private final String displayName;

	BuildingCategory(String displayName)
	{
		this.displayName = displayName;
	}

	public String getDisplayName()
	{
		return displayName;
	}
}
