package com.runehold.ui.village;

import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.BuildingType;
import com.runehold.domain.layout.GridPoint;
import com.runehold.ui.RuneholdAssets;
import com.runehold.ui.RuneholdViewModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

public final class VillageWindow
{
	public interface Commands
	{
		void build(BuildingType type, GridPoint destination);

		void move(BuildingType type, GridPoint destination);

		void upgrade(BuildingType type);

		void collectManaGrove();

		void completeConstruction();
	}

	private final JFrame frame;
	private final JPanel root = new JPanel(new BorderLayout());
	private final VillageHudPanel hud;
	private final JPanel bottom = new JPanel(new BorderLayout());
	private final JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 4));
	private final JLabel status = new JLabel();
	private final VillageBuildCatalogPanel catalogPanel;
	private final BuildingCatalog catalog;
	private final RuneholdAssets assets;
	private final Commands commands;
	private final VillageCanvas canvas;
	private RuneholdViewModel viewModel;
	private VillageAnimationSettings animationSettings;
	private boolean catalogVisible;
	private VillageInteractionModel.Mode lastMode;
	private BuildingType lastSelected;
	private JButton confirmButton;
	private final Timer constructionTimer;
	private final Timer animationTimer;

	public VillageWindow(
		RuneholdViewModel initialViewModel,
		BuildingCatalog catalog,
		RuneholdAssets assets)
	{
		this(initialViewModel, catalog, assets, new NoOpCommands());
	}

	public VillageWindow(
		RuneholdViewModel initialViewModel,
		BuildingCatalog catalog,
		RuneholdAssets assets,
		Commands commands)
	{
		this(initialViewModel, catalog, assets, commands, VillageAnimationSettings.defaults());
	}

	public VillageWindow(
		RuneholdViewModel initialViewModel,
		BuildingCatalog catalog,
		RuneholdAssets assets,
		Commands commands,
		VillageAnimationSettings animationSettings)
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			throw new IllegalStateException("VillageWindow must be created on the EDT");
		}
		viewModel = Objects.requireNonNull(initialViewModel, "initialViewModel");
		this.catalog = Objects.requireNonNull(catalog, "catalog");
		this.assets = Objects.requireNonNull(assets, "assets");
		this.commands = Objects.requireNonNull(commands, "commands");
		this.animationSettings = Objects.requireNonNull(animationSettings, "animationSettings");
		canvas = new VillageCanvas(viewModel, catalog, assets, new CanvasListener());
		canvas.setAnimationSettings(this.animationSettings);
		hud = new VillageHudPanel(catalog, assets);
		catalogPanel = new VillageBuildCatalogPanel(
			catalog,
			assets,
			canvas::beginPlacement,
			commands::upgrade,
			() -> setCatalogVisible(false));

		root.setBackground(VillageTheme.BACKGROUND);
		root.add(canvas, BorderLayout.CENTER);
		root.add(hud, BorderLayout.NORTH);
		root.add(bottom, BorderLayout.SOUTH);
		hud.refresh(viewModel);
		catalogPanel.refresh(viewModel);
		configureBottom();

		frame = new JFrame("Runehold Village - Builder");
		frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		frame.setContentPane(root);
		frame.setMinimumSize(new Dimension(760, 520));
		frame.setPreferredSize(new Dimension(1060, 720));
		frame.setResizable(true);
		frame.pack();
		frame.setLocationByPlatform(true);
		constructionTimer = new Timer(1_000, event -> commands.completeConstruction());
		animationTimer = new Timer(this.animationSettings.frameMillis(),
			event -> canvas.advanceAnimation(System.currentTimeMillis()));
		frame.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent event)
			{
				stopAnimationTimers();
			}
		});
	}

	public void showWindow()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(this::showWindow);
			return;
		}
		frame.setVisible(true);
		if (!constructionTimer.isRunning())
		{
			constructionTimer.start();
		}
		startAnimationTimer();
		frame.toFront();
		canvas.requestFocusInWindow();
	}

	public void updateAnimationSettings(VillageAnimationSettings settings)
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(() -> updateAnimationSettings(settings));
			return;
		}
		animationSettings = Objects.requireNonNull(settings, "settings");
		canvas.setAnimationSettings(animationSettings);
		animationTimer.setDelay(animationSettings.frameMillis());
		animationTimer.setInitialDelay(animationSettings.frameMillis());
		startAnimationTimer();
	}

	public void beginPlacement(BuildingType type)
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(() -> beginPlacement(type));
			return;
		}
		showWindow();
		canvas.beginPlacement(type);
	}

	public void refresh(RuneholdViewModel updatedViewModel)
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(() -> refresh(updatedViewModel));
			return;
		}
		viewModel = Objects.requireNonNull(updatedViewModel, "updatedViewModel");
		canvas.refresh(viewModel);
		hud.refresh(viewModel);
		catalogPanel.refresh(viewModel);
		lastMode = null;
		updateBottomBar();
	}

	public void close()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(this::close);
			return;
		}
		frame.dispose();
		stopAnimationTimers();
	}

	private void startAnimationTimer()
	{
		if (animationSettings.isEnabled() && frame.isVisible())
		{
			if (!animationTimer.isRunning())
			{
				animationTimer.start();
			}
		}
		else
		{
			animationTimer.stop();
		}
	}

	private void stopAnimationTimers()
	{
		constructionTimer.stop();
		animationTimer.stop();
	}

	private void configureBottom()
	{
		bottom.setBackground(VillageTheme.PANEL);
		bottom.setBorder(VillageTheme.stoneBorder(4));
		bottom.setPreferredSize(new Dimension(10, 58));
		actionBar.setOpaque(false);
		status.setFont(assets.regularFont(12f));
		status.setForeground(VillageTheme.MUTED);
		status.setHorizontalAlignment(SwingConstants.RIGHT);
		status.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 6));
		bottom.add(actionBar, BorderLayout.WEST);
		bottom.add(status, BorderLayout.CENTER);
		updateBottomBar();
	}

	private void updateBottomBar()
	{
		status.setText(canvas.getStatusText());
		VillageInteractionModel.Mode mode = canvas.getMode();
		BuildingType selected = canvas.getSelectedType();
		if (mode == lastMode && selected == lastSelected)
		{
			if (confirmButton != null)
			{
				confirmButton.setEnabled(canvas.isActivePlacementValid());
			}
			return;
		}

		lastMode = mode;
		lastSelected = selected;
		actionBar.removeAll();
		confirmButton = null;
		if (mode == VillageInteractionModel.Mode.PLACE
			|| mode == VillageInteractionModel.Mode.MOVE)
		{
			confirmButton = button("Confirm", "Confirm the highlighted position");
			confirmButton.setForeground(VillageTheme.VALID);
			confirmButton.setEnabled(canvas.isActivePlacementValid());
			confirmButton.addActionListener(event -> canvas.confirmInteraction());
			JButton cancel = button("Cancel", "Cancel without changing the village");
			cancel.setForeground(VillageTheme.ERROR);
			cancel.addActionListener(event -> canvas.cancelInteraction());
			actionBar.add(confirmButton);
			actionBar.add(cancel);
			actionBar.add(label(mode == VillageInteractionModel.Mode.PLACE
				? "PLACEMENT" : "MOVEMENT", 12f, VillageTheme.ORANGE, true));
		}
		else
		{
			JButton build = button("Build", "Open the construction catalogue");
			build.addActionListener(event -> toggleCatalog());
			JButton edit = button(mode == VillageInteractionModel.Mode.EDIT
				? "Finish edit" : "Edit", "Toggle village edit mode");
			edit.addActionListener(event -> canvas.toggleEditMode());
			JButton recenter = button("Recenter", "Reset camera position and zoom");
			recenter.addActionListener(event -> canvas.recenter());
			JButton zoomOut = button("-", "Zoom out");
			zoomOut.addActionListener(event -> canvas.zoomBy(-0.1));
			JButton zoomIn = button("+", "Zoom in");
			zoomIn.addActionListener(event -> canvas.zoomBy(0.1));
			actionBar.add(build);
			actionBar.add(edit);
			actionBar.add(recenter);
			actionBar.add(zoomOut);
			actionBar.add(label(Math.round(canvas.getZoom() * 100) + "%",
				11f, VillageTheme.MUTED, true));
			actionBar.add(zoomIn);

			if (selected != null)
			{
				RuneholdViewModel.BuildingView building = viewModel.getBuilding(selected);
				actionBar.add(Box.createHorizontalStrut(8));
				actionBar.add(label(building.getName() + " L" + building.getCurrentLevel(),
					12f, VillageTheme.ORANGE, true));
				JButton info = button("Info", "Show this structure's details");
				info.addActionListener(event -> canvas.showSelectedInfo());
				JButton move = button("Move", "Move this structure for free");
				move.addActionListener(event -> canvas.beginMoveSelected());
				actionBar.add(info);
				actionBar.add(move);
				if (building.getCurrentLevel() < catalog.getMaxLevel(selected))
				{
					JButton upgrade = button(building.getActionText(), building.getStatusText());
					upgrade.setEnabled(building.isActionEnabled());
					upgrade.addActionListener(event -> commands.upgrade(selected));
					actionBar.add(upgrade);
				}
				if (selected == BuildingType.MANA_GROVE && building.getCurrentLevel() > 0)
				{
					JButton collect = button("Collect " + viewModel.getCollectableGroveMana(),
						"Collect mana produced by the grove");
					collect.setEnabled(viewModel.getCollectableGroveMana() > 0);
					collect.addActionListener(event -> commands.collectManaGrove());
					actionBar.add(collect);
				}
			}
		}
		actionBar.revalidate();
		actionBar.repaint();
	}

	private void toggleCatalog()
	{
		setCatalogVisible(!catalogVisible);
	}

	private void setCatalogVisible(boolean visible)
	{
		catalogVisible = visible;
		if (visible)
		{
			root.add(catalogPanel, BorderLayout.EAST);
		}
		else
		{
			root.remove(catalogPanel);
		}
		root.revalidate();
		root.repaint();
		canvas.requestFocusInWindow();
	}

	private JButton button(String text, String tooltip)
	{
		JButton button = new JButton(text);
		button.setFont(assets.boldFont(14f));
		button.setToolTipText(tooltip);
		VillageTheme.styleButton(button);
		return button;
	}

	private JLabel label(String text, float size, java.awt.Color color, boolean bold)
	{
		JLabel label = new JLabel(text);
		label.setFont(bold ? assets.boldFont(size) : assets.regularFont(size));
		label.setForeground(color);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	private final class CanvasListener implements VillageCanvas.Listener
	{
		@Override
		public void selectionChanged(BuildingType selectedType)
		{
			updateBottomBar();
		}

		@Override
		public void interactionChanged()
		{
			updateBottomBar();
		}

		@Override
		public void confirmBuild(BuildingType type, GridPoint destination)
		{
			commands.build(type, destination);
		}

		@Override
		public void confirmMove(BuildingType type, GridPoint destination)
		{
			commands.move(type, destination);
		}
	}

	private static final class NoOpCommands implements Commands
	{
		@Override
		public void build(BuildingType type, GridPoint destination)
		{
		}

		@Override
		public void move(BuildingType type, GridPoint destination)
		{
		}

		@Override
		public void upgrade(BuildingType type)
		{
		}

		@Override
		public void collectManaGrove()
		{
		}

		@Override
		public void completeConstruction()
		{
		}
	}
}
