package com.runehold.ui;

import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.BuildingCategory;
import com.runehold.domain.BuildingType;
import com.runehold.domain.ConstructionJob;
import com.runehold.domain.ManaLedger;
import com.runehold.domain.ResourceType;
import com.runehold.domain.UpgradeResult;
import com.runehold.domain.Village;
import com.runehold.domain.VillageState;
import com.runehold.domain.Worker;
import com.runehold.domain.layout.Footprint;
import com.runehold.domain.layout.GridPoint;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class RuneholdViewModel
{
	private static final int TEST_BUILD_DURATION_SECONDS = 3;
	private final String manaText;
	private final String dailyProgressText;
	private final long mana;
	private final boolean unlimitedMana;
	private final int manaCapacity;
	private final int collectableGroveMana;
	private final ConstructionJob constructionJob;
	private final List<VillageResourceView> resources;
	private final List<BuildingView> buildings;
	private final Map<BuildingType, BuildingView> buildingsByType;
	private final Map<BuildingType, GridPoint> buildingPositions;

	private RuneholdViewModel(
		String manaText,
		String dailyProgressText,
		long mana,
		boolean unlimitedMana,
		int manaCapacity,
		int collectableGroveMana,
		ConstructionJob constructionJob,
		List<VillageResourceView> resources,
		List<BuildingView> buildings,
		Map<BuildingType, GridPoint> buildingPositions)
	{
		this.manaText = manaText;
		this.dailyProgressText = dailyProgressText;
		this.mana = mana;
		this.unlimitedMana = unlimitedMana;
		this.manaCapacity = manaCapacity;
		this.collectableGroveMana = collectableGroveMana;
		this.constructionJob = constructionJob;
		this.resources = Collections.unmodifiableList(new ArrayList<>(resources));
		this.buildings = Collections.unmodifiableList(new ArrayList<>(buildings));
		this.buildingPositions = Collections.unmodifiableMap(
			new EnumMap<>(buildingPositions));
		this.buildingsByType = new EnumMap<>(BuildingType.class);
		for (BuildingView building : buildings)
		{
			buildingsByType.put(building.getType(), building);
		}
	}

	public static RuneholdViewModel from(
		VillageState state,
		Village village,
		BuildingCatalog catalog)
	{
		Objects.requireNonNull(state, "state");
		Objects.requireNonNull(village, "village");
		Objects.requireNonNull(catalog, "catalog");

		List<BuildingView> buildings = new ArrayList<>();
		for (BuildingType type : BuildingType.values())
		{
			int currentLevel = village.levelOf(type);
			int maxLevel = catalog.getMaxLevel(type);
			UpgradeResult preview = village.previewUpgrade(type);
			buildings.add(toBuildingView(
				type,
				currentLevel,
				maxLevel,
				state.getMana(),
				village.hasUnlimitedMana(),
				catalog,
				preview));
		}

		return new RuneholdViewModel(
			village.hasUnlimitedMana()
				? "unlimited mana - TEST"
				: format(state.getMana()) + " mana",
			village.hasUnlimitedMana()
				? "Testing mode - XP tracking unchanged"
				: "Today: " + format(state.getManaEarnedToday()) + " / "
					+ format(ManaLedger.DAILY_MANA_CAP),
			state.getMana(),
			village.hasUnlimitedMana(),
			village.getManaCapacity(),
			village.peekCollectableGroveMana(),
			village.getConstructionJob(),
			resourcesFor(state, village, catalog),
			buildings,
			state.getBuildingPositions());
	}

	private static List<VillageResourceView> resourcesFor(
		VillageState state,
		Village village,
		BuildingCatalog catalog)
	{
		List<VillageResourceView> resources = new ArrayList<>();
		int collectableGroveMana = village.peekCollectableGroveMana();
		int placed = state.getBuildingPositions().size();
		int townHallLevel = village.levelOf(BuildingType.TOWN_HALL);
		int assignedWorkers = 0;
		for (Worker worker : state.getWorkers().values())
		{
			if (worker.getAssignment() != null)
			{
				assignedWorkers++;
			}
		}
		int population = state.getWorkers().size();
		int availableWorkers = population - assignedWorkers;
		resources.add(new VillageResourceView(
			VillageResourceView.Kind.MANA,
			"Mana",
			village.hasUnlimitedMana() ? "\u221e TEST" : format(state.getMana()),
			"Spendable village mana. It is consumed by building and upgrade orders."));
		resources.add(new VillageResourceView(
			VillageResourceView.Kind.MANA_CAPACITY,
			"Capacity",
			format(village.getManaCapacity()) + " mana max",
			"Capacity is the village mana storage limit. Town Hall and Mana Well levels increase it."));
		resources.add(new VillageResourceView(
			VillageResourceView.Kind.GROVE_MANA,
			"Grove harvest",
			format(collectableGroveMana) + " ready",
			"Grove is passive production: mana grows over time and is ready to collect when the blue icon appears."));
		resources.add(new VillageResourceView(
			VillageResourceView.Kind.DAILY_XP_MANA,
			"Daily XP mana",
			village.hasUnlimitedMana()
				? "Test mode"
				: format(state.getManaEarnedToday()) + " / " + format(ManaLedger.DAILY_MANA_CAP),
			"Mana earned today from XP conversion. Test mode does not alter XP tracking."));
		resources.add(new VillageResourceView(
			VillageResourceView.Kind.BUILDER,
			"Builder",
			village.getConstructionJob() == null ? "1 / 1 free" : "0 / 1 busy",
			"Builder availability. A busy builder blocks new timed construction orders."));
		resources.add(new VillageResourceView(
			VillageResourceView.Kind.VILLAGE,
			"Village",
			"TH L" + townHallLevel + " - " + placed + "/" + BuildingType.values().length,
			"Town Hall level and placed structures. "
				+ catalog.getDisplayName(BuildingType.TOWN_HALL)
				+ " unlocks higher requirements."));
		resources.add(new VillageResourceView(
			VillageResourceView.Kind.WORKERS,
			"Population",
			population + " total - " + availableWorkers + " free",
			"Population: " + population + " / " + population
				+ ". Available: " + availableWorkers + ". Assigned: " + assignedWorkers + "."));
		resources.add(new VillageResourceView(
			VillageResourceView.Kind.RESOURCES,
			"Resources",
			resourceSummary(state),
			"General village storage for gathered OSRS-style resources."));
		return resources;
	}

	private static String resourceSummary(VillageState state)
	{
		StringBuilder builder = new StringBuilder();
		for (ResourceType type : ResourceType.values())
		{
			long amount = state.getResources().get(type);
			if (amount <= 0)
			{
				continue;
			}
			if (builder.length() > 0)
			{
				builder.append(", ");
			}
			builder.append(type.getDisplayName()).append(' ').append(format(amount));
		}
		return builder.length() == 0 ? "None stored" : builder.toString();
	}

	private static BuildingView toBuildingView(
		BuildingType type,
		int currentLevel,
		int maxLevel,
		long mana,
		boolean unlimitedMana,
		BuildingCatalog catalog,
		UpgradeResult preview)
	{
		String levelText = currentLevel == 0
			? "Not built - Max " + maxLevel
			: "Level " + currentLevel + " / " + maxLevel;
		String statusText;
		String actionText;
		boolean actionEnabled;
		switch (preview.getStatus())
		{
			case SUCCESS:
				String verb = currentLevel == 0 ? "Build" : "Upgrade";
				statusText = unlimitedMana
					? "Testing mode - no mana spent"
					: "Ready for " + format(preview.getRequiredMana()) + " mana";
				actionText = unlimitedMana
					? verb + " (free)"
					: verb + " (" + format(preview.getRequiredMana()) + ")";
				actionEnabled = true;
				break;
			case LOCKED:
				statusText = "Requires Town Hall level " + preview.getRequiredTownHallLevel();
				actionText = "Locked";
				actionEnabled = false;
				break;
			case INSUFFICIENT_MANA:
				long missing = preview.getRequiredMana() - mana;
				statusText = "Requires " + format(preview.getRequiredMana())
					+ " mana (" + format(missing) + " missing)";
				actionText = "Need " + format(preview.getRequiredMana()) + " mana";
				actionEnabled = false;
				break;
			case MAX_LEVEL:
				statusText = "Maximum level reached";
				actionText = "Max level";
				actionEnabled = false;
				break;
			case BUILDER_BUSY:
				statusText = "Builder is busy with another construction";
				actionText = "Builder busy";
				actionEnabled = false;
				break;
			default:
				throw new IllegalStateException("unsupported upgrade status: " + preview.getStatus());
		}

		return new BuildingView(
			type,
			currentLevel,
			catalog.getDisplayName(type),
			catalog.getDescription(type),
			catalog.getCategory(type),
			catalog.getFootprint(type),
			unlimitedMana
				? TEST_BUILD_DURATION_SECONDS
				: catalog.getBuildDurationSeconds(type, Math.min(maxLevel, currentLevel + 1)),
			currentLevel < maxLevel
				? catalog.getCostForTargetLevel(type, currentLevel + 1) : 0,
			currentLevel < maxLevel
				? catalog.getRequiredTownHallLevel(type, currentLevel + 1) : 0,
			levelText,
			statusText,
			actionText,
			actionEnabled,
			preview.getStatus());
	}

	private static String format(long value)
	{
		return NumberFormat.getIntegerInstance(Locale.US).format(value);
	}

	public String getManaText()
	{
		return manaText;
	}

	public String getDailyProgressText()
	{
		return dailyProgressText;
	}

	public long getMana()
	{
		return mana;
	}

	public boolean hasUnlimitedMana()
	{
		return unlimitedMana;
	}

	public ConstructionJob getConstructionJob()
	{
		return constructionJob;
	}

	public int getManaCapacity()
	{
		return manaCapacity;
	}

	public int getCollectableGroveMana()
	{
		return collectableGroveMana;
	}

	public Map<BuildingType, GridPoint> getBuildingPositions()
	{
		return buildingPositions;
	}

	public List<VillageResourceView> getResources()
	{
		return resources;
	}

	public List<BuildingView> getBuildings()
	{
		return buildings;
	}

	public BuildingView getBuilding(BuildingType type)
	{
		return buildingsByType.get(type);
	}

	public static final class BuildingView
	{
		private final BuildingType type;
		private final int currentLevel;
		private final String name;
		private final String description;
		private final BuildingCategory category;
		private final Footprint footprint;
		private final int buildDurationSeconds;
		private final int nextCost;
		private final int requiredTownHallLevel;
		private final String levelText;
		private final String statusText;
		private final String actionText;
		private final boolean actionEnabled;
		private final UpgradeResult.Status status;

		private BuildingView(
			BuildingType type,
			int currentLevel,
			String name,
			String description,
			BuildingCategory category,
			Footprint footprint,
			int buildDurationSeconds,
			int nextCost,
			int requiredTownHallLevel,
			String levelText,
			String statusText,
			String actionText,
			boolean actionEnabled,
			UpgradeResult.Status status)
		{
			this.type = type;
			this.currentLevel = currentLevel;
			this.name = name;
			this.description = description;
			this.category = category;
			this.footprint = footprint;
			this.buildDurationSeconds = buildDurationSeconds;
			this.nextCost = nextCost;
			this.requiredTownHallLevel = requiredTownHallLevel;
			this.levelText = levelText;
			this.statusText = statusText;
			this.actionText = actionText;
			this.actionEnabled = actionEnabled;
			this.status = status;
		}

		public BuildingType getType()
		{
			return type;
		}

		public int getCurrentLevel()
		{
			return currentLevel;
		}

		public String getName()
		{
			return name;
		}

		public String getDescription()
		{
			return description;
		}

		public BuildingCategory getCategory()
		{
			return category;
		}

		public Footprint getFootprint()
		{
			return footprint;
		}

		public int getBuildDurationSeconds()
		{
			return buildDurationSeconds;
		}

		public int getNextCost()
		{
			return nextCost;
		}

		public int getRequiredTownHallLevel()
		{
			return requiredTownHallLevel;
		}

		public String getLevelText()
		{
			return levelText;
		}

		public String getStatusText()
		{
			return statusText;
		}

		public String getActionText()
		{
			return actionText;
		}

		public boolean isActionEnabled()
		{
			return actionEnabled;
		}

		public UpgradeResult.Status getStatus()
		{
			return status;
		}
	}
}
