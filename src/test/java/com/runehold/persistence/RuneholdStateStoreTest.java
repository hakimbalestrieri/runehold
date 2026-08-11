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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RuneholdStateStoreTest
{
	private static final LocalDate TODAY = LocalDate.of(2026, 8, 11);

	private FakeProfileConfiguration configuration;
	private RuneholdStateStore store;

	@Before
	public void setUp()
	{
		configuration = new FakeProfileConfiguration();
		store = new RuneholdStateStore(
			configuration,
			new RuneholdStateCodec(new Gson(), new BuildingCatalog()));
	}

	@Test
	public void doesNotReadOrWriteWithoutRuneScapeProfile()
	{
		VillageState state = store.load(TODAY);

		assertEquals(VillageState.STARTER_MANA, state.getMana());
		assertFalse(store.save(state));
		assertEquals(0, configuration.readCount);
		assertEquals(0, configuration.writeCount);
		assertNull(configuration.value);
	}

	@Test
	public void savesAndLoadsThroughProfileScopedConfiguration()
	{
		configuration.profileKey = "profile-1";
		VillageState state = VillageState.fresh(TODAY);
		new Village(state, new BuildingCatalog()).upgrade(BuildingType.MANA_WELL);

		assertTrue(store.save(state));
		VillageState restored = store.load(TODAY);

		assertEquals(1, configuration.writeCount);
		assertEquals(1, configuration.readCount);
		assertEquals("runehold", configuration.lastGroup);
		assertEquals("state", configuration.lastKey);
		assertEquals(150L, restored.getMana());
		assertEquals(1, restored.levelOf(BuildingType.MANA_WELL));
	}

	@Test
	public void loadingOnANewDayResetsOnlyDailyProgress()
	{
		configuration.profileKey = "profile-1";
		VillageState state = VillageState.fresh(TODAY);
		ManaLedger ledger = new ManaLedger(state, () -> TODAY);
		ledger.recordXp("MINING", 1_000);
		ledger.recordXp("MINING", 1_500);
		store.save(state);

		VillageState restored = store.load(TODAY.plusDays(1));

		assertEquals(255L, restored.getMana());
		assertEquals(0, restored.getManaEarnedToday());
		assertEquals(TODAY.plusDays(1), restored.getManaEarningDate());
	}

	private static final class FakeProfileConfiguration
		implements RuneholdStateStore.ProfileConfiguration
	{
		private String profileKey;
		private String value;
		private String lastGroup;
		private String lastKey;
		private int readCount;
		private int writeCount;

		@Override
		public String getProfileKey()
		{
			return profileKey;
		}

		@Override
		public String get(String group, String key)
		{
			readCount++;
			lastGroup = group;
			lastKey = key;
			return value;
		}

		@Override
		public void set(String group, String key, String newValue)
		{
			writeCount++;
			lastGroup = group;
			lastKey = key;
			value = newValue;
		}
	}
}
