package com.runehold.ui;

import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.BuildingType;
import com.runehold.domain.UpgradeResult;
import com.runehold.domain.Village;
import com.runehold.domain.VillageState;
import java.util.Objects;
import java.util.function.Consumer;

public final class RuneholdController
{
	private final VillageState state;
	private final Village village;
	private final BuildingCatalog catalog;
	private final Consumer<VillageState> saveState;

	public RuneholdController(
		VillageState state,
		Village village,
		BuildingCatalog catalog,
		Consumer<VillageState> saveState)
	{
		this.state = Objects.requireNonNull(state, "state");
		this.village = Objects.requireNonNull(village, "village");
		this.catalog = Objects.requireNonNull(catalog, "catalog");
		this.saveState = Objects.requireNonNull(saveState, "saveState");
	}

	public RuneholdViewModel getViewModel()
	{
		return RuneholdViewModel.from(state, village, catalog);
	}

	public UpgradeResult upgrade(BuildingType type)
	{
		UpgradeResult result = village.upgrade(type);
		if (result.isSuccess())
		{
			saveState.accept(state);
		}
		return result;
	}
}
