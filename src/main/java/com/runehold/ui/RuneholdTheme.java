package com.runehold.ui;

import java.awt.Color;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.border.Border;

final class RuneholdTheme
{
	static final Color BACKGROUND = new Color(0x1B1711);
	static final Color PANEL = new Color(0x302820);
	static final Color HEADER = new Color(0x4B4841);
	static final Color STONE_LIGHT = new Color(0x817B6E);
	static final Color STONE_MID = new Color(0x6D695E);
	static final Color STONE_DARK = new Color(0x2B2925);
	static final Color OUTLINE = new Color(0x100D0A);
	static final Color TEXT = new Color(0xE4D6AC);
	static final Color TEXT_MUTED = new Color(0xCFC09A);
	static final Color TEXT_DISABLED = new Color(0xAAA28E);
	static final Color ORANGE = new Color(0xFF981F);
	static final Color SUCCESS = new Color(0x8FAA5D);
	static final Color ERROR = new Color(0xD07A63);
	static final Color BUTTON = new Color(0x5A4631);
	static final Color BUTTON_HOVER = new Color(0x6A553D);
	static final Color BUTTON_PRESSED = new Color(0x3E3024);
	static final Color BUTTON_DISABLED = new Color(0x35312B);

	private RuneholdTheme()
	{
	}

	static Border stoneBorder(int padding)
	{
		Border carvedEdge = BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(OUTLINE),
			BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 1, 0, 0, STONE_LIGHT),
				BorderFactory.createMatteBorder(0, 0, 1, 1, STONE_DARK)));
		return BorderFactory.createCompoundBorder(
			carvedEdge,
			BorderFactory.createEmptyBorder(padding, padding, padding, padding));
	}

	static void styleActionButton(JButton button)
	{
		button.setUI(new RuneholdButtonUI());
		button.setForeground(TEXT);
		button.setBackground(BUTTON);
		button.setContentAreaFilled(false);
		button.setBorderPainted(false);
		button.setFocusPainted(true);
		button.setRolloverEnabled(true);
		button.setOpaque(false);
		button.setMargin(new Insets(4, 6, 4, 6));
	}

	static void styleLabel(JLabel label)
	{
		label.setUI(new RuneholdLabelUI());
	}
}
