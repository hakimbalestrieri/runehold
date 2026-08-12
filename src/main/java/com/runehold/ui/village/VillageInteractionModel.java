package com.runehold.ui.village;

import com.runehold.domain.BuildingType;
import com.runehold.domain.layout.GridPoint;
import java.util.Objects;

public final class VillageInteractionModel
{
	public enum Mode
	{
		VIEW,
		EDIT,
		PLACE,
		MOVE
	}

	private Mode mode = Mode.VIEW;
	private boolean editEnabled;
	private BuildingType selectedType;
	private BuildingType activeType;
	private GridPoint ghostPosition;
	private GridPoint originalPosition;

	public Mode getMode()
	{
		return mode;
	}

	public BuildingType getSelectedType()
	{
		return selectedType;
	}

	public BuildingType getActiveType()
	{
		return activeType;
	}

	public GridPoint getGhostPosition()
	{
		return ghostPosition;
	}

	public GridPoint getOriginalPosition()
	{
		return originalPosition;
	}

	public void select(BuildingType type)
	{
		if (mode == Mode.PLACE || mode == Mode.MOVE)
		{
			return;
		}
		selectedType = type;
	}

	public void toggleEdit()
	{
		if (mode == Mode.PLACE || mode == Mode.MOVE)
		{
			return;
		}
		editEnabled = !editEnabled;
		mode = editEnabled ? Mode.EDIT : Mode.VIEW;
	}

	public boolean isEditEnabled()
	{
		return editEnabled;
	}

	public void beginPlacement(BuildingType type, GridPoint initialPosition)
	{
		activeType = Objects.requireNonNull(type, "type");
		ghostPosition = Objects.requireNonNull(initialPosition, "initialPosition");
		originalPosition = null;
		selectedType = null;
		mode = Mode.PLACE;
	}

	public void beginMove(BuildingType type, GridPoint origin)
	{
		activeType = Objects.requireNonNull(type, "type");
		originalPosition = Objects.requireNonNull(origin, "origin");
		ghostPosition = origin;
		selectedType = type;
		mode = Mode.MOVE;
	}

	public void updateGhost(GridPoint position)
	{
		if (mode == Mode.PLACE || mode == Mode.MOVE)
		{
			ghostPosition = Objects.requireNonNull(position, "position");
		}
	}

	public void finish()
	{
		if (activeType != null)
		{
			selectedType = activeType;
		}
		mode = editEnabled ? Mode.EDIT : Mode.VIEW;
		activeType = null;
		ghostPosition = null;
		originalPosition = null;
	}

	public void cancel()
	{
		mode = editEnabled ? Mode.EDIT : Mode.VIEW;
		activeType = null;
		ghostPosition = null;
		originalPosition = null;
	}
}
