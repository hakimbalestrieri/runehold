package com.runehold.ui.village;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;

final class VillageTheme
{
	static final Color BACKGROUND = new Color(0x1B1711);
	static final Color PANEL = new Color(0x302820);
	static final Color HEADER = new Color(0x4B4841);
	static final Color STONE_LIGHT = new Color(0x817B6E);
	static final Color STONE_MID = new Color(0x6D695E);
	static final Color STONE_DARK = new Color(0x2B2925);
	static final Color OUTLINE = new Color(0x100D0A);
	static final Color TEXT = new Color(0xE4D6AC);
	static final Color MUTED = new Color(0xB9A77A);
	static final Color ORANGE = new Color(0xFF981F);
	static final Color VALID = new Color(0x8FAA5D);
	static final Color ERROR = new Color(0xD07A63);
	static final Color BUTTON = new Color(0x5A4631);
	static final Color BUTTON_HOVER = new Color(0x6A553D);
	static final Color BUTTON_DOWN = new Color(0x3E3024);
	static final Color BUTTON_DISABLED = new Color(0x35312B);

	private VillageTheme()
	{
	}

	static Border stoneBorder(int padding)
	{
		return BorderFactory.createCompoundBorder(
			BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(OUTLINE),
				BorderFactory.createCompoundBorder(
					BorderFactory.createMatteBorder(1, 1, 0, 0, STONE_LIGHT),
					BorderFactory.createMatteBorder(0, 0, 1, 1, STONE_DARK))),
			BorderFactory.createEmptyBorder(padding, padding, padding, padding));
	}

	static void styleButton(JButton button)
	{
		button.setUI(new FlatStoneButtonUI());
		button.setForeground(TEXT);
		button.setBackground(BUTTON);
		button.setBorder(stoneBorder(5));
		button.setFocusPainted(true);
		button.setRolloverEnabled(true);
		button.setOpaque(true);
	}

	private static final class FlatStoneButtonUI extends BasicButtonUI
	{
		@Override
		public void paint(Graphics graphics, JComponent component)
		{
			AbstractButton button = (AbstractButton) component;
			Color color;
			if (!button.isEnabled())
			{
				color = BUTTON_DISABLED;
			}
			else if (button.getModel().isPressed())
			{
				color = BUTTON_DOWN;
			}
			else if (button.getModel().isRollover())
			{
				color = BUTTON_HOVER;
			}
			else
			{
				color = BUTTON;
			}
			graphics.setColor(color);
			graphics.fillRect(0, 0, component.getWidth(), component.getHeight());
			super.paint(graphics, component);
		}
	}
}
