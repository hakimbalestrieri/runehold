package com.runehold.ui.village;

import com.runehold.domain.BuildingCatalog;
import com.runehold.domain.BuildingType;
import com.runehold.domain.layout.Footprint;
import com.runehold.domain.layout.GridPoint;
import com.runehold.domain.layout.VillageLayout;
import com.runehold.ui.RuneholdViewModel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public final class VillageAnimationModel
{
	private static final long WORKER_STEP_MILLIS = 700L;
	private static final long TROOP_STEP_MILLIS = 1_100L;

	private final BuildingCatalog catalog;
	private final Random random;
	private final List<VillageActor> actors = new ArrayList<>();
	private long lastStepAt;

	public VillageAnimationModel(BuildingCatalog catalog)
	{
		this(catalog, 0x52554E45L);
	}

	VillageAnimationModel(BuildingCatalog catalog, long seed)
	{
		this.catalog = Objects.requireNonNull(catalog, "catalog");
		this.random = new Random(seed);
	}

	public void refresh(
		RuneholdViewModel viewModel,
		VillageLayout layout,
		VillageAnimationSettings settings)
	{
		Objects.requireNonNull(viewModel, "viewModel");
		Objects.requireNonNull(layout, "layout");
		Objects.requireNonNull(settings, "settings");
		if (!settings.isEnabled())
		{
			actors.clear();
			return;
		}

		Set<GridPoint> occupied = occupiedCells(layout);
		syncWorker(viewModel, occupied);
		syncTroops(viewModel, occupied, settings.getCharacterDensity());
	}

	public boolean advance(long nowMillis, VillageAnimationSettings settings)
	{
		if (!settings.isEnabled())
		{
			return false;
		}

		long stepMillis = settings.isReduceMotion()
			? Math.max(WORKER_STEP_MILLIS, TROOP_STEP_MILLIS)
			: Math.min(WORKER_STEP_MILLIS, TROOP_STEP_MILLIS);
		if (lastStepAt == 0)
		{
			lastStepAt = nowMillis;
			return true;
		}
		if (nowMillis - lastStepAt < stepMillis)
		{
			return false;
		}

		for (int index = 0; index < actors.size(); index++)
		{
			actors.set(index, actors.get(index).step());
		}
		lastStepAt = nowMillis;
		return true;
	}

	public List<VillageActor> getActors()
	{
		return Collections.unmodifiableList(actors);
	}

	boolean hasWorker()
	{
		for (VillageActor actor : actors)
		{
			if (actor.getRole() == VillageActor.Role.WORKER)
			{
				return true;
			}
		}
		return false;
	}

	private void syncWorker(RuneholdViewModel viewModel, Set<GridPoint> occupied)
	{
		if (viewModel.getConstructionJob() == null)
		{
			removeRole(VillageActor.Role.WORKER);
			return;
		}
		if (hasWorker())
		{
			return;
		}

		GridPoint job = viewModel.getBuildingPositions().get(
			viewModel.getConstructionJob().getBuildingType());
		GridPoint hall = viewModel.getBuildingPositions().get(BuildingType.TOWN_HALL);
		GridPoint start = firstAdjacentFree(hall, catalog.getFootprint(BuildingType.TOWN_HALL),
			occupied);
		GridPoint destination = firstAdjacentFree(job,
			catalog.getFootprint(viewModel.getConstructionJob().getBuildingType()), occupied);
		if (start == null || destination == null)
		{
			return;
		}

		List<GridPoint> path = path(start, destination, occupied);
		if (path.isEmpty())
		{
			path = Collections.singletonList(destination);
		}
		actors.add(new VillageActor(
			VillageActor.Role.WORKER,
			path.get(0),
			path.size() > 1 ? VillageActor.directionBetween(path.get(0), path.get(1))
				: VillageActor.Direction.SOUTH,
			path.size() > 1 ? VillageActor.Pose.WALK : VillageActor.Pose.WORK,
			path,
			0));
	}

	private void syncTroops(
		RuneholdViewModel viewModel,
		Set<GridPoint> occupied,
		int requestedDensity)
	{
		if (viewModel.getBuilding(BuildingType.BARRACKS).getCurrentLevel() <= 0
			|| requestedDensity <= 0)
		{
			removeRole(VillageActor.Role.TROOP);
			return;
		}

		int targetCount = Math.min(requestedDensity,
			viewModel.getBuilding(BuildingType.BARRACKS).getCurrentLevel() + 1);
		while (countRole(VillageActor.Role.TROOP) < targetCount)
		{
			GridPoint barracks = viewModel.getBuildingPositions().get(BuildingType.BARRACKS);
			GridPoint start = firstAdjacentFree(barracks, catalog.getFootprint(BuildingType.BARRACKS),
				occupied);
			GridPoint destination = randomFree(occupied);
			if (start == null || destination == null)
			{
				break;
			}
			List<GridPoint> path = path(start, destination, occupied);
			if (path.size() < 2)
			{
				break;
			}
			actors.add(new VillageActor(
				VillageActor.Role.TROOP,
				path.get(0),
				VillageActor.directionBetween(path.get(0), path.get(1)),
				VillageActor.Pose.WALK,
				path,
				0));
		}
		while (countRole(VillageActor.Role.TROOP) > targetCount)
		{
			removeFirstRole(VillageActor.Role.TROOP);
		}
	}

	private Set<GridPoint> occupiedCells(VillageLayout layout)
	{
		Set<GridPoint> occupied = new HashSet<>();
		layout.getPlacements().forEach((type, placement) ->
		{
			Footprint footprint = catalog.getFootprint(type);
			for (int y = 0; y < footprint.getHeight(); y++)
			{
				for (int x = 0; x < footprint.getWidth(); x++)
				{
					occupied.add(new GridPoint(
						placement.getPosition().getX() + x,
						placement.getPosition().getY() + y));
				}
			}
		});
		return occupied;
	}

	private GridPoint firstAdjacentFree(
		GridPoint position,
		Footprint footprint,
		Set<GridPoint> occupied)
	{
		if (position == null)
		{
			return null;
		}
		for (int x = position.getX(); x < position.getX() + footprint.getWidth(); x++)
		{
			GridPoint north = new GridPoint(x, position.getY() - 1);
			if (isFree(north, occupied))
			{
				return north;
			}
			GridPoint south = new GridPoint(x, position.getY() + footprint.getHeight());
			if (isFree(south, occupied))
			{
				return south;
			}
		}
		for (int y = position.getY(); y < position.getY() + footprint.getHeight(); y++)
		{
			GridPoint west = new GridPoint(position.getX() - 1, y);
			if (isFree(west, occupied))
			{
				return west;
			}
			GridPoint east = new GridPoint(position.getX() + footprint.getWidth(), y);
			if (isFree(east, occupied))
			{
				return east;
			}
		}
		return null;
	}

	private List<GridPoint> path(GridPoint start, GridPoint target, Set<GridPoint> occupied)
	{
		ArrayDeque<GridPoint> open = new ArrayDeque<>();
		Map<GridPoint, GridPoint> parent = new HashMap<>();
		open.add(start);
		parent.put(start, start);
		while (!open.isEmpty())
		{
			GridPoint point = open.removeFirst();
			if (point.equals(target))
			{
				return reconstruct(parent, target);
			}
			for (GridPoint neighbor : neighbors(point))
			{
				if (!parent.containsKey(neighbor) && isFree(neighbor, occupied))
				{
					parent.put(neighbor, point);
					open.add(neighbor);
				}
			}
		}
		return Collections.emptyList();
	}

	private List<GridPoint> reconstruct(Map<GridPoint, GridPoint> parent, GridPoint target)
	{
		List<GridPoint> reversed = new ArrayList<>();
		GridPoint point = target;
		while (!parent.get(point).equals(point))
		{
			reversed.add(point);
			point = parent.get(point);
		}
		reversed.add(point);
		Collections.reverse(reversed);
		return reversed;
	}

	private List<GridPoint> neighbors(GridPoint point)
	{
		List<GridPoint> points = new ArrayList<>(4);
		points.add(new GridPoint(point.getX() + 1, point.getY()));
		points.add(new GridPoint(point.getX() - 1, point.getY()));
		points.add(new GridPoint(point.getX(), point.getY() + 1));
		points.add(new GridPoint(point.getX(), point.getY() - 1));
		return points;
	}

	private GridPoint randomFree(Set<GridPoint> occupied)
	{
		for (int attempt = 0; attempt < 80; attempt++)
		{
			GridPoint candidate = new GridPoint(
				random.nextInt(VillageLayout.COLUMNS),
				random.nextInt(VillageLayout.ROWS));
			if (isFree(candidate, occupied))
			{
				return candidate;
			}
		}
		return null;
	}

	private static boolean isFree(GridPoint point, Set<GridPoint> occupied)
	{
		return point.getX() >= 0
			&& point.getX() < VillageLayout.COLUMNS
			&& point.getY() >= 0
			&& point.getY() < VillageLayout.ROWS
			&& !occupied.contains(point);
	}

	private int countRole(VillageActor.Role role)
	{
		int count = 0;
		for (VillageActor actor : actors)
		{
			if (actor.getRole() == role)
			{
				count++;
			}
		}
		return count;
	}

	private void removeRole(VillageActor.Role role)
	{
		actors.removeIf(actor -> actor.getRole() == role);
	}

	private void removeFirstRole(VillageActor.Role role)
	{
		for (int index = 0; index < actors.size(); index++)
		{
			if (actors.get(index).getRole() == role)
			{
				actors.remove(index);
				return;
			}
		}
	}
}
