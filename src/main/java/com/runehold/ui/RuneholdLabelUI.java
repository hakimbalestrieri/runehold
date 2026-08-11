package com.runehold.ui;

import java.awt.Graphics;
import javax.swing.JLabel;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicLabelUI;

final class RuneholdLabelUI extends BasicLabelUI
{
	@Override
	protected void paintEnabledText(
		JLabel label,
		Graphics graphics,
		String text,
		int textX,
		int textY)
	{
		paintShadowedText(label, graphics, text, textX, textY, label.getForeground());
	}

	@Override
	protected void paintDisabledText(
		JLabel label,
		Graphics graphics,
		String text,
		int textX,
		int textY)
	{
		paintShadowedText(
			label,
			graphics,
			text,
			textX,
			textY,
			RuneholdTheme.TEXT_DISABLED);
	}

	private static void paintShadowedText(
		JLabel label,
		Graphics graphics,
		String text,
		int textX,
		int textY,
		java.awt.Color foreground)
	{
		int mnemonicIndex = label.getDisplayedMnemonicIndex();
		graphics.setColor(RuneholdTheme.OUTLINE);
		BasicGraphicsUtils.drawStringUnderlineCharAt(
			graphics,
			text,
			mnemonicIndex,
			textX + 1,
			textY + 1);
		graphics.setColor(foreground);
		BasicGraphicsUtils.drawStringUnderlineCharAt(
			graphics,
			text,
			mnemonicIndex,
			textX,
			textY);
	}
}
