package com.runehold.domain;

import com.runehold.domain.layout.GridPoint;
import java.util.List;
import java.util.Objects;

public final class AssignmentService
{
	static final GridPoint HOME = new GridPoint(6, 9);
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

	public AssignmentResult assign(VillageState state, String workerId, BuildingType siteType)
	{
		Objects.requireNonNull(state, "state");
		if (siteType == null || !siteCatalog.isSite(siteType))
		{
			return AssignmentResult.failure(AssignmentResult.Status.UNKNOWN_SITE,
				"Unknown gathering site");
		}
		Worker worker = state.getWorker(workerId);
		if (worker == null)
		{
			return AssignmentResult.failure(AssignmentResult.Status.UNKNOWN_WORKER,
				"Unknown worker");
		}
		int level = state.levelOf(siteType);
		GridPoint position = state.positionOf(siteType);
		if (level <= 0 || position == null)
		{
			return AssignmentResult.failure(AssignmentResult.Status.SITE_NOT_BUILT,
				buildingCatalog.getDisplayName(siteType) + " is not built yet");
		}
		if (worker.getAssignment() != null)
		{
			return AssignmentResult.failure(AssignmentResult.Status.ALREADY_ASSIGNED,
				worker.getName() + " is already assigned");
		}
		GatheringSiteState site = state.getGatheringSite(siteType);
		if (site == null)
		{
			return AssignmentResult.failure(AssignmentResult.Status.SITE_NOT_BUILT,
				buildingCatalog.getDisplayName(siteType) + " is not built yet");
		}
		GatheringSiteDefinition definition = siteCatalog.get(siteType);
		if (site.getAssignedWorkerIds().size() >= definition.maxWorkers(level))
		{
			return AssignmentResult.failure(AssignmentResult.Status.WORKER_LIMIT,
				"No worker slot available");
		}
		GridPoint access = accessiblePoint(state, worker, siteType, position);
		if (access == null)
		{
			return AssignmentResult.failure(AssignmentResult.Status.NO_ACCESSIBLE_PATH,
				"No accessible path");
		}

		site.addWorker(workerId);
		site.setBlockedReason(null);
		worker.assign(siteType, access, buildingCatalog.getDisplayName(siteType));
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
		BuildingType assignment = worker.getAssignment();
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

	/**
	 * Recalls every villager working a site, used when the site is removed or moved and
	 * its access tiles are no longer where the workers stand.
	 */
	public void recallAll(VillageState state, BuildingType siteType)
	{
		GatheringSiteState site = state.getGatheringSite(siteType);
		if (site == null)
		{
			return;
		}
		for (String workerId : site.getAssignedWorkerIds())
		{
			Worker worker = state.getWorker(workerId);
			if (worker != null)
			{
				worker.setIdle(HOME);
				state.putWorker(worker);
			}
		}
		site.clearWorkers();
		state.putGatheringSite(site);
	}

	private GridPoint accessiblePoint(
		VillageState state,
		Worker worker,
		BuildingType siteType,
		GridPoint position)
	{
		List<GridPoint> access = GatheringSiteCatalog.accessPoints(
			position,
			buildingCatalog.getFootprint(siteType));
		for (GridPoint candidate : access)
		{
			if (pathfinding.hasPath(state, worker.getPosition(), candidate))
			{
				return candidate;
			}
		}
		return null;
	}
}
