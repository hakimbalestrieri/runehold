package com.runehold.domain;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

public final class ManaLedger
{
	public static final int XP_PER_MANA = 100;
	public static final int DAILY_MANA_CAP = 10_000;

	private final VillageState state;
	private final Supplier<LocalDate> currentDate;

	public ManaLedger(VillageState state, Supplier<LocalDate> currentDate)
	{
		this.state = Objects.requireNonNull(state, "state");
		this.currentDate = Objects.requireNonNull(currentDate, "currentDate");
	}

	public ManaAward recordXp(String skillKey, int currentXp)
	{
		String normalizedSkillKey = normalizeSkillKey(skillKey);
		if (currentXp < 0)
		{
			throw new IllegalArgumentException("currentXp must not be negative");
		}

		LocalDate today = Objects.requireNonNull(currentDate.get(), "currentDate result");
		state.rollEarningDayIfNeeded(today);

		Integer previousXp = state.getXpBaseline(normalizedSkillKey);
		if (previousXp == null)
		{
			state.setXpBaseline(normalizedSkillKey, currentXp);
			return ManaAward.baseline(
				state.getXpRemainder(normalizedSkillKey),
				state.getManaEarnedToday());
		}

		if (currentXp <= previousXp)
		{
			state.setXpBaseline(normalizedSkillKey, currentXp);
			if (currentXp < previousXp)
			{
				state.setXpRemainder(normalizedSkillKey, 0);
			}

			return ManaAward.result(
				0,
				state.getXpRemainder(normalizedSkillKey),
				state.getManaEarnedToday());
		}

		long xpDelta = (long) currentXp - previousXp;
		long convertibleXp = xpDelta + state.getXpRemainder(normalizedSkillKey);
		long possibleMana = convertibleXp / XP_PER_MANA;
		int xpRemainder = (int) (convertibleXp % XP_PER_MANA);
		int availableToday = Math.max(0, DAILY_MANA_CAP - state.getManaEarnedToday());
		int manaAwarded = (int) Math.min(possibleMana, availableToday);

		state.setXpBaseline(normalizedSkillKey, currentXp);
		state.setXpRemainder(normalizedSkillKey, xpRemainder);
		state.addMana(manaAwarded);

		return ManaAward.result(
			manaAwarded,
			xpRemainder,
			state.getManaEarnedToday());
	}

	private static String normalizeSkillKey(String skillKey)
	{
		if (skillKey == null || skillKey.trim().isEmpty())
		{
			throw new IllegalArgumentException("skillKey must not be blank");
		}

		return skillKey.trim().toUpperCase(Locale.ROOT);
	}
}
