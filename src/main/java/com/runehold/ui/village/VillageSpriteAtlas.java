package com.runehold.ui.village;

import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.BuildingType;
import com.runehold.domain.layout.Footprint;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;

public final class VillageSpriteAtlas
{
	private static final int MAX_SCALED_ENTRIES = 96;
	private final Map<BuildingType, BufferedImage> originals =
		new EnumMap<>(BuildingType.class);
	private final Map<BuildingType, VillageSpriteMetadata> metadata =
		new EnumMap<>(BuildingType.class);
	private final Map<ScaleKey, BufferedImage> scaled =
		new LinkedHashMap<ScaleKey, BufferedImage>(MAX_SCALED_ENTRIES, 0.75f, true)
		{
			@Override
			protected boolean removeEldestEntry(Map.Entry<ScaleKey, BufferedImage> eldest)
			{
				return size() > MAX_SCALED_ENTRIES;
			}
		};

	public VillageSpriteAtlas()
	{
		load(BuildingType.TOWN_HALL, "/village/town_hall.png");
		load(BuildingType.MANA_WELL, "/village/mana_well.png");
		load(BuildingType.MANA_GROVE, "/village/mana_grove.png");
		load(BuildingType.BARRACKS, "/village/barracks.png");
		load(BuildingType.WORKSHOP, "/village/workshop.png");
		load(BuildingType.RUNE_BANNER, "/village/rune_banner.png");
		BuildingCatalog catalog = new BuildingCatalog();
		for (BuildingType type : BuildingType.values())
		{
			if (type.isGatheringSite())
			{
				// Gathering sites have no packaged artwork yet. Rather than ship a rushed
				// PNG, they render as a flat quarried plot in the village palette until the
				// art pass produces real sprites. The plot is authored at the same native
				// tile width as the buildings so it lands exactly on its footprint.
				originals.put(type, placeholderPlot(type, catalog.getFootprint(type)));
			}
		}
		for (Map.Entry<BuildingType, BufferedImage> entry : originals.entrySet())
		{
			metadata.put(entry.getKey(), measure(entry.getValue()));
		}
	}

	private static BufferedImage placeholderPlot(BuildingType type, Footprint footprint)
	{
		int half = VillageSpriteMetadata.NATIVE_TILE_WIDTH / 2;
		int quarter = half / 2;
		int width = (footprint.getWidth() + footprint.getHeight()) * half;
		int height = (footprint.getWidth() + footprint.getHeight()) * quarter + quarter;
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		Color base = placeholderColor(type);
		int baseY = height - quarter;
		// The plot is the footprint diamond itself, with a short vertical lip so the plot
		// reads as sunken ground rather than a flat decal.
		int[] xs = {width / 2, width, width / 2, 0};
		int[] topYs = {0, baseY / 2, baseY, baseY / 2};
		graphics.setColor(base.darker());
		graphics.fillPolygon(
			new int[]{0, width / 2, width, width / 2},
			new int[]{baseY / 2, baseY, baseY / 2, baseY + quarter},
			4);
		graphics.setColor(base);
		graphics.fillPolygon(xs, topYs, 4);
		graphics.setColor(base.brighter());
		graphics.drawLine(width / 2, 0, width - 1, baseY / 2);
		graphics.setColor(new Color(0x100D0A));
		graphics.drawPolygon(xs, topYs, 4);
		graphics.dispose();
		return image;
	}

	private static Color placeholderColor(BuildingType type)
	{
		switch (type)
		{
			case MINE:
				return new Color(0x6D695E);
			case FISHING_SPOT:
				return new Color(0x3F5A6B);
			case WOODCUTTING_GROVE:
				return new Color(0x4A5F32);
			case QUARRY:
				return new Color(0x817B6E);
			case FARM:
				return new Color(0x7A6A3A);
			case HERB_PATCH:
				return new Color(0x3E6B45);
			case CLAY_PIT:
				return new Color(0x6B4A3A);
			case RUNE_ESSENCE_SITE:
				return new Color(0x554A6B);
			default:
				return new Color(0x5A4631);
		}
	}

	/**
	 * Native bounds of the loaded artwork. This is the renderer's only source of sprite
	 * dimensions, so it cannot drift from the PNG on disk.
	 */
	public VillageSpriteMetadata metadata(BuildingType type)
	{
		VillageSpriteMetadata cached = metadata.get(Objects.requireNonNull(type, "type"));
		if (cached == null)
		{
			throw new IllegalArgumentException("missing village sprite for " + type);
		}
		return cached;
	}

