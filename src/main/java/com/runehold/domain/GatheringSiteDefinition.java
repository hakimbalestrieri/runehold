package com.runehold.domain;

import com.runehold.domain.layout.Footprint;
import com.runehold.domain.layout.GridPoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class GatheringSiteDefinition
{
	private final GatheringSiteType type;
	private final ResourceType resourceType;
	private final Footprint footprint;
	private final GridPoint position;
	private final int requiredTownHallLevel;
	private final int baseRatePerMinute;
	private final int baseStorage;
	private final int baseWorkers;
	private final int maxLevel;
	private final List<GridPoint> accessPoints;

	GatheringSiteDefinition(
		GatheringSiteType type,
		ResourceType resourceType,
		Footprint footprint,
		GridPoint position,
		int requiredTownHallLevel,
		int baseRatePerMinute,
		int baseStorage,
		int baseWorkers,
		int maxLevel,
		List<GridPoint> accessPoints)
	{
		this.type = Objects.requireNonNull(type, "type");
		this.resourceType = Objects.requireNonNull(resourceType, "resourceType");
		this.footprint = Objects.requireNonNull(footprint, "footprint");
		this.position = Objects.requireNonNull(position, "position");
		this.requiredTownHallLevel = requiredTownHallLevel;
		this.baseRatePerMinute = baseRatePerMinute;
		this.baseStorage = baseStorage;
		this.baseWorkers = baseWorkers;
		this.maxLevel = maxLevel;
		this.accessPoints = Collections.unmodifiableList(new ArrayList<>(
			Objects.requireNonNull(accessPoints, "accessPoints")));
	}

	public GatheringSiteType getType()
	{
		return type;
	}

	public ResourceType getResourceType()
	{
		return resourceType;
	}

	public Footprint getFootprint()
	{
		return footprint;
	}

	public GridPoint getPosition()
	{
		return position;
	}

	public int getRequiredTownHallLevel()
	{
		return requiredTownHallLevel;
	}

	public int getMaxLevel()
	{
		return maxLevel;
	}

	public List<GridPoint> getAccessPoints()
	{
		return accessPoints;
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

	public int upgradeCost(int targetLevel)
	{
		if (targetLevel < 2 || targetLevel > maxLevel)
		{
			throw new IllegalArgumentException("invalid target level for " + type);
		}
		return baseStorage * targetLevel;
	}
}
