package com.runehold.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class ResourceInventory
{
	private final Map<ResourceType, Long> amounts = new EnumMap<>(ResourceType.class);

	public ResourceInventory()
	{
	}

	public ResourceInventory(Map<ResourceType, Long> restoredAmounts)
	{
		Objects.requireNonNull(restoredAmounts, "restoredAmounts");
		for (Map.Entry<ResourceType, Long> entry : restoredAmounts.entrySet())
		{
			set(entry.getKey(), entry.getValue());
		}
	}

	public static ResourceInventory from(Map<ResourceType, Long> restoredAmounts)
	{
		return new ResourceInventory(restoredAmounts);
	}

	public long get(ResourceType type)
	{
		validateType(type);
		return amounts.getOrDefault(type, 0L);
	}

	public void set(ResourceType type, long amount)
	{
		validateType(type);
		if (amount < 0)
		{
			throw new IllegalArgumentException("resource amount must not be negative");
		}
		if (amount == 0)
		{
			amounts.remove(type);
			return;
		}
		amounts.put(type, amount);
	}

	public long add(ResourceType type, long amount, long capacity)
	{
		validateType(type);
		if (amount < 0 || capacity < 0)
		{
			throw new IllegalArgumentException("invalid resource addition");
		}
		long current = get(type);
		long accepted = Math.min(amount, Math.max(0, capacity - current));
		set(type, current + accepted);
		return accepted;
	}

	public long remove(ResourceType type, long amount)
	{
		validateType(type);
		if (amount < 0)
		{
			throw new IllegalArgumentException("invalid resource removal");
		}
		long current = get(type);
		long removed = Math.min(current, amount);
		set(type, current - removed);
		return removed;
	}

	public Map<ResourceType, Long> asMap()
	{
		return Collections.unmodifiableMap(new EnumMap<>(amounts));
	}

	public ResourceInventory copy()
	{
		return new ResourceInventory(amounts);
	}

	private static void validateType(ResourceType type)
	{
		if (type == null)
		{
			throw new IllegalArgumentException("resource type is required");
		}
	}
}
