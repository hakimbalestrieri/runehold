package com.runehold.domain;

import java.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VillageTest
{
	private VillageState state;
	private Village village;

	@Before
	public void setUp()
	{
		state = VillageState.fresh(LocalDate.of(2026, 8, 11));
		village = new Village(state, new BuildingCatalog());
	}

	@Test
	public void freshVillageContainsOnlyLevelOneTownHall()
	{
		assertEquals(1, village.levelOf(BuildingType.TOWN_HALL));
		assertEquals(0, village.levelOf(BuildingType.MANA_WELL));
		assertEquals(0, village.levelOf(BuildingType.BARRACKS));
		assertEquals(0, village.levelOf(BuildingType.WORKSHOP));
	}

	@Test
	public void constructsUnlockedBuildingAndDebitsExactCost()
	{
		UpgradeResult result = village.upgrade(BuildingType.MANA_WELL);

		assertTrue(result.isSuccess());
		assertEquals(UpgradeResult.Status.SUCCESS, result.getStatus());
		assertEquals(1, result.getNewLevel());
		assertEquals(100, result.getRequiredMana());
		assertEquals(150L, state.getMana());
		assertEquals(1, village.levelOf(BuildingType.MANA_WELL));
	}

	@Test
	public void townHallUpgradeUnlocksBarracksConstruction()
	{
		state.addMana(1_000);

		UpgradeResult locked = village.upgrade(BuildingType.BARRACKS);
		UpgradeResult townHall = village.upgrade(BuildingType.TOWN_HALL);
		UpgradeResult barracks = village.upgrade(BuildingType.BARRACKS);

		assertEquals(UpgradeResult.Status.LOCKED, locked.getStatus());
		assertEquals(2, locked.getRequiredTownHallLevel());
		assertTrue(townHall.isSuccess());
		assertTrue(barracks.isSuccess());
		assertEquals(1, village.levelOf(BuildingType.BARRACKS));
	}

	@Test
	public void townHallGatesEachSubsequentBuildingLevel()
	{
		state.addMana(10_000);
		assertTrue(village.upgrade(BuildingType.MANA_WELL).isSuccess());
		long manaBefore = state.getMana();

		UpgradeResult result = village.upgrade(BuildingType.MANA_WELL);

		assertEquals(UpgradeResult.Status.LOCKED, result.getStatus());
		assertEquals(2, result.getRequiredTownHallLevel());
		assertEquals(manaBefore, state.getMana());
		assertEquals(1, village.levelOf(BuildingType.MANA_WELL));
	}

	@Test
	public void insufficientManaDoesNotMutateVillage()
	{
		assertTrue(village.upgrade(BuildingType.MANA_WELL).isSuccess());
		long manaBefore = state.getMana();

		UpgradeResult result = village.upgrade(BuildingType.TOWN_HALL);

		assertFalse(result.isSuccess());
		assertEquals(UpgradeResult.Status.INSUFFICIENT_MANA, result.getStatus());
		assertEquals(200, result.getRequiredMana());
		assertEquals(manaBefore, state.getMana());
		assertEquals(1, village.levelOf(BuildingType.TOWN_HALL));
	}

	@Test
	public void unlimitedManaModeBypassesCostsWithoutChangingStoredBalance()
	{
		village = new Village(state, new BuildingCatalog(), true);

		assertTrue(village.upgrade(BuildingType.TOWN_HALL).isSuccess());
		assertTrue(village.upgrade(BuildingType.BARRACKS).isSuccess());

		assertTrue(village.hasUnlimitedMana());
		assertEquals(250L, state.getMana());
		assertEquals(2, village.levelOf(BuildingType.TOWN_HALL));
		assertEquals(1, village.levelOf(BuildingType.BARRACKS));
	}

	@Test
	public void previewReportsNextUpgradeWithoutMutation()
	{
		long manaBefore = state.getMana();

		UpgradeResult preview = village.previewUpgrade(BuildingType.MANA_WELL);

		assertEquals(UpgradeResult.Status.SUCCESS, preview.getStatus());
		assertEquals(1, preview.getNewLevel());
		assertEquals(100, preview.getRequiredMana());
		assertEquals(manaBefore, state.getMana());
		assertEquals(0, village.levelOf(BuildingType.MANA_WELL));
	}

	@Test
	public void maximumLevelDoesNotMutateVillage()
	{
		state.addMana(100_000);
		while (village.levelOf(BuildingType.TOWN_HALL) < 5)
		{
			assertTrue(village.upgrade(BuildingType.TOWN_HALL).isSuccess());
		}
		long manaBefore = state.getMana();

		UpgradeResult result = village.upgrade(BuildingType.TOWN_HALL);

		assertEquals(UpgradeResult.Status.MAX_LEVEL, result.getStatus());
		assertEquals(manaBefore, state.getMana());
		assertEquals(5, village.levelOf(BuildingType.TOWN_HALL));
	}

	@Test
	public void catalogContainsDeterministicProgression()
	{
		BuildingCatalog catalog = new BuildingCatalog();

		assertEquals(5, catalog.getMaxLevel(BuildingType.TOWN_HALL));
		assertEquals(200, catalog.getCostForTargetLevel(BuildingType.TOWN_HALL, 2));
		assertEquals(5_000, catalog.getCostForTargetLevel(BuildingType.MANA_WELL, 5));
		assertEquals(2, catalog.getRequiredTownHallLevel(BuildingType.BARRACKS, 1));
		assertEquals(5, catalog.getRequiredTownHallLevel(BuildingType.WORKSHOP, 3));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsUnknownBuilding()
	{
		village.upgrade(null);
	}
}
