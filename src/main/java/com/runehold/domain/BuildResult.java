package com.runehold.domain;

import com.runehold.domain.layout.PlacementResult;

public final class BuildResult
{
	public enum Status
	{
		SUCCESS,
		ALREADY_BUILT,
		LOCKED,
		INSUFFICIENT_MANA,
		BUILDER_BUSY,
		OUT_OF_BOUNDS,
		OCCUPIED
	}

	private final Status status;
	private final BuildingType type;
	private final int cost;
	private final BuildingType blockingType;

	private BuildResult(Status status, BuildingType type, int cost, BuildingType blockingType)
	{
		this.status = status;
		this.type = type;
		this.cost = cost;
		this.blockingType = blockingType;
	}

	public static BuildResult success(BuildingType type, int cost)
	{
		return new BuildResult(Status.SUCCESS, type, cost, null);
	}

	public static BuildResult alreadyBuilt(BuildingType type)
	{
		return new BuildResult(Status.ALREADY_BUILT, type, 0, null);
	}

	public static BuildResult fromUpgrade(BuildingType type, UpgradeResult result)
	{
		Status mapped = result.getStatus() == UpgradeResult.Status.LOCKED
			? Status.LOCKED : Status.INSUFFICIENT_MANA;
		return new BuildResult(mapped, type, result.getRequiredMana(), null);
	}

	public static BuildResult fromPlacement(BuildingType type, int cost, PlacementResult result)
	{
		Status mapped = result.getStatus() == PlacementResult.Status.OUT_OF_BOUNDS
			? Status.OUT_OF_BOUNDS : Status.OCCUPIED;
		return new BuildResult(mapped, type, cost, result.getBlockingType());
	}

	public static BuildResult builderBusy(BuildingType type)
	{
		return new BuildResult(Status.BUILDER_BUSY, type, 0, null);
	}

	public boolean isSuccess()
	{
		return status == Status.SUCCESS;
	}

	public Status getStatus()
	{
		return status;
	}

	public BuildingType getType()
	{
		return type;
	}

	public int getCost()
	{
		return cost;
	}

	public BuildingType getBlockingType()
	{
		return blockingType;
	}
}
