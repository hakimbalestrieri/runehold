package com.runehold.domain;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class VillageState
{
	public static final long STARTER_MANA = 250L;
	public static final int MAX_TRACKED_SKILLS = 64;

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
		for (BuildingType type : BuildingType.values())
		{
			buildingLevels.put(type, 0);
		}
		buildingLevels.put(BuildingType.TOWN_HALL, 1);
	}

	public static VillageState fresh(LocalDate today)
	{
		return new VillageState(STARTER_MANA, today);
	}

	public static VillageState restore(
		long mana,
		Map<String, Integer> xpBaselines,
		Map<String, Integer> xpRemainders,
		int manaEarnedToday,
		LocalDate manaEarningDate,
		Map<BuildingType, Integer> buildingLevels)
	{
		Objects.requireNonNull(xpBaselines, "xpBaselines");
		Objects.requireNonNull(xpRemainders, "xpRemainders");
		Objects.requireNonNull(buildingLevels, "buildingLevels");
		if (mana < 0 || mana > Long.MAX_VALUE - ManaLedger.DAILY_MANA_CAP)
		{
			throw new IllegalArgumentException("invalid mana balance");
		}
		if (manaEarnedToday < 0 || manaEarnedToday > ManaLedger.DAILY_MANA_CAP)
		{
			throw new IllegalArgumentException("invalid daily mana");
		}
		if (xpBaselines.size() > MAX_TRACKED_SKILLS
			|| xpRemainders.size() > MAX_TRACKED_SKILLS)
		{
			throw new IllegalArgumentException("too many skill tracking entries");
		}

		VillageState state = new VillageState(mana, manaEarningDate);
		state.manaEarnedToday = manaEarnedToday;
		state.xpBaselines.clear();
		for (Map.Entry<String, Integer> entry : xpBaselines.entrySet())
		{
			validateSkillEntry(entry.getKey(), entry.getValue(), false);
			state.xpBaselines.put(entry.getKey(), entry.getValue());
		}
		state.xpRemainders.clear();
		for (Map.Entry<String, Integer> entry : xpRemainders.entrySet())
		{
			validateSkillEntry(entry.getKey(), entry.getValue(), true);
			if (entry.getValue() > 0)
			{
				state.xpRemainders.put(entry.getKey(), entry.getValue());
			}
		}
		state.buildingLevels.clear();
		state.buildingLevels.putAll(buildingLevels);
		if (state.levelOf(BuildingType.TOWN_HALL) < 1)
		{
			throw new IllegalArgumentException("Town Hall must be present");
		}
		return state;
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

	public Map<String, Integer> getXpBaselines()
	{
		return Collections.unmodifiableMap(new HashMap<>(xpBaselines));
	}

	public Map<String, Integer> getXpRemainders()
	{
		return Collections.unmodifiableMap(new HashMap<>(xpRemainders));
	}

	public Map<BuildingType, Integer> getBuildingLevels()
	{
		return Collections.unmodifiableMap(new EnumMap<>(buildingLevels));
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

	public void rollEarningDayIfNeeded(LocalDate date)
	{
		Objects.requireNonNull(date, "date");
		if (!date.equals(manaEarningDate))
		{
			beginEarningDay(date);
		}
	}

	public void clearXpBaselines()
	{
		xpBaselines.clear();
	}

	public void clearXpTracking()
	{
		xpBaselines.clear();
		xpRemainders.clear();
	}

	private static void validateSkillEntry(String key, Integer value, boolean remainder)
	{
		if (key == null
			|| key.trim().isEmpty()
			|| key.length() > 32
			|| value == null
			|| value < 0)
		{
			throw new IllegalArgumentException("invalid skill tracking entry");
		}
		if (remainder && value >= ManaLedger.XP_PER_MANA)
		{
			throw new IllegalArgumentException("invalid XP remainder");
		}
	}
}
