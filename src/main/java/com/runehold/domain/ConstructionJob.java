package com.runehold.domain;

import java.time.Instant;
import java.util.Objects;

public final class ConstructionJob
{
	private final BuildingType buildingType;
	private final int targetLevel;
	private final Instant completesAt;

	public ConstructionJob(BuildingType buildingType, int targetLevel, Instant completesAt)
	{
		this.buildingType = Objects.requireNonNull(buildingType, "buildingType");
		if (targetLevel < 1)
		{
			throw new IllegalArgumentException("target level must be positive");
		}
		this.targetLevel = targetLevel;
		this.completesAt = Objects.requireNonNull(completesAt, "completesAt");
	}

	public BuildingType getBuildingType()
	{
		return buildingType;
	}

	public int getTargetLevel()
	{
		return targetLevel;
	}

	public Instant getCompletesAt()
	{
		return completesAt;
	}
}
