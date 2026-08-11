package com.runehold.ui;

import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.BuildingType;
import com.runehold.domain.ManaLedger;
import com.runehold.domain.UpgradeResult;
import com.runehold.domain.Village;
import com.runehold.domain.VillageState;
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
	private final String manaText;
	private final String dailyProgressText;
	private final List<BuildingView> buildings;
	private final Map<BuildingType, BuildingView> buildingsByType;

	private RuneholdViewModel(
		String manaText,
		String dailyProgressText,
		List<BuildingView> buildings)
	{
		this.manaText = manaText;
		this.dailyProgressText = dailyProgressText;
		this.buildings = Collections.unmodifiableList(new ArrayList<>(buildings));
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
				catalog,
				preview));
		}

		return new RuneholdViewModel(
			format(state.getMana()) + " mana",
			"Today: " + format(state.getManaEarnedToday()) + " / "
				+ format(ManaLedger.DAILY_MANA_CAP),
			buildings);
	}

	private static BuildingView toBuildingView(
		BuildingType type,
		int currentLevel,
		int maxLevel,
		long mana,
		BuildingCatalog catalog,
		UpgradeResult preview)
	{
		String levelText = currentLevel == 0
			? "Not built · Max " + maxLevel
			: "Level " + currentLevel + " / " + maxLevel;
		String statusText;
		String actionText;
		boolean actionEnabled;

		switch (preview.getStatus())
		{
			case SUCCESS:
				String verb = currentLevel == 0 ? "Build" : "Upgrade";
				statusText = "Ready for " + format(preview.getRequiredMana()) + " mana";
				actionText = verb + " (" + format(preview.getRequiredMana()) + ")";
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
			default:
				throw new IllegalStateException("unsupported upgrade status: " + preview.getStatus());
		}

		return new BuildingView(
			type,
			catalog.getDisplayName(type),
			catalog.getDescription(type),
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
		private final String name;
		private final String description;
		private final String levelText;
		private final String statusText;
		private final String actionText;
		private final boolean actionEnabled;
		private final UpgradeResult.Status status;

		private BuildingView(
			BuildingType type,
			String name,
			String description,
			String levelText,
			String statusText,
			String actionText,
			boolean actionEnabled,
			UpgradeResult.Status status)
		{
			this.type = type;
			this.name = name;
			this.description = description;
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

		public String getName()
		{
			return name;
		}

		public String getDescription()
		{
			return description;
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
