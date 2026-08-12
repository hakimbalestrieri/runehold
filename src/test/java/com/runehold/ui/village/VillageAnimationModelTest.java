package com.runehold.ui.village;

import com.runehold.RuneholdAnimationQuality;
import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.BuildingType;
import com.runehold.domain.ConstructionJob;
import com.runehold.domain.Village;
import com.runehold.domain.VillageState;
import com.runehold.domain.layout.Footprint;
import com.runehold.domain.layout.GridPoint;
import com.runehold.domain.layout.VillageLayout;
import com.runehold.ui.RuneholdViewModel;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VillageAnimationModelTest
{
	private final BuildingCatalog catalog = new BuildingCatalog();

	@Test
	public void workerAppearsOnlyDuringConstruction()
	{
		VillageAnimationModel model = new VillageAnimationModel(catalog, 123L);
		VillageState active = stateWithConstruction();

		model.refresh(view(active), layout(active), settings(true, 0));

		assertTrue(model.hasWorker());

		VillageState idle = stateWithBarracks(false);
		model.refresh(view(idle), layout(idle), settings(true, 0));

		assertFalse(model.hasWorker());
	}

	@Test
	public void disabledAnimationsRemoveAllCharacters()
	{
		VillageAnimationModel model = new VillageAnimationModel(catalog, 123L);
		VillageState state = stateWithConstruction();

		model.refresh(view(state), layout(state), settings(true, 2));
		assertFalse(model.getActors().isEmpty());

		model.refresh(view(state), layout(state), settings(false, 2));

		assertTrue(model.getActors().isEmpty());
	}

	@Test
	public void troopDensityIsBoundedBySettings()
	{
		VillageAnimationModel model = new VillageAnimationModel(catalog, 123L);
		VillageState state = stateWithBarracks(true);

		model.refresh(view(state), layout(state), settings(true, 4));

		long troops = model.getActors().stream()
			.filter(actor -> actor.getRole() == VillageActor.Role.TROOP)
			.count();
		assertEquals(2L, troops);
	}

	@Test
	public void actorPathsStayInsideVillageAndAvoidBuildingFootprints()
	{
		VillageAnimationModel model = new VillageAnimationModel(catalog, 123L);
		VillageState state = stateWithBarracks(true);
		VillageLayout layout = layout(state);

		model.refresh(view(state), layout, settings(true, 2));

		for (VillageActor actor : model.getActors())
		{
			for (GridPoint point : actor.getPath())
			{
				assertTrue(point.getX() >= 0 && point.getX() < VillageLayout.COLUMNS);
				assertTrue(point.getY() >= 0 && point.getY() < VillageLayout.ROWS);
				assertFalse(isOccupied(point, layout));
			}
		}
	}

	@Test
	public void advancingAnimationsMovesByDiscreteSteps()
	{
		VillageAnimationModel model = new VillageAnimationModel(catalog, 123L);
		VillageState state = stateWithConstruction();
		model.refresh(view(state), layout(state), settings(true, 0));
		GridPoint before = model.getActors().get(0).getPosition();

		model.advance(1_000L, settings(true, 0));
		model.advance(2_000L, settings(true, 0));

		assertFalse(before.equals(model.getActors().get(0).getPosition()));
	}

	private RuneholdViewModel view(VillageState state)
	{
		return RuneholdViewModel.from(state, new Village(state, catalog, false), catalog);
	}

	private VillageLayout layout(VillageState state)
	{
		return VillageLayout.restore(catalog, state.getBuildingPositions());
	}

	private VillageAnimationSettings settings(boolean enabled, int density)
	{
		return new VillageAnimationSettings(
			enabled,
			density,
			RuneholdAnimationQuality.STANDARD,
			false);
	}

	private VillageState stateWithConstruction()
	{
		Map<BuildingType, Integer> levels = levels(false);
		Map<BuildingType, GridPoint> positions = positions(false);
		positions.put(BuildingType.MANA_WELL, new GridPoint(1, 12));
		return VillageState.restore(
			500,
			Collections.emptyMap(),
			Collections.emptyMap(),
			0,
			LocalDate.of(2026, 8, 12),
			levels,
			positions,
			new ConstructionJob(BuildingType.MANA_WELL, 1,
				Instant.parse("2026-08-12T10:00:00Z")));
	}

	private VillageState stateWithBarracks(boolean barracks)
	{
		return VillageState.restore(
			500,
			Collections.emptyMap(),
			Collections.emptyMap(),
			0,
			LocalDate.of(2026, 8, 12),
			levels(barracks),
			positions(barracks));
	}

	private Map<BuildingType, Integer> levels(boolean barracks)
	{
		Map<BuildingType, Integer> levels = new EnumMap<>(BuildingType.class);
		for (BuildingType type : BuildingType.values())
		{
			levels.put(type, 0);
		}
		levels.put(BuildingType.TOWN_HALL, 1);
		if (barracks)
		{
			levels.put(BuildingType.BARRACKS, 1);
		}
		return levels;
	}

	private Map<BuildingType, GridPoint> positions(boolean barracks)
	{
		Map<BuildingType, GridPoint> positions = new EnumMap<>(BuildingType.class);
		positions.put(BuildingType.TOWN_HALL, VillageLayout.defaultPosition(BuildingType.TOWN_HALL));
		if (barracks)
		{
			positions.put(BuildingType.BARRACKS, new GridPoint(11, 4));
		}
		return positions;
	}

	private boolean isOccupied(GridPoint point, VillageLayout layout)
	{
		for (Map.Entry<BuildingType, com.runehold.domain.layout.BuildingPlacement> entry
			: layout.getPlacements().entrySet())
		{
			GridPoint position = entry.getValue().getPosition();
			Footprint footprint = catalog.getFootprint(entry.getKey());
			if (point.getX() >= position.getX()
				&& point.getX() < position.getX() + footprint.getWidth()
				&& point.getY() >= position.getY()
				&& point.getY() < position.getY() + footprint.getHeight())
			{
				return true;
			}
		}
		return false;
	}
}
