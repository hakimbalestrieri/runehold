package com.runehold.domain;

import com.runehold.domain.layout.Footprint;
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
			BuildingCategory.CORE,
			new Footprint(4, 4),
			new int[]{0, 0, 300, 1_200, 7_200, 28_800},
			new int[]{0, 0, 200, 600, 1_500, 4_000},
			new int[]{0, 0, 0, 0, 0, 0}));
		definitions.put(BuildingType.MANA_WELL, new Definition(
			"Mana Well",
			"Increases the village mana storage capacity.",
			BuildingCategory.PRODUCTION,
			new Footprint(2, 2),
			new int[]{0, 30, 180, 900, 3_600, 14_400},
			new int[]{0, 100, 250, 750, 2_000, 5_000},
			new int[]{0, 1, 2, 3, 4, 5}));
		definitions.put(BuildingType.MANA_GROVE, new Definition(
			"Mana Grove",
			"Produces collectable mana over time.",
			BuildingCategory.PRODUCTION,
			new Footprint(3, 3),
			new int[]{0, 45, 240, 1_200, 4_800},
			new int[]{0, 150, 450, 1_200, 3_200},
			new int[]{0, 1, 2, 3, 4}));
		definitions.put(BuildingType.BARRACKS, new Definition(
			"Barracks",
			"Trains the guard that protects your hold.",
			BuildingCategory.MILITARY,
			new Footprint(3, 3),
			new int[]{0, 120, 600, 3_600, 14_400},
			new int[]{0, 300, 900, 2_400, 6_000},
			new int[]{0, 2, 3, 4, 5}));
		definitions.put(BuildingType.WORKSHOP, new Definition(
			"Workshop",
			"Builds tools and defenses for the village.",
			BuildingCategory.UTILITY,
			new Footprint(3, 3),
			new int[]{0, 300, 1_800, 10_800},
			new int[]{0, 800, 2_500, 7_000},
			new int[]{0, 3, 4, 5}));
		definitions.put(BuildingType.RUNE_BANNER, new Definition(
			"Rune Banner",
			"Marks the ground claimed by Runehold.",
			BuildingCategory.DECORATION,
			new Footprint(1, 1),
			new int[]{0, 5},
			new int[]{0, 25},
			new int[]{0, 1}));
		definitions.put(BuildingType.MINE, new Definition(
			"Mine",
			"Villagers assigned here gather ore.",
			BuildingCategory.GATHERING,
			new Footprint(3, 3),
			new int[]{0, 60, 300, 1_500, 6_000},
			new int[]{0, 50, 200, 600, 1_800},
			new int[]{0, 1, 2, 3, 4}));
		definitions.put(BuildingType.FISHING_SPOT, new Definition(
			"Fishing Spot",
			"Villagers assigned here gather fish.",
			BuildingCategory.GATHERING,
			new Footprint(3, 2),
			new int[]{0, 60, 300, 1_500, 6_000},
			new int[]{0, 50, 200, 600, 1_800},
			new int[]{0, 1, 2, 3, 4}));
		definitions.put(BuildingType.WOODCUTTING_GROVE, new Definition(
			"Woodcutting Grove",
			"Villagers assigned here gather logs.",
			BuildingCategory.GATHERING,
			new Footprint(3, 3),
			new int[]{0, 60, 300, 1_500, 6_000},
			new int[]{0, 50, 200, 600, 1_800},
			new int[]{0, 1, 2, 3, 4}));
		definitions.put(BuildingType.QUARRY, new Definition(
			"Quarry",
			"Villagers assigned here gather stone.",
			BuildingCategory.GATHERING,
			new Footprint(3, 3),
			new int[]{0, 90, 420, 1_800, 7_200},
			new int[]{0, 75, 250, 750, 2_200},
			new int[]{0, 1, 2, 3, 4}));
		definitions.put(BuildingType.FARM, new Definition(
			"Farm",
			"Villagers assigned here gather crops.",
			BuildingCategory.GATHERING,
			new Footprint(3, 2),
			new int[]{0, 90, 420, 1_800, 7_200},
			new int[]{0, 75, 250, 750, 2_200},
			new int[]{0, 1, 2, 3, 4}));
		definitions.put(BuildingType.HERB_PATCH, new Definition(
			"Herb Patch",
			"Villagers assigned here gather herbs.",
			BuildingCategory.GATHERING,
			new Footprint(2, 2),
			new int[]{0, 180, 900, 3_600},
			new int[]{0, 200, 700, 2_000},
			new int[]{0, 2, 3, 4}));
		definitions.put(BuildingType.CLAY_PIT, new Definition(
			"Clay Pit",
			"Villagers assigned here gather clay.",
			BuildingCategory.GATHERING,
			new Footprint(2, 2),
			new int[]{0, 60, 300, 1_500},
			new int[]{0, 60, 220, 700},
			new int[]{0, 1, 2, 3}));
		definitions.put(BuildingType.RUNE_ESSENCE_SITE, new Definition(
			"Rune Essence Site",
			"Villagers assigned here gather rune essence.",
			BuildingCategory.GATHERING,
			new Footprint(2, 2),
			new int[]{0, 300, 1_200, 4_800},
			new int[]{0, 400, 1_200, 3_000},
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

	public Footprint getFootprint(BuildingType type)
	{
		return definitionFor(type).footprint;
	}

	public BuildingCategory getCategory(BuildingType type)
	{
		return definitionFor(type).category;
	}

	public int getBuildDurationSeconds(BuildingType type)
	{
		return getBuildDurationSeconds(type, 1);
	}

	public int getBuildDurationSeconds(BuildingType type, int targetLevel)
	{
		Definition definition = definitionFor(type);
		validateTargetLevel(type, targetLevel, definition);
		return definition.durationByTargetLevel[targetLevel];
	}

	public int getManaCapacityForLevel(BuildingType type, int level)
	{
		if (level < 0 || level > getMaxLevel(type))
		{
			throw new IllegalArgumentException("invalid level " + level + " for " + type);
		}
		if (type == BuildingType.TOWN_HALL)
		{
			return 500 + Math.max(0, level - 1) * 250;
		}
		if (type == BuildingType.MANA_WELL)
		{
			return level * 350;
		}
		return 0;
	}

	public int getManaProductionPerMinuteForLevel(BuildingType type, int level)
	{
		if (level < 0 || level > getMaxLevel(type))
		{
			throw new IllegalArgumentException("invalid level " + level + " for " + type);
		}
		if (type == BuildingType.MANA_GROVE)
		{
			return level * 2;
		}
		return 0;
	}

	public int getPassiveStorageForLevel(BuildingType type, int level)
	{
		if (level < 0 || level > getMaxLevel(type))
		{
			throw new IllegalArgumentException("invalid level " + level + " for " + type);
		}
		if (type == BuildingType.MANA_GROVE)
		{
			return level * 100;
		}
		return 0;
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
		if (definition.durationByTargetLevel.length != definition.costByTargetLevel.length
			|| targetLevel < 1
			|| targetLevel >= definition.costByTargetLevel.length)
		{
			throw new IllegalArgumentException(
				"invalid target level " + targetLevel + " for " + type);
		}
	}

	private static final class Definition
	{
		private final String displayName;
		private final String description;
		private final BuildingCategory category;
		private final Footprint footprint;
		private final int[] durationByTargetLevel;
		private final int[] costByTargetLevel;
		private final int[] requiredTownHallByTargetLevel;

		private Definition(
			String displayName,
			String description,
			BuildingCategory category,
			Footprint footprint,
			int[] durationByTargetLevel,
			int[] costByTargetLevel,
			int[] requiredTownHallByTargetLevel)
		{
			this.displayName = displayName;
			this.description = description;
			this.category = category;
			this.footprint = footprint;
			this.durationByTargetLevel = durationByTargetLevel;
			this.costByTargetLevel = costByTargetLevel;
			this.requiredTownHallByTargetLevel = requiredTownHallByTargetLevel;
		}
	}
}
