package com.runehold.domain;

import com.runehold.domain.layout.Footprint;
import com.runehold.domain.layout.GridPoint;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class GatheringSiteCatalog
{
	private final Map<GatheringSiteType, GatheringSiteDefinition> definitions =
		new EnumMap<>(GatheringSiteType.class);

	public GatheringSiteCatalog()
	{
		add(GatheringSiteType.MINE, ResourceType.ORE, new Footprint(3, 3),
			new GridPoint(1, 1), 1, 3, 60, 1, 4,
			new GridPoint(4, 2), new GridPoint(2, 4));
		add(GatheringSiteType.FISHING_SPOT, ResourceType.FISH, new Footprint(3, 2),
			new GridPoint(14, 1), 1, 4, 70, 1, 4,
			new GridPoint(13, 2), new GridPoint(14, 3));
		add(GatheringSiteType.WOODCUTTING_GROVE, ResourceType.LOGS, new Footprint(3, 3),
			new GridPoint(1, 14), 1, 4, 80, 1, 4,
			new GridPoint(4, 15), new GridPoint(2, 13));
		add(GatheringSiteType.QUARRY, ResourceType.STONE, new Footprint(3, 3),
			new GridPoint(14, 14), 1, 3, 80, 1, 4,
			new GridPoint(13, 15), new GridPoint(15, 13));
		add(GatheringSiteType.FARM, ResourceType.CROPS, new Footprint(3, 2),
			new GridPoint(6, 14), 1, 5, 90, 2, 4,
			new GridPoint(6, 13), new GridPoint(9, 15));
		add(GatheringSiteType.HERB_PATCH, ResourceType.HERBS, new Footprint(2, 2),
			new GridPoint(10, 14), 2, 1, 30, 1, 3,
			new GridPoint(10, 13), new GridPoint(12, 14));
		add(GatheringSiteType.CLAY_PIT, ResourceType.CLAY, new Footprint(2, 2),
			new GridPoint(14, 10), 1, 3, 60, 1, 3,
			new GridPoint(13, 10), new GridPoint(14, 12));
		add(GatheringSiteType.RUNE_ESSENCE_SITE, ResourceType.RUNE_ESSENCE,
			new Footprint(2, 2), new GridPoint(9, 1), 3, 1, 25, 1, 3,
			new GridPoint(9, 3), new GridPoint(11, 2));
	}

	private void add(
		GatheringSiteType type,
		ResourceType resource,
		Footprint footprint,
		GridPoint position,
		int requiredTownHallLevel,
		int baseRatePerMinute,
		int baseStorage,
		int baseWorkers,
		int maxLevel,
		GridPoint... accessPoints)
	{
		definitions.put(type, new GatheringSiteDefinition(
			type,
			resource,
			footprint,
			position,
			requiredTownHallLevel,
			baseRatePerMinute,
			baseStorage,
			baseWorkers,
			maxLevel,
			Arrays.asList(accessPoints)));
	}

	public GatheringSiteDefinition get(GatheringSiteType type)
	{
		GatheringSiteDefinition definition = definitions.get(
			Objects.requireNonNull(type, "type"));
		if (definition == null)
		{
			throw new IllegalArgumentException("unknown site: " + type);
		}
		return definition;
	}

	public Map<GatheringSiteType, GatheringSiteDefinition> all()
	{
		return Collections.unmodifiableMap(new EnumMap<>(definitions));
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