	/**
	 * Measures the opaque bounding box so the renderer can anchor on the artwork rather
	 * than on the canvas, which may be padded asymmetrically.
	 */
	private static VillageSpriteMetadata measure(BufferedImage image)
	{
		int minX = image.getWidth();
		int maxX = -1;
		int maxY = -1;
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				if ((image.getRGB(x, y) >>> 24) == 0)
				{
					continue;
				}
				if (x < minX)
				{
					minX = x;
				}
				if (x > maxX)
				{
					maxX = x;
				}
				maxY = y;
			}
		}
		if (maxX < 0)
		{
			return new VillageSpriteMetadata(
				image.getWidth(), image.getHeight(), image.getWidth() / 2, image.getHeight());
		}
		return new VillageSpriteMetadata(
			image.getWidth(),
			image.getHeight(),
			(minX + maxX + 1) / 2,
			maxY + 1);
	}

	public boolean has(BuildingType type)
	{
		return type != null && originals.containsKey(type);
	}

	public BufferedImage get(BuildingType type, int requestedWidth)
	{
		return get(type, 1, requestedWidth);
	}

	public BufferedImage get(BuildingType type, int level, int requestedWidth)
	{
		BufferedImage original = originals.get(Objects.requireNonNull(type, "type"));
		if (original == null)
		{
			throw new IllegalArgumentException("missing village sprite for " + type);
		}
		int width = Math.max(8, requestedWidth);
		int visualLevel = Math.max(1, level);
		if (width == original.getWidth() && visualLevel == 1)
		{
			return original;
		}

		ScaleKey key = new ScaleKey(type, visualLevel, width);
		BufferedImage cached = scaled.get(key);
		if (cached != null)
		{
			return cached;
		}

		int height = Math.max(8,
			(int) Math.round(original.getHeight() * (width / (double) original.getWidth())));
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(
			RenderingHints.KEY_INTERPOLATION,
			RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		graphics.drawImage(original, 0, 0, width, height, null);
		paintLevelDetails(graphics, type, visualLevel, width, height);
		graphics.dispose();
		scaled.put(key, image);
		return image;
	}

	private static void paintLevelDetails(
		Graphics2D graphics,
		BuildingType type,
		int level,
		int width,
		int height)
	{
		if (level <= 1)
		{
			return;
		}

		int unit = Math.max(1, width / 64);
		int tier = Math.min(4, level - 1);
		Color gold = new Color(0xD8A73A);
		Color bronze = new Color(0x8B5A2B);
		Color mana = new Color(0x478CD6);
		Color shadow = new Color(0x21170F, true);

		for (int index = 0; index < tier; index++)
		{
			int x = width / 2 - tier * unit * 3 + index * unit * 6;
			int y = Math.max(unit * 4, height / 4 - index * unit);
			graphics.setColor(shadow);
			graphics.fillRect(x - unit, y + unit, unit * 4, unit * 2);
			graphics.setColor(index % 2 == 0 ? gold : bronze);
			graphics.fillRect(x, y, unit * 3, unit * 3);
		}

		switch (type)
		{
			case MANA_WELL:
			case MANA_GROVE:
				graphics.setColor(mana);
				graphics.fillRect(width / 2 - unit * 2, height / 2, unit * 4, unit * 2);
				graphics.fillRect(width / 2 + unit * 3, height / 2 + unit * 2, unit * 2, unit * 2);
				break;
			case BARRACKS:
				graphics.setColor(gold);
				graphics.fillRect(width / 2 - unit * 5, height / 3, unit * 3, unit * 5);
				graphics.fillRect(width / 2 + unit * 2, height / 3, unit * 3, unit * 5);
				break;
			case WORKSHOP:
				graphics.setColor(new Color(0xB8A17B));
				graphics.fillRect(width / 2 + unit * 5, height / 4, unit * 4, unit * 2);
				graphics.fillRect(width / 2 + unit * 7, height / 4 - unit * 3, unit * 2, unit * 3);
				break;
			case TOWN_HALL:
				graphics.setColor(gold);
				graphics.fillRect(width / 2 - unit * 8, height / 3, unit * 16, unit * 2);
				break;
			case RUNE_BANNER:
				graphics.setColor(mana);
				graphics.fillRect(width / 2 - unit, height / 3, unit * 2, unit * 2);
				break;
			default:
				break;
		}
	}

	private void load(BuildingType type, String path)
	{
		try (InputStream stream = VillageSpriteAtlas.class.getResourceAsStream(path))
		{
			if (stream == null)
			{
				throw new IllegalStateException("missing packaged sprite: " + path);
			}
			BufferedImage image = ImageIO.read(stream);
			if (image == null)
			{
				throw new IllegalStateException("invalid packaged sprite: " + path);
			}
			originals.put(type, image);
		}
		catch (IOException ex)
		{
			throw new IllegalStateException("unable to load sprite: " + path, ex);
		}
	}

	private static final class ScaleKey
	{
		private final BuildingType type;
		private final int level;
		private final int width;

		private ScaleKey(BuildingType type, int level, int width)
		{
			this.type = type;
			this.level = level;
			this.width = width;
		}

		@Override
		public boolean equals(Object other)
		{
			if (this == other)
			{
				return true;
			}
			if (!(other instanceof ScaleKey))
			{
				return false;
			}
			ScaleKey key = (ScaleKey) other;
			return type == key.type && level == key.level && width == key.width;
		}

		@Override
		public int hashCode()
		{
			int result = type.hashCode();
			result = 31 * result + level;
			result = 31 * result + width;
			return result;
		}
	}
}
