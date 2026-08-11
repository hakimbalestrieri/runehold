package com.runehold.domain;

import java.util.Objects;

public final class Village
{
	private final VillageState state;
	private final BuildingCatalog catalog;

	public Village(VillageState state, BuildingCatalog catalog)
	{
		this.state = Objects.requireNonNull(state, "state");
		this.catalog = Objects.requireNonNull(catalog, "catalog");
	}

	public int levelOf(BuildingType type)
	{
		return state.levelOf(type);
	}

	public UpgradeResult upgrade(BuildingType type)
	{
		if (type == null)
		{
			throw new IllegalArgumentException("building type is required");
		}

		int currentLevel = state.levelOf(type);
		if (currentLevel >= catalog.getMaxLevel(type))
		{
			return UpgradeResult.maxLevel(type, currentLevel);
		}

		int targetLevel = currentLevel + 1;
		int requiredTownHallLevel = catalog.getRequiredTownHallLevel(type, targetLevel);
		if (type != BuildingType.TOWN_HALL
			&& state.levelOf(BuildingType.TOWN_HALL) < requiredTownHallLevel)
		{
			return UpgradeResult.locked(type, currentLevel, requiredTownHallLevel);
		}

		int requiredMana = catalog.getCostForTargetLevel(type, targetLevel);
		if (state.getMana() < requiredMana)
		{
			return UpgradeResult.insufficientMana(type, currentLevel, requiredMana);
		}

		state.spendMana(requiredMana);
		state.setBuildingLevel(type, targetLevel);
		return UpgradeResult.success(type, targetLevel, requiredMana);
	}
}
