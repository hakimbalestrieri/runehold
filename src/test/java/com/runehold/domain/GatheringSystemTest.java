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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GatheringSystemTest
{
	private static final Instant START = Instant.parse("2026-08-12T09:00:00Z");

	private BuildingCatalog buildingCatalog;
	private GatheringSiteCatalog siteCatalog;
	private VillageState state;
	private Village village;
	private Instant now;

	@Before
	public void setUp()
	{
		buildingCatalog = new BuildingCatalog();
		siteCatalog = new GatheringSiteCatalog();
		state = VillageState.fresh(LocalDate.of(2026, 8, 12));
		now = START;
		village = villageAt(now);
	}

	private Village villageAt(Instant instant)
	{
		return new Village(state, buildingCatalog, true, Clock.fixed(instant, ZoneOffset.UTC));
	}

	/** Moves the shared clock forward and rebinds the village to it. */
	private void advance(long seconds)
	{
		now = now.plusSeconds(seconds);
		village = villageAt(now);
	}

	/**
	 * Places a site through the build catalogue and waits out its construction, as the
	 * player would. Testing mode builds in three seconds.
	 */
	private void place(BuildingType type, GridPoint destination)
	{
		assertTrue(village.build(type, destination).isSuccess());
		advance(5);
		assertTrue(village.completeConstructionIfReady());
	}

	@Test
	public void buildingKindIsDeclaredPerConstantNotInferredFromOrder()
	{
		assertEquals(BuildingType.Kind.STRUCTURE, BuildingType.TOWN_HALL.getKind());
		assertEquals(BuildingType.Kind.GATHERING_SITE, BuildingType.MINE.getKind());
		assertFalse(BuildingType.RUNE_BANNER.isGatheringSite());
		assertTrue(BuildingType.RUNE_ESSENCE_SITE.isGatheringSite());

		int sites = 0;
		for (BuildingType type : BuildingType.values())
		{
			assertEquals(type + " kind and predicate disagree",
				type.getKind() == BuildingType.Kind.GATHERING_SITE, type.isGatheringSite());
			if (type.isGatheringSite())
			{
				sites++;
			}
		}
		assertEquals(8, sites);
	}

	@Test
	public void freshVillageHasPopulationAndNoGatheringSitePlacedYet()
	{
		assertEquals(8, state.getWorkers().size());
		assertEquals(8, state.getGatheringSites().size());
		for (BuildingType type : BuildingType.values())
		{
			if (type.isGatheringSite())
			{
				assertEquals(0, state.levelOf(type));
				assertNull(state.positionOf(type));
				assertEquals(0, state.getGatheringSite(type).getStoredAmount());
			}
		}
	}

	@Test
	public void aSiteMustBePlacedOnTheMapBeforeWorkersCanBeAssigned()
	{
		AssignmentResult refused = village.assignWorker("worker-1", BuildingType.MINE);

		assertEquals(AssignmentResult.Status.SITE_NOT_BUILT, refused.getStatus());
		assertEquals("Mine is not built yet", refused.getMessage());
		assertNull(state.getWorker("worker-1").getAssignment());
	}

	@Test
	public void aPlacedSiteOccupiesTheLayoutAndRejectsOverlappingBuildings()
	{
		place(BuildingType.MINE, new GridPoint(1, 1));

		assertEquals(new GridPoint(1, 1), state.positionOf(BuildingType.MINE));
		assertFalse(village.previewBuild(BuildingType.MANA_GROVE, new GridPoint(2, 2))
			.isSuccess());
		assertFalse(village.previewBuild(BuildingType.QUARRY, new GridPoint(3, 3))
			.isSuccess());
	}

	@Test
	public void assignsAvailableWorkerAndRejectsDuplicateAssignment()
	{
		place(BuildingType.MINE, new GridPoint(1, 1));

		AssignmentResult first = village.assignWorker("worker-1", BuildingType.MINE);
		AssignmentResult duplicate = village.assignWorker("worker-1", BuildingType.MINE);

		assertTrue(first.isSuccess());
		assertEquals(WorkerState.WORKING, state.getWorker("worker-1").getState());
		assertEquals(BuildingType.MINE, state.getWorker("worker-1").getAssignment());
		assertEquals(1, state.getGatheringSite(
			BuildingType.MINE).getAssignedWorkerIds().size());
		assertEquals(AssignmentResult.Status.ALREADY_ASSIGNED, duplicate.getStatus());
	}

	@Test
	public void assignmentIsRefusedOnceEveryWorkerSlotIsTaken()
	{
		place(BuildingType.MINE, new GridPoint(1, 1));
		assertTrue(village.assignWorker("worker-1", BuildingType.MINE).isSuccess());

		AssignmentResult refused = village.assignWorker("worker-2", BuildingType.MINE);

		assertEquals(AssignmentResult.Status.WORKER_LIMIT, refused.getStatus());
		assertEquals(1, state.getGatheringSite(
			BuildingType.MINE).getAssignedWorkerIds().size());
	}

	@Test
	public void unknownWorkerAndUnknownSiteAreRefusedWithoutMutation()
	{
		place(BuildingType.MINE, new GridPoint(1, 1));

		assertEquals(AssignmentResult.Status.UNKNOWN_WORKER,
			village.assignWorker("worker-99", BuildingType.MINE).getStatus());
		assertEquals(AssignmentResult.Status.UNKNOWN_SITE,
			village.assignWorker("worker-1", BuildingType.TOWN_HALL).getStatus());
		assertTrue(state.getGatheringSite(BuildingType.MINE).getAssignedWorkerIds().isEmpty());
	}

	@Test
	public void removeWorkerStopsSiteProduction()
	{
		place(BuildingType.MINE, new GridPoint(1, 1));
		assertTrue(village.assignWorker("worker-1", BuildingType.MINE).isSuccess());
		assertTrue(village.removeWorker("worker-1").isSuccess());

		assertEquals(WorkerState.IDLE, state.getWorker("worker-1").getState());
		assertTrue(state.getGatheringSite(
			BuildingType.MINE).getAssignedWorkerIds().isEmpty());
	}

	@Test
	public void movingASiteRecallsItsWorkers()
	{
		place(BuildingType.MINE, new GridPoint(1, 1));
		assertTrue(village.assignWorker("worker-1", BuildingType.MINE).isSuccess());

		assertTrue(village.move(BuildingType.MINE, new GridPoint(13, 1)).isSuccess());

		assertEquals(new GridPoint(13, 1), state.positionOf(BuildingType.MINE));
		assertTrue(state.getGatheringSite(BuildingType.MINE).getAssignedWorkerIds().isEmpty());
		assertEquals(WorkerState.IDLE, state.getWorker("worker-1").getState());
	}

	@Test
	public void productionRequiresAssignedWorkerAndRespectsLocalCapacity()
	{
		place(BuildingType.MINE, new GridPoint(1, 1));

		advance(1_800);
		village.updateGatheringProduction();
		assertEquals(0, state.getGatheringSite(BuildingType.MINE).getStoredAmount());

		village.assignWorker("worker-1", BuildingType.MINE);
		advance(3_600);
		village.updateGatheringProduction();

		GatheringSiteState mine = state.getGatheringSite(BuildingType.MINE);
		assertEquals(siteCatalog.get(BuildingType.MINE).storageCapacity(1),
			mine.getStoredAmount());
		assertEquals("Local storage full", mine.getBlockedReason());
	}

	@Test
	public void aSiteDoesNotProduceForTheTimeItSpentUnbuilt()
	{
		advance(36_000);
		place(BuildingType.MINE, new GridPoint(1, 1));
		assertTrue(village.assignWorker("worker-1", BuildingType.MINE).isSuccess());

		advance(60);
		village.updateGatheringProduction();

		assertEquals(3, state.getGatheringSite(BuildingType.MINE).getStoredAmount());
	}

	@Test
	public void collectionMovesLocalStorageToGeneralInventory()
	{
		place(BuildingType.MINE, new GridPoint(1, 1));
		assertTrue(village.assignWorker("worker-1", BuildingType.MINE).isSuccess());

		advance(600);
		ResourceCollectResult result = village.collectGatheringSite(BuildingType.MINE);

		assertEquals(ResourceType.ORE, result.getResourceType());
		assertEquals(30, result.getCollected());
		assertEquals(30, state.getResources().get(ResourceType.ORE));
		assertEquals(0, state.getGatheringSite(BuildingType.MINE).getStoredAmount());
	}

	@Test
	public void collectingAnEmptySiteReportsNothingAndLeavesStateUntouched()
	{
		place(BuildingType.MINE, new GridPoint(1, 1));

		ResourceCollectResult result = village.collectGatheringSite(BuildingType.MINE);

		assertEquals(0, result.getCollected());
		assertEquals(0, result.getRemainingAtSite());
		assertFalse(result.isStorageFull());
		assertEquals(0, state.getResources().get(ResourceType.ORE));
	}

	@Test
	public void offlineProgressIsCappedAppliedOnceAndTheSurplusIsForfeited()
	{
		place(BuildingType.WOODCUTTING_GROVE, new GridPoint(1, 14));
		assertTrue(village.assignWorker("worker-1", BuildingType.WOODCUTTING_GROVE).isSuccess());
		state.setLastOfflineProgressAtEpochMillis(now.toEpochMilli());
		advance(54_000);

		OfflineProgressSummary summary = village.applyOfflineProgress();
		OfflineProgressSummary second = village.applyOfflineProgress();

		assertEquals(480, summary.getAppliedMinutes());
		assertTrue(summary.getProduced().get(ResourceType.LOGS) > 0);
		assertEquals(0, (int) second.getProduced().get(ResourceType.LOGS));

		// The forfeited interval must not be recoverable by a later uncapped update.
		village.collectGatheringSite(BuildingType.WOODCUTTING_GROVE);
		village.updateGatheringProduction();
		assertEquals(0, state.getGatheringSite(
			BuildingType.WOODCUTTING_GROVE).getStoredAmount());
	}

	@Test
	public void pathfindingRejectsBlockedOrOutOfBoundsDestination()
	{
		PathfindingService pathfinding = new PathfindingService(buildingCatalog);

		assertTrue(pathfinding.hasPath(state, new GridPoint(6, 9), new GridPoint(4, 2)));
		assertFalse(pathfinding.hasPath(state, new GridPoint(6, 9), new GridPoint(-1, 0)));
	}

	@Test
	public void accessPointsRingTheFootprintAndStayInsideThePlot()
	{
		assertTrue(GatheringSiteCatalog.accessPoints(
			new GridPoint(1, 1),
			buildingCatalog.getFootprint(BuildingType.MINE)).contains(new GridPoint(4, 1)));
		assertFalse(GatheringSiteCatalog.accessPoints(
			new GridPoint(0, 0),
			buildingCatalog.getFootprint(BuildingType.MINE)).contains(new GridPoint(-1, 0)));
	}
}
