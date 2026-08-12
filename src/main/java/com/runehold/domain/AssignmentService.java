package com.runehold.domain;

import com.runehold.domain.layout.GridPoint;
import java.util.Objects;

public final class AssignmentService
{
	private static final GridPoint HOME = new GridPoint(6, 9);
	private final BuildingCatalog buildingCatalog;
	private final GatheringSiteCatalog siteCatalog;
	private final PathfindingService pathfinding;

	public AssignmentService(
		BuildingCatalog buildingCatalog,
		GatheringSiteCatalog siteCatalog,
		PathfindingService pathfinding)
	{
		this.buildingCatalog = Objects.requireNonNull(buildingCatalog, "buildingCatalog");
		this.siteCatalog = Objects.requireNonNull(siteCatalog, "siteCatalog");
		this.pathfinding = Objects.requireNonNull(pathfinding, "pathfinding");
	}

	public AssignmentResult assign(VillageState state, String workerId, GatheringSiteType siteType)
	{
		Objects.requireNonNull(state, "state");
		Worker worker = state.getWorker(workerId);
		if (worker == null)
		{
			return AssignmentResult.failure(AssignmentResult.Status.UNKNOWN_WORKER,
				"Unknown worker");
		}
		GatheringSiteState site = state.getGatheringSite(siteType);
		GatheringSiteDefinition definition = siteCatalog.get(siteType);
		if (site == null || site.getLevel() <= 0)
		{
			return AssignmentResult.failure(AssignmentResult.Status.SITE_LOCKED,
				"Site is locked");
		}
		if (state.levelOf(BuildingType.TOWN_HALL) < definition.getRequiredTownHallLevel())
		{
			return AssignmentResult.failure(AssignmentResult.Status.SITE_LOCKED,
				"Requires Town Hall level " + definition.getRequiredTownHallLevel());
		}
		if (worker.getAssignment() != null)
		{
			return AssignmentResult.failure(AssignmentResult.Status.ALREADY_ASSIGNED,
				worker.getName() + " is already assigned");
		}
		if (site.getAssignedWorkerIds().size() >= definition.maxWorkers(site.getLevel()))
		{
			return AssignmentResult.failure(AssignmentResult.Status.WORKER_LIMIT,
				"No worker slot available");
		}
		GridPoint access = accessiblePoint(state, worker, definition);
		if (access == null)
		{
			site.setBlockedReason("No accessible path");
			return AssignmentResult.failure(AssignmentResult.Status.NO_ACCESSIBLE_PATH,
				"No accessible path");
		}
		site.addWorker(workerId);
		site.setBlockedReason(null);
		worker.assign(siteType, access);
		worker.arriveAtWork(access);
		state.putWorker(worker);
		state.putGatheringSite(site);
		return AssignmentResult.success();
	}

	public AssignmentResult remove(VillageState state, String workerId)
	{
		Objects.requireNonNull(state, "state");
		Worker worker = state.getWorker(workerId);
		if (worker == null)
		{
			return AssignmentResult.failure(AssignmentResult.Status.UNKNOWN_WORKER,
				"Unknown worker");
		}
		GatheringSiteType assignment = worker.getAssignment();
		if (assignment != null && state.getGatheringSite(assignment) != null)
		{
			GatheringSiteState site = state.getGatheringSite(assignment);
			site.removeWorker(workerId);
			state.putGatheringSite(site);
		}
		worker.setIdle(HOME);
		state.putWorker(worker);
		return AssignmentResult.success();
	}

	private GridPoint accessiblePoint(
		VillageState state,
		Worker worker,
		GatheringSiteDefinition definition)
	{
		for (GridPoint access : definition.getAccessPoints())
		{
			if (pathfinding.hasPath(state, worker.getPosition(), access))
			{
				return access;
			}
		}
		return null;
	}
}
