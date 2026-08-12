package com.runehold.ui.village;

import com.runehold.ui.VillageResourceView;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.swing.Icon;

final class VillageResourceIcon implements Icon
{
	private static final int SIZE = 42;
	private final VillageResourceView.Kind kind;

	VillageResourceIcon(VillageResourceView.Kind kind)
	{
		this.kind = kind;
	}

	@Override
	public int getIconWidth()
	{
		return SIZE;
	}

	@Override
	public int getIconHeight()
	{
		return SIZE;
	}

	@Override
	public void paintIcon(Component component, Graphics graphics, int x, int y)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		g.setColor(VillageTheme.OUTLINE);
		g.fillRect(x + 1, y + 1, SIZE - 2, SIZE - 2);
		g.setColor(new Color(0x4B4841));
		g.fillRect(x + 2, y + 2, SIZE - 4, SIZE - 4);
		switch (kind)
		{
			case MANA:
				paintManaDrop(g, x, y, new Color(0x4C8FE6));
				break;
			case MANA_CAPACITY:
				g.setColor(new Color(0x8B6D45));
				g.fillRect(x + 10, y + 15, 22, 17);
				g.setColor(new Color(0xD0B16B));
				g.drawRect(x + 10, y + 15, 22, 17);
				g.fillRect(x + 14, y + 10, 14, 6);
				break;
			case GROVE_MANA:
				g.setColor(new Color(0x31512B));
				g.fillRect(x + 19, y + 21, 5, 10);
				g.setColor(new Color(0x5F8D3E));
				g.fillPolygon(new int[]{x + 10, x + 21, x + 32},
					new int[]{y + 22, y + 8, y + 22}, 3);
				g.setColor(new Color(0x7EBB52));
				g.fillPolygon(new int[]{x + 13, x + 21, x + 29},
					new int[]{y + 27, y + 14, y + 27}, 3);
				g.setColor(new Color(0x6BB6FF));
				g.fillRect(x + 20, y + 12, 3, 8);
				break;
			case DAILY_XP_MANA:
				g.setColor(new Color(0xD7A33F));
				g.fillOval(x + 13, y + 10, 16, 16);
				g.drawLine(x + 21, y + 6, x + 21, y + 3);
				g.drawLine(x + 21, y + 29, x + 21, y + 36);
				g.drawLine(x + 8, y + 18, x + 4, y + 18);
				g.drawLine(x + 34, y + 18, x + 38, y + 18);
				break;
			case BUILDER:
				g.setColor(new Color(0xA57A45));
				g.fillRect(x + 11, y + 25, 22, 5);
				g.setColor(new Color(0xBFC1B8));
				g.fillRect(x + 20, y + 10, 6, 20);
				g.fillRect(x + 14, y + 10, 20, 5);
				break;
			case VILLAGE:
				g.setColor(new Color(0x8A5A32));
				g.fillRect(x + 11, y + 21, 20, 12);
				g.setColor(new Color(0xB17A3F));
				g.fillPolygon(new int[]{x + 8, x + 21, x + 34},
					new int[]{y + 22, y + 10, y + 22}, 3);
				g.setColor(new Color(0x4C8FE6));
				g.fillRect(x + 19, y + 25, 5, 8);
				break;
			case WORKERS:
				g.setColor(new Color(0xB9A77A));
				g.fillRect(x + 12, y + 12, 6, 6);
				g.fillRect(x + 24, y + 12, 6, 6);
				g.fillRect(x + 10, y + 20, 10, 12);
				g.fillRect(x + 22, y + 20, 10, 12);
				g.setColor(new Color(0x5A4631));
				g.fillRect(x + 13, y + 22, 4, 10);
				g.fillRect(x + 25, y + 22, 4, 10);
				break;
			case RESOURCES:
				g.setColor(new Color(0x765C36));
				g.fillRect(x + 11, y + 20, 20, 12);
				g.setColor(new Color(0xC7A640));
				g.fillRect(x + 14, y + 16, 14, 4);
				g.setColor(new Color(0x8F8A76));
				g.fillRect(x + 16, y + 10, 5, 5);
				g.fillRect(x + 23, y + 11, 5, 5);
				break;
			default:
				break;
		}
		g.dispose();
	}

	private static void paintManaDrop(Graphics2D g, int x, int y, Color color)
	{
		g.setColor(color);
		Polygon drop = new Polygon(
			new int[]{x + 21, x + 29, x + 27, x + 21, x + 15, x + 13},
			new int[]{y + 7, y + 23, y + 32, y + 35, y + 32, y + 23},
			6);
		g.fillPolygon(drop);
		g.setColor(new Color(0xA9D4FF));
		g.fillRect(x + 20, y + 15, 3, 12);
	}
}
