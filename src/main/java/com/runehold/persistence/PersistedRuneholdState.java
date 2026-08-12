package com.runehold.persistence;

import com.runehold.domain.BuildingType;
import com.runehold.domain.ConstructionJob;
import com.runehold.domain.GatheringSiteState;
import com.runehold.domain.GatheringSiteType;
import com.runehold.domain.ResourceType;
import com.runehold.domain.VillageState;
import com.runehold.domain.Worker;
import com.runehold.domain.layout.GridPoint;
import java.util.LinkedHashMap;
import java.util.Map;

final class PersistedRuneholdState
{
	static final int CURRENT_SCHEMA_VERSION = 5;

	int schemaVersion;
	long mana;
	int manaEarnedToday;
	String manaEarningDate;
	Map<String, Integer> xpBaselines;
	Map<String, Integer> xpRemainders;
	Map<String, Integer> buildingLevels;
	Map<String, PersistedGridPoint> buildingPositions;
	PersistedConstructionJob constructionJob;
	int storedGroveMana;
	long groveProductionUpdatedAtEpochMillis;
	Map<String, Long> resources;
	Map<String, PersistedGatheringSite> gatheringSites;
	Map<String, PersistedWorker> workers;
	long lastOfflineProgressAtEpochMillis;

	private PersistedRuneholdState()
	{
	}

	static PersistedRuneholdState fromDomain(VillageState state)
	{
		PersistedRuneholdState persisted = new PersistedRuneholdState();
		persisted.schemaVersion = CURRENT_SCHEMA_VERSION;
		persisted.mana = state.getMana();
		persisted.manaEarnedToday = state.getManaEarnedToday();
		persisted.manaEarningDate = state.getManaEarningDate().toString();
		persisted.xpBaselines = new LinkedHashMap<>(state.getXpBaselines());
		persisted.xpRemainders = new LinkedHashMap<>(state.getXpRemainders());
		persisted.buildingLevels = new LinkedHashMap<>();
		persisted.buildingPositions = new LinkedHashMap<>();
		for (BuildingType type : BuildingType.values())
		{
			persisted.buildingLevels.put(type.name(), state.levelOf(type));
			GridPoint point = state.positionOf(type);
			if (point != null)
			{
				persisted.buildingPositions.put(
					type.name(),
					new PersistedGridPoint(point.getX(), point.getY()));
			}
		}
		ConstructionJob job = state.getConstructionJob();
		if (job != null)
		{
			persisted.constructionJob = new PersistedConstructionJob(
				job.getBuildingType().name(),
				job.getTargetLevel(),
				job.getCompletesAt().toEpochMilli());
		}
		persisted.storedGroveMana = state.getStoredGroveMana();
		persisted.groveProductionUpdatedAtEpochMillis = state.getGroveProductionUpdatedAtEpochMillis();
		persisted.resources = new LinkedHashMap<>();
		for (Map.Entry<ResourceType, Long> entry : state.getResources().asMap().entrySet())
		{
			persisted.resources.put(entry.getKey().name(), entry.getValue());
		}
		persisted.gatheringSites = new LinkedHashMap<>();
		for (GatheringSiteState site : state.getGatheringSites().values())
		{
			persisted.gatheringSites.put(site.getType().name(), new PersistedGatheringSite(
				site.getLevel(),
				site.getStoredAmount(),
				site.getUpdatedAtEpochMillis(),
				site.getAssignedWorkerIds(),
				site.getBlockedReason()));
		}
		persisted.workers = new LinkedHashMap<>();
		for (Worker worker : state.getWorkers().values())
		{
			persisted.workers.put(worker.getId(), new PersistedWorker(
				worker.getName(),
				worker.getState().name(),
				worker.getAssignment() == null ? null : worker.getAssignment().name(),
				new PersistedGridPoint(worker.getPosition().getX(), worker.getPosition().getY()),
				worker.getDestination() == null ? null
					: new PersistedGridPoint(
						worker.getDestination().getX(),
						worker.getDestination().getY()),
				worker.getRole()));
		}
		persisted.lastOfflineProgressAtEpochMillis =
			state.getLastOfflineProgressAtEpochMillis();
		return persisted;
	}

	static final class PersistedGridPoint
	{
		int x;
		int y;

		private PersistedGridPoint()
		{
		}

		private PersistedGridPoint(int x, int y)
		{
			this.x = x;
			this.y = y;
		}
	}

	static final class PersistedConstructionJob
	{
		String buildingType;
		int targetLevel;
		long completesAtEpochMillis;

		private PersistedConstructionJob()
		{
		}

		private PersistedConstructionJob(String buildingType, int targetLevel,
			long completesAtEpochMillis)
		{
			this.buildingType = buildingType;
			this.targetLevel = targetLevel;
			this.completesAtEpochMillis = completesAtEpochMillis;
		}
	}

	static final class PersistedGatheringSite
	{
		int level;
		int storedAmount;
		long updatedAtEpochMillis;
		java.util.List<String> assignedWorkerIds;
		String blockedReason;

		private PersistedGatheringSite()
		{
		}

		private PersistedGatheringSite(
			int level,
			int storedAmount,
			long updatedAtEpochMillis,
			java.util.List<String> assignedWorkerIds,
			String blockedReason)
		{
			this.level = level;
			this.storedAmount = storedAmount;
			this.updatedAtEpochMillis = updatedAtEpochMillis;
			this.assignedWorkerIds = assignedWorkerIds;
			this.blockedReason = blockedReason;
		}
	}

	static final class PersistedWorker
	{
		String name;
		String state;
		String assignment;
		PersistedGridPoint position;
		PersistedGridPoint destination;
		String role;

		private PersistedWorker()
		{
		}

		private PersistedWorker(
			String name,
			String state,
			String assignment,
			PersistedGridPoint position,
			PersistedGridPoint destination,
			String role)
		{
			this.name = name;
			this.state = state;
			this.assignment = assignment;
			this.position = position;
			this.destination = destination;
			this.role = role;
		}
	}
}
