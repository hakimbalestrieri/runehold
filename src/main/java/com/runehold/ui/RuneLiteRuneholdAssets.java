package com.runehold.ui;

import com.runehold.domain.BuildingType;
import java.awt.Font;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JLabel;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * Loads OSRS fonts and item sprites from the running RuneLite client. No
 * extracted Jagex image or font file is redistributed in the plugin JAR.
 *
 * @see <a href="https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/ui/FontManager.java">RuneLite FontManager</a>
 * @see <a href="https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/game/ItemManager.java">RuneLite ItemManager</a>
 */
public final class RuneLiteRuneholdAssets implements RuneholdAssets
{
	private final ItemManager itemManager;

	public RuneLiteRuneholdAssets(ItemManager itemManager)
	{
		this.itemManager = Objects.requireNonNull(itemManager, "itemManager");
	}

	@Override
	public Font regularFont(float size)
	{
		return FontManager.getRunescapeFont().deriveFont(Font.PLAIN, size);
	}

	@Override
	public Font boldFont(float size)
	{
		return FontManager.getRunescapeBoldFont().deriveFont(Font.BOLD, size);
	}

	@Override
	public void addManaIcon(JLabel label)
	{
		addItemIcon(ItemID.WATERRUNE, label);
	}

	@Override
	public void addBuildingIcon(BuildingType type, JLabel label)
	{
		addItemIcon(itemIdFor(type), label);
	}

	@Override
	public void addUpgradeIcon(JButton button)
	{
		AsyncBufferedImage image = itemManager.getImage(ItemID.HAMMER);
		image.addTo(button);
	}

	static int itemIdFor(BuildingType type)
	{
		switch (Objects.requireNonNull(type, "type"))
		{
			case TOWN_HALL:
				return ItemID.SKILLCAPE_CONSTRUCTION;
			case MANA_WELL:
				return ItemID.WATERRUNE;
			case MANA_GROVE:
				return ItemID.WATERRUNE;
			case BARRACKS:
				return ItemID.BRONZE_SWORD;
			case WORKSHOP:
				return ItemID.HAMMER;
			case RUNE_BANNER:
				return ItemID.SKILLCAPE_CONSTRUCTION;
			default:
				throw new IllegalArgumentException("Unsupported building type: " + type);
		}
	}

	private void addItemIcon(int itemId, JLabel label)
	{
		AsyncBufferedImage image = itemManager.getImage(itemId);
		image.addTo(label);
	}
}
