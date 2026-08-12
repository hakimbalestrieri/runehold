package com.runehold.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class ProductionService
{
	private final GatheringSiteCatalog catalog;
	private final Clock clock;

	public ProductionService(GatheringSiteCatalog catalog, Clock clock)
	{
		this.catalog = Objects.requireNonNull(catalog, "catalog");
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	public Map<ResourceType, Integer> updateAll(VillageState state)
	{
		return updateAll(state, Instant.now(clock).toEpochMilli(), Long.MAX_VALUE);
	}

	public Map<ResourceType, Integer> updateAll(
		VillageState state,
		long nowEpochMillis,
		long capMinutes)
	{
		Objects.requireNonNull(state, "state");
		Map<ResourceType, Integer> produced = emptyResourceMap();
		for (GatheringSiteState site : state.gatheringSiteStates())
		{
			int amount = updateSite(state, site, nowEpochMillis, capMinutes);
			if (amount > 0)
			{
				ResourceType resource = catalog.get(site.getType()).getResourceType();
				produced.put(resource, produced.get(resource) + amount);
			}
		}
		return produced;
	}

	private int updateSite(
		VillageState state,
		GatheringSiteState site,
		long nowEpochMillis,
		long capMinutes)
	{
		GatheringSiteDefinition definition = catalog.get(site.getType());
		int level = state.levelOf(site.getType());
		if (level <= 0)
		{
			site.setBlockedReason("Not built");
			site.setUpdatedAtEpochMillis(Math.max(site.getUpdatedAtEpochMillis(), nowEpochMillis));
			return 0;
		}
		if (site.getAssignedWorkerIds().isEmpty())
		{
			site.setBlockedReason("No workers assigned");
			site.setUpdatedAtEpochMillis(Math.max(site.getUpdatedAtEpochMillis(), nowEpochMillis));
			return 0;
		}
		int capacity = definition.storageCapacity(level);
		if (site.getStoredAmount() >= capacity)
		{
			site.setBlockedReason("Local storage full");
			site.setUpdatedAtEpochMillis(Math.max(site.getUpdatedAtEpochMillis(), nowEpochMillis));
			return 0;
		}
		long elapsedMillis = Math.max(0, nowEpochMillis - site.getUpdatedAtEpochMillis());
		long minutes = Math.min(capMinutes, elapsedMillis / 60_000L);
		if (minutes <= 0)
		{
			return 0;
		}
		int rate = definition.productionPerMinute(level, site.getAssignedWorkerIds().size());
		int produced = (int) Math.min((long) rate * minutes, capacity - site.getStoredAmount());
		site.setStoredAmount(site.getStoredAmount() + produced);
		// The remaining seconds are deliberately carried so short ticks accumulate.
		site.setUpdatedAtEpochMillis(site.getUpdatedAtEpochMillis() + minutes * 60_000L);
		site.setBlockedReason(site.getStoredAmount() >= capacity ? "Local storage full" : null);
		return produced;
	}

	public ResourceCollectResult collect(VillageState state, BuildingType siteType)
	{
		updateAll(state);
		GatheringSiteState site = state.getGatheringSite(siteType);
		GatheringSiteDefinition definition = catalog.get(siteType);
		ResourceType resource = definition.getResourceType();
		int stored = site == null ? 0 : site.getStoredAmount();
		if (stored <= 0)
		{
			return new ResourceCollectResult(resource, 0, 0, false);
		}
		int accepted = (int) state.mutableResources().add(
			resource,
			stored,
			catalog.generalStorageCapacity(resource));
		site.setStoredAmount(stored - accepted);
		state.putGatheringSite(site);
		return new ResourceCollectResult(
			resource,
			accepted,
			site.getStoredAmount(),
			accepted < stored);
	}

	private static Map<ResourceType, Integer> emptyResourceMap()
	{
		Map<ResourceType, Integer> map = new EnumMap<>(ResourceType.class);
		for (ResourceType type : ResourceType.values())
		{
			map.put(type, 0);
		}
		return map;
	}
}
