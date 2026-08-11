package com.runehold.ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;

final class RuneholdButtonUI extends BasicButtonUI
{
	@Override
	public void paint(Graphics graphics, JComponent component)
	{
		AbstractButton button = (AbstractButton) component;
		ButtonModel model = button.getModel();
		Graphics2D pixelGraphics = (Graphics2D) graphics.create();
		try
		{
			pixelGraphics.setRenderingHint(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_OFF);
			if (!model.isEnabled())
			{
				pixelGraphics.setColor(RuneholdTheme.BUTTON_DISABLED);
			}
			else if (model.isPressed() && model.isArmed())
			{
				pixelGraphics.setColor(RuneholdTheme.BUTTON_PRESSED);
			}
			else if (model.isRollover())
			{
				pixelGraphics.setColor(RuneholdTheme.BUTTON_HOVER);
			}
			else
			{
				pixelGraphics.setColor(RuneholdTheme.BUTTON);
			}
			pixelGraphics.fillRect(0, 0, component.getWidth(), component.getHeight());
			paintCarvedEdge(pixelGraphics, component, model.isPressed() && model.isArmed());
		}
		finally
		{
			pixelGraphics.dispose();
		}

		super.paint(graphics, component);
	}

	private static void paintCarvedEdge(
		Graphics2D graphics,
		JComponent component,
		boolean pressed)
	{
		int right = component.getWidth() - 1;
		int bottom = component.getHeight() - 1;
		graphics.setColor(RuneholdTheme.OUTLINE);
		graphics.drawRect(0, 0, right, bottom);

		graphics.setColor(pressed ? RuneholdTheme.STONE_DARK : RuneholdTheme.STONE_LIGHT);
		graphics.drawLine(1, 1, right - 1, 1);
		graphics.drawLine(1, 1, 1, bottom - 1);
		graphics.setColor(pressed ? RuneholdTheme.STONE_LIGHT : RuneholdTheme.STONE_DARK);
		graphics.drawLine(1, bottom - 1, right - 1, bottom - 1);
		graphics.drawLine(right - 1, 1, right - 1, bottom - 1);
	}

	@Override
	protected void paintButtonPressed(Graphics graphics, AbstractButton button)
	{
		// The complete pressed state is painted in paint() with a hard inset bevel.
	}

	@Override
	protected void paintFocus(
		Graphics graphics,
		AbstractButton button,
		Rectangle viewRect,
		Rectangle textRect,
		Rectangle iconRect)
	{
		graphics.setColor(RuneholdTheme.ORANGE);
		BasicGraphicsUtils.drawDashedRect(
			graphics,
			3,
			3,
			Math.max(0, button.getWidth() - 6),
			Math.max(0, button.getHeight() - 6));
	}

	@Override
	protected void paintText(
		Graphics graphics,
		AbstractButton button,
		Rectangle textRect,
		String text)
	{
		FontMetrics metrics = graphics.getFontMetrics();
		int baseline = textRect.y + metrics.getAscent();
		int mnemonicIndex = button.getDisplayedMnemonicIndex();
		graphics.setColor(RuneholdTheme.OUTLINE);
		BasicGraphicsUtils.drawStringUnderlineCharAt(
			graphics,
			text,
			mnemonicIndex,
			textRect.x + 1,
			baseline + 1);
		graphics.setColor(button.isEnabled()
			? button.getForeground()
			: RuneholdTheme.TEXT_DISABLED);
		BasicGraphicsUtils.drawStringUnderlineCharAt(
			graphics,
			text,
			mnemonicIndex,
			textRect.x,
			baseline);
	}
}
