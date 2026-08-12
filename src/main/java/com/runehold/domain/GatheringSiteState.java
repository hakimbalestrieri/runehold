package com.runehold.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Production state for a placed gathering site. The site's level, footprint and
 * position belong to the building layout, not here.
 */
public final class GatheringSiteState
{
	private final BuildingType type;
	private int storedAmount;
	private long updatedAtEpochMillis;
	private final List<String> assignedWorkerIds = new ArrayList<>();
	private String blockedReason;

	public GatheringSiteState(
		BuildingType type,
		int storedAmount,
		long updatedAtEpochMillis,
		List<String> assignedWorkerIds,
		String blockedReason)
	{
		this.type = Objects.requireNonNull(type, "type");
		if (!type.isGatheringSite())
		{
			throw new IllegalArgumentException("not a gathering site: " + type);
		}
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

	public static GatheringSiteState idle(BuildingType type, long now)
	{
		return new GatheringSiteState(type, 0, Math.max(0, now), null, null);
	}

	public BuildingType getType()
	{
		return type;
	}

	public int getStoredAmount()
	{
		return storedAmount;
	}

	void setStoredAmount(int storedAmount)
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

	void setUpdatedAtEpochMillis(long updatedAtEpochMillis)
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

	void addWorker(String workerId)
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

	void removeWorker(String workerId)
	{
		assignedWorkerIds.remove(workerId);
	}

	void clearWorkers()
	{
		assignedWorkerIds.clear();
	}

	public String getBlockedReason()
	{
		return blockedReason;
	}

	void setBlockedReason(String blockedReason)
	{
		this.blockedReason = blockedReason;
	}
}
