package com.runehold.domain.layout;

import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.BuildingType;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class VillageLayout
{
	public static final int COLUMNS = 18;
	public static final int ROWS = 18;
	private static final GridPoint INITIAL_TOWN_HALL_POSITION = new GridPoint(7, 7);

	private final BuildingCatalog catalog;
	private final Map<BuildingType, BuildingPlacement> placements =
		new EnumMap<>(BuildingType.class);

	private VillageLayout(BuildingCatalog catalog)
	{
		this.catalog = Objects.requireNonNull(catalog, "catalog");
	}

	public static VillageLayout fresh(BuildingCatalog catalog)
	{
		VillageLayout layout = new VillageLayout(catalog);
		layout.placements.put(
			BuildingType.TOWN_HALL,
			new BuildingPlacement(BuildingType.TOWN_HALL, INITIAL_TOWN_HALL_POSITION));
		return layout;
	}

	public Map<BuildingType, BuildingPlacement> getPlacements()
	{
		Map<BuildingType, BuildingPlacement> copy = new EnumMap<>(BuildingType.class);
		copy.putAll(placements);
		return Collections.unmodifiableMap(copy);
	}

	public BuildingPlacement getPlacement(BuildingType type)
	{
		validateType(type);
		return placements.get(type);
	}

	public PlacementResult place(BuildingType type, GridPoint destination)
	{
		PlacementResult preview = previewPlace(type, destination);
		if (!preview.isSuccess())
		{
			return preview;
		}

		placements.put(type, new BuildingPlacement(type, destination));
		return preview;
	}

	public PlacementResult previewPlace(BuildingType type, GridPoint destination)
	{
		validateCommand(type, destination);
		if (placements.containsKey(type))
		{
			return PlacementResult.alreadyPlaced();
		}

		return validateDestination(type, destination, null);
	}

	public PlacementResult move(BuildingType type, GridPoint destination)
	{
		PlacementResult preview = previewMove(type, destination);
		if (!preview.isSuccess())
		{
			return preview;
		}

		placements.put(type, new BuildingPlacement(type, destination));
		return preview;
	}

	public PlacementResult previewMove(BuildingType type, GridPoint destination)
	{
		validateCommand(type, destination);
		if (!placements.containsKey(type))
		{
			return PlacementResult.notPlaced();
		}

		return validateDestination(type, destination, type);
	}

	private PlacementResult validateDestination(
		BuildingType type,
		GridPoint destination,
		BuildingType ignoredType)
	{
		Footprint footprint = catalog.getFootprint(type);
		if (!isInsidePlot(destination, footprint))
		{
			return PlacementResult.outOfBounds();
		}

		for (BuildingPlacement placement : placements.values())
		{
			if (placement.getType() == ignoredType)
			{
				continue;
			}
			if (overlaps(
				destination,
				footprint,
				placement.getPosition(),
				catalog.getFootprint(placement.getType())))
			{
				return PlacementResult.occupied(placement.getType());
			}
		}

		return PlacementResult.success();
	}

	private static boolean isInsidePlot(GridPoint position, Footprint footprint)
	{
		return position.getX() >= 0
			&& position.getY() >= 0
			&& position.getX() <= COLUMNS - footprint.getWidth()
			&& position.getY() <= ROWS - footprint.getHeight();
	}

	private static boolean overlaps(
		GridPoint firstPosition,
		Footprint firstFootprint,
		GridPoint secondPosition,
		Footprint secondFootprint)
	{
		return firstPosition.getX() < secondPosition.getX() + secondFootprint.getWidth()
			&& firstPosition.getX() + firstFootprint.getWidth() > secondPosition.getX()
			&& firstPosition.getY() < secondPosition.getY() + secondFootprint.getHeight()
			&& firstPosition.getY() + firstFootprint.getHeight() > secondPosition.getY();
	}

	private static void validateCommand(BuildingType type, GridPoint destination)
	{
		validateType(type);
		if (destination == null)
		{
			throw new IllegalArgumentException("destination is required");
		}
	}

	private static void validateType(BuildingType type)
	{
		if (type == null)
		{
			throw new IllegalArgumentException("building type is required");
		}
	}
}
