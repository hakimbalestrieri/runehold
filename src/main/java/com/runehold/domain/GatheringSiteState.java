package com.runehold.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class GatheringSiteState
{
	private final GatheringSiteType type;
	private int level;
	private int storedAmount;
	private long updatedAtEpochMillis;
	private final List<String> assignedWorkerIds = new ArrayList<>();
	private String blockedReason;

	public GatheringSiteState(
		GatheringSiteType type,
		int level,
		int storedAmount,
		long updatedAtEpochMillis,
		List<String> assignedWorkerIds,
		String blockedReason)
	{
		this.type = Objects.requireNonNull(type, "type");
		setLevel(level);
		setStoredAmount(storedAmount);
		if (updatedAtEpochMillis < 0)
		{
			throw new IllegalArgumentException("invalid site timestamp");
		}
		this.updatedAtEpochMillis = updatedAtEpochMillis;
		if (assignedWorkerIds != null)
		{
			for (String workerId : assignedWorkerIds)
			{
				addWorker(workerId);
			}
		}
		this.blockedReason = blockedReason;
	}

	public static GatheringSiteState unlocked(GatheringSiteType type, long now)
	{
		return new GatheringSiteState(type, 1, 0, Math.max(0, now), null, null);
	}

	public GatheringSiteType getType()
	{
		return type;
	}

	public int getLevel()
	{
		return level;
	}

	public void setLevel(int level)
	{
		if (level < 0)
		{
			throw new IllegalArgumentException("invalid site level");
		}
		this.level = level;
	}

	public int getStoredAmount()
	{
		return storedAmount;
	}

	public void setStoredAmount(int storedAmount)
	{
		if (storedAmount < 0)
		{
			throw new IllegalArgumentException("invalid stored amount");
		}
		this.storedAmount = storedAmount;
	}

	public long getUpdatedAtEpochMillis()
	{
		return updatedAtEpochMillis;
	}

	public void setUpdatedAtEpochMillis(long updatedAtEpochMillis)
	{
		if (updatedAtEpochMillis < 0)
		{
			throw new IllegalArgumentException("invalid site timestamp");
		}
		this.updatedAtEpochMillis = updatedAtEpochMillis;
	}

	public List<String> getAssignedWorkerIds()
	{
		return Collections.unmodifiableList(new ArrayList<>(assignedWorkerIds));
	}

	public void addWorker(String workerId)
	{
		if (workerId == null || workerId.trim().isEmpty())
		{
			throw new IllegalArgumentException("invalid worker id");
		}
		if (!assignedWorkerIds.contains(workerId))
		{
			assignedWorkerIds.add(workerId);
		}
	}

	public void removeWorker(String workerId)
	{
		assignedWorkerIds.remove(workerId);
	}

	public String getBlockedReason()
	{
		return blockedReason;
	}

	public void setBlockedReason(String blockedReason)
	{
		this.blockedReason = blockedReason;
	}
}
