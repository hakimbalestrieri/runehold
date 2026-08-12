package com.runehold.ui.village;

import com.runehold.domain.layout.GridPoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class VillageActor
{
	public enum Role
	{
		WORKER,
		TROOP
	}

	public enum Pose
	{
		IDLE,
		WALK,
		WORK
	}

	public enum Direction
	{
		NORTH,
		EAST,
		SOUTH,
		WEST
	}

	private final Role role;
	private final GridPoint position;
	private final Direction direction;
	private final Pose pose;
	private final List<GridPoint> path;
	private final int pathIndex;

	VillageActor(
		Role role,
		GridPoint position,
		Direction direction,
		Pose pose,
		List<GridPoint> path,
		int pathIndex)
	{
		this.role = role;
		this.position = position;
		this.direction = direction;
		this.pose = pose;
		this.path = Collections.unmodifiableList(new ArrayList<>(path));
		this.pathIndex = pathIndex;
	}

	public Role getRole()
	{
		return role;
	}

	public GridPoint getPosition()
	{
		return position;
	}

	public Direction getDirection()
	{
		return direction;
	}

	public Pose getPose()
	{
		return pose;
	}

	public List<GridPoint> getPath()
	{
		return path;
	}

	int getPathIndex()
	{
		return pathIndex;
	}

	VillageActor step()
	{
		if (pathIndex + 1 >= path.size())
		{
			Pose finalPose = role == Role.WORKER ? Pose.WORK : Pose.IDLE;
			return new VillageActor(role, position, direction, finalPose, path, pathIndex);
		}

		GridPoint next = path.get(pathIndex + 1);
		return new VillageActor(
			role,
			next,
			directionBetween(position, next),
			Pose.WALK,
			path,
			pathIndex + 1);
	}

	static Direction directionBetween(GridPoint from, GridPoint to)
	{
		int dx = to.getX() - from.getX();
		int dy = to.getY() - from.getY();
		if (Math.abs(dx) > Math.abs(dy))
		{
			return dx >= 0 ? Direction.EAST : Direction.WEST;
		}
		return dy >= 0 ? Direction.SOUTH : Direction.NORTH;
	}
}
