package com.runehold.domain;

import java.util.EnumMap;
import java.util.Map;

public final class BuildingCatalog
{
	private final Map<BuildingType, Definition> definitions = new EnumMap<>(BuildingType.class);

	public BuildingCatalog()
	{
		definitions.put(BuildingType.TOWN_HALL, new Definition(
			"Town Hall",
			"Unlocks higher building levels.",
			new int[]{0, 0, 200, 600, 1_500, 4_000},
			new int[]{0, 0, 0, 0, 0, 0}));
		definitions.put(BuildingType.MANA_WELL, new Definition(
			"Mana Well",
			"Prepares your hold for future mana systems.",
			new int[]{0, 100, 250, 750, 2_000, 5_000},
			new int[]{0, 1, 2, 3, 4, 5}));
		definitions.put(BuildingType.BARRACKS, new Definition(
			"Barracks",
			"Prepares your hold for future raiding units.",
			new int[]{0, 300, 900, 2_400, 6_000},
			new int[]{0, 2, 3, 4, 5}));
		definitions.put(BuildingType.WORKSHOP, new Definition(
			"Workshop",
			"Prepares your hold for future defenses.",
			new int[]{0, 800, 2_500, 7_000},
			new int[]{0, 3, 4, 5}));
	}

	public String getDisplayName(BuildingType type)
	{
		return definitionFor(type).displayName;
	}

	public String getDescription(BuildingType type)
	{
		return definitionFor(type).description;
	}

	public int getMaxLevel(BuildingType type)
	{
		return definitionFor(type).costByTargetLevel.length - 1;
	}

	public int getCostForTargetLevel(BuildingType type, int targetLevel)
	{
		Definition definition = definitionFor(type);
		validateTargetLevel(type, targetLevel, definition);
		return definition.costByTargetLevel[targetLevel];
	}

	public int getRequiredTownHallLevel(BuildingType type, int targetLevel)
	{
		Definition definition = definitionFor(type);
		validateTargetLevel(type, targetLevel, definition);
		return definition.requiredTownHallByTargetLevel[targetLevel];
	}

	private Definition definitionFor(BuildingType type)
	{
		if (type == null)
		{
			throw new IllegalArgumentException("building type is required");
		}

		Definition definition = definitions.get(type);
		if (definition == null)
		{
			throw new IllegalArgumentException("unknown building type: " + type);
		}
		return definition;
	}

	private static void validateTargetLevel(
		BuildingType type,
		int targetLevel,
		Definition definition)
	{
		if (targetLevel < 1 || targetLevel >= definition.costByTargetLevel.length)
		{
			throw new IllegalArgumentException(
				"invalid target level " + targetLevel + " for " + type);
		}
	}

	private static final class Definition
	{
		private final String displayName;
		private final String description;
		private final int[] costByTargetLevel;
		private final int[] requiredTownHallByTargetLevel;

		private Definition(
			String displayName,
			String description,
			int[] costByTargetLevel,
			int[] requiredTownHallByTargetLevel)
		{
			this.displayName = displayName;
			this.description = description;
			this.costByTargetLevel = costByTargetLevel;
			this.requiredTownHallByTargetLevel = requiredTownHallByTargetLevel;
		}
	}
}
