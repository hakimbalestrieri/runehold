package com.runehold.domain;

import java.util.Objects;

/**
 * Production rules for a gathering site. Placement, footprint, cost, level cap and
 * Town Hall requirement all live in {@link BuildingCatalog}, because a gathering site
 * is an ordinary placeable building.
 */
public final class GatheringSiteDefinition
{
	private final BuildingType type;
	private final ResourceType resourceType;
	private final int baseRatePerMinute;
	private final int baseStorage;
	private final int baseWorkers;

	GatheringSiteDefinition(
		BuildingType type,
		ResourceType resourceType,
		int baseRatePerMinute,
		int baseStorage,
		int baseWorkers)
	{
		this.type = Objects.requireNonNull(type, "type");
		this.resourceType = Objects.requireNonNull(resourceType, "resourceType");
		this.baseRatePerMinute = baseRatePerMinute;
		this.baseStorage = baseStorage;
		this.baseWorkers = baseWorkers;
	}

	public BuildingType getType()
	{
		return type;
	}

	public ResourceType getResourceType()
	{
		return resourceType;
	}

	public int productionPerMinute(int level, int workers)
	{
		if (level <= 0 || workers <= 0)
		{
			return 0;
		}
		return baseRatePerMinute * level * workers;
	}

	public int storageCapacity(int level)
	{
		return level <= 0 ? 0 : baseStorage * level;
	}

	public int maxWorkers(int level)
	{
		return level <= 0 ? 0 : baseWorkers + Math.max(0, level - 1);
	}
}
