package com.runehold.ui;

import com.runehold.domain.BuildingType;
import com.runehold.domain.UpgradeResult;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

final class BuildingRow extends JPanel
{
	BuildingRow(
		RuneholdViewModel.BuildingView building,
		Consumer<BuildingType> onUpgrade,
		RuneholdAssets assets)
	{
		setLayout(new BorderLayout(0, 5));
		setBackground(RuneholdTheme.PANEL);
		setBorder(RuneholdTheme.stoneBorder(6));
		setMaximumSize(new Dimension(Integer.MAX_VALUE, 146));
		setAlignmentX(LEFT_ALIGNMENT);

		JPanel heading = new JPanel(new GridLayout(1, 2, 6, 0));
		heading.setOpaque(false);
		JLabel name = new JLabel(building.getName());
		RuneholdTheme.styleLabel(name);
		name.setFont(assets.boldFont(14f));
		name.setForeground(RuneholdTheme.ORANGE);
		name.setIconTextGap(4);
		assets.addBuildingIcon(building.getType(), name);
		JLabel level = new JLabel(building.getLevelText(), JLabel.RIGHT);
		RuneholdTheme.styleLabel(level);
		level.setFont(assets.regularFont(11f));
		level.setForeground(RuneholdTheme.TEXT_MUTED);
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
		description.setForeground(RuneholdTheme.TEXT);
		description.setFont(assets.regularFont(11f));
		description.setRows(2);
		description.setBorder(null);
		details.add(description, BorderLayout.CENTER);

		JLabel status = new JLabel(building.getStatusText());
		RuneholdTheme.styleLabel(status);
		status.setFont(assets.regularFont(11f));
		status.setForeground(statusColor(building.getStatus()));
		status.getAccessibleContext().setAccessibleName(
			building.getName() + " status: " + building.getStatusText());
		details.add(status, BorderLayout.SOUTH);
		add(details, BorderLayout.CENTER);

		JButton action = new JButton(building.getActionText());
		action.setFont(assets.boldFont(12f));
		action.setIconTextGap(4);
		RuneholdTheme.styleActionButton(action);
		assets.addUpgradeIcon(action);
		action.setEnabled(building.isActionEnabled());
		action.setToolTipText(building.getStatusText());
		action.getAccessibleContext().setAccessibleName(
			building.getActionText() + " for " + building.getName());
		action.getAccessibleContext().setAccessibleDescription(building.getStatusText());
		action.addActionListener(event ->
		{
			action.setEnabled(false);
			onUpgrade.accept(building.getType());
		});
		add(action, BorderLayout.SOUTH);
	}

	private static Color statusColor(UpgradeResult.Status status)
	{
		switch (status)
		{
			case SUCCESS:
				return RuneholdTheme.SUCCESS;
			case INSUFFICIENT_MANA:
				return RuneholdTheme.ERROR;
			case MAX_LEVEL:
				return RuneholdTheme.ORANGE;
			case LOCKED:
			default:
				return RuneholdTheme.TEXT_MUTED;
		}
	}
}
