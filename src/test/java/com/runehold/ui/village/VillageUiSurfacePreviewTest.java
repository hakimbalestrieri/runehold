package com.runehold.ui.village;

import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.Village;
import com.runehold.domain.VillageState;
import com.runehold.ui.RuneholdAssets;
import com.runehold.ui.RuneholdViewModel;
import com.runehold.domain.BuildingType;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class VillageUiSurfacePreviewTest
{
	@Test
	public void writesHudAndBuildCataloguePreview() throws Exception
	{
		BuildingCatalog catalog = new BuildingCatalog();
		VillageState state = VillageState.fresh(LocalDate.of(2026, 8, 12));
		Village village = new Village(state, catalog, true);
		RuneholdViewModel viewModel = RuneholdViewModel.from(state, village, catalog);
		TestAssets assets = new TestAssets();
		BufferedImage image = new BufferedImage(760, 720, BufferedImage.TYPE_INT_ARGB);

		SwingUtilities.invokeAndWait(() ->
		{
			VillageHudPanel hud = new VillageHudPanel(catalog, assets);
			hud.refresh(viewModel);
			hud.setBounds(0, 0, 760, 182);
			layoutDeep(hud);

			VillageBuildCatalogPanel catalogue = new VillageBuildCatalogPanel(
				catalog,
				assets,
				ignored -> { },
				ignored -> { },
				() -> { });
			catalogue.refresh(viewModel);
			catalogue.setBounds(0, 190, 560, 520);
			layoutDeep(catalogue);

			Graphics2D graphics = image.createGraphics();
			graphics.setColor(VillageTheme.BACKGROUND);
			graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
			hud.paint(graphics);
			graphics.translate(0, 190);
			catalogue.paint(graphics);
			graphics.dispose();
		});

		File preview = new File("build/reports/village-ui-surfaces.png");
		preview.getParentFile().mkdirs();
		javax.imageio.ImageIO.write(image, "png", preview);
		assertTrue(preview.isFile());
	}

	@Test
	public void buildCatalogueUpgradesAlreadyBuiltStructures() throws Exception
	{
		BuildingCatalog catalog = new BuildingCatalog();
		VillageState state = VillageState.fresh(LocalDate.of(2026, 8, 12));
		Village village = new Village(state, catalog, true);
		RuneholdViewModel viewModel = RuneholdViewModel.from(state, village, catalog);
		AtomicReference<BuildingType> upgraded = new AtomicReference<>();
		VillageBuildCatalogPanel catalogue = new VillageBuildCatalogPanel(
			catalog,
			new TestAssets(),
			ignored -> { },
			upgraded::set,
			() -> { });

		SwingUtilities.invokeAndWait(() ->
		{
			catalogue.refresh(viewModel);
			JButton upgrade = catalogue.actionButtonForTest(
				viewModel.getBuilding(BuildingType.TOWN_HALL));
			assertTrue(upgrade.getText().contains("Upgrade"));
			upgrade.doClick();
		});

		assertTrue(upgraded.get() == BuildingType.TOWN_HALL);
	}

	@Test
	public void buildCatalogueExplainsManaGroveProductionUpgrade() throws Exception
	{
		BuildingCatalog catalog = new BuildingCatalog();
		VillageState state = VillageState.fresh(LocalDate.of(2026, 8, 12));
		Village village = new Village(state, catalog, true);
		assertTrue(village.upgrade(BuildingType.TOWN_HALL).isSuccess());
		assertTrue(village.upgrade(BuildingType.MANA_GROVE).isSuccess());
		RuneholdViewModel viewModel = RuneholdViewModel.from(state, village, catalog);
		VillageBuildCatalogPanel catalogue = new VillageBuildCatalogPanel(
			catalog,
			new TestAssets(),
			ignored -> { },
			ignored -> { },
			() -> { });

		SwingUtilities.invokeAndWait(() ->
		{
			catalogue.refresh(viewModel);
			catalogue.setBounds(0, 0, 560, 520);
			layoutDeep(catalogue);
		});

		assertTrue(containsLabelText(catalogue, "Production: 2 -> 4 mana / min"));
		assertTrue(containsLabelText(catalogue, "Grove storage: 100 -> 200 mana"));
	}

	private static void layoutDeep(JComponent component)
	{
		component.doLayout();
		for (Component child : component.getComponents())
		{
			if (child instanceof JComponent)
			{
				layoutDeep((JComponent) child);
			}
		}
	}

	private static JButton findButtonContaining(Container root, String text)
	{
		for (Component child : root.getComponents())
		{
			if (child instanceof JButton && ((JButton) child).getText().contains(text))
			{
				return (JButton) child;
			}
			if (child instanceof Container)
			{
				JButton found = findButtonContaining((Container) child, text);
				if (found != null)
				{
					return found;
				}
			}
		}
		throw new AssertionError("button not found containing: " + text);
	}

	private static boolean containsLabelText(Container root, String text)
	{
		for (Component child : root.getComponents())
		{
			if (child instanceof JLabel && text.equals(((JLabel) child).getText()))
			{
				return true;
			}
			if (child instanceof Container && containsLabelText((Container) child, text))
			{
				return true;
			}
		}
		return false;
	}

	private static final class TestAssets implements RuneholdAssets
	{
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
			label.setIcon(new VillageResourceIcon(com.runehold.ui.VillageResourceView.Kind.MANA));
		}

		@Override
		public void addBuildingIcon(BuildingType type, JLabel label)
		{
			label.setText(type.name());
		}

		@Override
		public void addGatheringIcon(com.runehold.domain.BuildingType type, JLabel label)
		{
			label.setIcon(new VillageResourceIcon(
				com.runehold.ui.VillageResourceView.Kind.RESOURCES));
		}

		@Override
		public void addUpgradeIcon(JButton button)
		{
			button.setBackground(new Color(0x4B4841));
		}
	}
}
