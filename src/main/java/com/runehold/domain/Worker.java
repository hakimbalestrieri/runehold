package com.runehold.domain;

import com.runehold.domain.layout.GridPoint;
import java.util.Objects;

public final class Worker
{
	private final String id;
	private final String name;
	private WorkerState state;
	private GatheringSiteType assignment;
	private GridPoint position;
	private GridPoint destination;
	private String role;

	public Worker(
		String id,
		String name,
		WorkerState state,
		GatheringSiteType assignment,
		GridPoint position,
		GridPoint destination,
		String role)
	{
		this.id = requireText(id, "id");
		this.name = requireText(name, "name");
		this.state = Objects.requireNonNull(state, "state");
		this.assignment = assignment;
		this.position = Objects.requireNonNull(position, "position");
		this.destination = destination;
		this.role = requireText(role, "role");
	}

	public static Worker settler(int index, GridPoint home)
	{
		return new Worker(
			"worker-" + index,
			"Villager " + index,
			WorkerState.IDLE,
			null,
			home,
			null,
			"Worker");
	}

	public String getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public WorkerState getState()
	{
		return state;
	}

	public GatheringSiteType getAssignment()
	{
		return assignment;
	}

	public GridPoint getPosition()
	{
		return position;
	}

	public GridPoint getDestination()
	{
		return destination;
	}

	public String getRole()
	{
		return role;
	}

	void assign(GatheringSiteType site, GridPoint destination)
	{
		assignment = Objects.requireNonNull(site, "site");
		this.destination = Objects.requireNonNull(destination, "destination");
		state = WorkerState.WALKING_TO_WORK;
		role = site.getDisplayName() + " Worker";
	}

	void arriveAtWork(GridPoint workTile)
	{
		position = Objects.requireNonNull(workTile, "workTile");
		destination = workTile;
		state = WorkerState.WORKING;
	}

	void unassign(GridPoint home)
	{
		assignment = null;
		destination = home;
		state = WorkerState.RETURNING;
		role = "Worker";
	}

	void waitForPath()
	{
		state = WorkerState.WAITING;
	}

	void setIdle(GridPoint home)
	{
		position = Objects.requireNonNull(home, "home");
		destination = null;
		assignment = null;
		state = WorkerState.IDLE;
		role = "Worker";
	}

	private static String requireText(String value, String name)
	{
		if (value == null || value.trim().isEmpty() || value.length() > 48)
		{
			throw new IllegalArgumentException("invalid " + name);
		}
		return value;
	}
}
