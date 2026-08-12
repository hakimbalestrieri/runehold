package com.runehold.domain;

import com.runehold.domain.layout.GridPoint;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GatheringSystemTest
{
	private BuildingCatalog buildingCatalog;
	private GatheringSiteCatalog siteCatalog;
	private VillageState state;
	private Village village;

	@Before
	public void setUp()
	{
		buildingCatalog = new BuildingCatalog();
		siteCatalog = new GatheringSiteCatalog();
		state = VillageState.fresh(LocalDate.of(2026, 8, 12));
		village = new Village(state, buildingCatalog, true,
			Clock.fixed(Instant.parse("2026-08-12T09:00:00Z"), ZoneOffset.UTC));
	}

	@Test
	public void freshVillageHasPopulationAndUnlockedGatheringSites()
	{
		assertEquals(8, state.getWorkers().size());
		assertEquals(8, state.getGatheringSites().size());
		assertEquals(1, state.getGatheringSite(GatheringSiteType.MINE).getLevel());
		assertEquals(0, state.getGatheringSite(
			GatheringSiteType.RUNE_ESSENCE_SITE).getLevel());
	}

	@Test
	public void assignsAvailableWorkerAndRejectsDuplicateAssignment()
	{
		AssignmentResult first = village.assignWorker("worker-1", GatheringSiteType.MINE);
		AssignmentResult duplicate = village.assignWorker(
			"worker-1",
			GatheringSiteType.QUARRY);

		assertTrue(first.isSuccess());
		assertEquals(WorkerState.WORKING, state.getWorker("worker-1").getState());
		assertEquals(GatheringSiteType.MINE, state.getWorker("worker-1").getAssignment());
		assertEquals(1, state.getGatheringSite(
			GatheringSiteType.MINE).getAssignedWorkerIds().size());
		assertEquals(AssignmentResult.Status.ALREADY_ASSIGNED, duplicate.getStatus());
	}

	@Test
	public void removeWorkerStopsSiteProduction()
	{
		assertTrue(village.assignWorker("worker-1", GatheringSiteType.MINE).isSuccess());
		assertTrue(village.removeWorker("worker-1").isSuccess());

		assertEquals(WorkerState.IDLE, state.getWorker("worker-1").getState());
		assertTrue(state.getGatheringSite(
			GatheringSiteType.MINE).getAssignedWorkerIds().isEmpty());
	}

	@Test
	public void productionRequiresAssignedWorkerAndRespectsLocalCapacity()
	{
		village = new Village(state, buildingCatalog, true,
			Clock.fixed(Instant.parse("2026-08-12T10:00:00Z"), ZoneOffset.UTC));

		village.updateGatheringProduction();
		assertEquals(0, state.getGatheringSite(GatheringSiteType.MINE).getStoredAmount());

		village.assignWorker("worker-1", GatheringSiteType.MINE);
		village = new Village(state, buildingCatalog, true,
			Clock.fixed(Instant.parse("2026-08-12T10:30:00Z"), ZoneOffset.UTC));
		village.updateGatheringProduction();

		GatheringSiteState mine = state.getGatheringSite(GatheringSiteType.MINE);
		assertEquals(siteCatalog.get(GatheringSiteType.MINE).storageCapacity(1),
			mine.getStoredAmount());
		assertEquals("Local storage full", mine.getBlockedReason());
	}

	@Test
	public void collectionMovesLocalStorageToGeneralInventory()
	{
		assertTrue(village.assignWorker("worker-1", GatheringSiteType.MINE).isSuccess());
		village = new Village(state, buildingCatalog, true,
			Clock.fixed(Instant.parse("2026-08-12T09:10:00Z"), ZoneOffset.UTC));
		ResourceCollectResult result = village.collectGatheringSite(GatheringSiteType.MINE);

		assertEquals(ResourceType.ORE, result.getResourceType());
		assertEquals(30, result.getCollected());
		assertEquals(30, state.getResources().get(ResourceType.ORE));
		assertEquals(0, state.getGatheringSite(GatheringSiteType.MINE).getStoredAmount());
	}

	@Test
	public void offlineProgressIsCappedAndAppliedOnce()
	{
		assertTrue(village.assignWorker("worker-1", GatheringSiteType.WOODCUTTING_GROVE).isSuccess());
		state.setLastOfflineProgressAtEpochMillis(
			Instant.parse("2026-08-12T09:00:00Z").toEpochMilli());
		village = new Village(state, buildingCatalog, true,
			Clock.fixed(Instant.parse("2026-08-13T00:00:00Z"), ZoneOffset.UTC));

		OfflineProgressSummary summary = village.applyOfflineProgress();
		OfflineProgressSummary second = village.applyOfflineProgress();

		assertEquals(480, summary.getAppliedMinutes());
		assertTrue(summary.getProduced().get(ResourceType.LOGS) > 0);
		assertEquals(0, (int) second.getProduced().get(ResourceType.LOGS));
	}

	@Test
	public void pathfindingRejectsBlockedOrOutOfBoundsDestination()
	{
		PathfindingService pathfinding = new PathfindingService(
			buildingCatalog,
			siteCatalog);

		assertTrue(pathfinding.hasPath(state, new GridPoint(6, 9), new GridPoint(4, 2)));
		assertFalse(pathfinding.hasPath(state, new GridPoint(6, 9), new GridPoint(-1, 0)));
	}
}
