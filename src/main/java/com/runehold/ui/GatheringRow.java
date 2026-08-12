package com.runehold.ui;

import com.runehold.domain.BuildingType;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

final class GatheringRow extends JPanel
{
	GatheringRow(
		RuneholdViewModel.GatheringSiteView site,
		RuneholdPanel.GatheringCommands commands,
		java.util.function.Consumer<BuildingType> onBuild,
		RuneholdAssets assets)
	{
		setLayout(new BorderLayout(0, 5));
		setBackground(RuneholdTheme.PANEL);
		setBorder(RuneholdTheme.stoneBorder(6));
		setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));
		setAlignmentX(LEFT_ALIGNMENT);

		// The level text needs only its preferred width; an even split would clip the
		// longer site names, which are the row's only identifier.
		JPanel heading = new JPanel(new BorderLayout(6, 0));
		heading.setOpaque(false);
		JLabel name = new JLabel(site.getName());
		RuneholdTheme.styleLabel(name);
		name.setFont(assets.boldFont(14f));
		name.setForeground(RuneholdTheme.ORANGE);
		name.setIconTextGap(4);
		name.setToolTipText(site.getName());
		assets.addGatheringIcon(site.getType(), name);
		JLabel level = new JLabel(site.getLevelText(), JLabel.RIGHT);
		RuneholdTheme.styleLabel(level);
		level.setFont(assets.regularFont(11f));
		level.setForeground(RuneholdTheme.TEXT_MUTED);
		heading.add(name, BorderLayout.CENTER);
		heading.add(level, BorderLayout.EAST);
		add(heading, BorderLayout.NORTH);

		JPanel details = new JPanel();
		details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
		details.setOpaque(false);
		details.add(detailLabel(site.getWorkersText(), RuneholdTheme.TEXT, assets));
		details.add(detailLabel(site.getStorageText(), RuneholdTheme.TEXT, assets));
		JLabel status = detailLabel(site.getStatusText(), statusColor(site.getStatus()), assets);
		status.getAccessibleContext().setAccessibleName(
			site.getName() + " status: " + site.getStatusText());
		details.add(status);
		add(details, BorderLayout.CENTER);

		if (!site.isBuilt())
		{
			JButton build = actionButton(site.getBuildActionText(), assets);
			build.setEnabled(site.isBuildEnabled());
			build.setToolTipText(site.getStatusText());
			build.getAccessibleContext().setAccessibleName(
				site.getBuildActionText() + " the " + site.getName());
			build.getAccessibleContext().setAccessibleDescription(site.getStatusText());
			build.addActionListener(event ->
			{
				build.setEnabled(false);
				onBuild.accept(site.getType());
			});
			add(build, BorderLayout.SOUTH);
			return;
		}

		JPanel actions = new JPanel(new BorderLayout(4, 0));
		actions.setOpaque(false);

		JPanel workerActions = new JPanel();
		workerActions.setLayout(new BoxLayout(workerActions, BoxLayout.X_AXIS));
		workerActions.setOpaque(false);
		JButton assign = actionButton("+", assets);
		assign.setEnabled(site.isAssignEnabled());
		assign.setToolTipText(site.getAssignActionText());
		assign.getAccessibleContext().setAccessibleName(
			site.getAssignActionText() + " at " + site.getName());
		assign.getAccessibleContext().setAccessibleDescription(site.getWorkersText());
		assign.addActionListener(event ->
		{
			String workerId = site.getAssignableWorkerId();
			if (workerId == null)
			{
				return;
			}
			assign.setEnabled(false);
			commands.assign(workerId, site.getType());
		});
		// ASCII only: the RuneScape faces are physical TTFs with no glyph fallback, so a
		// typographic minus sign would render as .notdef next to the "+".
		JButton release = actionButton("-", assets);
		release.setEnabled(site.isReleaseEnabled());
		release.setToolTipText(site.isReleaseEnabled()
			? "Recall a worker from " + site.getName()
			: "No worker to recall");
		release.getAccessibleContext().setAccessibleName(
			"Recall a worker from " + site.getName());
		release.getAccessibleContext().setAccessibleDescription(site.getWorkersText());
		release.addActionListener(event ->
		{
			String workerId = site.getReleasableWorkerId();
			if (workerId == null)
			{
				return;
			}
			release.setEnabled(false);
			commands.release(workerId);
		});
		workerActions.add(assign);
		workerActions.add(Box.createHorizontalStrut(4));
		workerActions.add(release);
		actions.add(workerActions, BorderLayout.WEST);

		JButton collect = actionButton(site.getCollectActionText(), assets);
		collect.setEnabled(site.isCollectEnabled());
		collect.setToolTipText(site.getStorageText());
		collect.getAccessibleContext().setAccessibleName(
			site.getCollectActionText() + " from " + site.getName());
		collect.getAccessibleContext().setAccessibleDescription(site.getStorageText());
		collect.addActionListener(event ->
		{
			collect.setEnabled(false);
			commands.collect(site.getType());
		});
		actions.add(collect, BorderLayout.CENTER);
		add(actions, BorderLayout.SOUTH);
	}

	private static JLabel detailLabel(String text, Color foreground, RuneholdAssets assets)
	{
		JLabel label = new JLabel(text);
		RuneholdTheme.styleLabel(label);
		label.setFont(assets.regularFont(11f));
		label.setForeground(foreground);
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	private static JButton actionButton(String text, RuneholdAssets assets)
	{
		JButton button = new JButton(text);
		button.setFont(assets.boldFont(12f));
		RuneholdTheme.styleActionButton(button);
		button.setPreferredSize(new Dimension(28, 24));
		return button;
	}

	private static Color statusColor(RuneholdViewModel.GatheringSiteView.Status status)
	{
		switch (status)
		{
			case PRODUCING:
				return RuneholdTheme.SUCCESS;
			case FULL:
				return RuneholdTheme.ORANGE;
			case BLOCKED:
				return RuneholdTheme.ERROR;
			case IDLE:
			case NOT_BUILT:
			default:
				return RuneholdTheme.TEXT_MUTED;
		}
	}
}
