package com.runehold.domain;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class VillageState
{
	public static final long STARTER_MANA = 250L;

	private long mana;
	private final Map<String, Integer> xpBaselines;
	private final Map<String, Integer> xpRemainders;
	private final Map<BuildingType, Integer> buildingLevels;
	private int manaEarnedToday;
	private LocalDate manaEarningDate;

	private VillageState(long mana, LocalDate manaEarningDate)
	{
		this.mana = mana;
		this.manaEarningDate = Objects.requireNonNull(manaEarningDate, "manaEarningDate");
		xpBaselines = new HashMap<>();
		xpRemainders = new HashMap<>();
		buildingLevels = new EnumMap<>(BuildingType.class);
		buildingLevels.put(BuildingType.TOWN_HALL, 1);
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

	public int levelOf(BuildingType type)
	{
		if (type == null)
		{
			throw new IllegalArgumentException("building type is required");
		}
		return buildingLevels.getOrDefault(type, 0);
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

	void spendMana(int amount)
	{
		if (amount < 0 || amount > mana)
		{
			throw new IllegalArgumentException("invalid mana spend: " + amount);
		}
		mana -= amount;
	}

	void setBuildingLevel(BuildingType type, int level)
	{
		if (type == null || level < 0)
		{
			throw new IllegalArgumentException("invalid building level");
		}
		buildingLevels.put(type, level);
	}

	void beginEarningDay(LocalDate date)
	{
		manaEarningDate = Objects.requireNonNull(date, "date");
		manaEarnedToday = 0;
	}
}
