package com.runehold.ui.village;

import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.BuildingType;
import com.runehold.domain.layout.BuildingPlacement;
import com.runehold.domain.layout.Footprint;
import com.runehold.domain.layout.GridPoint;
import com.runehold.domain.layout.PlacementResult;
import com.runehold.domain.layout.VillageLayout;
import com.runehold.ui.RuneholdAssets;
import com.runehold.ui.RuneholdViewModel;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.accessibility.AccessibleContext;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public final class VillageCanvas extends JComponent
{
	public interface Listener
	{
		void selectionChanged(BuildingType selectedType);

		void interactionChanged();

		void confirmBuild(BuildingType type, GridPoint destination);

		void confirmMove(BuildingType type, GridPoint destination);
	}

	public static final int NATIVE_WIDTH = 768;
	public static final int NATIVE_HEIGHT = 512;
	public static final double MIN_ZOOM = 0.75;
	public static final double MAX_ZOOM = 1.75;
	private static final int MIN_TILE_WIDTH = 22;
	private static final int MAX_TILE_WIDTH = 68;

	private static final Color VOID = new Color(0x17130E);
	private static final Color OUTLINE = new Color(0x1B1711);
	private static final Color GRASS_A = new Color(0x56643A);
	private static final Color GRASS_B = new Color(0x606C3C);
	private static final Color GRASS_C = new Color(0x4E5D34);
	private static final Color GRID = new Color(0x3E492D);
	private static final Color DIRT = new Color(0x765C36);
	private static final Color CLIFF_TOP = new Color(0x716346);
	private static final Color CLIFF_SIDE = new Color(0x443924);
	private static final Color CLIFF_DARK = new Color(0x2B2418);
	private static final Color VALID = new Color(0x87B653);
	private static final Color INVALID = new Color(0xC95C4B);
	private static final Color SELECTED = new Color(0xF2C94C);
	private static final Color ORANGE = new Color(0xFF981F);

	private final BuildingCatalog catalog;
	private final RuneholdAssets assets;
	private final VillageSpriteAtlas sprites;
	private final VillageActorSprites actorSprites = new VillageActorSprites();
	private final VillageAnimationModel animations;
	private final VillageInteractionModel interaction = new VillageInteractionModel();
	private final Listener listener;
	private VillageAnimationSettings animationSettings = VillageAnimationSettings.defaults();
	private RuneholdViewModel viewModel;
	private VillageLayout layout;
	private GridPoint hoveredTile;
	private double zoom = 1.0;
	private int panX;
	private int panY;
	private Point dragOrigin;
	private int panOriginX;
	private int panOriginY;
	private int animationFrame;
	private boolean dragging;
	private boolean canPan;
	private String statusText = "Select a building or open Build.";

	public VillageCanvas(
		RuneholdViewModel initialViewModel,
		BuildingCatalog catalog,
		RuneholdAssets assets)
	{
		this(initialViewModel, catalog, assets, new NoOpListener());
	}

	public VillageCanvas(
		RuneholdViewModel initialViewModel,
		BuildingCatalog catalog,
		RuneholdAssets assets,
		Listener listener)
	{
		this.catalog = Objects.requireNonNull(catalog, "catalog");
		this.assets = Objects.requireNonNull(assets, "assets");
		this.listener = Objects.requireNonNull(listener, "listener");
		sprites = new VillageSpriteAtlas();
		animations = new VillageAnimationModel(catalog);
		setPreferredSize(new Dimension(NATIVE_WIDTH, NATIVE_HEIGHT));
		setMinimumSize(new Dimension(520, 340));
		setFocusable(true);
		setOpaque(true);
		installMouseControls();
		installKeyboardControls();
		refresh(initialViewModel);
	}

	public void refresh(RuneholdViewModel updatedViewModel)
	{
		viewModel = Objects.requireNonNull(updatedViewModel, "updatedViewModel");
		layout = VillageLayout.restore(catalog, viewModel.getBuildingPositions());
		animations.refresh(viewModel, layout, animationSettings);
		int builtCount = layout.getPlacements().size();
		getAccessibleContext().setAccessibleName(
			"Interactive Runehold village with " + builtCount
				+ (builtCount == 1 ? " building" : " buildings"));
		repaint();
	}

	public void setAnimationSettings(VillageAnimationSettings settings)
	{
		animationSettings = Objects.requireNonNull(settings, "settings");
		animations.refresh(viewModel, layout, animationSettings);
		repaint();
	}

	public void advanceAnimation(long nowMillis)
	{
		if (animations.advance(nowMillis, animationSettings))
		{
			animationFrame++;
			repaint();
		}
	}

	public VillageInteractionModel.Mode getMode()
	{
		return interaction.getMode();
	}

	public BuildingType getSelectedType()
	{
		return interaction.getSelectedType();
	}

	public BuildingType getActiveType()
	{
		return interaction.getActiveType();
	}

	public GridPoint getGhostPosition()
	{
		return interaction.getGhostPosition();
	}

	public String getStatusText()
	{
		return statusText;
	}

	public double getZoom()
	{
		return zoom;
	}

	public void beginPlacement(BuildingType type)
	{
		if (viewModel.getBuilding(type).getCurrentLevel() > 0)
		{
			statusText = catalog.getDisplayName(type) + " is already built.";
			fireInteractionChanged();
			return;
		}
		GridPoint start = hoveredTile;
		if (start == null
			|| !layout.previewPlace(type, start).isSuccess()
			|| !spriteFitsTerrain(type, start))
		{
			start = findFirstVisuallySafePosition(type);
		}
		if (start == null)
		{
			statusText = "No free area for " + catalog.getDisplayName(type) + ".";
			fireInteractionChanged();
			return;
		}
		interaction.beginPlacement(type, start);
		statusText = "Place " + catalog.getDisplayName(type)
			+ " - click a tile, then Confirm.";
		setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		fireInteractionChanged();
	}

	public void beginMoveSelected()
	{
		BuildingType selected = interaction.getSelectedType();
		if (selected == null || viewModel.getBuildingPositions().get(selected) == null)
		{
			statusText = "Select a built structure first.";
			fireInteractionChanged();
			return;
		}
		interaction.beginMove(selected, viewModel.getBuildingPositions().get(selected));
		statusText = "Move " + catalog.getDisplayName(selected)
			+ " - choose a new tile, then Confirm.";
		setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		fireInteractionChanged();
	}

	public void showSelectedInfo()
	{
		BuildingType selected = interaction.getSelectedType();
		if (selected == null)
		{
			statusText = "Select a structure first.";
		}
		else
		{
			RuneholdViewModel.BuildingView building = viewModel.getBuilding(selected);
			statusText = building.getName() + " - " + building.getDescription()
				+ " - " + building.getFootprint().getWidth() + "x"
				+ building.getFootprint().getHeight() + " footprint.";
		}
		fireInteractionChanged();
	}

	public void toggleEditMode()
	{
		interaction.toggleEdit();
		statusText = interaction.getMode() == VillageInteractionModel.Mode.EDIT
			? "Edit mode - select a structure to move it."
			: "Edit mode closed.";
		fireInteractionChanged();
	}

	public void confirmInteraction()
	{
		BuildingType type = interaction.getActiveType();
		GridPoint destination = interaction.getGhostPosition();
		if (type == null || destination == null)
		{
			return;
		}

		PlacementResult result = interaction.getMode() == VillageInteractionModel.Mode.PLACE
			? layout.previewPlace(type, destination)
			: layout.previewMove(type, destination);
		if (!result.isSuccess())
		{
			statusText = placementError(result);
			fireInteractionChanged();
			return;
		}
		if (interaction.getMode() == VillageInteractionModel.Mode.PLACE)
		{
			listener.confirmBuild(type, destination);
			statusText = catalog.getDisplayName(type) + " construction ordered.";
		}
		else
		{
			listener.confirmMove(type, destination);
			statusText = catalog.getDisplayName(type) + " moved.";
		}
		interaction.finish();
		setCursor(Cursor.getDefaultCursor());
		fireInteractionChanged();
	}

	public void cancelInteraction()
	{
		if (interaction.getMode() != VillageInteractionModel.Mode.PLACE
			&& interaction.getMode() != VillageInteractionModel.Mode.MOVE)
		{
			return;
		}
		interaction.cancel();
		statusText = "Action cancelled - village unchanged.";
		setCursor(Cursor.getDefaultCursor());
		fireInteractionChanged();
	}

	public void recenter()
	{
		zoom = 1.0;
		panX = 0;
		panY = 0;
		statusText = "Camera recentered.";
		repaint();
		listener.interactionChanged();
	}

	public void zoomBy(double amount)
	{
		zoom = clamp(zoom + amount, MIN_ZOOM, MAX_ZOOM);
		statusText = "Zoom " + Math.round(zoom * 100) + "%";
		repaint();
		listener.interactionChanged();
	}

	public boolean isActivePlacementValid()
	{
		BuildingType type = interaction.getActiveType();
		GridPoint position = interaction.getGhostPosition();
		if (type == null || position == null)
		{
			return false;
		}
		PlacementResult result = interaction.getMode() == VillageInteractionModel.Mode.PLACE
			? layout.previewPlace(type, position)
			: layout.previewMove(type, position);
		return result.isSuccess();
	}

	@Override
	public AccessibleContext getAccessibleContext()
	{
		if (accessibleContext == null)
		{
			accessibleContext = new AccessibleVillageCanvas();
		}
		return accessibleContext;
	}

	protected final class AccessibleVillageCanvas extends AccessibleJComponent
	{
		private static final long serialVersionUID = 1L;
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		g.setRenderingHint(
			RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		g.setRenderingHint(
			RenderingHints.KEY_INTERPOLATION,
			RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.setColor(VOID);
		g.fillRect(0, 0, getWidth(), getHeight());

		IsometricProjection projection = projection();
		drawTerrainSides(g, projection);
		drawTerrain(g, projection);
		drawSelection(g, projection);
		drawBuildings(g, projection);
		g.dispose();
	}

	private void drawTerrainSides(Graphics2D g, IsometricProjection projection)
	{
		int depth = Math.max(7, projection.getTileHeight() / 2);
		for (int index = 0; index < VillageLayout.COLUMNS; index++)
		{
			Point north = projection.toScreen(index, VillageLayout.ROWS);
			Point east = projection.toScreen(index + 1, VillageLayout.ROWS);
			Polygon side = new Polygon(
				new int[]{north.x, east.x, east.x, north.x},
				new int[]{north.y, east.y, east.y + depth, north.y + depth}, 4);
			g.setColor(index % 2 == 0 ? CLIFF_SIDE : CLIFF_DARK);
			g.fillPolygon(side);
			g.setColor(OUTLINE);
			g.drawPolygon(side);
		}
		for (int index = 0; index < VillageLayout.ROWS; index++)
		{
			Point north = projection.toScreen(VillageLayout.COLUMNS, index);
			Point east = projection.toScreen(VillageLayout.COLUMNS, index + 1);
			Polygon side = new Polygon(
				new int[]{north.x, east.x, east.x, north.x},
				new int[]{north.y, east.y, east.y + depth, north.y + depth}, 4);
			g.setColor(index % 2 == 0 ? CLIFF_TOP : CLIFF_SIDE);
			g.fillPolygon(side);
			g.setColor(OUTLINE);
			g.drawPolygon(side);
		}
	}

	private void drawTerrain(Graphics2D g, IsometricProjection projection)
	{
		for (int sum = 0; sum <= VillageLayout.COLUMNS + VillageLayout.ROWS - 2; sum++)
		{
			for (int x = 0; x < VillageLayout.COLUMNS; x++)
			{
				int y = sum - x;
				if (y < 0 || y >= VillageLayout.ROWS)
				{
					continue;
				}
				Polygon tile = tile(projection.toScreen(x, y), projection);
				g.setColor(terrainColor(x, y));
				g.fillPolygon(tile);
				g.setColor(GRID);
				g.drawPolygon(tile);
				drawTerrainDetail(g, tile, x, y);
			}
		}
	}

	private void drawTerrainDetail(Graphics2D g, Polygon tile, int x, int y)
	{
		int hash = Math.abs(x * 47 + y * 83 + x * y * 11);
		int centerX = (tile.xpoints[0] + tile.xpoints[2]) / 2;
		int centerY = (tile.ypoints[0] + tile.ypoints[2]) / 2;
		if (hash % 23 == 0)
		{
			g.setColor(new Color(0x8A8250));
			g.fillRect(centerX - 1, centerY - 2, 2, 2);
			g.setColor(new Color(0xD1B14C));
			g.fillRect(centerX + 1, centerY - 3, 1, 1);
		}
		else if (hash % 19 == 0)
		{
			g.setColor(new Color(0x403C2D));
			g.fillPolygon(new int[]{centerX - 2, centerX + 2, centerX + 3, centerX},
				new int[]{centerY + 1, centerY - 1, centerY + 2, centerY + 3}, 4);
		}
	}

	private void drawSelection(Graphics2D g, IsometricProjection projection)
	{
		BuildingType selected = interaction.getSelectedType();
		if (selected == null || interaction.getMode() == VillageInteractionModel.Mode.MOVE)
		{
			return;
		}
		GridPoint position = viewModel.getBuildingPositions().get(selected);
		if (position != null)
		{
			drawFootprint(g, projection, position, catalog.getFootprint(selected), SELECTED, false);
		}
	}

	private void drawBuildings(Graphics2D g, IsometricProjection projection)
	{
		List<RenderItem> items = new ArrayList<>();
		for (BuildingPlacement placement : layout.getPlacements().values())
		{
			if (interaction.getMode() == VillageInteractionModel.Mode.MOVE
				&& placement.getType() == interaction.getActiveType())
			{
				continue;
			}
			items.add(new RenderItem(
				placement.getType(),
				placement.getPosition(),
				visualLevel(placement.getType()),
				false,
				isUnderConstruction(placement.getType())));
		}
		for (VillageActor actor : animations.getActors())
		{
			items.add(new RenderItem(actor));
		}
		if ((interaction.getMode() == VillageInteractionModel.Mode.PLACE
			|| interaction.getMode() == VillageInteractionModel.Mode.MOVE)
			&& interaction.getGhostPosition() != null)
		{
			items.add(new RenderItem(
				interaction.getActiveType(),
				interaction.getGhostPosition(),
				visualLevel(interaction.getActiveType()),
				true,
				false));
		}
		items.sort(Comparator.comparingInt(this::depthOf));

		for (RenderItem item : items)
		{
			if (item.actor == null)
			{
				drawBuilding(g, projection, item);
			}
			else
			{
				drawActor(g, projection, item.actor);
			}
		}
	}

	private void drawActor(Graphics2D g, IsometricProjection projection, VillageActor actor)
	{
		Point top = projection.toScreen(actor.getPosition());
		int centerX = top.x;
		int footY = top.y + projection.getTileHeight() / 2 + 4;
		int targetHeight = Math.max(18, projection.getTileHeight() + 12);
		BufferedImage sprite = actorSprites.get(actor, targetHeight, animationFrame);
		int drawX = centerX - sprite.getWidth() / 2;
		int drawY = footY - sprite.getHeight();

		g.setColor(new Color(0x12, 0x0E, 0x09, 150));
		g.fillOval(centerX - projection.getTileWidth() / 7, footY - 3,
			projection.getTileWidth() / 4, 5);
		g.drawImage(sprite, drawX, drawY, null);
		if (actor.getRole() == VillageActor.Role.WORKER
			&& actor.getPose() == VillageActor.Pose.WORK
			&& (animationFrame & 1) == 0)
		{
			g.setColor(new Color(0xBBA468));
			g.fillRect(centerX + 8, drawY + 9, 2, 2);
			g.fillRect(centerX + 12, drawY + 13, 1, 1);
		}
	}

	private void drawBuilding(Graphics2D g, IsometricProjection projection, RenderItem item)
	{
		Footprint footprint = catalog.getFootprint(item.type);
		if (item.ghost)
		{
			boolean valid = isActivePlacementValid();
			drawFootprint(g, projection, item.position, footprint,
				valid ? VALID : INVALID, true);
		}
		else
		{
			drawFoundation(g, projection, item.position, footprint);
		}

		Rectangle bounds = spriteBounds(projection, item.type, item.position);
		int centerX = footprintCenterX(projection, item.type, item.position);
		BufferedImage sprite = sprites.get(item.type, item.level, bounds.width);
		int drawX = bounds.x;
		int drawY = bounds.y;

		Composite oldComposite = g.getComposite();
		if (item.ghost)
		{
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.58f));
		}
		else if (item.construction)
		{
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.72f));
		}
		g.drawImage(sprite, drawX, drawY, null);
		g.setComposite(oldComposite);
		if (!item.ghost)
		{
			drawAmbientBuildingEffect(g, item.type, centerX, drawY, sprite.getWidth(), sprite.getHeight());
		}
		if (item.construction)
		{
			drawConstructionFrame(g, drawX, drawY, sprite.getWidth(), sprite.getHeight());
			drawBoxedText(g, "Building L" + item.level, centerX, Math.max(17, drawY - 3), ORANGE);
		}

		if (!item.ghost && !item.construction && item.type == interaction.getSelectedType())
		{
			drawBuildingLabel(g, item.type, centerX, drawY - 3);
		}
		if (!item.ghost && !item.construction
			&& item.type == BuildingType.MANA_GROVE
			&& viewModel.getCollectableGroveMana() > 0)
		{
			drawManaReadyIcon(g, centerX + sprite.getWidth() / 4, drawY + 8);
		}
		if (item.ghost)
		{
			drawGhostLabel(g, item.type, centerX, drawY - 3, activePreview());
		}
	}

	private void drawAmbientBuildingEffect(
		Graphics2D g,
		BuildingType type,
		int centerX,
		int y,
		int width,
		int height)
	{
		if (!animationSettings.isEnabled() || animationSettings.isReduceMotion())
		{
			return;
		}
		int tick = animationFrame & 3;
		switch (type)
		{
			case MANA_WELL:
				g.setColor(new Color(tick < 2 ? 0x61A6EA : 0x3976B4));
				g.fillRect(centerX - width / 10, y + height * 3 / 5, width / 5, 2);
				break;
			case RUNE_BANNER:
				g.setColor(new Color(tick < 2 ? 0x4B8FE4 : 0x2F6EAF));
				g.fillRect(centerX + 2, y + height / 4 + tick % 2, Math.max(3, width / 5), 3);
				break;
			case WORKSHOP:
				if (tick == 0)
				{
					g.setColor(new Color(0xD7A33F));
					g.fillRect(centerX + width / 5, y + height / 3, 2, 2);
				}
				break;
			case TOWN_HALL:
				if (tick < 2)
				{
					g.setColor(new Color(0x6D695E));
					g.fillRect(centerX + width / 6, Math.max(4, y + height / 8 - tick * 3), 3, 2);
				}
				break;
			default:
				break;
		}
	}

	/**
	 * Screen rectangle the sprite is drawn into.
	 *
	 * <p>Two invariants live here. The size comes from the artwork and the zoom only, so
	 * a building is never smaller in one corner of the map than in another. The position
	 * anchors the sprite's opaque content — not its canvas — onto the footprint diamond,
	 * so a sprite with uneven padding still stands on its own tiles.
	 */
	private Rectangle spriteBounds(
		IsometricProjection projection,
		BuildingType type,
		GridPoint position)
	{
		Footprint footprint = catalog.getFootprint(type);
		Point south = projection.toScreen(
			position.getX() + footprint.getWidth(),
			position.getY() + footprint.getHeight());
		VillageSpriteMetadata metadata = sprites.metadata(type);
		int tileWidth = projection.getTileWidth();
		return new Rectangle(
			footprintCenterX(projection, type, position) - metadata.contentCenterX(tileWidth),
			south.y
				- VillageSpriteMetadata.baselineInset(projection.getTileHeight())
				- metadata.contentBottom(tileWidth),
			metadata.scaledWidth(tileWidth),
			metadata.scaledHeight(tileWidth));
	}

	private int footprintCenterX(
		IsometricProjection projection,
		BuildingType type,
		GridPoint position)
	{
		Footprint footprint = catalog.getFootprint(type);
		Point north = projection.toScreen(position);
		Point south = projection.toScreen(
			position.getX() + footprint.getWidth(),
			position.getY() + footprint.getHeight());
		return (north.x + south.x) / 2;
	}

	Rectangle spriteBoundsForTest(BuildingType type, GridPoint position)
	{
		return spriteBounds(projection(), type, position);
	}

	int tileWidthForTest()
	{
		return projection().getTileWidth();
	}

	int footprintCenterXForTest(BuildingType type, GridPoint position)
	{
		return footprintCenterX(projection(), type, position);
	}

	int footprintBaselineYForTest(BuildingType type, GridPoint position)
	{
		IsometricProjection projection = projection();
		Footprint footprint = catalog.getFootprint(type);
		return projection.toScreen(
			position.getX() + footprint.getWidth(),
			position.getY() + footprint.getHeight()).y
			- VillageSpriteMetadata.baselineInset(projection.getTileHeight());
	}

	private void drawManaReadyIcon(Graphics2D g, int centerX, int topY)
	{
		int size = 24;
		int x = centerX - size / 2;
		int y = Math.max(6, topY);
		g.setColor(new Color(0x100D0A));
		g.fillOval(x - 2, y - 2, size + 4, size + 4);
		g.setColor(new Color(0xE0C25A));
		g.fillOval(x, y, size, size);
		g.setColor(new Color(0x2D2A43));
		g.fillOval(x + 3, y + 3, size - 6, size - 6);
		g.setColor(new Color(0x488CE0));
		Polygon drop = new Polygon(
			new int[]{centerX, centerX + 5, centerX + 3, centerX - 3, centerX - 5},
			new int[]{y + 5, y + 13, y + 19, y + 19, y + 13},
			5);
		g.fillPolygon(drop);
		g.setColor(new Color(0x9EC8FF));
		g.fillRect(centerX - 1, y + 9, 2, 6);
	}

	private void drawFoundation(
		Graphics2D g,
		IsometricProjection projection,
		GridPoint position,
		Footprint footprint)
	{
		Polygon diamond = footprintDiamond(projection, position, footprint);
		g.setColor(new Color(0x41321F));
		g.fillPolygon(diamond);
		g.setColor(OUTLINE);
		g.drawPolygon(diamond);
	}

	private void drawConstructionFrame(
		Graphics2D g,
		int x,
		int y,
		int width,
		int height)
	{
		int unit = Math.max(2, width / 32);
		g.setColor(new Color(0x9C6C3A));
		g.fillRect(x + width / 5, y + height / 3, unit, height / 2);
		g.fillRect(x + width * 4 / 5, y + height / 3, unit, height / 2);
		g.fillRect(x + width / 5, y + height / 3, width * 3 / 5, unit);
		g.setColor(OUTLINE);
		g.drawRect(x + width / 5, y + height / 3, width * 3 / 5, height / 2);
	}

	private void drawFootprint(
		Graphics2D g,
		IsometricProjection projection,
		GridPoint position,
		Footprint footprint,
		Color color,
		boolean fill)
	{
		Composite old = g.getComposite();
		for (int y = 0; y < footprint.getHeight(); y++)
		{
			for (int x = 0; x < footprint.getWidth(); x++)
			{
				Polygon tile = tile(projection.toScreen(
					position.getX() + x,
					position.getY() + y), projection);
				if (fill)
				{
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.46f));
					g.setColor(color);
					g.fillPolygon(tile);
				}
				g.setComposite(old);
				g.setColor(color);
				g.drawPolygon(tile);
			}
		}
		g.setComposite(old);
		g.setColor(color);
		g.drawPolygon(footprintDiamond(projection, position, footprint));
	}

	private void drawBuildingLabel(Graphics2D g, BuildingType type, int centerX, int topY)
	{
		RuneholdViewModel.BuildingView building = viewModel.getBuilding(type);
		String text = building.getName() + "  L" + building.getCurrentLevel();
		drawBoxedText(g, text, centerX, Math.max(17, topY), ORANGE);
	}

	private void drawGhostLabel(
		Graphics2D g,
		BuildingType type,
		int centerX,
		int topY,
		PlacementResult result)
	{
		String cost = viewModel.hasUnlimitedMana()
			? "FREE - TEST"
			: viewModel.getBuilding(type).getNextCost() + " mana";
		boolean success = result.isSuccess();
		String text = success ? "VALID - " + cost
			: placementError(result).toUpperCase();
		drawBoxedText(g, text, centerX, Math.max(17, topY),
			success ? VALID : INVALID);
	}

	private void drawBoxedText(Graphics2D g, String text, int centerX, int baseline, Color color)
	{
		Font font = assets.boldFont(11f);
		g.setFont(font);
		int width = g.getFontMetrics().stringWidth(text);
		int boxX = centerX - width / 2 - 5;
		int boxY = baseline - 13;
		g.setColor(OUTLINE);
		g.fillRect(boxX, boxY, width + 10, 17);
		g.setColor(new Color(0x6D695E));
		g.drawRect(boxX + 1, boxY + 1, width + 7, 14);
		drawShadowText(g, text, centerX - width / 2, baseline, font, color);
	}

	private void installMouseControls()
	{
		MouseAdapter mouse = new MouseAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent event)
			{
				updateHoveredTile(event.getPoint());
				updateCursor(event.getPoint());
			}

			@Override
			public void mousePressed(MouseEvent event)
			{
				requestFocusInWindow();
				dragOrigin = event.getPoint();
				panOriginX = panX;
				panOriginY = panY;
				dragging = false;
				BuildingType hit = buildingAt(event.getPoint());
				canPan = SwingUtilities.isMiddleMouseButton(event)
					|| SwingUtilities.isRightMouseButton(event)
					|| (SwingUtilities.isLeftMouseButton(event)
						&& hit == null
						&& interaction.getMode() != VillageInteractionModel.Mode.PLACE
						&& interaction.getMode() != VillageInteractionModel.Mode.MOVE);
			}

			@Override
			public void mouseDragged(MouseEvent event)
			{
				if (!canPan || dragOrigin == null)
				{
					return;
				}
				int dx = event.getX() - dragOrigin.x;
				int dy = event.getY() - dragOrigin.y;
				if (Math.abs(dx) + Math.abs(dy) > 4)
				{
					dragging = true;
				}
				panX = clamp(panOriginX + dx, -getWidth() / 2, getWidth() / 2);
				panY = clamp(panOriginY + dy, -getHeight() / 3, getHeight() / 3);
				setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
				repaint();
			}

			@Override
			public void mouseReleased(MouseEvent event)
			{
				if (!dragging && SwingUtilities.isLeftMouseButton(event))
				{
					handleClick(event.getPoint());
				}
				dragOrigin = null;
				canPan = false;
				dragging = false;
				updateCursor(event.getPoint());
			}

			@Override
			public void mouseWheelMoved(MouseWheelEvent event)
			{
				zoomBy(event.getPreciseWheelRotation() < 0 ? 0.1 : -0.1);
			}

			@Override
			public void mouseExited(MouseEvent event)
			{
				hoveredTile = null;
				if (interaction.getMode() == VillageInteractionModel.Mode.VIEW)
				{
					setCursor(Cursor.getDefaultCursor());
				}
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
		addMouseWheelListener(mouse);
	}

	private void installKeyboardControls()
	{
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "confirm", this::confirmInteraction);
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel", this::cancelInteraction);
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "recenter", this::recenter);
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0), "zoomIn", () -> zoomBy(0.1));
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), "zoomOut", () -> zoomBy(-0.1));
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "left", () -> moveGhost(-1, 0));
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "right", () -> moveGhost(1, 0));
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "up", () -> moveGhost(0, -1));
		bindKey(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "down", () -> moveGhost(0, 1));
	}

	private void bindKey(KeyStroke key, String name, Runnable action)
	{
		getInputMap(WHEN_FOCUSED).put(key, name);
		getActionMap().put(name, new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				action.run();
			}
		});
	}

	private void moveGhost(int dx, int dy)
	{
		GridPoint point = interaction.getGhostPosition();
		if (point == null)
		{
			return;
		}
		interaction.updateGhost(new GridPoint(point.getX() + dx, point.getY() + dy));
		fireInteractionChanged();
	}

	private void handleClick(Point point)
	{
		updateHoveredTile(point);
		if (interaction.getMode() == VillageInteractionModel.Mode.PLACE
			|| interaction.getMode() == VillageInteractionModel.Mode.MOVE)
		{
			if (hoveredTile != null)
			{
				interaction.updateGhost(hoveredTile);
				if (isActivePlacementValid())
				{
					confirmInteraction();
					return;
				}
				statusText = placementError(activePreview());
				fireInteractionChanged();
			}
			return;
		}

		BuildingType selected = buildingAt(point);
		if (interaction.getMode() == VillageInteractionModel.Mode.EDIT && selected != null)
		{
			interaction.beginMove(selected, viewModel.getBuildingPositions().get(selected));
			statusText = "Move " + catalog.getDisplayName(selected)
				+ " - choose a new tile, then Confirm.";
			fireInteractionChanged();
			return;
		}
		interaction.select(selected);
		statusText = selected == null
			? "Nothing selected."
			: catalog.getDisplayName(selected) + " selected.";
		listener.selectionChanged(selected);
		fireInteractionChanged();
	}

	private void updateHoveredTile(Point point)
	{
		GridPoint grid = projection().toGrid(point.x, point.y);
		hoveredTile = grid.getX() >= 0 && grid.getX() < VillageLayout.COLUMNS
			&& grid.getY() >= 0 && grid.getY() < VillageLayout.ROWS ? grid : null;
		if ((interaction.getMode() == VillageInteractionModel.Mode.PLACE
			|| interaction.getMode() == VillageInteractionModel.Mode.MOVE)
			&& hoveredTile != null)
		{
			interaction.updateGhost(hoveredTile);
			repaint();
			listener.interactionChanged();
		}
	}

	private void updateCursor(Point point)
	{
		if (interaction.getMode() == VillageInteractionModel.Mode.PLACE
			|| interaction.getMode() == VillageInteractionModel.Mode.MOVE)
		{
			setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		}
		else if (buildingAt(point) != null)
		{
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}
		else
		{
			setCursor(Cursor.getDefaultCursor());
		}
	}

	private BuildingType buildingAt(Point point)
	{
		List<BuildingPlacement> placements = new ArrayList<>(layout.getPlacements().values());
		placements.sort(Comparator.comparingInt(
			(BuildingPlacement placement) -> depthOf(new RenderItem(
				placement.getType(),
				placement.getPosition(),
				visualLevel(placement.getType()),
				false,
				isUnderConstruction(placement.getType())))).reversed());
		IsometricProjection projection = projection();
		for (BuildingPlacement placement : placements)
		{
			if (footprintDiamond(
				projection,
				placement.getPosition(),
				catalog.getFootprint(placement.getType())).contains(point))
			{
				return placement.getType();
			}
		}
		return null;
	}

	private PlacementResult activePreview()
	{
		BuildingType type = interaction.getActiveType();
		GridPoint position = interaction.getGhostPosition();
		if (type == null || position == null)
		{
			return PlacementResult.outOfBounds();
		}
		return interaction.getMode() == VillageInteractionModel.Mode.PLACE
			? layout.previewPlace(type, position)
			: layout.previewMove(type, position);
	}

	private GridPoint findFirstVisuallySafePosition(BuildingType type)
	{
		GridPoint firstValid = null;
		for (int y = 0; y < VillageLayout.ROWS; y++)
		{
			for (int x = 0; x < VillageLayout.COLUMNS; x++)
			{
				GridPoint point = new GridPoint(x, y);
				if (layout.previewPlace(type, point).isSuccess())
				{
					if (firstValid == null)
					{
						firstValid = point;
					}
					if (spriteFitsTerrain(type, point))
					{
						return point;
					}
				}
			}
		}
		return firstValid;
	}

	private boolean spriteFitsTerrain(BuildingType type, GridPoint position)
	{
		IsometricProjection projection = projection();
		Footprint footprint = catalog.getFootprint(type);
		Rectangle bounds = spriteBounds(projection, type, position);
		int drawX = bounds.x;
		int drawY = bounds.y;
		int targetWidth = bounds.width;
		int minX = projection.toScreen(0, VillageLayout.ROWS).x;
		int maxX = projection.toScreen(VillageLayout.COLUMNS, 0).x;
		int minY = projection.toScreen(0, 0).y - projection.getTileHeight() / 2;
		int tolerance = Math.max(4, projection.getTileWidth() / 6);
		return drawX >= minX - tolerance
			&& drawX + targetWidth <= maxX + tolerance
			&& drawY >= minY;
	}

	private int depthOf(RenderItem item)
	{
		if (item.actor != null)
		{
			GridPoint position = item.actor.getPosition();
			return position.getX() + position.getY() + 1;
		}
		Footprint footprint = catalog.getFootprint(item.type);
		return item.position.getX() + footprint.getWidth()
			+ item.position.getY() + footprint.getHeight();
	}

	private int visualLevel(BuildingType type)
	{
		RuneholdViewModel.BuildingView building = viewModel.getBuilding(type);
		if (building == null)
		{
			return 1;
		}
		if (isUnderConstruction(type) && viewModel.getConstructionJob() != null)
		{
			return viewModel.getConstructionJob().getTargetLevel();
		}
		return Math.max(1, building.getCurrentLevel());
	}

	private boolean isUnderConstruction(BuildingType type)
	{
		return viewModel.getConstructionJob() != null
			&& viewModel.getConstructionJob().getBuildingType() == type;
	}

	int depthForTest(BuildingType type, GridPoint position)
	{
		return depthOf(new RenderItem(type, position, 1, false, false));
	}

	Point screenPointForTest(GridPoint position)
	{
		return projection().toScreen(position);
	}

	void updateGhostForTest(GridPoint position)
	{
		interaction.updateGhost(position);
	}

	PlacementResult activePreviewForTest()
	{
		return activePreview();
	}

	boolean spriteFitsTerrainForTest(BuildingType type, GridPoint position)
	{
		return spriteFitsTerrain(type, position);
	}

	private IsometricProjection projection()
	{
		double widthFit = Math.max(1, getWidth()) * 0.84 / VillageLayout.COLUMNS;
		double heightFit = Math.max(1, getHeight()) * 1.42 / VillageLayout.ROWS;
		int tileWidth = (int) Math.round(Math.min(widthFit, heightFit) * zoom);
		tileWidth = clamp(tileWidth, MIN_TILE_WIDTH, MAX_TILE_WIDTH);
		if ((tileWidth & 1) != 0)
		{
			tileWidth++;
		}
		int tileHeight = Math.max(10, tileWidth / 2);
		if ((tileHeight & 1) != 0)
		{
			tileHeight++;
		}
		int originY = Math.max(32, (int) Math.round(getHeight() * 0.08)) + panY;
		return new IsometricProjection(
			getWidth() / 2 + panX,
			originY,
			tileWidth,
			tileHeight);
	}

	private static Color terrainColor(int x, int y)
	{
		int variant = Math.abs(x * 31 + y * 17 + x * y * 7) % 9;
		if (variant == 0 || variant == 5)
		{
			return GRASS_C;
		}
		return (x + y) % 2 == 0 ? GRASS_A : GRASS_B;
	}

	private static Polygon tile(Point top, IsometricProjection projection)
	{
		int halfWidth = projection.getTileWidth() / 2;
		int halfHeight = projection.getTileHeight() / 2;
		return new Polygon(
			new int[]{top.x, top.x + halfWidth, top.x, top.x - halfWidth},
			new int[]{top.y, top.y + halfHeight, top.y + projection.getTileHeight(),
				top.y + halfHeight}, 4);
	}

	private static Polygon footprintDiamond(
		IsometricProjection projection,
		GridPoint position,
		Footprint footprint)
	{
		Point north = projection.toScreen(position);
		Point east = projection.toScreen(position.getX() + footprint.getWidth(), position.getY());
		Point south = projection.toScreen(
			position.getX() + footprint.getWidth(),
			position.getY() + footprint.getHeight());
		Point west = projection.toScreen(position.getX(), position.getY() + footprint.getHeight());
		return new Polygon(
			new int[]{north.x, east.x, south.x, west.x},
			new int[]{north.y, east.y, south.y, west.y}, 4);
	}


	private String placementError(PlacementResult result)
	{
		switch (result.getStatus())
		{
			case OUT_OF_BOUNDS:
				return "Outside the village boundary.";
			case OCCUPIED:
				return "Blocked by " + catalog.getDisplayName(result.getBlockingType()) + ".";
			case ALREADY_PLACED:
				return "This structure is already built.";
			case NOT_PLACED:
				return "This structure is not built.";
			case BUILDER_BUSY:
				return "Builder is busy.";
			case SUCCESS:
				return "Valid position.";
			default:
				return "Invalid position.";
		}
	}

	private void fireInteractionChanged()
	{
		repaint();
		listener.interactionChanged();
	}

	private static void drawShadowText(
		Graphics2D g,
		String text,
		int x,
		int y,
		Font font,
		Color color)
	{
		g.setFont(font);
		g.setColor(OUTLINE);
		g.drawString(text, x + 1, y + 1);
		g.setColor(color);
		g.drawString(text, x, y);
	}

	private static int clamp(int value, int minimum, int maximum)
	{
		return Math.max(minimum, Math.min(maximum, value));
	}

	private static double clamp(double value, double minimum, double maximum)
	{
		return Math.max(minimum, Math.min(maximum, value));
	}

	private static final class RenderItem
	{
		private final BuildingType type;
		private final GridPoint position;
		private final int level;
		private final boolean ghost;
		private final boolean construction;
		private final VillageActor actor;

		private RenderItem(
			BuildingType type,
			GridPoint position,
			int level,
			boolean ghost,
			boolean construction)
		{
			this.type = type;
			this.position = position;
			this.level = level;
			this.ghost = ghost;
			this.construction = construction;
			this.actor = null;
		}

		private RenderItem(VillageActor actor)
		{
			this.type = null;
			this.position = actor.getPosition();
			this.level = 1;
			this.ghost = false;
			this.construction = false;
			this.actor = actor;
		}
	}

	private static final class NoOpListener implements Listener
	{
		@Override
		public void selectionChanged(BuildingType selectedType)
		{
		}

		@Override
		public void interactionChanged()
		{
		}

		@Override
		public void confirmBuild(BuildingType type, GridPoint destination)
		{
		}

		@Override
		public void confirmMove(BuildingType type, GridPoint destination)
		{
		}
	}
}
