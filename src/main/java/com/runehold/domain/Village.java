package com.runehold.domain;

import com.runehold.domain.layout.GridPoint;
import com.runehold.domain.layout.PlacementResult;
import com.runehold.domain.layout.VillageLayout;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class Village
{
	private static final int TEST_BUILD_DURATION_SECONDS = 3;
	private final VillageState state;
	private final BuildingCatalog catalog;
	private final GatheringSiteCatalog gatheringCatalog = new GatheringSiteCatalog();
	private final boolean unlimitedMana;
	private final Clock clock;
	private final ProductionService productionService;
	private final AssignmentService assignmentService;
	private final OfflineProgressService offlineProgressService;

	public Village(VillageState state, BuildingCatalog catalog)
	{
		this(state, catalog, false, Clock.systemUTC());
	}

	public Village(VillageState state, BuildingCatalog catalog, boolean unlimitedMana)
	{
		this(state, catalog, unlimitedMana, Clock.systemUTC());
	}

	public Village(VillageState state, BuildingCatalog catalog, boolean unlimitedMana, Clock clock)
	{
		this.state = Objects.requireNonNull(state, "state");
		this.catalog = Objects.requireNonNull(catalog, "catalog");
		this.unlimitedMana = unlimitedMana;
		this.clock = Objects.requireNonNull(clock, "clock");
		productionService = new ProductionService(gatheringCatalog, clock);
		assignmentService = new AssignmentService(
			catalog,
			gatheringCatalog,
			new PathfindingService(catalog, gatheringCatalog));
		offlineProgressService = new OfflineProgressService(
			gatheringCatalog,
			productionService,
			clock);
	}

	public int levelOf(BuildingType type)
	{
		return state.levelOf(type);
	}

	public UpgradeResult upgrade(BuildingType type)
	{
		completeConstructionIfReady();
		if (state.getConstructionJob() != null)
		{
			return UpgradeResult.builderBusy(type, levelOf(type));
		}
		if (type != null && levelOf(type) == 0)
		{
			VillageLayout layout = layout();
			GridPoint destination = layout.findFirstAvailable(type);
			if (destination == null)
			{
				throw new IllegalStateException("no available plot for " + type);
			}
			UpgradeResult preview = previewUpgrade(type);
			if (!preview.isSuccess())
			{
				return preview;
			}
			PlacementResult placement = layout.previewPlace(type, destination);
			if (!placement.isSuccess())
			{
				throw new IllegalStateException("default construction failed: " + placement.getStatus());
			}
			if (!unlimitedMana)
			{
				state.spendMana(preview.getRequiredMana());
			}
			state.setBuildingLevel(type, preview.getNewLevel());
			state.setBuildingPosition(type, destination);
			return preview;
		}
		UpgradeResult result = previewUpgrade(type);
		if (!result.isSuccess())
		{
			return result;
		}

		if (!unlimitedMana)
		{
			state.spendMana(result.getRequiredMana());
		}
		state.setBuildingLevel(type, result.getNewLevel());
		return result;
	}

	public UpgradeResult beginTimedUpgrade(BuildingType type)
	{
		completeConstructionIfReady();
		UpgradeResult result = previewUpgrade(type);
		if (!result.isSuccess())
		{
			return result;
		}
		spendManaUnlessTesting(result.getRequiredMana());
		state.setConstructionJob(new ConstructionJob(
			type,
			result.getNewLevel(),
			Instant.now(clock).plusSeconds(
				constructionDurationSeconds(type, result.getNewLevel()))));
		return result;
	}

	public BuildResult build(BuildingType type, GridPoint destination)
	{
		if (type == null || destination == null)
		{
			throw new IllegalArgumentException("building and destination are required");
		}
		completeConstructionIfReady();
		if (state.getConstructionJob() != null)
		{
			return BuildResult.builderBusy(type);
		}
		if (levelOf(type) > 0)
		{
			return BuildResult.alreadyBuilt(type);
		}

		UpgradeResult upgrade = previewUpgrade(type);
		if (!upgrade.isSuccess())
		{
			return BuildResult.fromUpgrade(type, upgrade);
		}

		PlacementResult placement = layout().previewPlace(type, destination);
		if (!placement.isSuccess())
		{
			return BuildResult.fromPlacement(type, upgrade.getRequiredMana(), placement);
		}

		spendManaUnlessTesting(upgrade.getRequiredMana());
		state.setBuildingPosition(type, destination);
		state.setConstructionJob(new ConstructionJob(
			type,
			1,
			Instant.now(clock).plusSeconds(constructionDurationSeconds(type, 1))));
		return BuildResult.success(type, upgrade.getRequiredMana());
	}

	private void spendManaUnlessTesting(int mana)
	{
		if (!unlimitedMana)
		{
			state.spendMana(mana);
		}
	}

	private int constructionDurationSeconds(BuildingType type, int targetLevel)
	{
		return unlimitedMana
			? TEST_BUILD_DURATION_SECONDS
			: catalog.getBuildDurationSeconds(type, targetLevel);
	}

	public PlacementResult previewBuild(BuildingType type, GridPoint destination)
	{
		if (type == null || destination == null)
		{
			throw new IllegalArgumentException("building and destination are required");
		}
		return layout().previewPlace(type, destination);
	}

	public PlacementResult previewMove(BuildingType type, GridPoint destination)
	{
		return layout().previewMove(type, destination);
	}

	public PlacementResult move(BuildingType type, GridPoint destination)
	{
		completeConstructionIfReady();
		if (state.getConstructionJob() != null)
		{
			return PlacementResult.busy();
		}
		VillageLayout layout = layout();
		PlacementResult result = layout.move(type, destination);
		if (result.isSuccess())
		{
			state.setBuildingPosition(type, destination);
		}
		return result;
	}

	public GridPoint positionOf(BuildingType type)
	{
		return state.positionOf(type);
	}

	public VillageLayout layout()
	{
		return VillageLayout.restore(catalog, state.getBuildingPositions());
	}

	public boolean completeConstructionIfReady()
	{
		ConstructionJob job = state.getConstructionJob();
		if (job == null || Instant.now(clock).isBefore(job.getCompletesAt()))
		{
			return false;
		}
		state.setBuildingLevel(job.getBuildingType(), job.getTargetLevel());
		startProductionClockIfNeeded(job.getBuildingType());
		state.setConstructionJob(null);
		return true;
	}

	private void startProductionClockIfNeeded(BuildingType type)
	{
		if (type == BuildingType.MANA_GROVE
			&& state.getGroveProductionUpdatedAtEpochMillis() == 0)
		{
			state.setStoredGroveMana(state.getStoredGroveMana(), Instant.now(clock).toEpochMilli());
		}
	}

	public ConstructionJob getConstructionJob()
	{
		return state.getConstructionJob();
	}

	public int getManaCapacity()
	{
		int capacity = 0;
		for (BuildingType type : BuildingType.values())
		{
			capacity += catalog.getManaCapacityForLevel(type, state.levelOf(type));
		}
		return capacity;
	}

	public CollectResult collectManaGrove()
	{
		completeConstructionIfReady();
		updateGroveProduction();
		int collected = state.getStoredGroveMana();
		if (collected > 0 && !unlimitedMana)
		{
			state.addMana(collected);
		}
		state.setStoredGroveMana(0, Instant.now(clock).toEpochMilli());
		return new CollectResult(collected);
	}

	public AssignmentResult assignWorker(String workerId, GatheringSiteType siteType)
	{
		productionService.updateAll(state);
		return assignmentService.assign(state, workerId, siteType);
	}

	public AssignmentResult removeWorker(String workerId)
	{
		productionService.updateAll(state);
		return assignmentService.remove(state, workerId);
	}

	public ResourceCollectResult collectGatheringSite(GatheringSiteType siteType)
	{
		return productionService.collect(state, siteType);
	}

	public OfflineProgressSummary applyOfflineProgress()
	{
		return offlineProgressService.apply(state);
	}

	public void updateGatheringProduction()
	{
		productionService.updateAll(state);
	}

	public GatheringSiteCatalog gatheringCatalog()
	{
		return gatheringCatalog;
	}

	public int getCollectableGroveMana()
	{
		updateGroveProduction();
		return state.getStoredGroveMana();
	}

	public int peekCollectableGroveMana()
	{
		int groveLevel = state.levelOf(BuildingType.MANA_GROVE);
		if (groveLevel <= 0)
		{
			return 0;
		}
		long previous = state.getGroveProductionUpdatedAtEpochMillis();
		if (previous == 0)
		{
			return state.getStoredGroveMana();
		}
		long now = Instant.now(clock).toEpochMilli();
		long intervals = Math.max(0, (now - previous) / 60_000L);
		int storageCap = catalog.getPassiveStorageForLevel(BuildingType.MANA_GROVE, groveLevel);
		long produced = (long) catalog.getManaProductionPerMinuteForLevel(
			BuildingType.MANA_GROVE,
			groveLevel) * intervals;
		return (int) Math.min(storageCap, state.getStoredGroveMana() + produced);
	}

	private void updateGroveProduction()
	{
		int groveLevel = state.levelOf(BuildingType.MANA_GROVE);
		if (groveLevel <= 0)
		{
			return;
		}
		long now = Instant.now(clock).toEpochMilli();
		long previous = state.getGroveProductionUpdatedAtEpochMillis();
		if (previous == 0)
		{
			state.setStoredGroveMana(state.getStoredGroveMana(), now);
			return;
		}
		long intervals = Math.max(0, (now - previous) / 60_000L);
		if (intervals == 0)
		{
			return;
		}
		state.setStoredGroveMana(peekCollectableGroveMana(), now);
	}

	public boolean hasUnlimitedMana()
	{
		return unlimitedMana;
	}

	public UpgradeResult previewUpgrade(BuildingType type)
	{
		if (type == null)
		{
			throw new IllegalArgumentException("building type is required");
		}
		if (state.getConstructionJob() != null)
		{
			return UpgradeResult.builderBusy(type, state.levelOf(type));
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
		if (!unlimitedMana && state.getMana() < requiredMana)
		{
			return UpgradeResult.insufficientMana(type, currentLevel, requiredMana);
		}

		return UpgradeResult.success(type, targetLevel, requiredMana);
	}
}
