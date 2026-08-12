package com.runehold.persistence;

import com.google.gson.Gson;
import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.BuildingType;
import com.runehold.domain.GatheringSiteType;
import com.runehold.domain.ManaLedger;
import com.runehold.domain.ResourceType;
import com.runehold.domain.Village;
import com.runehold.domain.VillageState;
import com.runehold.domain.ConstructionJob;
import com.runehold.domain.WorkerState;
import com.runehold.domain.layout.GridPoint;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneOffset;
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
		Village originalVillage = new Village(original, new BuildingCatalog());
		originalVillage.build(BuildingType.MANA_WELL, new GridPoint(1, 12));
		originalVillage.move(BuildingType.TOWN_HALL, new GridPoint(8, 7));

		VillageState restored = codec.decode(codec.encode(original), TODAY.plusDays(1));

		assertEquals(original.getMana(), restored.getMana());
		assertEquals(original.getManaEarnedToday(), restored.getManaEarnedToday());
		assertEquals(original.getManaEarningDate(), restored.getManaEarningDate());
		assertEquals(original.getXpBaselines(), restored.getXpBaselines());
		assertEquals(original.getXpRemainders(), restored.getXpRemainders());
		assertEquals(original.getBuildingLevels(), restored.getBuildingLevels());
		assertEquals(original.getBuildingPositions(), restored.getBuildingPositions());
	}

	@Test
	public void roundTripsGatheringResourcesWorkersAndAssignments()
	{
		VillageState original = VillageState.fresh(TODAY);
		Village village = new Village(original, new BuildingCatalog(), true,
			Clock.fixed(Instant.parse("2026-08-11T09:00:00Z"), ZoneOffset.UTC));
		village.assignWorker("worker-1", GatheringSiteType.MINE);
		village = new Village(original, new BuildingCatalog(), true,
			Clock.fixed(Instant.parse("2026-08-11T09:10:00Z"), ZoneOffset.UTC));
		village.collectGatheringSite(GatheringSiteType.MINE);

		VillageState restored = codec.decode(codec.encode(original), TODAY);

		assertEquals(30, restored.getResources().get(ResourceType.ORE));
		assertEquals(GatheringSiteType.MINE, restored.getWorker("worker-1").getAssignment());
		assertEquals(WorkerState.WORKING, restored.getWorker("worker-1").getState());
		assertEquals(1, restored.getGatheringSite(
			GatheringSiteType.MINE).getAssignedWorkerIds().size());
	}

	@Test
	public void migratesVersionOneStateToDeterministicPositions()
	{
		String json = "{"
			+ "\"schemaVersion\":1,"
			+ "\"mana\":150,"
			+ "\"manaEarnedToday\":0,"
			+ "\"manaEarningDate\":\"2026-08-11\","
			+ "\"xpBaselines\":{},"
			+ "\"xpRemainders\":{},"
			+ "\"buildingLevels\":{\"TOWN_HALL\":1,\"MANA_WELL\":1}"
			+ "}";

		VillageState restored = codec.decode(json, TODAY);

		assertEquals(new GridPoint(7, 7), restored.positionOf(BuildingType.TOWN_HALL));
		assertEquals(new GridPoint(3, 9), restored.positionOf(BuildingType.MANA_WELL));
	}

	@Test
	public void rejectsCollidingVersionTwoPositions()
	{
		String json = "{"
			+ "\"schemaVersion\":2,"
			+ "\"mana\":150,"
			+ "\"manaEarnedToday\":0,"
			+ "\"manaEarningDate\":\"2026-08-11\","
			+ "\"xpBaselines\":{},"
			+ "\"xpRemainders\":{},"
			+ "\"buildingLevels\":{\"TOWN_HALL\":1,\"MANA_WELL\":1},"
			+ "\"buildingPositions\":{"
			+ "\"TOWN_HALL\":{\"x\":7,\"y\":7},"
			+ "\"MANA_WELL\":{\"x\":7,\"y\":7}}"
			+ "}";

		assertFresh(codec.decode(json, TODAY));
	}

	@Test
	public void roundTripsActiveConstructionJob()
	{
		VillageState original = VillageState.fresh(TODAY);
		Map<BuildingType, Integer> levels = original.getBuildingLevels();
		Map<BuildingType, GridPoint> positions = new java.util.EnumMap<>(BuildingType.class);
		positions.put(BuildingType.TOWN_HALL, new GridPoint(7, 7));
		positions.put(BuildingType.MANA_WELL, new GridPoint(1, 12));
		VillageState underConstruction = VillageState.restore(
			250, new LinkedHashMap<>(), new LinkedHashMap<>(), 0, TODAY, levels, positions,
			new ConstructionJob(BuildingType.MANA_WELL, 1,
				Instant.parse("2026-08-12T10:00:00Z")));

		VillageState restored = codec.decode(codec.encode(underConstruction), TODAY);

		assertEquals(BuildingType.MANA_WELL,
			restored.getConstructionJob().getBuildingType());
		assertEquals(new GridPoint(1, 12), restored.positionOf(BuildingType.MANA_WELL));
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
