package com.runehold.domain;

import com.runehold.domain.layout.GridPoint;
import com.runehold.domain.layout.VillageLayout;
import java.time.LocalDate;
import java.time.Instant;
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
	private final Map<BuildingType, GridPoint> buildingPositions;
	private ResourceInventory resources = new ResourceInventory();
	private final Map<BuildingType, GatheringSiteState> gatheringSites =
		new EnumMap<>(BuildingType.class);
	private final Map<String, Worker> workers = new HashMap<>();
	private ConstructionJob constructionJob;
	private int storedGroveMana;
	private long groveProductionUpdatedAtEpochMillis;
	private long lastOfflineProgressAtEpochMillis;
	private int manaEarnedToday;
	private LocalDate manaEarningDate;

	private VillageState(long mana, LocalDate manaEarningDate)
	{
		this.mana = mana;
		this.manaEarningDate = Objects.requireNonNull(manaEarningDate, "manaEarningDate");
		xpBaselines = new HashMap<>();
		xpRemainders = new HashMap<>();
		buildingLevels = new EnumMap<>(BuildingType.class);
		buildingPositions = new EnumMap<>(BuildingType.class);
		for (BuildingType type : BuildingType.values())
		{
			buildingLevels.put(type, 0);
		}
		buildingLevels.put(BuildingType.TOWN_HALL, 1);
		buildingPositions.put(
			BuildingType.TOWN_HALL,
			VillageLayout.defaultPosition(BuildingType.TOWN_HALL));
		initializeDefaultGathering(manaEarningDate.atStartOfDay()
			.toInstant(java.time.ZoneOffset.UTC).toEpochMilli());
		initializeDefaultWorkers();
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
		Map<BuildingType, GridPoint> positions = new EnumMap<>(BuildingType.class);
		for (Map.Entry<BuildingType, Integer> entry : buildingLevels.entrySet())
		{
			if (entry.getValue() != null && entry.getValue() > 0)
			{
				positions.put(entry.getKey(), VillageLayout.defaultPosition(entry.getKey()));
			}
		}
		return restore(
			mana,
			xpBaselines,
			xpRemainders,
			manaEarnedToday,
			manaEarningDate,
			buildingLevels,
			positions);
	}

	public static VillageState restore(
		long mana,
		Map<String, Integer> xpBaselines,
		Map<String, Integer> xpRemainders,
		int manaEarnedToday,
		LocalDate manaEarningDate,
		Map<BuildingType, Integer> buildingLevels,
		Map<BuildingType, GridPoint> buildingPositions)
	{
		return restore(
			mana, xpBaselines, xpRemainders, manaEarnedToday, manaEarningDate,
			buildingLevels, buildingPositions, null);
	}

	public static VillageState restore(
		long mana,
		Map<String, Integer> xpBaselines,
		Map<String, Integer> xpRemainders,
		int manaEarnedToday,
		LocalDate manaEarningDate,
		Map<BuildingType, Integer> buildingLevels,
		Map<BuildingType, GridPoint> buildingPositions,
		ConstructionJob constructionJob)
	{
		return restore(
			mana, xpBaselines, xpRemainders, manaEarnedToday, manaEarningDate,
			buildingLevels, buildingPositions, constructionJob, 0, 0);
	}

	public static VillageState restore(
		long mana,
		Map<String, Integer> xpBaselines,
		Map<String, Integer> xpRemainders,
		int manaEarnedToday,
		LocalDate manaEarningDate,
		Map<BuildingType, Integer> buildingLevels,
		Map<BuildingType, GridPoint> buildingPositions,
		ConstructionJob constructionJob,
		int storedGroveMana,
		long groveProductionUpdatedAtEpochMillis)
	{
		return restore(
			mana, xpBaselines, xpRemainders, manaEarnedToday, manaEarningDate,
			buildingLevels, buildingPositions, constructionJob, storedGroveMana,
			groveProductionUpdatedAtEpochMillis, null, null, null, 0);
	}

	public static VillageState restore(
		long mana,
		Map<String, Integer> xpBaselines,
		Map<String, Integer> xpRemainders,
		int manaEarnedToday,
		LocalDate manaEarningDate,
		Map<BuildingType, Integer> buildingLevels,
		Map<BuildingType, GridPoint> buildingPositions,
		ConstructionJob constructionJob,
		int storedGroveMana,
		long groveProductionUpdatedAtEpochMillis,
		ResourceInventory resources,
		Map<BuildingType, GatheringSiteState> gatheringSites,
		Map<String, Worker> workers,
		long lastOfflineProgressAtEpochMillis)
	{
		Objects.requireNonNull(xpBaselines, "xpBaselines");
		Objects.requireNonNull(xpRemainders, "xpRemainders");
		Objects.requireNonNull(buildingLevels, "buildingLevels");
		Objects.requireNonNull(buildingPositions, "buildingPositions");
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
		state.buildingPositions.clear();
		state.buildingPositions.putAll(buildingPositions);
		state.resources = resources == null ? new ResourceInventory() : resources;
		state.initializeDefaultGathering(lastOfflineProgressAtEpochMillis);
		if (gatheringSites != null)
		{
			state.gatheringSites.putAll(gatheringSites);
		}
		state.workers.clear();
		if (workers == null || workers.isEmpty())
		{
			state.initializeDefaultWorkers();
		}
		else
		{
			state.workers.putAll(workers);
		}
		state.lastOfflineProgressAtEpochMillis = Math.max(0, lastOfflineProgressAtEpochMillis);
		state.constructionJob = constructionJob;
		state.setStoredGroveMana(storedGroveMana, groveProductionUpdatedAtEpochMillis);
		if (state.levelOf(BuildingType.TOWN_HALL) < 1)
		{
			throw new IllegalArgumentException("Town Hall must be present");
		}
		for (BuildingType type : BuildingType.values())
		{
			boolean built = state.levelOf(type) > 0
				|| (constructionJob != null && constructionJob.getBuildingType() == type);
			if (built != state.buildingPositions.containsKey(type))
			{
				throw new IllegalArgumentException("building position mismatch for " + type);
			}
		}
		return state;
	}

	/**
	 * Every gathering site carries production state from the start; whether it produces
	 * is decided by its building level, which is zero until the player builds it.
	 */
	private void initializeDefaultGathering(long timestamp)
	{
		gatheringSites.clear();
		for (BuildingType type : BuildingType.values())
		{
			if (type.isGatheringSite())
			{
				gatheringSites.put(type, GatheringSiteState.idle(type, timestamp));
			}
		}
		if (lastOfflineProgressAtEpochMillis == 0)
		{
			lastOfflineProgressAtEpochMillis = Math.max(0, timestamp);
		}
	}

	private void initializeDefaultWorkers()
	{
		workers.clear();
		for (int i = 1; i <= 8; i++)
		{
			workers.put("worker-" + i, Worker.settler(i, new GridPoint(6, 9)));
		}
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

	public Map<BuildingType, GridPoint> getBuildingPositions()
	{
		return Collections.unmodifiableMap(new EnumMap<>(buildingPositions));
	}

	public ResourceInventory getResources()
	{
		return ResourceInventory.from(resources.asMap());
	}

	public Map<BuildingType, GatheringSiteState> getGatheringSites()
	{
		return Collections.unmodifiableMap(new EnumMap<>(gatheringSites));
	}

	public GatheringSiteState getGatheringSite(BuildingType type)
	{
		return gatheringSites.get(type);
	}

	java.util.Collection<GatheringSiteState> gatheringSiteStates()
	{
		return gatheringSites.values();
	}

	public Map<String, Worker> getWorkers()
	{
		return Collections.unmodifiableMap(new HashMap<>(workers));
	}

	public Worker getWorker(String workerId)
	{
		return workers.get(workerId);
	}

	public long getLastOfflineProgressAtEpochMillis()
	{
		return lastOfflineProgressAtEpochMillis;
	}

	public GridPoint positionOf(BuildingType type)
	{
		if (type == null)
		{
			throw new IllegalArgumentException("building type is required");
		}
		return buildingPositions.get(type);
	}

	public ConstructionJob getConstructionJob()
	{
		return constructionJob;
	}

	public int getStoredGroveMana()
	{
		return storedGroveMana;
	}

	public long getGroveProductionUpdatedAtEpochMillis()
	{
		return groveProductionUpdatedAtEpochMillis;
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

	void setBuildingPosition(BuildingType type, GridPoint position)
	{
		if (type == null || position == null)
		{
			throw new IllegalArgumentException("invalid building position");
		}
		buildingPositions.put(type, position);
	}

	void setConstructionJob(ConstructionJob job)
	{
		constructionJob = job;
	}

	void setStoredGroveMana(int storedMana, long updatedAtEpochMillis)
	{
		if (storedMana < 0 || updatedAtEpochMillis < 0)
		{
			throw new IllegalArgumentException("invalid stored mana state");
		}
		storedGroveMana = storedMana;
		groveProductionUpdatedAtEpochMillis = updatedAtEpochMillis;
	}

	void setLastOfflineProgressAtEpochMillis(long timestamp)
	{
		if (timestamp < 0)
		{
			throw new IllegalArgumentException("invalid offline timestamp");
		}
		lastOfflineProgressAtEpochMillis = timestamp;
	}

	void putGatheringSite(GatheringSiteState site)
	{
		gatheringSites.put(site.getType(), site);
	}

	void putWorker(Worker worker)
	{
		workers.put(worker.getId(), worker);
	}

	ResourceInventory mutableResources()
	{
		return resources;
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
