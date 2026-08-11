package com.runehold.domain;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class VillageState
{
	public static final long STARTER_MANA = 250L;

	private long mana;
	private final Map<String, Integer> xpBaselines;
	private final Map<String, Integer> xpRemainders;
	private int manaEarnedToday;
	private LocalDate manaEarningDate;

	private VillageState(long mana, LocalDate manaEarningDate)
	{
		this.mana = mana;
		this.manaEarningDate = Objects.requireNonNull(manaEarningDate, "manaEarningDate");
		xpBaselines = new HashMap<>();
		xpRemainders = new HashMap<>();
	}

	public static VillageState fresh(LocalDate today)
	{
		return new VillageState(STARTER_MANA, today);
	}

	public long getMana()
	{
		return mana;
	}

	public int getManaEarnedToday()
	{
		return manaEarnedToday;
	}

	public LocalDate getManaEarningDate()
	{
		return manaEarningDate;
	}

	public int getXpRemainder(String skillKey)
	{
		return xpRemainders.getOrDefault(skillKey, 0);
	}

	Integer getXpBaseline(String skillKey)
	{
		return xpBaselines.get(skillKey);
	}

	void setXpBaseline(String skillKey, int xp)
	{
		xpBaselines.put(skillKey, xp);
	}

	void setXpRemainder(String skillKey, int xpRemainder)
	{
		if (xpRemainder == 0)
		{
			xpRemainders.remove(skillKey);
			return;
		}

		xpRemainders.put(skillKey, xpRemainder);
	}

	void addMana(int amount)
	{
		if (amount < 0)
		{
			throw new IllegalArgumentException("amount must not be negative");
		}

		mana += amount;
		manaEarnedToday += amount;
	}

	void beginEarningDay(LocalDate date)
	{
		manaEarningDate = Objects.requireNonNull(date, "date");
		manaEarnedToday = 0;
	}
}
