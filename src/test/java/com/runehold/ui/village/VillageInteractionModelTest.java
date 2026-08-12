package com.runehold.ui.village;

import com.runehold.domain.BuildingType;
import com.runehold.domain.layout.GridPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class VillageInteractionModelTest
{
	@Test
	public void cancelledPlacementReturnsToViewWithoutSelection()
	{
		VillageInteractionModel model = new VillageInteractionModel();
		model.beginPlacement(BuildingType.MANA_WELL, new GridPoint(2, 2));
		model.updateGhost(new GridPoint(3, 4));

		model.cancel();

		assertEquals(VillageInteractionModel.Mode.VIEW, model.getMode());
		assertNull(model.getActiveType());
		assertNull(model.getGhostPosition());
	}

	@Test
	public void cancelledMoveRetainsOriginForCallerAndNeverCommitsState()
	{
		VillageInteractionModel model = new VillageInteractionModel();
		GridPoint origin = new GridPoint(7, 7);
		model.beginMove(BuildingType.TOWN_HALL, origin);
		model.updateGhost(new GridPoint(10, 10));

		assertEquals(origin, model.getOriginalPosition());
		model.cancel();

		assertEquals(VillageInteractionModel.Mode.VIEW, model.getMode());
		assertEquals(BuildingType.TOWN_HALL, model.getSelectedType());
	}

	@Test
	public void finishingPlacementSelectsNewBuilding()
	{
		VillageInteractionModel model = new VillageInteractionModel();
		model.beginPlacement(BuildingType.RUNE_BANNER, new GridPoint(0, 0));

		model.finish();

		assertEquals(BuildingType.RUNE_BANNER, model.getSelectedType());
	}
}
