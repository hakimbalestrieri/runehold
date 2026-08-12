package com.runehold.persistence;

import com.google.gson.Gson;
import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.BuildingType;
import com.runehold.domain.ConstructionJob;
import com.runehold.domain.GatheringSiteState;
import com.runehold.domain.GatheringSiteType;
import com.runehold.domain.ResourceInventory;
import com.runehold.domain.ResourceType;
import com.runehold.domain.VillageState;
import com.runehold.domain.Worker;
import com.runehold.domain.WorkerState;
import com.runehold.domain.layout.GridPoint;
import com.runehold.domain.layout.VillageLayout;
import java.time.LocalDate;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class RuneholdStateCodec
{
	static final int MAX_STATE_JSON_LENGTH = 64 * 1024;

	private final Gson gson;
	private final BuildingCatalog catalog;

	public RuneholdStateCodec(Gson gson, BuildingCatalog catalog)
	{
		this.gson = Objects.requireNonNull(gson, "gson");
		this.catalog = Objects.requireNonNull(catalog, "catalog");
	}

	public String encode(VillageState state)
	{
		return gson.toJson(PersistedRuneholdState.fromDomain(
			Objects.requireNonNull(state, "state")));
	}

	public VillageState decode(String json, LocalDate today)
	{
		Objects.requireNonNull(today, "today");
		if (json == null || json.length() > MAX_STATE_JSON_LENGTH || json.trim().isEmpty())
		{
			return VillageState.fresh(today);
		}

		try
		{
			PersistedRuneholdState persisted = gson.fromJson(json, PersistedRuneholdState.class);
			return restoreValidated(persisted);
		}
		catch (RuntimeException ex)
		{
			return VillageState.fresh(today);
		}
	}

	private VillageState restoreValidated(PersistedRuneholdState persisted)
	{
		if (persisted == null
			|| (persisted.schemaVersion != 1
				&& persisted.schemaVersion != 2
				&& persisted.schemaVersion != 3
				&& persisted.schemaVersion != 4
				&& persisted.schemaVersion != PersistedRuneholdState.CURRENT_SCHEMA_VERSION)
			|| persisted.manaEarningDate == null
			|| persisted.xpBaselines == null
			|| persisted.xpRemainders == null
			|| persisted.buildingLevels == null)
		{
			throw new IllegalArgumentException("incomplete Runehold state");
		}

		Map<BuildingType, Integer> buildingLevels = validateBuildingLevels(
			persisted.buildingLevels);
		ConstructionJob job = persisted.schemaVersion < 3 ? null
			: validateConstructionJob(persisted.constructionJob, buildingLevels);
		Map<BuildingType, GridPoint> positions = persisted.schemaVersion == 1
			? migrateLegacyPositions(buildingLevels)
			: validatePositions(persisted.buildingPositions, buildingLevels, job);

		return VillageState.restore(
			persisted.mana,
			persisted.xpBaselines,
			persisted.xpRemainders,
			persisted.manaEarnedToday,
			LocalDate.parse(persisted.manaEarningDate),
			buildingLevels,
			positions,
			job,
			persisted.schemaVersion < 4 ? 0 : persisted.storedGroveMana,
			persisted.schemaVersion < 4 ? 0 : persisted.groveProductionUpdatedAtEpochMillis,
			persisted.schemaVersion < 5 ? null : validateResources(persisted.resources),
			persisted.schemaVersion < 5 ? null : validateGatheringSites(persisted.gatheringSites),
			persisted.schemaVersion < 5 ? null : validateWorkers(persisted.workers),
			persisted.schemaVersion < 5 ? 0 : persisted.lastOfflineProgressAtEpochMillis);
	}

	private ResourceInventory validateResources(Map<String, Long> persistedResources)
	{
		if (persistedResources == null)
		{
			return new ResourceInventory();
		}
		Map<ResourceType, Long> resources = new EnumMap<>(ResourceType.class);
		for (Map.Entry<String, Long> entry : persistedResources.entrySet())
		{
			ResourceType type = ResourceType.valueOf(entry.getKey());
			Long amount = entry.getValue();
			if (amount == null || amount < 0 || amount > Integer.MAX_VALUE)
			{
				throw new IllegalArgumentException("invalid resource amount");
			}
			resources.put(type, amount);
		}
		return ResourceInventory.from(resources);
	}

	private Map<GatheringSiteType, GatheringSiteState> validateGatheringSites(
		Map<String, PersistedRuneholdState.PersistedGatheringSite> persistedSites)
	{
		if (persistedSites == null)
		{
			return null;
		}
		Map<GatheringSiteType, GatheringSiteState> sites =
			new EnumMap<>(GatheringSiteType.class);
		for (Map.Entry<String, PersistedRuneholdState.PersistedGatheringSite> entry
			: persistedSites.entrySet())
		{
			GatheringSiteType type = GatheringSiteType.valueOf(entry.getKey());
			PersistedRuneholdState.PersistedGatheringSite persisted = entry.getValue();
			if (persisted == null)
			{
				throw new IllegalArgumentException("missing gathering site");
			}
			sites.put(type, new GatheringSiteState(
				type,
				persisted.level,
				persisted.storedAmount,
				persisted.updatedAtEpochMillis,
				persisted.assignedWorkerIds,
				persisted.blockedReason));
		}
		for (GatheringSiteType type : GatheringSiteType.values())
		{
			if (!sites.containsKey(type))
			{
				sites.put(type, new GatheringSiteState(
					type,
					type == GatheringSiteType.RUNE_ESSENCE_SITE ? 0 : 1,
					0,
					0,
					null,
					type == GatheringSiteType.RUNE_ESSENCE_SITE
						? "Requires Town Hall level 3" : null));
			}
		}
		return sites;
	}

	private Map<String, Worker> validateWorkers(
		Map<String, PersistedRuneholdState.PersistedWorker> persistedWorkers)
	{
		if (persistedWorkers == null)
		{
			return null;
		}
		Map<String, Worker> workers = new LinkedHashMap<>();
		Set<String> assignedWorkerIds = new HashSet<>();
		for (Map.Entry<String, PersistedRuneholdState.PersistedWorker> entry
			: persistedWorkers.entrySet())
		{
			PersistedRuneholdState.PersistedWorker persisted = entry.getValue();
			if (persisted == null || persisted.position == null)
			{
				throw new IllegalArgumentException("missing worker");
			}
			GatheringSiteType assignment = persisted.assignment == null
				? null
				: GatheringSiteType.valueOf(persisted.assignment);
			if (assignment != null && !assignedWorkerIds.add(entry.getKey()))
			{
				throw new IllegalArgumentException("duplicate worker assignment");
			}
			Worker worker = new Worker(
				entry.getKey(),
				persisted.name,
				WorkerState.valueOf(persisted.state),
				assignment,
				new GridPoint(persisted.position.x, persisted.position.y),
				persisted.destination == null ? null
					: new GridPoint(persisted.destination.x, persisted.destination.y),
				persisted.role == null ? "Worker" : persisted.role);
			workers.put(worker.getId(), worker);
		}
		return workers;
	}

	private ConstructionJob validateConstructionJob(
		PersistedRuneholdState.PersistedConstructionJob persistedJob,
		Map<BuildingType, Integer> levels)
	{
		if (persistedJob == null)
		{
			return null;
		}
		BuildingType type = BuildingType.valueOf(persistedJob.buildingType);
		if (persistedJob.targetLevel < 1
			|| persistedJob.targetLevel > catalog.getMaxLevel(type)
			|| levels.get(type) >= persistedJob.targetLevel
			|| persistedJob.completesAtEpochMillis <= 0)
		{
			throw new IllegalArgumentException("invalid construction job");
		}
		return new ConstructionJob(type, persistedJob.targetLevel,
			Instant.ofEpochMilli(persistedJob.completesAtEpochMillis));
	}

	private Map<BuildingType, GridPoint> migrateLegacyPositions(
		Map<BuildingType, Integer> levels)
	{
		Map<BuildingType, GridPoint> positions = new EnumMap<>(BuildingType.class);
		for (BuildingType type : BuildingType.values())
		{
			if (levels.get(type) > 0)
			{
				positions.put(type, VillageLayout.defaultPosition(type));
			}
		}
		VillageLayout.restore(catalog, positions);
		return positions;
	}

	private Map<BuildingType, GridPoint> validatePositions(
		Map<String, PersistedRuneholdState.PersistedGridPoint> persistedPositions,
		Map<BuildingType, Integer> levels,
		ConstructionJob job)
	{
		if (persistedPositions == null)
		{
			throw new IllegalArgumentException("missing building positions");
		}
		Map<BuildingType, GridPoint> positions = new EnumMap<>(BuildingType.class);
		for (Map.Entry<String, PersistedRuneholdState.PersistedGridPoint> entry
			: persistedPositions.entrySet())
		{
			BuildingType type = BuildingType.valueOf(entry.getKey());
			PersistedRuneholdState.PersistedGridPoint point = entry.getValue();
			if (point == null)
			{
				throw new IllegalArgumentException("missing position for " + type);
			}
			positions.put(type, new GridPoint(point.x, point.y));
		}

		for (BuildingType type : BuildingType.values())
		{
			boolean built = levels.get(type) > 0
				|| (job != null && job.getBuildingType() == type);
			if (built != positions.containsKey(type))
			{
				throw new IllegalArgumentException("building position mismatch for " + type);
			}
		}
		VillageLayout.restore(catalog, positions);
		return positions;
	}

	private Map<BuildingType, Integer> validateBuildingLevels(Map<String, Integer> persistedLevels)
	{
		for (String key : persistedLevels.keySet())
		{
			BuildingType.valueOf(key);
		}

		Map<BuildingType, Integer> levels = new EnumMap<>(BuildingType.class);
		for (BuildingType type : BuildingType.values())
		{
			Integer persistedLevel = persistedLevels.get(type.name());
			int level = persistedLevel == null ? 0 : persistedLevel;
			if (level < 0 || level > catalog.getMaxLevel(type))
			{
				throw new IllegalArgumentException("invalid level for " + type);
			}
			levels.put(type, level);
		}

		int townHallLevel = levels.get(BuildingType.TOWN_HALL);
		if (townHallLevel < 1)
		{
			throw new IllegalArgumentException("Town Hall must be present");
		}

		for (BuildingType type : BuildingType.values())
		{
			int level = levels.get(type);
			if (type != BuildingType.TOWN_HALL
				&& level > 0
				&& catalog.getRequiredTownHallLevel(type, level) > townHallLevel)
			{
				throw new IllegalArgumentException("building progression is locked");
			}
		}

		return levels;
	}
}
