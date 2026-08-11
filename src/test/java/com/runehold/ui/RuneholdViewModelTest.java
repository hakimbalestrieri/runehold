package com.runehold.ui;

import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.BuildingType;
import com.runehold.domain.UpgradeResult;
import com.runehold.domain.Village;
import com.runehold.domain.VillageState;
import java.awt.Component;
import java.awt.Container;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
	public void freshVillageHasReadableHeaderAndFourRows()
	{
		RuneholdViewModel viewModel = RuneholdViewModel.from(state, village, catalog);

		assertEquals("250 mana", viewModel.getManaText());
		assertEquals("Today: 0 / 10,000", viewModel.getDailyProgressText());
		assertEquals(4, viewModel.getBuildings().size());
		assertEquals("Level 1 / 5", viewModel.getBuilding(BuildingType.TOWN_HALL).getLevelText());
		assertEquals("Not built · Max 5", viewModel.getBuilding(BuildingType.MANA_WELL).getLevelText());
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
			ignored -> { })));

		assertEquals(4, countButtons(panel.get()));
		assertEquals(2, countEnabledButtons(panel.get()));
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
			}));
			JButton buildButton = findButton(panel.get(), "Build (100)");
			buildButton.doClick();
			buildButton.doClick();
		});

		assertEquals(BuildingType.MANA_WELL, requested.get());
		assertEquals(1, requestCount.get());
		assertEquals(250L, state.getMana());
		assertEquals(0, village.levelOf(BuildingType.MANA_WELL));
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
}
