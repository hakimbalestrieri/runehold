package com.runehold.ui.village;

import com.runehold.domain.BuildingType;
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
