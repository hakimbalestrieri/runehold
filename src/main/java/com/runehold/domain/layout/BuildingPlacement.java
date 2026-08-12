package com.runehold.domain.layout;

import com.runehold.domain.BuildingType;
import java.util.Objects;

public final class BuildingPlacement
{
	private final BuildingType type;
	private final GridPoint position;

	public BuildingPlacement(BuildingType type, GridPoint position)
	{
		this.type = Objects.requireNonNull(type, "type");
		this.position = Objects.requireNonNull(position, "position");
	}

	public BuildingType getType()
	{
		return type;
	}

	public GridPoint getPosition()
	{
		return position;
	}
}
