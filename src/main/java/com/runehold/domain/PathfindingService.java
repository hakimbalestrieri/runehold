package com.runehold.domain;

import com.runehold.domain.layout.BuildingPlacement;
import com.runehold.domain.layout.Footprint;
import com.runehold.domain.layout.GridPoint;
import com.runehold.domain.layout.VillageLayout;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

public final class PathfindingService
{
	private final BuildingCatalog buildingCatalog;

	public PathfindingService(BuildingCatalog buildingCatalog)
	{
		this.buildingCatalog = Objects.requireNonNull(buildingCatalog, "buildingCatalog");
	}

	public boolean hasPath(VillageState state, GridPoint start, GridPoint destination)
	{
		return distance(state, start, destination) >= 0;
	}

	public int distance(VillageState state, GridPoint start, GridPoint destination)
	{
		Objects.requireNonNull(state, "state");
		if (!inside(start) || !inside(destination))
		{
			return -1;
		}
		Set<GridPoint> blocked = blockedTiles(state);
		blocked.remove(start);
		blocked.remove(destination);
		Queue<GridPoint> queue = new ArrayDeque<>();
		Map<GridPoint, Integer> distance = new HashMap<>();
		queue.add(start);
		distance.put(start, 0);
		while (!queue.isEmpty())
		{
			GridPoint current = queue.remove();
			if (current.equals(destination))
			{
				return distance.get(current);
			}
			for (GridPoint next : neighbors(current))
			{
				if (!inside(next) || blocked.contains(next) || distance.containsKey(next))
				{
					continue;
				}
				distance.put(next, distance.get(current) + 1);
				queue.add(next);
			}
		}
		return -1;
	}

	/**
	 * Occupancy comes from the single village layout, which now owns gathering-site
	 * footprints too, so nothing re-derives placement rules here.
	 */
	private Set<GridPoint> blockedTiles(VillageState state)
	{
		Set<GridPoint> blocked = new HashSet<>();
		VillageLayout layout = state.getBuildingPositions().isEmpty()
			? VillageLayout.fresh(buildingCatalog)
			: VillageLayout.restore(buildingCatalog, state.getBuildingPositions());
		for (BuildingPlacement placement : layout.getPlacements().values())
		{
			addFootprint(blocked, placement.getPosition(),
				buildingCatalog.getFootprint(placement.getType()));
		}
		return blocked;
	}

	private static void addFootprint(Set<GridPoint> blocked, GridPoint position, Footprint footprint)
	{
		for (int y = 0; y < footprint.getHeight(); y++)
		{
			for (int x = 0; x < footprint.getWidth(); x++)
			{
				blocked.add(new GridPoint(position.getX() + x, position.getY() + y));
			}
		}
	}

	private static GridPoint[] neighbors(GridPoint point)
	{
		return new GridPoint[]{
			new GridPoint(point.getX() + 1, point.getY()),
			new GridPoint(point.getX() - 1, point.getY()),
			new GridPoint(point.getX(), point.getY() + 1),
			new GridPoint(point.getX(), point.getY() - 1)
		};
	}

	private static boolean inside(GridPoint point)
	{
		return point != null
			&& point.getX() >= 0
			&& point.getY() >= 0
			&& point.getX() < VillageLayout.COLUMNS
			&& point.getY() < VillageLayout.ROWS;
	}
}
