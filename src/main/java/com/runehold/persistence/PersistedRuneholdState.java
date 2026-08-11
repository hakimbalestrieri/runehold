package com.runehold.persistence;

import com.runehold.domain.BuildingType;
import com.runehold.domain.VillageState;
import java.util.LinkedHashMap;
import java.util.Map;

final class PersistedRuneholdState
{
	static final int CURRENT_SCHEMA_VERSION = 1;

	int schemaVersion;
	long mana;
	int manaEarnedToday;
	String manaEarningDate;
	Map<String, Integer> xpBaselines;
	Map<String, Integer> xpRemainders;
	Map<String, Integer> buildingLevels;

	private PersistedRuneholdState()
	{
	}

	static PersistedRuneholdState fromDomain(VillageState state)
	{
		PersistedRuneholdState persisted = new PersistedRuneholdState();
		persisted.schemaVersion = CURRENT_SCHEMA_VERSION;
		persisted.mana = state.getMana();
		persisted.manaEarnedToday = state.getManaEarnedToday();
		persisted.manaEarningDate = state.getManaEarningDate().toString();
		persisted.xpBaselines = new LinkedHashMap<>(state.getXpBaselines());
		persisted.xpRemainders = new LinkedHashMap<>(state.getXpRemainders());
		persisted.buildingLevels = new LinkedHashMap<>();
		for (BuildingType type : BuildingType.values())
		{
			persisted.buildingLevels.put(type.name(), state.levelOf(type));
		}
		return persisted;
	}
}
