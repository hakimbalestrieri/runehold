package com.runehold.ui;

import com.runehold.domain.BuildingType;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.PluginPanel;

public final class RuneholdPanel extends PluginPanel
{
	/**
	 * Worker and collection commands issued by the gathering rows. Implementations are
	 * expected to marshal the work onto RuneLite's client thread.
	 */
	public interface GatheringCommands
	{
		void assign(String workerId, BuildingType site);

		void release(String workerId);

		void collect(BuildingType site);
	}

	private static final GatheringCommands NO_GATHERING_COMMANDS = new GatheringCommands()
	{
		@Override
		public void assign(String workerId, BuildingType site)
		{
		}

		@Override
		public void release(String workerId)
		{
		}

		@Override
		public void collect(BuildingType site)
		{
		}
	};

	private final Consumer<BuildingType> onUpgrade;
	private final Runnable onOpenVillage;
	private final GatheringCommands gatheringCommands;
	private final RuneholdAssets assets;
	private final JPanel content = new JPanel();

	public RuneholdPanel(
		RuneholdViewModel initialViewModel,
		Consumer<BuildingType> onUpgrade,
		RuneholdAssets assets)
	{
		this(initialViewModel, onUpgrade, () -> { }, assets);
	}

	public RuneholdPanel(
		RuneholdViewModel initialViewModel,
		Consumer<BuildingType> onUpgrade,
		Runnable onOpenVillage,
		RuneholdAssets assets)
	{
		this(initialViewModel, onUpgrade, onOpenVillage, NO_GATHERING_COMMANDS, assets);
	}

	public RuneholdPanel(
		RuneholdViewModel initialViewModel,
		Consumer<BuildingType> onUpgrade,
		Runnable onOpenVillage,
		GatheringCommands gatheringCommands,
		RuneholdAssets assets)
	{
		this.onUpgrade = Objects.requireNonNull(onUpgrade, "onUpgrade");
		this.onOpenVillage = Objects.requireNonNull(onOpenVillage, "onOpenVillage");
		this.gatheringCommands = Objects.requireNonNull(gatheringCommands, "gatheringCommands");
		this.assets = Objects.requireNonNull(assets, "assets");
		setLayout(new BorderLayout());
		setBackground(RuneholdTheme.BACKGROUND);
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(RuneholdTheme.BACKGROUND);
		content.setBorder(javax.swing.BorderFactory.createEmptyBorder(7, 7, 7, 7));
		add(content, BorderLayout.NORTH);
		refresh(Objects.requireNonNull(initialViewModel, "initialViewModel"));
	}

	public void refresh(RuneholdViewModel viewModel)
	{
		refresh(viewModel, null);
	}

	/**
	 * Rebuilds the panel and, when {@code notice} is set, shows why the last command was
	 * refused. A refused command must never be silent.
	 */
	public void refresh(RuneholdViewModel viewModel, String notice)
	{
		Objects.requireNonNull(viewModel, "viewModel");
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(() -> refresh(viewModel, notice));
			return;
		}

		content.removeAll();
		content.add(createHeader(viewModel));
		content.add(Box.createVerticalStrut(7));
		if (notice != null && !notice.trim().isEmpty())
		{
			content.add(createNotice(notice));
			content.add(Box.createVerticalStrut(7));
		}
		content.add(createVillageButton());
		content.add(Box.createVerticalStrut(7));

		for (RuneholdViewModel.BuildingView building : viewModel.getStructures())
		{
			content.add(new BuildingRow(building, onUpgrade, assets));
			content.add(Box.createVerticalStrut(6));
		}

		content.add(createSectionHeader("GATHERING SITES"));
		content.add(Box.createVerticalStrut(6));
		for (RuneholdViewModel.GatheringSiteView site : viewModel.getGatheringSites())
		{
			content.add(new GatheringRow(site, gatheringCommands, onUpgrade, assets));
			content.add(Box.createVerticalStrut(6));
		}

		content.revalidate();
		content.repaint();
	}

	private JTextArea createNotice(String notice)
	{
		JTextArea label = new JTextArea(notice);
		label.setEditable(false);
		label.setFocusable(false);
		label.setLineWrap(true);
		label.setWrapStyleWord(true);
		label.setOpaque(true);
		label.setBackground(RuneholdTheme.PANEL);
		label.setBorder(RuneholdTheme.stoneBorder(6));
		label.setFont(assets.regularFont(11f));
		label.setForeground(RuneholdTheme.ERROR);
		label.setAlignmentX(LEFT_ALIGNMENT);
		label.getAccessibleContext().setAccessibleName("Last action: " + notice);
		return label;
	}

	private JLabel createSectionHeader(String text)
	{
		JLabel header = new JLabel(text);
		RuneholdTheme.styleLabel(header);
		header.setFont(assets.boldFont(13f));
		header.setForeground(RuneholdTheme.ORANGE);
		header.setAlignmentX(LEFT_ALIGNMENT);
		header.getAccessibleContext().setAccessibleName(text + " section");
		return header;
	}

	private JButton createVillageButton()
	{
		JButton openVillage = new JButton("Open village");
		openVillage.setFont(assets.boldFont(13f));
		RuneholdTheme.styleActionButton(openVillage);
		openVillage.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
		openVillage.setAlignmentX(LEFT_ALIGNMENT);
		openVillage.setToolTipText("Open the local Runehold village map");
		openVillage.getAccessibleContext().setAccessibleName("Open Runehold village");
		openVillage.addActionListener(event -> onOpenVillage.run());
		return openVillage;
	}

	private JPanel createHeader(RuneholdViewModel viewModel)
	{
		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setBackground(RuneholdTheme.HEADER);
		header.setBorder(RuneholdTheme.stoneBorder(7));
		header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));
		header.setAlignmentX(LEFT_ALIGNMENT);

		JLabel title = new JLabel("RUNEHOLD");
		RuneholdTheme.styleLabel(title);
		title.setFont(assets.boldFont(18f));
		title.setForeground(RuneholdTheme.ORANGE);
		JLabel mana = new JLabel(viewModel.getManaText());
		RuneholdTheme.styleLabel(mana);
		mana.setFont(assets.boldFont(14f));
		mana.setForeground(RuneholdTheme.TEXT);
		mana.setIconTextGap(6);
		assets.addManaIcon(mana);
		JLabel daily = new JLabel(viewModel.getDailyProgressText());
		RuneholdTheme.styleLabel(daily);
		daily.setFont(assets.regularFont(12f));
		daily.setForeground(RuneholdTheme.TEXT_MUTED);
		daily.getAccessibleContext().setAccessibleName(
			"Daily mana progress: " + viewModel.getDailyProgressText());

		header.add(title);
		header.add(Box.createVerticalStrut(4));
		header.add(mana);
		header.add(Box.createVerticalStrut(2));
		header.add(daily);
		return header;
	}
}
