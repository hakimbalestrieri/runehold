package com.runehold.persistence;

import com.google.gson.Gson;
import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.BuildingType;
import com.runehold.domain.ManaLedger;
import com.runehold.domain.Village;
import com.runehold.domain.VillageState;
import java.time.LocalDate;
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

	private static void assertFresh(VillageState state)
	{
		assertEquals(VillageState.STARTER_MANA, state.getMana());
		assertEquals(1, state.levelOf(BuildingType.TOWN_HALL));
		assertEquals(0, state.levelOf(BuildingType.MANA_WELL));
		assertEquals(TODAY, state.getManaEarningDate());
	}
}
