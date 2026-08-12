package com.runehold.ui.village;

import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.BuildingType;
import com.runehold.domain.Village;
import com.runehold.domain.VillageState;
import com.runehold.domain.layout.GridPoint;
import com.runehold.ui.RuneholdViewModel;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import javax.swing.JButton;
import javax.swing.JLabel;
import com.runehold.ui.RuneholdAssets;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Guards the sprite pipeline invariants that two rendering bugs came from: a second,
 * hand-maintained dimension table that disagreed with the artwork, and a sprite size
 * that depended on where the building stood on the map.
 */
public class VillageSpriteQaTest
{
	@Test
	public void everyBuildingTypeHasLoadableArtworkAndMetadata()
	{
		VillageSpriteAtlas atlas = new VillageSpriteAtlas();

		for (BuildingType type : BuildingType.values())
		{
			assertTrue("missing sprite for " + type, atlas.has(type));
			VillageSpriteMetadata metadata = atlas.metadata(type);
			assertTrue("empty bounds for " + type, metadata.getNativeWidth() > 0);
			assertTrue("empty bounds for " + type, metadata.getNativeHeight() > 0);
			BufferedImage image = atlas.get(type, 1, metadata.getNativeWidth());
			assertEquals("native request must not rescale " + type,
				metadata.getNativeWidth(), image.getWidth());
			assertEquals("native request must not rescale " + type,
				metadata.getNativeHeight(), image.getHeight());
		}
	}

	@Test
	public void metadataMatchesTheArtworkOnDiskRatherThanADeclaredTable()
	{
		VillageSpriteAtlas atlas = new VillageSpriteAtlas();

		for (BuildingType type : BuildingType.values())
		{
			BufferedImage original = atlas.get(type, 1, atlas.metadata(type).getNativeWidth());
			VillageSpriteMetadata metadata = atlas.metadata(type);

			assertEquals(type + " metadata width must come from the image",
				original.getWidth(), metadata.getNativeWidth());
			assertEquals(type + " metadata height must come from the image",
				original.getHeight(), metadata.getNativeHeight());
			assertEquals(type + " draws unscaled at the native tile width",
				metadata.getNativeWidth(),
				metadata.scaledWidth(VillageSpriteMetadata.NATIVE_TILE_WIDTH));
		}
	}

	@Test
	public void spritesUseBinaryAlphaWithoutATranslucentFringe()
	{
		VillageSpriteAtlas atlas = new VillageSpriteAtlas();

		for (BuildingType type : BuildingType.values())
		{
			VillageSpriteMetadata metadata = atlas.metadata(type);
			BufferedImage image = atlas.get(type, 1, metadata.getNativeWidth());
			for (int y = 0; y < image.getHeight(); y++)
			{
				for (int x = 0; x < image.getWidth(); x++)
				{
					int alpha = (image.getRGB(x, y) >>> 24);
					assertTrue(type + " has a semi-transparent pixel at " + x + "," + y
						+ " (alpha " + alpha + ")", alpha == 0 || alpha == 255);
				}
			}
		}
	}

	@Test
	public void aBuildingIsTheSameSizeWhereverItStandsOnTheMap()
	{
		VillageCanvas canvas = canvas();

		for (BuildingType type : BuildingType.values())
		{
			// The four extremes of the plot: north, east, south and west corners.
			Rectangle reference = canvas.spriteBoundsForTest(type, new GridPoint(0, 0));
			for (GridPoint corner : new GridPoint[]{
				new GridPoint(13, 0),
				new GridPoint(13, 13),
				new GridPoint(0, 13),
				new GridPoint(6, 6)})
			{
				Rectangle bounds = canvas.spriteBoundsForTest(type, corner);
				assertEquals(type + " width changes at " + corner.getX() + "," + corner.getY(),
					reference.width, bounds.width);
				assertEquals(type + " height changes at " + corner.getX() + "," + corner.getY(),
					reference.height, bounds.height);
			}
		}
	}

	@Test
	public void spriteContentStandsOnItsOwnFootprintDiamond()
	{
		BuildingCatalog catalog = new BuildingCatalog();
		VillageSpriteAtlas atlas = new VillageSpriteAtlas();
		VillageCanvas canvas = canvas();

		for (BuildingType type : BuildingType.values())
		{
			GridPoint position = new GridPoint(4, 4);
			Rectangle bounds = canvas.spriteBoundsForTest(type, position);
			VillageSpriteMetadata metadata = atlas.metadata(type);
			int tileWidth = canvas.tileWidthForTest();

			// The centre of the artwork, not of the padded canvas, must sit over the
			// centre of the footprint diamond.
			int contentCenterX = bounds.x + metadata.contentCenterX(tileWidth);
			assertEquals(type + " is not centred on its footprint",
				canvas.footprintCenterXForTest(type, position), contentCenterX);

			// The base of the artwork must sit on the diamond's front corner.
			int contentBottom = bounds.y + metadata.contentBottom(tileWidth);
			assertEquals(type + " does not stand on its footprint",
				canvas.footprintBaselineYForTest(type, position), contentBottom);
		}
	}

	private static VillageCanvas canvas()
	{
		BuildingCatalog catalog = new BuildingCatalog();
		VillageState state = VillageState.fresh(LocalDate.of(2026, 8, 12));
		Village village = new Village(state, catalog, true);
		VillageCanvas canvas = new VillageCanvas(
			RuneholdViewModel.from(state, village, catalog), catalog, new StubAssets());
		canvas.setSize(1024, 680);
		return canvas;
	}

	@Test
	public void everyBuildingTypeCanBeDrawnOnTheCanvas()
	{
		BuildingCatalog catalog = new BuildingCatalog();
		VillageState state = VillageState.fresh(LocalDate.of(2026, 8, 12));
		Village village = new Village(state, catalog, true);
		VillageCanvas canvas = new VillageCanvas(
			RuneholdViewModel.from(state, village, catalog), catalog, new StubAssets());
		canvas.setSize(1024, 680);

		for (BuildingType type : BuildingType.values())
		{
			canvas.beginPlacement(type);
			BufferedImage image = new BufferedImage(1024, 680, BufferedImage.TYPE_INT_ARGB);
			Graphics2D graphics = image.createGraphics();
			canvas.paint(graphics);
			graphics.dispose();
			canvas.cancelInteraction();
		}
	}

	private static final class StubAssets implements RuneholdAssets
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
		public void addGatheringIcon(BuildingType type, JLabel label)
		{
		}

		@Override
		public void addUpgradeIcon(JButton button)
		{
		}
	}
}
