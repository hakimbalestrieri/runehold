package com.runehold.domain.layout;

import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.BuildingType;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VillageLayoutTest
{
	private BuildingCatalog catalog;
	private VillageLayout layout;

	@Before
	public void setUp()
	{
		catalog = new BuildingCatalog();
		layout = VillageLayout.fresh(catalog);
	}

	@Test
	public void catalogDefinesEveryBuildingFootprint()
	{
		assertEquals(new Footprint(4, 4), catalog.getFootprint(BuildingType.TOWN_HALL));
		assertEquals(new Footprint(2, 2), catalog.getFootprint(BuildingType.MANA_WELL));
		assertEquals(new Footprint(3, 3), catalog.getFootprint(BuildingType.BARRACKS));
		assertEquals(new Footprint(3, 3), catalog.getFootprint(BuildingType.WORKSHOP));
	}

	@Test
	public void freshLayoutContainsTownHallAtDeterministicPosition()
	{
		Map<BuildingType, BuildingPlacement> placements = layout.getPlacements();

		assertEquals(1, placements.size());
		assertEquals(
			new GridPoint(7, 7),
			placements.get(BuildingType.TOWN_HALL).getPosition());
	}

	@Test
	public void createsDeterministicPreviewForEveryBuiltBuilding()
	{
		Map<BuildingType, Integer> levels = new java.util.EnumMap<>(BuildingType.class);
		levels.put(BuildingType.TOWN_HALL, 2);
		levels.put(BuildingType.MANA_WELL, 1);
		levels.put(BuildingType.BARRACKS, 1);
		levels.put(BuildingType.WORKSHOP, 1);

		VillageLayout restored = VillageLayout.forBuildingLevels(catalog, levels);

		assertEquals(new GridPoint(7, 7),
			restored.getPlacement(BuildingType.TOWN_HALL).getPosition());
		assertEquals(new GridPoint(3, 9),
			restored.getPlacement(BuildingType.MANA_WELL).getPosition());
		assertEquals(new GridPoint(11, 4),
			restored.getPlacement(BuildingType.BARRACKS).getPosition());
		assertEquals(new GridPoint(11, 11),
			restored.getPlacement(BuildingType.WORKSHOP).getPosition());
	}

	@Test
	public void placesBuildingAtInclusiveSouthEastBoundary()
	{
		PlacementResult result = layout.place(
			BuildingType.MANA_WELL,
			new GridPoint(16, 16));

		assertTrue(result.isSuccess());
		assertEquals(
			new GridPoint(16, 16),
			layout.getPlacement(BuildingType.MANA_WELL).getPosition());
	}

	@Test
	public void rejectsFootprintOutsidePlotWithoutMutation()
	{
		PlacementResult result = layout.place(
			BuildingType.MANA_WELL,
			new GridPoint(17, 17));

		assertFalse(result.isSuccess());
		assertEquals(PlacementResult.Status.OUT_OF_BOUNDS, result.getStatus());
		assertEquals(1, layout.getPlacements().size());
	}

	@Test
	public void acceptsFootprintsThatFitEvenWhenSpritesAreLarge()
	{
		PlacementResult result = layout.place(
			BuildingType.BARRACKS,
			new GridPoint(11, 1));

		assertTrue(result.isSuccess());
		assertEquals(
			new GridPoint(11, 1),
			layout.getPlacement(BuildingType.BARRACKS).getPosition());
		assertEquals(2, layout.getPlacements().size());
	}

	@Test
	public void rejectsNegativeCoordinateWithoutMutation()
	{
		PlacementResult result = layout.place(
			BuildingType.MANA_WELL,
			new GridPoint(-1, 0));

		assertEquals(PlacementResult.Status.OUT_OF_BOUNDS, result.getStatus());
		assertEquals(1, layout.getPlacements().size());
	}

	@Test
	public void rejectsCollisionWithoutMutation()
	{
		PlacementResult result = layout.place(
			BuildingType.MANA_WELL,
			new GridPoint(6, 6));

		assertFalse(result.isSuccess());
		assertEquals(PlacementResult.Status.OCCUPIED, result.getStatus());
		assertEquals(BuildingType.TOWN_HALL, result.getBlockingType());
		assertEquals(1, layout.getPlacements().size());
	}

	@Test
	public void rejectsSecondPlacementOfSameBuildingType()
	{
		assertTrue(layout.place(BuildingType.MANA_WELL, new GridPoint(0, 0)).isSuccess());

		PlacementResult result = layout.place(
			BuildingType.MANA_WELL,
			new GridPoint(16, 16));

		assertEquals(PlacementResult.Status.ALREADY_PLACED, result.getStatus());
		assertEquals(
			new GridPoint(0, 0),
			layout.getPlacement(BuildingType.MANA_WELL).getPosition());
	}

	@Test
	public void movingBuildingIgnoresItsCurrentFootprint()
	{
		PlacementResult result = layout.move(
			BuildingType.TOWN_HALL,
			new GridPoint(8, 7));

		assertTrue(result.isSuccess());
		assertEquals(
			new GridPoint(8, 7),
			layout.getPlacement(BuildingType.TOWN_HALL).getPosition());
	}

	@Test
	public void invalidMoveKeepsOriginalPosition()
	{
		GridPoint original = layout.getPlacement(BuildingType.TOWN_HALL).getPosition();

		PlacementResult result = layout.move(
			BuildingType.TOWN_HALL,
			new GridPoint(15, 15));

		assertEquals(PlacementResult.Status.OUT_OF_BOUNDS, result.getStatus());
		assertEquals(original, layout.getPlacement(BuildingType.TOWN_HALL).getPosition());
	}

	@Test
	public void collidingMoveKeepsOriginalPosition()
	{
		assertTrue(layout.place(BuildingType.MANA_WELL, new GridPoint(1, 1)).isSuccess());
		GridPoint original = layout.getPlacement(BuildingType.TOWN_HALL).getPosition();

		PlacementResult result = layout.move(
			BuildingType.TOWN_HALL,
			new GridPoint(1, 1));

		assertEquals(PlacementResult.Status.OCCUPIED, result.getStatus());
		assertEquals(BuildingType.MANA_WELL, result.getBlockingType());
		assertEquals(original, layout.getPlacement(BuildingType.TOWN_HALL).getPosition());
	}

	@Test
	public void rejectsMoveForBuildingThatIsNotPlaced()
	{
		PlacementResult result = layout.move(
			BuildingType.WORKSHOP,
			new GridPoint(0, 0));

		assertEquals(PlacementResult.Status.NOT_PLACED, result.getStatus());
		assertEquals(1, layout.getPlacements().size());
	}
}
