package com.runehold.ui;

import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.BuildingType;
import com.runehold.domain.UpgradeResult;
import com.runehold.domain.Village;
import com.runehold.domain.VillageState;
import com.runehold.domain.WorkerState;
import com.runehold.domain.layout.GridPoint;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.runelite.api.gameval.ItemID;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RuneholdViewModelTest
{
	private VillageState state;
	private Village village;
	private BuildingCatalog catalog;

	@Before
	public void setUp()
	{
		state = VillageState.fresh(LocalDate.of(2026, 8, 11));
		catalog = new BuildingCatalog();
		village = new Village(state, catalog);
	}

	@Test
	public void freshVillageHasReadableHeaderAndCatalogRows()
	{
		RuneholdViewModel viewModel = RuneholdViewModel.from(state, village, catalog);

		assertEquals("250 mana", viewModel.getManaText());
		assertEquals("Today: 0 / 10,000", viewModel.getDailyProgressText());
		assertEquals(14, viewModel.getBuildings().size());
		assertEquals(6, viewModel.getStructures().size());
		assertEquals("Level 1 / 5", viewModel.getBuilding(BuildingType.TOWN_HALL).getLevelText());
		assertEquals("Not built - Max 5", viewModel.getBuilding(BuildingType.MANA_WELL).getLevelText());
	}

	@Test
	public void actionStatesComeFromDomainPreview()
	{
		RuneholdViewModel viewModel = RuneholdViewModel.from(state, village, catalog);

		RuneholdViewModel.BuildingView townHall = viewModel.getBuilding(BuildingType.TOWN_HALL);
		RuneholdViewModel.BuildingView manaWell = viewModel.getBuilding(BuildingType.MANA_WELL);
		RuneholdViewModel.BuildingView barracks = viewModel.getBuilding(BuildingType.BARRACKS);

		assertTrue(townHall.isActionEnabled());
		assertEquals("Upgrade (200)", townHall.getActionText());
		assertTrue(manaWell.isActionEnabled());
		assertEquals("Build (100)", manaWell.getActionText());
		assertFalse(barracks.isActionEnabled());
		assertEquals(UpgradeResult.Status.LOCKED, barracks.getStatus());
		assertEquals("Requires Town Hall level 2", barracks.getStatusText());
	}

	@Test
	public void insufficientManaExplainsTheShortfall()
	{
		village.upgrade(BuildingType.MANA_WELL);

		RuneholdViewModel.BuildingView townHall = RuneholdViewModel
			.from(state, village, catalog)
			.getBuilding(BuildingType.TOWN_HALL);

		assertFalse(townHall.isActionEnabled());
		assertEquals(UpgradeResult.Status.INSUFFICIENT_MANA, townHall.getStatus());
		assertEquals("Requires 200 mana (50 missing)", townHall.getStatusText());
		assertEquals("Need 200 mana", townHall.getActionText());
	}

	@Test
	public void testingModeShowsUnlimitedManaAndKeepsActionsAffordable()
	{
		village = new Village(state, catalog, true);
		assertTrue(village.upgrade(BuildingType.TOWN_HALL).isSuccess());

		RuneholdViewModel viewModel = RuneholdViewModel.from(state, village, catalog);

		assertEquals("unlimited mana - TEST", viewModel.getManaText());
		assertEquals("Testing mode - XP tracking unchanged", viewModel.getDailyProgressText());
		assertTrue(viewModel.getBuilding(BuildingType.BARRACKS).isActionEnabled());
		assertEquals("Build (free)",
			viewModel.getBuilding(BuildingType.BARRACKS).getActionText());
	}

	@Test
	public void villageResourcesExplainCapacityAndGroveHarvest()
	{
		village = new Village(state, catalog, true);

		RuneholdViewModel viewModel = RuneholdViewModel.from(state, village, catalog);

		assertEquals(8, viewModel.getResources().size());
		assertEquals("Capacity",
			viewModel.getResources().get(1).getTitle());
		assertTrue(viewModel.getResources().get(1).getValue().contains("mana max"));
		assertTrue(viewModel.getResources().get(1).getTooltip().contains("storage limit"));
		assertEquals("Grove harvest",
			viewModel.getResources().get(2).getTitle());
		assertTrue(viewModel.getResources().get(2).getTooltip().contains("passive production"));
	}

	@Test
	public void controllerPersistsOnlySuccessfulUpgrades()
	{
		AtomicInteger saveCount = new AtomicInteger();
		RuneholdController controller = new RuneholdController(
			state,
			village,
			catalog,
			ignored -> saveCount.incrementAndGet());

		UpgradeResult locked = controller.upgrade(BuildingType.BARRACKS);
		UpgradeResult built = controller.upgrade(BuildingType.MANA_WELL);

		assertFalse(locked.isSuccess());
		assertTrue(built.isSuccess());
		assertEquals(1, saveCount.get());
		assertEquals("150 mana", controller.getViewModel().getManaText());
	}

	@Test
	public void panelBuildsFourActionButtonsOnTheEventDispatchThread() throws Exception
	{
		RuneholdController controller = new RuneholdController(
			state,
			village,
			catalog,
			ignored -> { });
		AtomicReference<RuneholdPanel> panel = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() -> panel.set(new RuneholdPanel(
			controller.getViewModel(),
			ignored -> { },
			new RecordingRuneholdAssets())));

		// One village launcher, six building actions and one place action per gathering
		// site, since no site has been placed on the map yet.
		assertEquals(15, countButtons(panel.get()));
	}

	@Test
	public void panelExposesExplicitVillageLauncher() throws Exception
	{
		RuneholdController controller = new RuneholdController(
			state,
			village,
			catalog,
			ignored -> { });
		AtomicInteger openCount = new AtomicInteger();
		AtomicReference<RuneholdPanel> panel = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() ->
		{
			panel.set(new RuneholdPanel(
				controller.getViewModel(),
				ignored -> { },
				openCount::incrementAndGet,
				new RecordingRuneholdAssets()));
			findButton(panel.get(), "Open village").doClick();
		});

		assertEquals(1, openCount.get());
	}

	@Test
	public void runtimeBuildingIconsUseRecognizableOsrsItems()
	{
		assertEquals(ItemID.SKILLCAPE_CONSTRUCTION,
			RuneLiteRuneholdAssets.itemIdFor(BuildingType.TOWN_HALL));
		assertEquals(ItemID.WATERRUNE,
			RuneLiteRuneholdAssets.itemIdFor(BuildingType.MANA_WELL));
		assertEquals(ItemID.BRONZE_SWORD,
			RuneLiteRuneholdAssets.itemIdFor(BuildingType.BARRACKS));
		assertEquals(ItemID.HAMMER,
			RuneLiteRuneholdAssets.itemIdFor(BuildingType.WORKSHOP));
	}

	@Test
	public void panelAppliesOsrsFontsAndRuntimeIcons() throws Exception
	{
		RuneholdController controller = new RuneholdController(
			state,
			village,
			catalog,
			ignored -> { });
		RecordingRuneholdAssets assets = new RecordingRuneholdAssets();
		AtomicReference<RuneholdPanel> panel = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() -> panel.set(new RuneholdPanel(
			controller.getViewModel(),
			ignored -> { },
			assets)));

		JLabel title = findLabel(panel.get(), "RUNEHOLD");
		assertEquals(Font.MONOSPACED, title.getFont().getFamily());
		assertEquals(Font.BOLD, title.getFont().getStyle());
		assertEquals(1, assets.manaIconCount);
		assertEquals(6, assets.upgradeIconCount);
		for (BuildingType type : BuildingType.values())
		{
			Map<BuildingType, Integer> counts = type.isGatheringSite()
				? assets.gatheringIconCounts
				: assets.buildingIconCounts;
			assertEquals(Integer.valueOf(1), counts.get(type));
		}
	}

	@Test
	public void gatheringIconsUseCanonicalOsrsResourceSprites()
	{
		assertEquals(ItemID.IRON_ORE,
			RuneLiteRuneholdAssets.itemIdFor(BuildingType.MINE));
		assertEquals(ItemID.RAW_SHRIMP,
			RuneLiteRuneholdAssets.itemIdFor(BuildingType.FISHING_SPOT));
		assertEquals(ItemID.LOGS,
			RuneLiteRuneholdAssets.itemIdFor(BuildingType.WOODCUTTING_GROVE));
		assertEquals(ItemID.LIMESTONE,
			RuneLiteRuneholdAssets.itemIdFor(BuildingType.QUARRY));
		assertEquals(ItemID.POTATO,
			RuneLiteRuneholdAssets.itemIdFor(BuildingType.FARM));
		assertEquals(ItemID.GUAM_LEAF,
			RuneLiteRuneholdAssets.itemIdFor(BuildingType.HERB_PATCH));
		assertEquals(ItemID.CLAY,
			RuneLiteRuneholdAssets.itemIdFor(BuildingType.CLAY_PIT));
		assertEquals(ItemID.BLANKRUNE,
			RuneLiteRuneholdAssets.itemIdFor(BuildingType.RUNE_ESSENCE_SITE));
	}

	@Test
	public void panelUsesCompactOsrsStoneTheme() throws Exception
	{
		RuneholdController controller = new RuneholdController(
			state,
			village,
			catalog,
			ignored -> { });
		AtomicReference<RuneholdPanel> panel = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() -> panel.set(new RuneholdPanel(
			controller.getViewModel(),
			ignored -> { },
			new RecordingRuneholdAssets())));

		assertEquals(RuneholdTheme.BACKGROUND, panel.get().getBackground());
		for (BuildingRow row : findComponents(panel.get(), BuildingRow.class))
		{
			assertEquals(RuneholdTheme.PANEL, row.getBackground());
			assertTrue(row.getMaximumSize().height <= 146);
		}
		for (JButton button : findComponents(panel.get(), JButton.class))
		{
			assertTrue(button.getUI() instanceof RuneholdButtonUI);
			assertFalse(button.isContentAreaFilled());
			assertFalse(button.isBorderPainted());
		}
		for (JLabel label : findComponents(panel.get(), JLabel.class))
		{
			assertTrue(label.getUI() instanceof RuneholdLabelUI);
		}
	}

	@Test
	public void panelEmitsUpgradeRequestWithoutMutatingDomainOnTheEdt() throws Exception
	{
		RuneholdController controller = new RuneholdController(
			state,
			village,
			catalog,
			ignored -> { });
		AtomicReference<BuildingType> requested = new AtomicReference<>();
		AtomicInteger requestCount = new AtomicInteger();
		AtomicReference<RuneholdPanel> panel = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() ->
		{
			panel.set(new RuneholdPanel(controller.getViewModel(), type ->
			{
				requested.set(type);
				requestCount.incrementAndGet();
			}, new RecordingRuneholdAssets()));
			JButton buildButton = findButton(panel.get(), "Build (100)");
			buildButton.doClick();
			buildButton.doClick();
		});

		assertEquals(BuildingType.MANA_WELL, requested.get());
		assertEquals(1, requestCount.get());
		assertEquals(250L, state.getMana());
		assertEquals(0, village.levelOf(BuildingType.MANA_WELL));
	}

	@Test
	public void gatheringSiteViewsExplainIdleLockedAndProducingSites()
	{
		RuneholdViewModel viewModel = RuneholdViewModel.from(state, village, catalog);

		assertEquals(8, viewModel.getGatheringSites().size());
		// Gathering sites stay in getBuildings() so the village window can place them,
		// but must not also appear among the side panel's building rows.
		for (RuneholdViewModel.BuildingView building : viewModel.getStructures())
		{
			assertFalse(building.getType().isGatheringSite());
		}
		assertNotNull(viewModel.getBuilding(BuildingType.MINE));

		RuneholdViewModel.GatheringSiteView mine =
			viewModel.getGatheringSite(BuildingType.MINE);
		assertEquals(RuneholdViewModel.GatheringSiteView.Status.NOT_BUILT, mine.getStatus());
		assertEquals("Not built", mine.getLevelText());
		assertEquals("Place it on the map", mine.getWorkersText());
		assertEquals("Ready to place - 50 mana", mine.getStatusText());
		assertEquals("Place (50)", mine.getBuildActionText());
		assertTrue(mine.isBuildEnabled());
		assertFalse(mine.isBuilt());
		assertFalse(mine.isAssignEnabled());
		assertEquals("Not built", mine.getAssignActionText());

		RuneholdViewModel.GatheringSiteView essence =
			viewModel.getGatheringSite(BuildingType.RUNE_ESSENCE_SITE);
		assertEquals(RuneholdViewModel.GatheringSiteView.Status.NOT_BUILT, essence.getStatus());
		assertEquals("Needs Town Hall 3", essence.getStatusText());
		assertFalse(essence.isBuildEnabled());
		assertEquals("Locked", essence.getBuildActionText());
	}

	@Test
	public void aPlacedSiteWithNoWorkerReportsItselfIdle()
	{
		Village timed = new Village(state, catalog, true,
			Clock.fixed(Instant.parse("2026-08-12T09:00:00Z"), ZoneOffset.UTC));
		assertTrue(timed.build(BuildingType.MINE, new GridPoint(1, 1)).isSuccess());
		timed = new Village(state, catalog, true,
			Clock.fixed(Instant.parse("2026-08-12T09:00:05Z"), ZoneOffset.UTC));
		assertTrue(timed.completeConstructionIfReady());

		RuneholdViewModel.GatheringSiteView mine = RuneholdViewModel
			.from(state, timed, catalog)
			.getGatheringSite(BuildingType.MINE);

		assertTrue(mine.isBuilt());
		assertEquals(RuneholdViewModel.GatheringSiteView.Status.IDLE, mine.getStatus());
		assertEquals("No workers assigned", mine.getStatusText());
		assertEquals("0 / 1 workers", mine.getWorkersText());
		assertEquals("Level 1 / 4", mine.getLevelText());
		assertTrue(mine.isAssignEnabled());
		assertEquals("worker-1", mine.getAssignableWorkerId());
		assertFalse(mine.isCollectEnabled());
	}

	@Test
	public void gatheringSiteViewReportsProductionAndCollectableStorage()
	{
		Village timed = new Village(state, catalog, true,
			Clock.fixed(Instant.parse("2026-08-12T09:00:00Z"), ZoneOffset.UTC));
		assertTrue(timed.build(BuildingType.MINE, new GridPoint(1, 1)).isSuccess());
		timed = new Village(state, catalog, true,
			Clock.fixed(Instant.parse("2026-08-12T09:00:05Z"), ZoneOffset.UTC));
		assertTrue(timed.completeConstructionIfReady());
		assertTrue(timed.assignWorker("worker-1", BuildingType.MINE).isSuccess());

		Village later = new Village(state, catalog, true,
			Clock.fixed(Instant.parse("2026-08-12T09:10:05Z"), ZoneOffset.UTC));
		RuneholdViewModel viewModel = RuneholdViewModel.from(state, later, catalog);
		RuneholdViewModel.GatheringSiteView mine =
			viewModel.getGatheringSite(BuildingType.MINE);

		assertEquals("1 / 1 workers", mine.getWorkersText());
		assertEquals(30, mine.getStored());
		assertEquals("30 / 60 ore", mine.getStorageText());
		assertEquals(RuneholdViewModel.GatheringSiteView.Status.PRODUCING, mine.getStatus());
		assertEquals("Producing 3 ore / min", mine.getStatusText());
		assertTrue(mine.isCollectEnabled());
		assertEquals("Collect 30 ore", mine.getCollectActionText());
		assertEquals("worker-1", mine.getReleasableWorkerId());
		assertFalse(mine.isAssignEnabled());
		assertEquals("Site full", mine.getAssignActionText());
		assertEquals(0, state.getGatheringSite(BuildingType.MINE).getStoredAmount());
	}

	@Test
	public void gatheringSiteViewReportsFullLocalStorage()
	{
		Village timed = new Village(state, catalog, true,
			Clock.fixed(Instant.parse("2026-08-12T09:00:00Z"), ZoneOffset.UTC));
		assertTrue(timed.build(BuildingType.MINE, new GridPoint(1, 1)).isSuccess());
		timed = new Village(state, catalog, true,
			Clock.fixed(Instant.parse("2026-08-12T09:00:05Z"), ZoneOffset.UTC));
		assertTrue(timed.completeConstructionIfReady());
		assertTrue(timed.assignWorker("worker-1", BuildingType.MINE).isSuccess());

		Village later = new Village(state, catalog, true,
			Clock.fixed(Instant.parse("2026-08-12T10:00:05Z"), ZoneOffset.UTC));
		RuneholdViewModel.GatheringSiteView mine = RuneholdViewModel
			.from(state, later, catalog)
			.getGatheringSite(BuildingType.MINE);

		assertEquals(60, mine.getStored());
		assertEquals(RuneholdViewModel.GatheringSiteView.Status.FULL, mine.getStatus());
		assertEquals("Storage full - collect", mine.getStatusText());
		assertTrue(mine.isCollectEnabled());
	}

	@Test
	public void anUnplacedSiteOffersPlacementInsteadOfWorkerActions() throws Exception
	{
		RuneholdController controller = new RuneholdController(
			state,
			village,
			catalog,
			ignored -> { });
		AtomicReference<BuildingType> requested = new AtomicReference<>();
		AtomicReference<RuneholdPanel> panel = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() ->
		{
			panel.set(new RuneholdPanel(
				controller.getViewModel(),
				requested::set,
				() -> { },
				new RecordingRuneholdAssets()));
			GatheringRow mineRow = findComponents(panel.get(), GatheringRow.class).get(0);
			assertNull(findButton(mineRow, "+"));
			findButton(mineRow, "Place (50)").doClick();
		});

		assertEquals(BuildingType.MINE, requested.get());
		assertEquals(0, village.levelOf(BuildingType.MINE));
	}

	@Test
	public void panelInvokesReleaseAndCollectWhenTheyAreActuallyAvailable() throws Exception
	{
		Village timed = new Village(state, catalog, true,
			Clock.fixed(Instant.parse("2026-08-12T09:00:00Z"), ZoneOffset.UTC));
		assertTrue(timed.build(BuildingType.MINE, new GridPoint(1, 1)).isSuccess());
		timed = new Village(state, catalog, true,
			Clock.fixed(Instant.parse("2026-08-12T09:00:05Z"), ZoneOffset.UTC));
		assertTrue(timed.completeConstructionIfReady());
		assertTrue(timed.assignWorker("worker-1", BuildingType.MINE).isSuccess());
		Village later = new Village(state, catalog, true,
			Clock.fixed(Instant.parse("2026-08-12T09:10:05Z"), ZoneOffset.UTC));

		AtomicReference<String> releasedWorker = new AtomicReference<>();
		AtomicReference<BuildingType> collectedSite = new AtomicReference<>();
		AtomicReference<RuneholdPanel> panel = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() ->
		{
			panel.set(new RuneholdPanel(
				RuneholdViewModel.from(state, later, catalog),
				ignored -> { },
				() -> { },
				new RecordingGatheringCommands(releasedWorker, collectedSite),
				new RecordingRuneholdAssets()));
			GatheringRow mineRow = findComponents(panel.get(), GatheringRow.class).get(0);
			findButton(mineRow, "-").doClick();
			findButton(mineRow, "Collect 30 ore").doClick();
		});

		assertEquals("worker-1", releasedWorker.get());
		assertEquals(BuildingType.MINE, collectedSite.get());
	}

	@Test
	public void panelShowsWhyTheLastCommandWasRefused() throws Exception
	{
		RuneholdController controller = new RuneholdController(
			state,
			village,
			catalog,
			ignored -> { });
		AtomicReference<RuneholdPanel> panel = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() ->
		{
			panel.set(new RuneholdPanel(
				controller.getViewModel(),
				ignored -> { },
				new RecordingRuneholdAssets()));
			panel.get().refresh(controller.getViewModel(), "Villager 1 is already assigned");
		});

		assertTrue(containsText(panel.get(), "Villager 1 is already assigned"));
	}

	private static final class RecordingGatheringCommands
		implements RuneholdPanel.GatheringCommands
	{
		private final AtomicReference<String> releasedWorker;
		private final AtomicReference<BuildingType> collectedSite;

		private RecordingGatheringCommands(
			AtomicReference<String> releasedWorker,
			AtomicReference<BuildingType> collectedSite)
		{
			this.releasedWorker = releasedWorker;
			this.collectedSite = collectedSite;
		}

		@Override
		public void assign(String workerId, BuildingType site)
		{
		}

		@Override
		public void release(String workerId)
		{
			releasedWorker.set(workerId);
		}

		@Override
		public void collect(BuildingType site)
		{
			collectedSite.set(site);
		}
	}

	private static boolean containsText(Container container, String text)
	{
		for (Component component : container.getComponents())
		{
			if (component instanceof javax.swing.text.JTextComponent
				&& text.equals(((javax.swing.text.JTextComponent) component).getText()))
			{
				return true;
			}
			if (component instanceof JLabel && text.equals(((JLabel) component).getText()))
			{
				return true;
			}
			if (component instanceof Container && containsText((Container) component, text))
			{
				return true;
			}
		}
		return false;
	}

	private static int countButtons(Container container)
	{
		return countButtons(container, false);
	}

	private static int countEnabledButtons(Container container)
	{
		return countButtons(container, true);
	}

	private static int countButtons(Container container, boolean enabledOnly)
	{
		int count = 0;
		for (Component component : container.getComponents())
		{
			if (component instanceof JButton && (!enabledOnly || component.isEnabled()))
			{
				count++;
			}
			if (component instanceof Container)
			{
				count += countButtons((Container) component, enabledOnly);
			}
		}
		return count;
	}

	private static JButton findButton(Container container, String text)
	{
		for (Component component : container.getComponents())
		{
			if (component instanceof JButton && text.equals(((JButton) component).getText()))
			{
				return (JButton) component;
			}
			if (component instanceof Container)
			{
				JButton nested = findButton((Container) component, text);
				if (nested != null)
				{
					return nested;
				}
			}
		}
		return null;
	}

	private static JLabel findLabel(Container container, String text)
	{
		for (Component component : container.getComponents())
		{
			if (component instanceof JLabel && text.equals(((JLabel) component).getText()))
			{
				return (JLabel) component;
			}
			if (component instanceof Container)
			{
				JLabel nested = findLabel((Container) component, text);
				if (nested != null)
				{
					return nested;
				}
			}
		}
		return null;
	}

	private static <T extends Component> java.util.List<T> findComponents(
		Container container,
		Class<T> type)
	{
		java.util.List<T> matches = new java.util.ArrayList<>();
		for (Component component : container.getComponents())
		{
			if (type.isInstance(component))
			{
				matches.add(type.cast(component));
			}
			if (component instanceof Container)
			{
				matches.addAll(findComponents((Container) component, type));
			}
		}
		return matches;
	}

	private static final class RecordingRuneholdAssets implements RuneholdAssets
	{
		private int manaIconCount;
		private int upgradeIconCount;
		private final Map<BuildingType, Integer> buildingIconCounts =
			new EnumMap<>(BuildingType.class);
		private final Map<BuildingType, Integer> gatheringIconCounts =
			new EnumMap<>(BuildingType.class);

		@Override
		public Font regularFont(float size)
		{
			return new Font(Font.MONOSPACED, Font.PLAIN, Math.round(size));
		}

		@Override
		public Font boldFont(float size)
		{
			return new Font(Font.MONOSPACED, Font.BOLD, Math.round(size));
		}

		@Override
		public void addManaIcon(JLabel label)
		{
			manaIconCount++;
		}

		@Override
		public void addBuildingIcon(BuildingType type, JLabel label)
		{
			buildingIconCounts.merge(type, 1, Integer::sum);
		}

		@Override
		public void addGatheringIcon(BuildingType type, JLabel label)
		{
			gatheringIconCounts.merge(type, 1, Integer::sum);
		}

		@Override
		public void addUpgradeIcon(JButton button)
		{
			upgradeIconCount++;
		}
	}
}
