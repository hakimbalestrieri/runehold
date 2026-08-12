package com.runehold.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class OfflineProgressSummary
{
	private final Map<ResourceType, Integer> produced;
	private final long appliedMinutes;

	public OfflineProgressSummary(Map<ResourceType, Integer> produced, long appliedMinutes)
	{
		this.produced = Collections.unmodifiableMap(new EnumMap<>(produced));
		this.appliedMinutes = appliedMinutes;
	}

	public Map<ResourceType, Integer> getProduced()
	{
		return produced;
	}

	public long getAppliedMinutes()
	{
		return appliedMinutes;
	}

	public boolean isEmpty()
	{
		for (int amount : produced.values())
		{
			if (amount > 0)
			{
				return false;
			}
		}
		return true;
	}
}
