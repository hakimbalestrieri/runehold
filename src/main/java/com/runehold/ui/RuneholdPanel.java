package com.runehold.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public final class RuneholdPanel extends PluginPanel
{
	private final RuneholdController controller;
	private final JPanel content = new JPanel();

	public RuneholdPanel(RuneholdController controller)
	{
		this.controller = controller;
		setLayout(new BorderLayout());
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(content, BorderLayout.NORTH);
		refresh();
	}

	public void refresh()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(this::refresh);
			return;
		}

		RuneholdViewModel viewModel = controller.getViewModel();
		content.removeAll();
		content.add(createHeader(viewModel));
		content.add(Box.createVerticalStrut(10));

		for (RuneholdViewModel.BuildingView building : viewModel.getBuildings())
		{
			content.add(new BuildingRow(building, type ->
			{
				controller.upgrade(type);
				refresh();
			}));
			content.add(Box.createVerticalStrut(8));
		}

		content.revalidate();
		content.repaint();
	}

	private static JPanel createHeader(RuneholdViewModel viewModel)
	{
		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

		JLabel title = new JLabel("RUNEHOLD");
		title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));
		title.setForeground(ColorScheme.BRAND_ORANGE);
		JLabel mana = new JLabel(viewModel.getManaText());
		mana.setFont(mana.getFont().deriveFont(Font.BOLD, 15f));
		mana.setForeground(ColorScheme.TEXT_COLOR);
		JLabel daily = new JLabel(viewModel.getDailyProgressText());
		daily.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		daily.getAccessibleContext().setAccessibleName(
			"Daily mana progress: " + viewModel.getDailyProgressText());

		header.add(title);
		header.add(Box.createVerticalStrut(7));
		header.add(mana);
		header.add(Box.createVerticalStrut(3));
		header.add(daily);
		return header;
	}
}
