package com.runehold.domain;

import com.runehold.domain.layout.GridPoint;
import com.runehold.domain.layout.PlacementResult;
import java.time.LocalDate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VillageBuilderFlowTest
{
	private VillageState state;
	private Village village;

	@Before
	public void setUp()
	{
		state = VillageState.fresh(LocalDate.of(2026, 8, 12));
		village = new Village(state, new BuildingCatalog());
	}

	@Test
	public void previewAndCancelledPlacementSpendNothing()
	{
		long before = state.getMana();

		PlacementResult preview = village.previewBuild(
			BuildingType.MANA_WELL,
			new GridPoint(1, 12));

		assertTrue(preview.isSuccess());
		assertEquals(before, state.getMana());
		assertEquals(0, village.levelOf(BuildingType.MANA_WELL));
		assertNull(village.positionOf(BuildingType.MANA_WELL));
	}

	@Test
	public void confirmationPlacesBuildingAndSpendsExactCostOnce()
	{
		GridPoint destination = new GridPoint(1, 12);

		BuildResult result = village.build(BuildingType.MANA_WELL, destination);

		assertTrue(result.isSuccess());
		assertEquals(100, result.getCost());
		assertEquals(150L, state.getMana());
		assertEquals(0, village.levelOf(BuildingType.MANA_WELL));
		assertEquals(destination, village.positionOf(BuildingType.MANA_WELL));
		assertEquals(BuildingType.MANA_WELL, village.getConstructionJob().getBuildingType());
		assertFalse(village.build(BuildingType.MANA_WELL, new GridPoint(0, 0)).isSuccess());
		assertEquals(150L, state.getMana());
	}

	@Test
	public void invalidAndCollidingPlacementsNeverSpendMana()
	{
		long before = state.getMana();

		BuildResult outside = village.build(
			BuildingType.MANA_WELL,
			new GridPoint(17, 17));
		BuildResult collision = village.build(
			BuildingType.MANA_WELL,
			new GridPoint(7, 7));

		assertEquals(BuildResult.Status.OUT_OF_BOUNDS, outside.getStatus());
		assertEquals(BuildResult.Status.OCCUPIED, collision.getStatus());
		assertEquals(BuildingType.TOWN_HALL, collision.getBlockingType());
		assertEquals(before, state.getMana());
		assertEquals(0, village.levelOf(BuildingType.MANA_WELL));
	}

	@Test
	public void invalidMovePreservesOriginAndValidMoveCommits()
	{
		GridPoint origin = village.positionOf(BuildingType.TOWN_HALL);

		PlacementResult invalid = village.move(
			BuildingType.TOWN_HALL,
			new GridPoint(16, 16));
		assertFalse(invalid.isSuccess());
		assertEquals(origin, village.positionOf(BuildingType.TOWN_HALL));

		GridPoint destination = new GridPoint(8, 7);
		assertTrue(village.move(BuildingType.TOWN_HALL, destination).isSuccess());
		assertEquals(destination, village.positionOf(BuildingType.TOWN_HALL));
	}

	@Test
	public void unlimitedManaBuildsWithoutChangingStoredBalance()
	{
		Instant start = Instant.parse("2026-08-12T09:00:00Z");
		village = new Village(state, new BuildingCatalog(), true,
			Clock.fixed(start, ZoneOffset.UTC));

		assertTrue(village.build(
			BuildingType.MANA_GROVE,
			new GridPoint(1, 12)).isSuccess());

		assertEquals(VillageState.STARTER_MANA, state.getMana());
		assertEquals(0, village.levelOf(BuildingType.MANA_GROVE));
		assertEquals(BuildingType.MANA_GROVE, village.getConstructionJob().getBuildingType());

		village = new Village(state, new BuildingCatalog(), true,
			Clock.fixed(start.plusSeconds(4), ZoneOffset.UTC));
		assertTrue(village.completeConstructionIfReady());
		assertEquals(1, village.levelOf(BuildingType.MANA_GROVE));
		assertEquals(VillageState.STARTER_MANA, state.getMana());
	}

	@Test
	public void normalBuildReservesPositionAndCompletesAtItsPersistedDeadline()
	{
		Instant start = Instant.parse("2026-08-12T09:00:00Z");
		Clock beforeFinish = Clock.fixed(start, ZoneOffset.UTC);
		village = new Village(state, new BuildingCatalog(), false, beforeFinish);

		BuildResult started = village.build(BuildingType.MANA_WELL, new GridPoint(1, 12));

		assertTrue(started.isSuccess());
		assertEquals(150L, state.getMana());
		assertEquals(0, village.levelOf(BuildingType.MANA_WELL));
		assertEquals(new GridPoint(1, 12), village.positionOf(BuildingType.MANA_WELL));
		assertEquals(BuildingType.MANA_WELL, village.getConstructionJob().getBuildingType());
		assertFalse(village.completeConstructionIfReady());

		village = new Village(state, new BuildingCatalog(), false,
			Clock.fixed(start.plusSeconds(29), ZoneOffset.UTC));
		assertFalse(village.completeConstructionIfReady());

		village = new Village(state, new BuildingCatalog(), false,
			Clock.fixed(start.plusSeconds(31), ZoneOffset.UTC));
		assertTrue(village.completeConstructionIfReady());
		assertEquals(1, village.levelOf(BuildingType.MANA_WELL));
		assertNull(village.getConstructionJob());
	}

	@Test
	public void normalUpgradeReservesTheBuilderAndAppliesOnlyAfterDeadline()
	{
		Instant start = Instant.parse("2026-08-12T09:00:00Z");
		village = new Village(state, new BuildingCatalog(), false,
			Clock.fixed(start, ZoneOffset.UTC));

		assertTrue(village.beginTimedUpgrade(BuildingType.TOWN_HALL).isSuccess());
		assertEquals(50L, state.getMana());
		assertEquals(1, village.levelOf(BuildingType.TOWN_HALL));
		assertEquals(BuildingType.TOWN_HALL, village.getConstructionJob().getBuildingType());

		village = new Village(state, new BuildingCatalog(), false,
			Clock.fixed(start.plusSeconds(299), ZoneOffset.UTC));
		assertFalse(village.completeConstructionIfReady());

		village = new Village(state, new BuildingCatalog(), false,
			Clock.fixed(start.plusSeconds(301), ZoneOffset.UTC));
		assertTrue(village.completeConstructionIfReady());
		assertEquals(2, village.levelOf(BuildingType.TOWN_HALL));
	}

	@Test
	public void manaGroveAccruesAndCollectsPersistedProduction()
	{
		Instant start = Instant.parse("2026-08-12T09:00:00Z");
		village = new Village(state, new BuildingCatalog(), true,
			Clock.fixed(start, ZoneOffset.UTC));
		assertTrue(village.build(BuildingType.MANA_GROVE, new GridPoint(1, 2)).isSuccess());
		village = new Village(state, new BuildingCatalog(), true,
			Clock.fixed(start.plusSeconds(4), ZoneOffset.UTC));
		assertTrue(village.completeConstructionIfReady());

		village = new Village(state, new BuildingCatalog(), true,
			Clock.fixed(start.plusSeconds(125), ZoneOffset.UTC));
		assertEquals(4, village.getCollectableGroveMana());
		assertEquals(4, village.collectManaGrove().getManaCollected());
		assertEquals(0, village.getCollectableGroveMana());
	}

	@Test
	public void peekingGroveProductionDoesNotMutateState()
	{
		Instant start = Instant.parse("2026-08-12T09:00:00Z");
		village = new Village(state, new BuildingCatalog(), true,
			Clock.fixed(start, ZoneOffset.UTC));
		assertTrue(village.build(BuildingType.MANA_GROVE, new GridPoint(1, 2)).isSuccess());
		village = new Village(state, new BuildingCatalog(), true,
			Clock.fixed(start.plusSeconds(4), ZoneOffset.UTC));
		assertTrue(village.completeConstructionIfReady());
		long previousUpdatedAt = state.getGroveProductionUpdatedAtEpochMillis();

		village = new Village(state, new BuildingCatalog(), true,
			Clock.fixed(start.plusSeconds(125), ZoneOffset.UTC));
		assertEquals(4, village.peekCollectableGroveMana());
		assertEquals(0, state.getStoredGroveMana());
		assertEquals(previousUpdatedAt, state.getGroveProductionUpdatedAtEpochMillis());
	}
}
