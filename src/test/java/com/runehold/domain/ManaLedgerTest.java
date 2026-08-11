package com.runehold.domain;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ManaLedgerTest
{
	private static final LocalDate DAY_ONE = LocalDate.of(2026, 8, 11);

	private AtomicReference<LocalDate> currentDate;
	private VillageState state;
	private ManaLedger ledger;

	@Before
	public void setUp()
	{
		currentDate = new AtomicReference<>(DAY_ONE);
		state = VillageState.fresh(DAY_ONE);
		ledger = new ManaLedger(state, currentDate::get);
	}

	@Test
	public void freshVillageStartsWithStarterMana()
	{
		assertEquals(250L, state.getMana());
		assertEquals(0, state.getManaEarnedToday());
		assertEquals(DAY_ONE, state.getManaEarningDate());
	}

	@Test
	public void firstEventEstablishesBaselineWithoutAwardingMana()
	{
		ManaAward award = ledger.recordXp("WOODCUTTING", 1_000);

		assertTrue(award.isBaselineEstablished());
		assertEquals(0, award.getManaAwarded());
		assertEquals(250L, state.getMana());
	}

	@Test
	public void convertsPositiveXpAndCarriesRemainder()
	{
		ledger.recordXp("WOODCUTTING", 1_000);

		ManaAward first = ledger.recordXp("WOODCUTTING", 1_250);
		ManaAward second = ledger.recordXp("WOODCUTTING", 1_300);

		assertEquals(2, first.getManaAwarded());
		assertEquals(50, first.getXpRemainder());
		assertEquals(1, second.getManaAwarded());
		assertEquals(0, second.getXpRemainder());
		assertEquals(253L, state.getMana());
		assertEquals(3, state.getManaEarnedToday());
	}

	@Test
	public void skillRemaindersAreIndependent()
	{
		ledger.recordXp("MINING", 2_000);
		ledger.recordXp("FISHING", 5_000);
		ledger.recordXp("MINING", 2_090);
		ManaAward fishing = ledger.recordXp("FISHING", 5_020);

		assertEquals(0, fishing.getManaAwarded());
		assertEquals(20, fishing.getXpRemainder());
		assertEquals(90, state.getXpRemainder("MINING"));
	}

	@Test
	public void duplicateKeepsRemainderAndNegativeDeltaResetsIt()
	{
		ledger.recordXp("MINING", 1_000);
		ledger.recordXp("MINING", 1_050);

		ManaAward duplicate = ledger.recordXp("MINING", 1_050);
		ManaAward reset = ledger.recordXp("MINING", 500);
		ManaAward afterReset = ledger.recordXp("MINING", 600);

		assertEquals(0, duplicate.getManaAwarded());
		assertEquals(50, duplicate.getXpRemainder());
		assertFalse(reset.isBaselineEstablished());
		assertEquals(0, reset.getXpRemainder());
		assertEquals(1, afterReset.getManaAwarded());
	}

	@Test
	public void dailyCapCannotBeExceededOrCarriedToTomorrow()
	{
		ledger.recordXp("AGILITY", 0);

		ManaAward capped = ledger.recordXp("AGILITY", 2_000_000);
		ManaAward blocked = ledger.recordXp("AGILITY", 2_000_100);

		assertEquals(ManaLedger.DAILY_MANA_CAP, capped.getManaAwarded());
		assertEquals(ManaLedger.DAILY_MANA_CAP, state.getManaEarnedToday());
		assertEquals(10_250L, state.getMana());
		assertEquals(0, blocked.getManaAwarded());
		assertEquals(0, blocked.getXpRemainder());

		currentDate.set(DAY_ONE.plusDays(1));
		ManaAward tomorrow = ledger.recordXp("AGILITY", 2_000_200);

		assertEquals(1, tomorrow.getManaAwarded());
		assertEquals(1, state.getManaEarnedToday());
		assertEquals(DAY_ONE.plusDays(1), state.getManaEarningDate());
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsBlankSkillKeys()
	{
		ledger.recordXp("  ", 1_000);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNegativeCurrentXp()
	{
		ledger.recordXp("MINING", -1);
	}
}
