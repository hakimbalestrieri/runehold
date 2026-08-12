package com.runehold.ui.village;

import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.BuildingType;
import com.runehold.domain.Village;
import com.runehold.domain.VillageState;
import com.runehold.domain.layout.GridPoint;
import com.runehold.domain.layout.PlacementResult;
import com.runehold.ui.RuneholdAssets;
import com.runehold.ui.RuneholdViewModel;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JLabel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class VillageCanvasTest
{
	@Test
	public void rendersNativeVillageCanvasWithAccessibleSummary()
	{
		BuildingCatalog catalog = new BuildingCatalog();
		VillageState state = VillageState.fresh(LocalDate.of(2026, 8, 12));
		Village village = new Village(state, catalog, true);
		RuneholdViewModel viewModel = RuneholdViewModel.from(state, village, catalog);
		VillageCanvas canvas = new VillageCanvas(viewModel, catalog, new TestAssets());

		assertEquals(new Dimension(768, 512), canvas.getPreferredSize());
		assertTrue(canvas.getAccessibleContext().getAccessibleName().contains("1 building"));

		canvas.setSize(canvas.getPreferredSize());
		BufferedImage image = new BufferedImage(768, 512, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		canvas.paint(graphics);
		graphics.dispose();

		assertNotEquals(image.getRGB(0, 0), image.getRGB(384, 256));
	}

	@Test
	public void rendersReadableTerrainAtSmallAndLargeWindowSizes()
	{
		BuildingCatalog catalog = new BuildingCatalog();
		VillageState state = VillageState.fresh(LocalDate.of(2026, 8, 12));
		Village village = new Village(state, catalog, true);
		VillageCanvas canvas = new VillageCanvas(
			RuneholdViewModel.from(state, village, catalog), catalog, new TestAssets());

		BufferedImage small = paint(canvas, 760, 520);
		BufferedImage large = paint(canvas, 1280, 760);

		assertNotEquals(small.getRGB(0, 0), small.getRGB(380, 260));
		assertNotEquals(large.getRGB(0, 0), large.getRGB(640, 380));
	}

	@Test
	public void deeperTilesSortAfterNorthernTiles()
	{
		BuildingCatalog catalog = new BuildingCatalog();
		VillageState state = VillageState.fresh(LocalDate.of(2026, 8, 12));
		Village village = new Village(state, catalog, true);
		VillageCanvas canvas = new VillageCanvas(
			RuneholdViewModel.from(state, village, catalog), catalog, new TestAssets());

		int northern = canvas.depthForTest(BuildingType.MANA_WELL, new GridPoint(2, 2));
		int southern = canvas.depthForTest(BuildingType.MANA_WELL, new GridPoint(8, 8));

		assertTrue(southern > northern);
	}

	@Test
	public void spriteAtlasKeepsDistinctPixelLevelVariants()
	{
		VillageSpriteAtlas atlas = new VillageSpriteAtlas();
		BufferedImage levelOne = atlas.get(BuildingType.TOWN_HALL, 1, 96);
		BufferedImage levelThree = atlas.get(BuildingType.TOWN_HALL, 3, 96);

		assertEquals(levelOne.getWidth(), levelThree.getWidth());
		assertTrue(hasDifferentPixel(levelOne, levelThree));
	}

	@Test
	public void placementModeUsesBoundedZoomAndCancelLeavesStateUntouched()
	{
		BuildingCatalog catalog = new BuildingCatalog();
		VillageState state = VillageState.fresh(LocalDate.of(2026, 8, 12));
		Village village = new Village(state, catalog, true);
		VillageCanvas canvas = new VillageCanvas(
			RuneholdViewModel.from(state, village, catalog), catalog, new TestAssets());
		canvas.setSize(1024, 680);

		canvas.beginPlacement(BuildingType.MANA_WELL);
		assertEquals(VillageInteractionModel.Mode.PLACE, canvas.getMode());
		assertTrue(canvas.isActivePlacementValid());
		canvas.zoomBy(10);
		assertEquals(VillageCanvas.MAX_ZOOM, canvas.getZoom(), 0.001);
		canvas.zoomBy(-10);
		assertEquals(VillageCanvas.MIN_ZOOM, canvas.getZoom(), 0.001);
		canvas.cancelInteraction();

		assertEquals(VillageInteractionModel.Mode.VIEW, canvas.getMode());
		assertEquals(0, village.levelOf(BuildingType.MANA_WELL));
	}

	@Test
	public void leftClickOnValidGhostConfirmsPlacement()
	{
		BuildingCatalog catalog = new BuildingCatalog();
		VillageState state = VillageState.fresh(LocalDate.of(2026, 8, 12));
		Village village = new Village(state, catalog, true);
		AtomicReference<GridPoint> confirmed = new AtomicReference<>();
		VillageCanvas canvas = new VillageCanvas(
			RuneholdViewModel.from(state, village, catalog),
			catalog,
			new TestAssets(),
			new VillageCanvas.Listener()
			{
				@Override
				public void selectionChanged(BuildingType selectedType)
				{
				}

				@Override
				public void interactionChanged()
				{
				}

				@Override
				public void confirmBuild(BuildingType type, GridPoint destination)
				{
					confirmed.set(destination);
				}

				@Override
				public void confirmMove(BuildingType type, GridPoint destination)
				{
				}
			});
		canvas.setSize(1024, 680);

		canvas.beginPlacement(BuildingType.MANA_WELL);
		GridPoint destination = new GridPoint(1, 12);
		Point click = canvas.screenPointForTest(destination);
		dispatchLeftMouse(canvas, MouseEvent.MOUSE_MOVED, click);
		dispatchLeftMouse(canvas, MouseEvent.MOUSE_RELEASED, click);

		assertEquals(destination, confirmed.get());
		assertEquals(VillageInteractionModel.Mode.VIEW, canvas.getMode());
	}

	@Test
	public void edgePlacementUsesFootprintValidityNotSpriteBounds()
	{
		BuildingCatalog catalog = new BuildingCatalog();
		VillageState state = VillageState.fresh(LocalDate.of(2026, 8, 12));
		Village village = new Village(state, catalog, true);
		AtomicReference<GridPoint> confirmed = new AtomicReference<>();
		VillageCanvas canvas = new VillageCanvas(
			RuneholdViewModel.from(state, village, catalog),
			catalog,
			new TestAssets(),
			new VillageCanvas.Listener()
			{
				@Override
				public void selectionChanged(BuildingType selectedType)
				{
				}

				@Override
				public void interactionChanged()
				{
				}

				@Override
				public void confirmBuild(BuildingType type, GridPoint destination)
				{
					confirmed.set(destination);
				}

				@Override
				public void confirmMove(BuildingType type, GridPoint destination)
				{
				}
			});
		canvas.setSize(1024, 680);

		canvas.beginPlacement(BuildingType.MANA_GROVE);
		GridPoint edgeDestination = new GridPoint(15, 15);
		canvas.updateGhostForTest(edgeDestination);

		assertEquals(edgeDestination, canvas.getGhostPosition());
		assertEquals(PlacementResult.Status.SUCCESS,
			canvas.activePreviewForTest().getStatus());
		assertTrue(canvas.isActivePlacementValid());

		canvas.confirmInteraction();

		assertEquals(edgeDestination, confirmed.get());
		assertEquals(VillageInteractionModel.Mode.VIEW, canvas.getMode());
	}

	@Test
	public void writesRepresentativeNativeSizePreview() throws Exception
	{
		BuildingCatalog catalog = new BuildingCatalog();
		Instant start = Instant.parse("2026-08-12T09:00:00Z");
		Map<BuildingType, Integer> levels = new EnumMap<>(BuildingType.class);
		Map<BuildingType, GridPoint> positions = new EnumMap<>(BuildingType.class);
		for (BuildingType type : BuildingType.values())
		{
			levels.put(type, 0);
		}
		levels.put(BuildingType.TOWN_HALL, 3);
		levels.put(BuildingType.MANA_GROVE, 1);
		levels.put(BuildingType.MANA_WELL, 1);
		levels.put(BuildingType.BARRACKS, 1);
		levels.put(BuildingType.RUNE_BANNER, 1);
		positions.put(BuildingType.TOWN_HALL, new GridPoint(7, 7));
		positions.put(BuildingType.MANA_GROVE, new GridPoint(1, 2));
		positions.put(BuildingType.MANA_WELL, new GridPoint(1, 12));
		positions.put(BuildingType.BARRACKS, new GridPoint(11, 4));
		positions.put(BuildingType.RUNE_BANNER, new GridPoint(10, 14));
		VillageState state = VillageState.restore(
			250,
			Collections.emptyMap(),
			Collections.emptyMap(),
			0,
			LocalDate.of(2026, 8, 12),
			levels,
			positions,
			null,
			0,
			start.toEpochMilli());
		Village village = new Village(state, catalog, true,
			Clock.fixed(start.plusSeconds(181), ZoneOffset.UTC));
		VillageCanvas canvas = new VillageCanvas(
			RuneholdViewModel.from(state, village, catalog), catalog, new TestAssets());
		canvas.setSize(1024, 680);
		BufferedImage image = new BufferedImage(1024, 680, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		canvas.paint(graphics);
		graphics.dispose();
		File preview = new File("build/reports/village-preview.png");
		preview.getParentFile().mkdirs();
		javax.imageio.ImageIO.write(image, "png", preview);
		assertTrue(preview.isFile());
	}

	private static BufferedImage paint(VillageCanvas canvas, int width, int height)
	{
		canvas.setSize(width, height);
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		canvas.paint(graphics);
		graphics.dispose();
		return image;
	}

	private static void dispatchLeftMouse(VillageCanvas canvas, int id, Point point)
	{
		canvas.dispatchEvent(new MouseEvent(
			canvas,
			id,
			System.currentTimeMillis(),
			0,
			point.x,
			point.y,
			1,
			false,
			MouseEvent.BUTTON1));
	}

	private static boolean hasDifferentPixel(BufferedImage first, BufferedImage second)
	{
		for (int y = 0; y < first.getHeight(); y++)
		{
			for (int x = 0; x < first.getWidth(); x++)
			{
				if (first.getRGB(x, y) != second.getRGB(x, y))
				{
					return true;
				}
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
		}

		@Override
		public void addBuildingIcon(BuildingType type, JLabel label)
		{
		}

		@Override
		public void addGatheringIcon(com.runehold.domain.BuildingType type, JLabel label)
		{
		}

		@Override
		public void addUpgradeIcon(JButton button)
		{
		}
	}
}
