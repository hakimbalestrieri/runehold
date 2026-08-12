package com.runehold.domain.layout;

import com.runehold.domain.BuildingType;

public final class PlacementResult
{
	public enum Status
	{
		SUCCESS,
		OUT_OF_BOUNDS,
		OCCUPIED,
		ALREADY_PLACED,
		NOT_PLACED
	}

	private final Status status;
	private final BuildingType blockingType;

	private PlacementResult(Status status, BuildingType blockingType)
	{
		this.status = status;
		this.blockingType = blockingType;
	}

	public static PlacementResult success()
	{
		return new PlacementResult(Status.SUCCESS, null);
	}

	public static PlacementResult outOfBounds()
	{
		return new PlacementResult(Status.OUT_OF_BOUNDS, null);
	}

	public static PlacementResult occupied(BuildingType blockingType)
	{
		return new PlacementResult(Status.OCCUPIED, blockingType);
	}

	public static PlacementResult alreadyPlaced()
	{
		return new PlacementResult(Status.ALREADY_PLACED, null);
	}

	public static PlacementResult notPlaced()
	{
		return new PlacementResult(Status.NOT_PLACED, null);
	}

	public boolean isSuccess()
	{
		return status == Status.SUCCESS;
	}

	public Status getStatus()
	{
		return status;
	}

	public BuildingType getBlockingType()
	{
		return blockingType;
	}
}
