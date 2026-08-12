package com.runehold.ui;

import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.BuildResult;
import com.runehold.domain.CollectResult;
import com.runehold.domain.AssignmentResult;
import com.runehold.domain.BuildingType;
import com.runehold.domain.OfflineProgressSummary;
import com.runehold.domain.ResourceCollectResult;
import com.runehold.domain.UpgradeResult;
import com.runehold.domain.Village;
import com.runehold.domain.VillageState;
import com.runehold.domain.layout.GridPoint;
import com.runehold.domain.layout.PlacementResult;
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

	public UpgradeResult beginTimedUpgrade(BuildingType type)
	{
		UpgradeResult result = village.beginTimedUpgrade(type);
		if (result.isSuccess())
		{
			saveState.accept(state);
		}
		return result;
	}

	public BuildResult build(BuildingType type, GridPoint destination)
	{
		BuildResult result = village.build(type, destination);
		if (result.isSuccess())
		{
			saveState.accept(state);
		}
		return result;
	}

	public PlacementResult move(BuildingType type, GridPoint destination)
	{
		PlacementResult result = village.move(type, destination);
		if (result.isSuccess())
		{
			saveState.accept(state);
		}
		return result;
	}

	public boolean completeConstructionIfReady()
	{
		boolean completed = village.completeConstructionIfReady();
		if (completed)
		{
			saveState.accept(state);
		}
		return completed;
	}

	public CollectResult collectManaGrove()
	{
		CollectResult result = village.collectManaGrove();
		if (result.getManaCollected() > 0)
		{
			saveState.accept(state);
		}
		return result;
	}

	public AssignmentResult assignWorker(String workerId, BuildingType siteType)
	{
		AssignmentResult result = village.assignWorker(workerId, siteType);
		if (result.isSuccess())
		{
			saveState.accept(state);
		}
		return result;
	}

	public AssignmentResult removeWorker(String workerId)
	{
		AssignmentResult result = village.removeWorker(workerId);
		if (result.isSuccess())
		{
			saveState.accept(state);
		}
		return result;
	}

	public ResourceCollectResult collectGatheringSite(BuildingType siteType)
	{
		ResourceCollectResult result = village.collectGatheringSite(siteType);
		if (result.getCollected() > 0 || result.getRemainingAtSite() > 0)
		{
			saveState.accept(state);
		}
		return result;
	}

	public OfflineProgressSummary applyOfflineProgress()
	{
		OfflineProgressSummary summary = village.applyOfflineProgress();
		saveState.accept(state);
		return summary;
	}
}
