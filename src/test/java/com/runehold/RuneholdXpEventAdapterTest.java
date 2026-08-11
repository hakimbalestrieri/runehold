package com.runehold;

import com.runehold.domain.ManaLedger;
import com.runehold.domain.VillageState;
import java.time.LocalDate;
import net.runelite.api.Skill;
import net.runelite.api.events.StatChanged;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RuneholdXpEventAdapterTest
{
	private VillageState state;
	private RuneholdXpEventAdapter adapter;

	@Before
	public void setUp()
	{
		LocalDate today = LocalDate.of(2026, 8, 11);
		state = VillageState.fresh(today);
		adapter = new RuneholdXpEventAdapter(new ManaLedger(state, () -> today));
	}

	@Test
	public void mapsSkillAndCurrentXpIntoLedger()
	{
		int baselineAward = adapter.record(new StatChanged(Skill.ATTACK, 1_000, 10, 10));
		int earned = adapter.record(new StatChanged(Skill.ATTACK, 1_250, 11, 11));

		assertEquals(0, baselineAward);
		assertEquals(2, earned);
		assertEquals(252L, state.getMana());
	}

	@Test
	public void ignoresOverallXpToAvoidDoubleCounting()
	{
		int earned = adapter.record(new StatChanged(null, 10_000, 100, 100));

		assertEquals(0, earned);
		assertTrue(state.getXpBaselines().isEmpty());
		assertEquals(250L, state.getMana());
	}
}
