package com.runehold.domain;

public enum BuildingType
{
	TOWN_HALL(Kind.STRUCTURE),
	MANA_WELL(Kind.STRUCTURE),
	MANA_GROVE(Kind.STRUCTURE),
	BARRACKS(Kind.STRUCTURE),
	WORKSHOP(Kind.STRUCTURE),
	RUNE_BANNER(Kind.STRUCTURE),
	MINE(Kind.GATHERING_SITE),
	FISHING_SPOT(Kind.GATHERING_SITE),
	WOODCUTTING_GROVE(Kind.GATHERING_SITE),
	QUARRY(Kind.GATHERING_SITE),
	FARM(Kind.GATHERING_SITE),
	HERB_PATCH(Kind.GATHERING_SITE),
	CLAY_PIT(Kind.GATHERING_SITE),
	RUNE_ESSENCE_SITE(Kind.GATHERING_SITE);

	/**
	 * What a building type is, declared per constant rather than inferred from
	 * declaration order. Reordering or inserting a constant must never silently
	 * reclassify a building.
	 */
	public enum Kind
	{
		STRUCTURE,
		GATHERING_SITE
	}

	private final Kind kind;

	BuildingType(Kind kind)
	{
		this.kind = kind;
	}

	public Kind getKind()
	{
		return kind;
	}

	/**
	 * Gathering sites are ordinary placeable buildings that additionally carry a
	 * production site, so a single layout owns every footprint on the map.
	 */
	public boolean isGatheringSite()
	{
		return kind == Kind.GATHERING_SITE;
	}
}
