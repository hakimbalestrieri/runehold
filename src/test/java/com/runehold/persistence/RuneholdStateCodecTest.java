package com.runehold.persistence;

import com.google.gson.Gson;
import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.BuildingType;
import com.runehold.domain.ManaLedger;
import com.runehold.domain.Village;
import com.runehold.domain.VillageState;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RuneholdStateCodecTest
{
	private static final LocalDate TODAY = LocalDate.of(2026, 8, 11);

	private RuneholdStateCodec codec;

	@Before
	public void setUp()
	{
		codec = new RuneholdStateCodec(new Gson(), new BuildingCatalog());
	}

	@Test
	public void roundTripsCompleteState()
	{
		VillageState original = VillageState.fresh(TODAY);
		ManaLedger ledger = new ManaLedger(original, () -> TODAY);
		ledger.recordXp("MINING", 1_000);
		ledger.recordXp("MINING", 1_250);
		new Village(original, new BuildingCatalog()).upgrade(BuildingType.MANA_WELL);

		VillageState restored = codec.decode(codec.encode(original), TODAY.plusDays(1));

		assertEquals(original.getMana(), restored.getMana());
		assertEquals(original.getManaEarnedToday(), restored.getManaEarnedToday());
		assertEquals(original.getManaEarningDate(), restored.getManaEarningDate());
		assertEquals(original.getXpBaselines(), restored.getXpBaselines());
		assertEquals(original.getXpRemainders(), restored.getXpRemainders());
		assertEquals(original.getBuildingLevels(), restored.getBuildingLevels());
	}

	@Test
	public void malformedJsonFallsBackToFreshState()
	{
		assertFresh(codec.decode("{ definitely not json", TODAY));
	}

	@Test
	public void unsupportedSchemaFallsBackToFreshState()
	{
		String json = "{\"schemaVersion\":99,\"mana\":999999}";

		assertFresh(codec.decode(json, TODAY));
	}

	@Test
	public void invalidDomainValuesFallBackToFreshState()
	{
		String json = "{"
			+ "\"schemaVersion\":1,"
			+ "\"mana\":-1,"
			+ "\"manaEarnedToday\":0,"
			+ "\"manaEarningDate\":\"2026-08-11\","
			+ "\"xpBaselines\":{},"
			+ "\"xpRemainders\":{},"
			+ "\"buildingLevels\":{\"TOWN_HALL\":1}"
			+ "}";

		assertFresh(codec.decode(json, TODAY));
	}

	@Test
	public void impossibleBuildingProgressionFallsBackToFreshState()
	{
		String json = "{"
			+ "\"schemaVersion\":1,"
			+ "\"mana\":250,"
			+ "\"manaEarnedToday\":0,"
			+ "\"manaEarningDate\":\"2026-08-11\","
			+ "\"xpBaselines\":{},"
			+ "\"xpRemainders\":{},"
			+ "\"buildingLevels\":{\"TOWN_HALL\":1,\"WORKSHOP\":3}"
			+ "}";

		assertFresh(codec.decode(json, TODAY));
	}

	@Test
	public void blankStateFallsBackToFreshState()
	{
		assertFresh(codec.decode(null, TODAY));
		assertFresh(codec.decode("  ", TODAY));
	}

	@Test
	public void oversizedStateFallsBackBeforeParsing()
	{
		String oversized = "x".repeat(RuneholdStateCodec.MAX_STATE_JSON_LENGTH + 1);

		assertFresh(codec.decode(oversized, TODAY));
	}

	@Test
	public void excessiveSkillTrackingEntriesFallBackToFreshState()
	{
		Map<String, Integer> baselines = new LinkedHashMap<>();
		for (int index = 0; index <= VillageState.MAX_TRACKED_SKILLS; index++)
		{
			baselines.put("SKILL_" + index, index);
		}

		Map<String, Object> persisted = new LinkedHashMap<>();
		persisted.put("schemaVersion", 1);
		persisted.put("mana", 250);
		persisted.put("manaEarnedToday", 0);
		persisted.put("manaEarningDate", "2026-08-11");
		persisted.put("xpBaselines", baselines);
		persisted.put("xpRemainders", new LinkedHashMap<>());
		Map<String, Integer> buildings = new LinkedHashMap<>();
		buildings.put("TOWN_HALL", 1);
		persisted.put("buildingLevels", buildings);

		assertFresh(codec.decode(new Gson().toJson(persisted), TODAY));
	}

	private static void assertFresh(VillageState state)
	{
		assertEquals(VillageState.STARTER_MANA, state.getMana());
		assertEquals(1, state.levelOf(BuildingType.TOWN_HALL));
		assertEquals(0, state.levelOf(BuildingType.MANA_WELL));
		assertEquals(TODAY, state.getManaEarningDate());
	}
}
