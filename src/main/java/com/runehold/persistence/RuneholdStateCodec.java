package com.runehold.persistence;

import com.google.gson.Gson;
import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.BuildingType;
import com.runehold.domain.VillageState;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class RuneholdStateCodec
{
	static final int MAX_STATE_JSON_LENGTH = 64 * 1024;

	private final Gson gson;
	private final BuildingCatalog catalog;

	public RuneholdStateCodec(Gson gson, BuildingCatalog catalog)
	{
		this.gson = Objects.requireNonNull(gson, "gson");
		this.catalog = Objects.requireNonNull(catalog, "catalog");
	}

	public String encode(VillageState state)
	{
		return gson.toJson(PersistedRuneholdState.fromDomain(
			Objects.requireNonNull(state, "state")));
	}

	public VillageState decode(String json, LocalDate today)
	{
		Objects.requireNonNull(today, "today");
		if (json == null || json.length() > MAX_STATE_JSON_LENGTH || json.trim().isEmpty())
		{
			return VillageState.fresh(today);
		}

		try
		{
			PersistedRuneholdState persisted = gson.fromJson(json, PersistedRuneholdState.class);
			return restoreValidated(persisted);
		}
		catch (RuntimeException ex)
		{
			return VillageState.fresh(today);
		}
	}

	private VillageState restoreValidated(PersistedRuneholdState persisted)
	{
		if (persisted == null
			|| persisted.schemaVersion != PersistedRuneholdState.CURRENT_SCHEMA_VERSION
			|| persisted.manaEarningDate == null
			|| persisted.xpBaselines == null
			|| persisted.xpRemainders == null
			|| persisted.buildingLevels == null)
		{
			throw new IllegalArgumentException("incomplete Runehold state");
		}

		Map<BuildingType, Integer> buildingLevels = validateBuildingLevels(
			persisted.buildingLevels);

		return VillageState.restore(
			persisted.mana,
			persisted.xpBaselines,
			persisted.xpRemainders,
			persisted.manaEarnedToday,
			LocalDate.parse(persisted.manaEarningDate),
			buildingLevels);
	}

	private Map<BuildingType, Integer> validateBuildingLevels(Map<String, Integer> persistedLevels)
	{
		for (String key : persistedLevels.keySet())
		{
			BuildingType.valueOf(key);
		}

		Map<BuildingType, Integer> levels = new EnumMap<>(BuildingType.class);
		for (BuildingType type : BuildingType.values())
		{
			Integer persistedLevel = persistedLevels.get(type.name());
			int level = persistedLevel == null ? 0 : persistedLevel;
			if (level < 0 || level > catalog.getMaxLevel(type))
			{
				throw new IllegalArgumentException("invalid level for " + type);
			}
			levels.put(type, level);
		}

		int townHallLevel = levels.get(BuildingType.TOWN_HALL);
		if (townHallLevel < 1)
		{
			throw new IllegalArgumentException("Town Hall must be present");
		}

		for (BuildingType type : BuildingType.values())
		{
			int level = levels.get(type);
			if (type != BuildingType.TOWN_HALL
				&& level > 0
				&& catalog.getRequiredTownHallLevel(type, level) > townHallLevel)
			{
				throw new IllegalArgumentException("building progression is locked");
			}
		}

		return levels;
	}
}
