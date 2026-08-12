package com.runehold.ui;

import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.BuildingCategory;
import com.runehold.domain.BuildingType;
import com.runehold.domain.ConstructionJob;
import com.runehold.domain.GatheringSiteDefinition;
import com.runehold.domain.GatheringSiteState;
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
	private final List<GatheringSiteView> gatheringSites;
	private final Map<BuildingType, GatheringSiteView> gatheringSitesByType;
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
		List<GatheringSiteView> gatheringSites,
		Map<BuildingType, GridPoint> buildingPositions)
	{
		this.gatheringSites = Collections.unmodifiableList(new ArrayList<>(gatheringSites));
		this.gatheringSitesByType = new EnumMap<>(BuildingType.class);
		for (GatheringSiteView site : gatheringSites)
		{
			gatheringSitesByType.put(site.getType(), site);
		}
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

		// Every building type gets a view, gathering sites included: the village window
		// places and upgrades them through the same build catalogue as any structure.
		// The side panel is what chooses to render them as gathering rows instead.
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
			gatheringSitesFor(state, village, catalog),
			state.getBuildingPositions());
	}

	private static List<GatheringSiteView> gatheringSitesFor(
		VillageState state,
		Village village,
		BuildingCatalog catalog)
	{
		List<String> idleWorkerIds = new ArrayList<>();
		for (String workerId : sortedWorkerIds(state))
		{
			if (state.getWorker(workerId).getAssignment() == null)
			{
				idleWorkerIds.add(workerId);
			}
		}
		String nextIdleWorkerId = idleWorkerIds.isEmpty() ? null : idleWorkerIds.get(0);

		List<GatheringSiteView> views = new ArrayList<>();
		for (BuildingType type : BuildingType.values())
		{
			if (type.isGatheringSite())
			{
				views.add(toGatheringSiteView(state, village, catalog, type, nextIdleWorkerId));
			}
		}
		return views;
	}

	private static GatheringSiteView toGatheringSiteView(
		VillageState state,
		Village village,
		BuildingCatalog catalog,
		BuildingType type,
		String nextIdleWorkerId)
	{
		GatheringSiteDefinition definition = village.gatheringCatalog().get(type);
		GatheringSiteState site = state.getGatheringSite(type);
		int level = village.levelOf(type);
		List<String> assigned = site == null
			? Collections.<String>emptyList()
			: site.getAssignedWorkerIds();
		int workers = assigned.size();
		int maxWorkers = definition.maxWorkers(level);
		int stored = village.peekStoredResource(type);
		int capacity = definition.storageCapacity(level);
		int ratePerMinute = definition.productionPerMinute(level, workers);
		String resourceName = definition.getResourceType().getDisplayName();
		int maxLevel = catalog.getMaxLevel(type);
		UpgradeResult preview = village.previewUpgrade(type);
		// A site that is not built has no workers, no storage and no production; its row
		// offers the build action instead.
		boolean built = level > 0;

		String levelText = built
			? "Level " + level + " / " + maxLevel
			: "Not built";
		String workersText = built
			? workers + " / " + maxWorkers + " workers"
			: "Place it on the map";
		String storageText = built
			? format(stored) + " / " + format(capacity) + " " + resourceName.toLowerCase(Locale.US)
			: "Produces " + resourceName.toLowerCase(Locale.US);
		String rateText = ratePerMinute > 0
			? format(ratePerMinute) + " " + resourceName.toLowerCase(Locale.US) + " / min"
			: "Not producing";

		GatheringSiteView.Status status;
		String statusText;
		// The side panel is roughly 195px wide inside the row border, so status text is
		// kept short enough to render without truncation at the panel's font size.
		if (!built)
		{
			status = GatheringSiteView.Status.NOT_BUILT;
			statusText = buildStatusText(catalog, type, preview, village.hasUnlimitedMana());
		}
		else if (site != null && "No accessible path".equals(site.getBlockedReason()))
		{
			status = GatheringSiteView.Status.BLOCKED;
			statusText = "No accessible path";
		}
		else if (workers == 0)
		{
			status = GatheringSiteView.Status.IDLE;
			statusText = "No workers assigned";
		}
		else if (stored >= capacity)
		{
			status = GatheringSiteView.Status.FULL;
			statusText = "Storage full - collect";
		}
		else if (site != null && site.getBlockedReason() != null)
		{
			status = GatheringSiteView.Status.BLOCKED;
			statusText = site.getBlockedReason();
		}
		else
		{
			status = GatheringSiteView.Status.PRODUCING;
			statusText = "Producing " + rateText;
		}

		boolean workerSlotFree = built && workers < maxWorkers;
		boolean assignEnabled = workerSlotFree && nextIdleWorkerId != null;
		String assignActionText;
		if (!built)
		{
			assignActionText = "Not built";
		}
		else if (!workerSlotFree)
		{
			assignActionText = "Site full";
		}
		else if (nextIdleWorkerId == null)
		{
			assignActionText = "No free worker";
		}
		else
		{
			assignActionText = "Assign worker";
		}

		String releaseWorkerId = workers == 0 ? null : assigned.get(workers - 1);
		boolean collectEnabled = stored > 0;

		return new GatheringSiteView(
			type,
			catalog.getDisplayName(type),
			resourceName,
			level,
			workers,
			maxWorkers,
			stored,
			capacity,
			ratePerMinute,
			levelText,
			workersText,
			storageText,
			rateText,
			statusText,
			status,
			assignEnabled ? nextIdleWorkerId : null,
			assignEnabled,
			assignActionText,
			releaseWorkerId,
			collectEnabled,
			collectEnabled
				? "Collect " + format(stored) + " " + resourceName.toLowerCase(Locale.US)
				: "Nothing to collect",
			built,
			preview.isSuccess(),
			buildActionText(catalog, type, preview, village.hasUnlimitedMana()));
	}

	private static String buildStatusText(
		BuildingCatalog catalog,
		BuildingType type,
		UpgradeResult preview,
		boolean unlimitedMana)
	{
		switch (preview.getStatus())
		{
			case SUCCESS:
				return unlimitedMana
					? "Ready to place - free"
					: "Ready to place - " + format(preview.getRequiredMana()) + " mana";
			case LOCKED:
				return "Needs Town Hall " + preview.getRequiredTownHallLevel();
			case INSUFFICIENT_MANA:
				return "Needs " + format(preview.getRequiredMana()) + " mana";
			case BUILDER_BUSY:
				return "Builder is busy";
			default:
				return catalog.getDescription(type);
		}
	}

	private static String buildActionText(
		BuildingCatalog catalog,
		BuildingType type,
		UpgradeResult preview,
		boolean unlimitedMana)
	{
		switch (preview.getStatus())
		{
			case SUCCESS:
				return unlimitedMana ? "Place (free)" : "Place (" + format(preview.getRequiredMana()) + ")";
			case LOCKED:
				return "Locked";
			case INSUFFICIENT_MANA:
				return "Need " + format(preview.getRequiredMana()) + " mana";
			case BUILDER_BUSY:
				return "Builder busy";
			case MAX_LEVEL:
				return "Max level";
			default:
				return catalog.getDisplayName(type);
		}
	}

	private static List<String> sortedWorkerIds(VillageState state)
	{
		List<String> workerIds = new ArrayList<>(state.getWorkers().keySet());
		Collections.sort(workerIds, RuneholdViewModel::compareWorkerIds);
		return workerIds;
	}

	/**
	 * Orders {@code worker-2} before {@code worker-10} so the assign button always picks
	 * the same villager for the same state.
	 */
	private static int compareWorkerIds(String left, String right)
	{
		Integer leftIndex = trailingIndex(left);
		Integer rightIndex = trailingIndex(right);
		if (leftIndex != null && rightIndex != null && !leftIndex.equals(rightIndex))
		{
			return leftIndex.compareTo(rightIndex);
		}
		return left.compareTo(right);
	}

	private static Integer trailingIndex(String workerId)
	{
		int separator = workerId.lastIndexOf('-');
		if (separator < 0 || separator == workerId.length() - 1)
		{
			return null;
		}
		try
		{
			return Integer.valueOf(workerId.substring(separator + 1));
		}
		catch (NumberFormatException ex)
		{
			return null;
		}
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

	/**
	 * Buildings that are not gathering sites. The side panel lists these as building
	 * rows and renders the sites as gathering rows instead, so neither appears twice.
	 */
	public List<BuildingView> getStructures()
	{
		List<BuildingView> structures = new ArrayList<>();
		for (BuildingView building : buildings)
		{
			if (!building.getType().isGatheringSite())
			{
				structures.add(building);
			}
		}
		return Collections.unmodifiableList(structures);
	}

	public BuildingView getBuilding(BuildingType type)
	{
		return buildingsByType.get(type);
	}

	public List<GatheringSiteView> getGatheringSites()
	{
		return gatheringSites;
	}

	public GatheringSiteView getGatheringSite(BuildingType type)
	{
		return gatheringSitesByType.get(type);
	}

	public static final class GatheringSiteView
	{
		public enum Status
		{
			PRODUCING,
			IDLE,
			FULL,
			BLOCKED,
			NOT_BUILT
		}

		private final BuildingType type;
		private final String name;
		private final String resourceName;
		private final int level;
		private final int assignedWorkers;
		private final int maxWorkers;
		private final int stored;
		private final int storageCapacity;
		private final int ratePerMinute;
		private final String levelText;
		private final String workersText;
		private final String storageText;
		private final String rateText;
		private final String statusText;
		private final Status status;
		private final String assignableWorkerId;
		private final boolean assignEnabled;
		private final String assignActionText;
		private final String releasableWorkerId;
		private final boolean collectEnabled;
		private final String collectActionText;
		private final boolean built;
		private final boolean buildEnabled;
		private final String buildActionText;

		private GatheringSiteView(
			BuildingType type,
			String name,
			String resourceName,
			int level,
			int assignedWorkers,
			int maxWorkers,
			int stored,
			int storageCapacity,
			int ratePerMinute,
			String levelText,
			String workersText,
			String storageText,
			String rateText,
			String statusText,
			Status status,
			String assignableWorkerId,
			boolean assignEnabled,
			String assignActionText,
			String releasableWorkerId,
			boolean collectEnabled,
			String collectActionText,
			boolean built,
			boolean buildEnabled,
			String buildActionText)
		{
			this.built = built;
			this.buildEnabled = buildEnabled;
			this.buildActionText = buildActionText;
			this.type = type;
			this.name = name;
			this.resourceName = resourceName;
			this.level = level;
			this.assignedWorkers = assignedWorkers;
			this.maxWorkers = maxWorkers;
			this.stored = stored;
			this.storageCapacity = storageCapacity;
			this.ratePerMinute = ratePerMinute;
			this.levelText = levelText;
			this.workersText = workersText;
			this.storageText = storageText;
			this.rateText = rateText;
			this.statusText = statusText;
			this.status = status;
			this.assignableWorkerId = assignableWorkerId;
			this.assignEnabled = assignEnabled;
			this.assignActionText = assignActionText;
			this.releasableWorkerId = releasableWorkerId;
			this.collectEnabled = collectEnabled;
			this.collectActionText = collectActionText;
		}

		public BuildingType getType()
		{
			return type;
		}

		public String getName()
		{
			return name;
		}

		public String getResourceName()
		{
			return resourceName;
		}

		public int getLevel()
		{
			return level;
		}

		public int getAssignedWorkers()
		{
			return assignedWorkers;
		}

		public int getMaxWorkers()
		{
			return maxWorkers;
		}

		public int getStored()
		{
			return stored;
		}

		public int getStorageCapacity()
		{
			return storageCapacity;
		}

		public int getRatePerMinute()
		{
			return ratePerMinute;
		}

		public String getLevelText()
		{
			return levelText;
		}

		public String getWorkersText()
		{
			return workersText;
		}

		public String getStorageText()
		{
			return storageText;
		}

		public String getRateText()
		{
			return rateText;
		}

		public String getStatusText()
		{
			return statusText;
		}

		public Status getStatus()
		{
			return status;
		}

		/**
		 * The villager the assign action will send to this site, or {@code null} when no
		 * assignment is possible.
		 */
		public String getAssignableWorkerId()
		{
			return assignableWorkerId;
		}

		public boolean isAssignEnabled()
		{
			return assignEnabled;
		}

		public String getAssignActionText()
		{
			return assignActionText;
		}

		/**
		 * The villager the release action will recall, or {@code null} when the site has
		 * no assigned worker.
		 */
		public String getReleasableWorkerId()
		{
			return releasableWorkerId;
		}

		public boolean isReleaseEnabled()
		{
			return releasableWorkerId != null;
		}

		public boolean isCollectEnabled()
		{
			return collectEnabled;
		}

		public String getCollectActionText()
		{
			return collectActionText;
		}

		/**
		 * A site that has not been placed on the map yet offers a build action instead of
		 * worker and collection actions.
		 */
		public boolean isBuilt()
		{
			return built;
		}

		public boolean isBuildEnabled()
		{
			return buildEnabled;
		}

		public String getBuildActionText()
		{
			return buildActionText;
		}
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
