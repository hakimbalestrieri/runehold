package com.runehold.ui;

import com.runehold.domain.BuildingType;
import com.runehold.domain.UpgradeResult;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import net.runelite.client.ui.ColorScheme;

final class BuildingRow extends JPanel
{
	BuildingRow(RuneholdViewModel.BuildingView building, Consumer<BuildingType> onUpgrade)
	{
		setLayout(new BorderLayout(0, 7));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.BORDER_COLOR),
			BorderFactory.createEmptyBorder(9, 9, 9, 9)));
		setMaximumSize(new Dimension(Integer.MAX_VALUE, 174));

		JPanel heading = new JPanel(new GridLayout(1, 2, 6, 0));
		heading.setOpaque(false);
		JLabel name = new JLabel(building.getName());
		name.setFont(name.getFont().deriveFont(Font.BOLD));
		name.setForeground(ColorScheme.TEXT_COLOR);
		JLabel level = new JLabel(building.getLevelText(), JLabel.RIGHT);
		level.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		heading.add(name);
		heading.add(level);
		add(heading, BorderLayout.NORTH);

		JPanel details = new JPanel(new BorderLayout(0, 5));
		details.setOpaque(false);
		JTextArea description = new JTextArea(building.getDescription());
		description.setEditable(false);
		description.setFocusable(false);
		description.setLineWrap(true);
		description.setWrapStyleWord(true);
		description.setOpaque(false);
		description.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		description.setFont(description.getFont().deriveFont(11f));
		description.setBorder(null);
		details.add(description, BorderLayout.CENTER);

		JLabel status = new JLabel(building.getStatusText());
		status.setForeground(statusColor(building.getStatus()));
		status.getAccessibleContext().setAccessibleName(
			building.getName() + " status: " + building.getStatusText());
		details.add(status, BorderLayout.SOUTH);
		add(details, BorderLayout.CENTER);

		JButton action = new JButton(building.getActionText());
		action.setEnabled(building.isActionEnabled());
		action.setToolTipText(building.getStatusText());
		action.getAccessibleContext().setAccessibleName(
			building.getActionText() + " for " + building.getName());
		action.getAccessibleContext().setAccessibleDescription(building.getStatusText());
		action.addActionListener(event -> onUpgrade.accept(building.getType()));
		add(action, BorderLayout.SOUTH);
	}

	private static Color statusColor(UpgradeResult.Status status)
	{
		switch (status)
		{
			case SUCCESS:
				return ColorScheme.PROGRESS_COMPLETE_COLOR;
			case INSUFFICIENT_MANA:
				return ColorScheme.PROGRESS_ERROR_COLOR;
			case MAX_LEVEL:
				return ColorScheme.BRAND_ORANGE;
			case LOCKED:
			default:
				return ColorScheme.LIGHT_GRAY_COLOR;
		}
	}
}
