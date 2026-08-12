package com.runehold.domain;

public final class UpgradeResult
{
	public enum Status
	{
		SUCCESS,
		LOCKED,
		INSUFFICIENT_MANA,
		BUILDER_BUSY,
		MAX_LEVEL
	}

	private final Status status;
	private final BuildingType buildingType;
	private final int newLevel;
	private final int requiredMana;
	private final int requiredTownHallLevel;

	private UpgradeResult(
		Status status,
		BuildingType buildingType,
		int newLevel,
		int requiredMana,
		int requiredTownHallLevel)
	{
		this.status = status;
		this.buildingType = buildingType;
		this.newLevel = newLevel;
		this.requiredMana = requiredMana;
		this.requiredTownHallLevel = requiredTownHallLevel;
	}

	static UpgradeResult success(BuildingType type, int newLevel, int manaSpent)
	{
		return new UpgradeResult(Status.SUCCESS, type, newLevel, manaSpent, 0);
	}

	static UpgradeResult locked(BuildingType type, int currentLevel, int requiredTownHallLevel)
	{
		return new UpgradeResult(Status.LOCKED, type, currentLevel, 0, requiredTownHallLevel);
	}

	static UpgradeResult insufficientMana(BuildingType type, int currentLevel, int requiredMana)
	{
		return new UpgradeResult(Status.INSUFFICIENT_MANA, type, currentLevel, requiredMana, 0);
	}

	static UpgradeResult builderBusy(BuildingType type, int currentLevel)
	{
		return new UpgradeResult(Status.BUILDER_BUSY, type, currentLevel, 0, 0);
	}

	static UpgradeResult maxLevel(BuildingType type, int currentLevel)
	{
		return new UpgradeResult(Status.MAX_LEVEL, type, currentLevel, 0, 0);
	}

	public boolean isSuccess()
	{
		return status == Status.SUCCESS;
	}

	public Status getStatus()
	{
		return status;
	}

	public BuildingType getBuildingType()
	{
		return buildingType;
	}

	public int getNewLevel()
	{
		return newLevel;
	}

	public int getRequiredMana()
	{
		return requiredMana;
	}

	public int getRequiredTownHallLevel()
	{
		return requiredTownHallLevel;
	}
}
