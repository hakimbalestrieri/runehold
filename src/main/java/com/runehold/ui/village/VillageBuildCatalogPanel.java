package com.runehold.ui.village;

import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.BuildingCategory;
import com.runehold.domain.BuildingType;
import com.runehold.ui.RuneholdAssets;
import com.runehold.ui.RuneholdViewModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

final class VillageBuildCatalogPanel extends JPanel
{
	private final BuildingCatalog catalog;
	private final RuneholdAssets assets;
	private final Consumer<BuildingType> placeHandler;
	private final Consumer<BuildingType> upgradeHandler;
	private final Runnable closeHandler;
	private final VillageSpriteAtlas catalogSprites = new VillageSpriteAtlas();
	private final JTabbedPane tabs = new JTabbedPane();
	private RuneholdViewModel viewModel;

	VillageBuildCatalogPanel(
		BuildingCatalog catalog,
		RuneholdAssets assets,
		Consumer<BuildingType> placeHandler,
		Consumer<BuildingType> upgradeHandler,
		Runnable closeHandler)
	{
		super(new BorderLayout());
		this.catalog = Objects.requireNonNull(catalog, "catalog");
		this.assets = Objects.requireNonNull(assets, "assets");
		this.placeHandler = Objects.requireNonNull(placeHandler, "placeHandler");
		this.upgradeHandler = Objects.requireNonNull(upgradeHandler, "upgradeHandler");
		this.closeHandler = Objects.requireNonNull(closeHandler, "closeHandler");
		setBackground(VillageTheme.BACKGROUND);
		setBorder(VillageTheme.stoneBorder(5));
		setPreferredSize(new Dimension(560, 10));
		configureTabs();
	}

	void refresh(RuneholdViewModel updatedViewModel)
	{
		viewModel = Objects.requireNonNull(updatedViewModel, "updatedViewModel");
		int selectedIndex = Math.max(0, tabs.getSelectedIndex());
		removeAll();
		add(heading(), BorderLayout.NORTH);
		rebuildTabs();
		add(tabs, BorderLayout.CENTER);
		if (tabs.getTabCount() > selectedIndex)
		{
			tabs.setSelectedIndex(selectedIndex);
		}
		revalidate();
		repaint();
	}

	private void configureTabs()
	{
		tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		tabs.setFont(assets.boldFont(16f));
		tabs.setBackground(VillageTheme.STONE_DARK);
		tabs.setForeground(VillageTheme.TEXT);
	}

	private JPanel heading()
	{
		JPanel heading = new JPanel(new BorderLayout());
		heading.setOpaque(false);
		heading.setBorder(BorderFactory.createEmptyBorder(3, 2, 9, 2));
		heading.add(label("BUILD CATALOGUE", 22f, VillageTheme.ORANGE, true), BorderLayout.WEST);
		JButton close = button("X", "Close catalogue");
		close.addActionListener(event -> closeHandler.run());
		heading.add(close, BorderLayout.EAST);
		return heading;
	}

	private void rebuildTabs()
	{
		tabs.removeAll();
		Map<BuildingCategory, JPanel> categoryPanels = new EnumMap<>(BuildingCategory.class);
		for (BuildingCategory category : BuildingCategory.values())
		{
			JPanel list = new JPanel();
			list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
			list.setBackground(VillageTheme.BACKGROUND);
			list.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
			categoryPanels.put(category, list);
			JScrollPane scroll = new JScrollPane(list);
			scroll.setBorder(null);
			scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			scroll.getVerticalScrollBar().setUnitIncrement(18);
			tabs.addTab(tabName(category), scroll);
		}

		for (RuneholdViewModel.BuildingView building : viewModel.getBuildings())
		{
			JPanel list = categoryPanels.get(building.getCategory());
			list.add(createCatalogRow(building));
			list.add(Box.createVerticalStrut(6));
		}
	}

