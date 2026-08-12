package com.runehold.ui.village;

import com.runehold.domain.BuildingCatalog;
import com.runehold.ui.RuneholdAssets;
import com.runehold.ui.RuneholdViewModel;
import com.runehold.ui.VillageResourceView;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

final class VillageHudPanel extends JPanel
{
	private final BuildingCatalog catalog;
	private final RuneholdAssets assets;

	VillageHudPanel(BuildingCatalog catalog, RuneholdAssets assets)
	{
		super(new BorderLayout());
		this.catalog = Objects.requireNonNull(catalog, "catalog");
		this.assets = Objects.requireNonNull(assets, "assets");
		setBackground(VillageTheme.HEADER);
		setBorder(VillageTheme.stoneBorder(8));
		setPreferredSize(new Dimension(10, 182));
	}

	void refresh(RuneholdViewModel viewModel)
	{
		removeAll();
		JLabel title = label("RUNEHOLD VILLAGE", 25f, VillageTheme.ORANGE, true);
		title.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 18));
		add(title, BorderLayout.NORTH);

		JPanel resources = new JPanel(new GridLayout(2, 3, 12, 10));
		resources.setOpaque(false);
		resources.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 8));
		for (VillageResourceView resource : viewModel.getResources())
		{
			resources.add(resourceChip(resource));
		}
		add(resources, BorderLayout.CENTER);

		if (viewModel.getConstructionJob() != null)
		{
			long seconds = Math.max(0, Duration.between(Instant.now(),
				viewModel.getConstructionJob().getCompletesAt()).getSeconds());
			JLabel builder = label(
				"Building "
					+ catalog.getDisplayName(viewModel.getConstructionJob().getBuildingType())
					+ " - " + seconds + "s",
				16f,
				VillageTheme.ORANGE,
				true);
			builder.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
			add(builder, BorderLayout.SOUTH);
		}
		revalidate();
		repaint();
	}

	private JPanel resourceChip(VillageResourceView resource)
	{
		JPanel chip = new JPanel(new BorderLayout(8, 0));
		chip.setOpaque(true);
		chip.setBackground(VillageTheme.PANEL);
		chip.setBorder(VillageTheme.stoneBorder(4));
		chip.setPreferredSize(new Dimension(220, 68));
		chip.setMinimumSize(new Dimension(180, 64));
		chip.setToolTipText(resource.getTooltip());

		JLabel icon = new JLabel(new VillageResourceIcon(resource.getKind()));
		icon.setPreferredSize(new Dimension(52, 50));
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		icon.setVerticalAlignment(SwingConstants.CENTER);
		icon.setToolTipText(resource.getTooltip());
		chip.add(icon, BorderLayout.WEST);

		JPanel text = new JPanel();
		text.setOpaque(false);
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		JLabel title = label(resource.getTitle(), 17f, VillageTheme.MUTED, true);
		JLabel amount = label(resource.getValue(), 17f, VillageTheme.TEXT, true);
		title.setToolTipText(resource.getTooltip());
		amount.setToolTipText(resource.getTooltip());
		text.add(title);
		text.add(amount);
		chip.add(text, BorderLayout.CENTER);
		return chip;
	}

	private JLabel label(String text, float size, java.awt.Color color, boolean bold)
	{
		JLabel label = new JLabel(text);
		label.setFont(bold ? assets.boldFont(size) : assets.regularFont(size));
		label.setForeground(color);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}
}
