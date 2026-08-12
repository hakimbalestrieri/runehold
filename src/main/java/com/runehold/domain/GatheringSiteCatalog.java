package com.runehold.domain;

import com.runehold.domain.layout.Footprint;
import com.runehold.domain.layout.GridPoint;
import com.runehold.domain.layout.VillageLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GatheringSiteCatalog
{
	private final Map<BuildingType, GatheringSiteDefinition> definitions =
		new EnumMap<>(BuildingType.class);

	public GatheringSiteCatalog()
	{
		add(BuildingType.MINE, ResourceType.ORE, 3, 60, 1);
		add(BuildingType.FISHING_SPOT, ResourceType.FISH, 4, 70, 1);
		add(BuildingType.WOODCUTTING_GROVE, ResourceType.LOGS, 4, 80, 1);
		add(BuildingType.QUARRY, ResourceType.STONE, 3, 80, 1);
		add(BuildingType.FARM, ResourceType.CROPS, 5, 90, 2);
		add(BuildingType.HERB_PATCH, ResourceType.HERBS, 1, 30, 1);
		add(BuildingType.CLAY_PIT, ResourceType.CLAY, 3, 60, 1);
		add(BuildingType.RUNE_ESSENCE_SITE, ResourceType.RUNE_ESSENCE, 1, 25, 1);
	}

	private void add(
		BuildingType type,
		ResourceType resource,
		int baseRatePerMinute,
		int baseStorage,
		int baseWorkers)
	{
		definitions.put(type, new GatheringSiteDefinition(
			type,
			resource,
			baseRatePerMinute,
			baseStorage,
			baseWorkers));
	}

	public GatheringSiteDefinition get(BuildingType type)
	{
		GatheringSiteDefinition definition = definitions.get(
			Objects.requireNonNull(type, "type"));
		if (definition == null)
		{
			throw new IllegalArgumentException("unknown site: " + type);
		}
		return definition;
	}

	public boolean isSite(BuildingType type)
	{
		return type != null && definitions.containsKey(type);
	}

	public Map<BuildingType, GatheringSiteDefinition> all()
	{
		return Collections.unmodifiableMap(new EnumMap<>(definitions));
	}

	/**
	 * The tiles a villager can stand on to work a site placed at {@code position}: the
	 * ring immediately around its footprint, clipped to the plot. Access points follow
	 * the site, so moving a site moves where its workers stand.
	 */
	public static List<GridPoint> accessPoints(GridPoint position, Footprint footprint)
	{
		Objects.requireNonNull(position, "position");
		Objects.requireNonNull(footprint, "footprint");
		List<GridPoint> points = new ArrayList<>();
		int left = position.getX() - 1;
		int right = position.getX() + footprint.getWidth();
		int top = position.getY() - 1;
		int bottom = position.getY() + footprint.getHeight();
		for (int x = position.getX(); x < right; x++)
		{
			addIfInside(points, x, top);
			addIfInside(points, x, bottom);
		}
		for (int y = position.getY(); y < bottom; y++)
		{
			addIfInside(points, left, y);
			addIfInside(points, right, y);
		}
		return points;
	}

	private static void addIfInside(List<GridPoint> points, int x, int y)
	{
		if (x >= 0 && y >= 0 && x < VillageLayout.COLUMNS && y < VillageLayout.ROWS)
		{
			points.add(new GridPoint(x, y));
		}
	}

	public int generalStorageCapacity(ResourceType type)
	{
		Objects.requireNonNull(type, "type");
		return 1_000;
	}

	public int offlineCapMinutes()
	{
		return 8 * 60;
	}
}