	private JPanel createCatalogRow(RuneholdViewModel.BuildingView building)
	{
		JPanel row = new JPanel(new BorderLayout(10, 4));
		row.setBackground(VillageTheme.PANEL);
		row.setBorder(VillageTheme.stoneBorder(7));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 274));

		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(128, 116));
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		icon.setVerticalAlignment(SwingConstants.CENTER);
		icon.setIcon(new ImageIcon(catalogSprites.get(
			building.getType(),
			Math.max(1, building.getCurrentLevel()),
			catalogIconWidth(building.getType()))));
		row.add(icon, BorderLayout.WEST);

		JPanel text = new JPanel();
		text.setOpaque(false);
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.add(label(building.getName(), 20f, VillageTheme.ORANGE, true));
		text.add(label("<html><body style='width:360px'>"
			+ building.getDescription() + "</body></html>", 15f, VillageTheme.MUTED, false));
		text.add(label("Size: " + building.getFootprint().getWidth() + "x"
			+ building.getFootprint().getHeight() + " tiles", 15f, VillageTheme.TEXT, false));
		text.add(label("Build time: " + building.getBuildDurationSeconds() + " seconds",
			15f, VillageTheme.TEXT, false));
		for (String requirement : requirementLines(building))
		{
			text.add(label(requirement, 15f, VillageTheme.TEXT, false));
		}
		text.add(label(building.getCurrentLevel() > 0 ? "Owned: 1 built / 1 allowed"
			: "Owned: 0 built / 1 allowed", 15f, VillageTheme.MUTED, false));
		row.add(text, BorderLayout.CENTER);

		JButton action = actionButton(building);
		row.add(action, BorderLayout.SOUTH);
		return row;
	}

	private JButton actionButton(RuneholdViewModel.BuildingView building)
	{
		if (building.getCurrentLevel() <= 0)
		{
			JButton action = button("Place", building.getStatusText());
			action.setEnabled(building.isActionEnabled());
			action.addActionListener(event -> placeHandler.accept(building.getType()));
			return action;
		}
		if (building.getCurrentLevel() >= catalog.getMaxLevel(building.getType()))
		{
			JButton action = button("Max level", "This building is fully upgraded.");
			action.setEnabled(false);
			return action;
		}
		JButton action = button(building.getActionText(), building.getStatusText());
		action.setEnabled(building.isActionEnabled());
		action.addActionListener(event -> upgradeHandler.accept(building.getType()));
		return action;
	}

	JButton actionButtonForTest(RuneholdViewModel.BuildingView building)
	{
		return actionButton(building);
	}

	private String[] requirementLines(RuneholdViewModel.BuildingView building)
	{
		int buildCost = catalog.getCostForTargetLevel(building.getType(), 1);
		int townHallLevel = catalog.getRequiredTownHallLevel(building.getType(), 1);
		String mana = viewModel.hasUnlimitedMana() && buildCost > 0
			? "Required mana: " + format(buildCost) + " (ignored in test mode)"
			: "Required mana: " + format(buildCost);
		String townHall = townHallLevel <= 0
			? "Required Town Hall: none"
			: "Required Town Hall: level " + townHallLevel;
		String nextTownHall = building.getRequiredTownHallLevel() <= 0
			? "none"
			: "level " + building.getRequiredTownHallLevel();
		if (building.getCurrentLevel() >= catalog.getMaxLevel(building.getType()))
		{
			List<String> lines = new ArrayList<>();
			lines.add(mana);
			lines.add(townHall);
			addProductionLines(lines, building);
			lines.add("Next upgrade: maximum level reached");
			return lines.toArray(new String[0]);
		}
		if (building.getCurrentLevel() > 0)
		{
			List<String> lines = new ArrayList<>();
			lines.add(mana);
			lines.add(townHall);
			addProductionLines(lines, building);
			lines.add("Next upgrade mana: " + format(building.getNextCost()));
			lines.add("Next upgrade Town Hall: " + nextTownHall);
			return lines.toArray(new String[0]);
		}
		List<String> lines = new ArrayList<>();
		lines.add(mana);
		lines.add(townHall);
		addProductionLines(lines, building);
		lines.add("Availability: " + building.getStatusText());
		return lines.toArray(new String[0]);
	}

	private void addProductionLines(
		List<String> lines,
		RuneholdViewModel.BuildingView building)
	{
		if (building.getType() != BuildingType.MANA_GROVE)
		{
			return;
		}
		int currentLevel = building.getCurrentLevel();
		int nextLevel = Math.min(catalog.getMaxLevel(building.getType()), currentLevel + 1);
		int currentProduction = catalog.getManaProductionPerMinuteForLevel(
			building.getType(),
			currentLevel);
		int nextProduction = catalog.getManaProductionPerMinuteForLevel(
			building.getType(),
			nextLevel);
		int currentStorage = catalog.getPassiveStorageForLevel(
			building.getType(),
			currentLevel);
		int nextStorage = catalog.getPassiveStorageForLevel(
			building.getType(),
			nextLevel);
		lines.add("Production: " + currentProduction + " -> "
			+ nextProduction + " mana / min");
		lines.add("Grove storage: " + currentStorage + " -> "
			+ nextStorage + " mana");
	}

	private static String tabName(BuildingCategory category)
	{
		switch (category)
		{
			case PRODUCTION:
				return "Production";
			case DECORATION:
				return "Decoration";
			default:
				return category.getDisplayName();
		}
	}

	private static int catalogIconWidth(BuildingType type)
	{
		switch (type)
		{
			case RUNE_BANNER:
				return 50;
			case MANA_WELL:
				return 76;
			case MANA_GROVE:
				return 96;
			default:
				return 104;
		}
	}

	private JButton button(String text, String tooltip)
	{
		JButton button = new JButton(text);
		button.setFont(assets.boldFont(16f));
		button.setToolTipText(tooltip);
		VillageTheme.styleButton(button);
		return button;
	}

	private JLabel label(String text, float size, java.awt.Color color, boolean bold)
	{
		JLabel label = new JLabel(text);
		label.setFont(bold ? assets.boldFont(size) : assets.regularFont(size));
		label.setForeground(color);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	private static String format(long value)
	{
		return NumberFormat.getIntegerInstance(Locale.US).format(value);
	}
}
