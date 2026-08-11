package com.runehold.ui;

import com.runehold.domain.BuildingType;
import java.awt.BorderLayout;
import java.util.Objects;
import java.util.function.Consumer;
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
	private final Consumer<BuildingType> onUpgrade;
	private final RuneholdAssets assets;
	private final JPanel content = new JPanel();

	public RuneholdPanel(
		RuneholdViewModel initialViewModel,
		Consumer<BuildingType> onUpgrade,
		RuneholdAssets assets)
	{
		this.onUpgrade = Objects.requireNonNull(onUpgrade, "onUpgrade");
		this.assets = Objects.requireNonNull(assets, "assets");
		setLayout(new BorderLayout());
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);
		content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(content, BorderLayout.NORTH);
		refresh(Objects.requireNonNull(initialViewModel, "initialViewModel"));
	}

	public void refresh(RuneholdViewModel viewModel)
	{
		Objects.requireNonNull(viewModel, "viewModel");
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(() -> refresh(viewModel));
			return;
		}

		content.removeAll();
		content.add(createHeader(viewModel));
		content.add(Box.createVerticalStrut(10));

		for (RuneholdViewModel.BuildingView building : viewModel.getBuildings())
		{
			content.add(new BuildingRow(building, onUpgrade, assets));
			content.add(Box.createVerticalStrut(8));
		}

		content.revalidate();
		content.repaint();
	}

	private JPanel createHeader(RuneholdViewModel viewModel)
	{
		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

		JLabel title = new JLabel("RUNEHOLD");
		title.setFont(assets.boldFont(20f));
		title.setForeground(ColorScheme.BRAND_ORANGE);
		JLabel mana = new JLabel(viewModel.getManaText());
		mana.setFont(assets.boldFont(16f));
		mana.setForeground(ColorScheme.TEXT_COLOR);
		mana.setIconTextGap(6);
		assets.addManaIcon(mana);
		JLabel daily = new JLabel(viewModel.getDailyProgressText());
		daily.setFont(assets.regularFont(14f));
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
