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
	private static final Map<BuildingType, GridPoint> DEFAULT_POSITIONS =
		new EnumMap<>(BuildingType.class);

	static
	{
		DEFAULT_POSITIONS.put(BuildingType.TOWN_HALL, new GridPoint(7, 7));
		DEFAULT_POSITIONS.put(BuildingType.MANA_WELL, new GridPoint(3, 9));
		DEFAULT_POSITIONS.put(BuildingType.MANA_GROVE, new GridPoint(3, 3));
		DEFAULT_POSITIONS.put(BuildingType.BARRACKS, new GridPoint(11, 4));
		DEFAULT_POSITIONS.put(BuildingType.WORKSHOP, new GridPoint(11, 11));
		DEFAULT_POSITIONS.put(BuildingType.RUNE_BANNER, new GridPoint(9, 14));
	}

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
			new BuildingPlacement(
				BuildingType.TOWN_HALL,
				DEFAULT_POSITIONS.get(BuildingType.TOWN_HALL)));
		return layout;
	}

	public static VillageLayout forBuildingLevels(
		BuildingCatalog catalog,
		Map<BuildingType, Integer> buildingLevels)
	{
		Objects.requireNonNull(buildingLevels, "buildingLevels");
		VillageLayout layout = new VillageLayout(catalog);
		for (BuildingType type : BuildingType.values())
		{
			Integer level = buildingLevels.get(type);
			if (level == null || level <= 0)
			{
				continue;
			}

			PlacementResult result = layout.place(type, DEFAULT_POSITIONS.get(type));
			if (!result.isSuccess())
			{
				throw new IllegalStateException("invalid default placement for " + type);
			}
		}
		return layout;
	}

	public static VillageLayout restore(
		BuildingCatalog catalog,
		Map<BuildingType, GridPoint> positions)
	{
		Objects.requireNonNull(positions, "positions");
		VillageLayout layout = new VillageLayout(catalog);
		for (BuildingType type : BuildingType.values())
		{
			GridPoint position = positions.get(type);
			if (position == null)
			{
				continue;
			}
			PlacementResult result = layout.place(type, position);
			if (!result.isSuccess())
			{
				throw new IllegalArgumentException(
					"invalid restored placement for " + type + ": " + result.getStatus());
			}
		}
		return layout;
	}

	public static GridPoint defaultPosition(BuildingType type)
	{
		validateType(type);
		GridPoint position = DEFAULT_POSITIONS.get(type);
		if (position == null)
		{
			throw new IllegalArgumentException("no default position for " + type);
		}
		return position;
	}

	public GridPoint findFirstAvailable(BuildingType type)
	{
		validateType(type);
		for (int y = 0; y < ROWS; y++)
		{
			for (int x = 0; x < COLUMNS; x++)
			{
				GridPoint point = new GridPoint(x, y);
				if (previewPlace(type, point).isSuccess())
				{
					return point;
				}
			}
		}
		return null;
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
