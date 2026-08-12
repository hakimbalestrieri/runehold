package com.runehold.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public final class OfflineProgressService
{
	private final GatheringSiteCatalog catalog;
	private final ProductionService productionService;
	private final Clock clock;

	public OfflineProgressService(
		GatheringSiteCatalog catalog,
		ProductionService productionService,
		Clock clock)
	{
		this.catalog = Objects.requireNonNull(catalog, "catalog");
		this.productionService = Objects.requireNonNull(productionService, "productionService");
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	public OfflineProgressSummary apply(VillageState state)
	{
		return apply(state, Instant.now(clock).toEpochMilli());
	}

	OfflineProgressSummary apply(VillageState state, long nowEpochMillis)
	{
		Objects.requireNonNull(state, "state");
		long previous = state.getLastOfflineProgressAtEpochMillis();
		long elapsedMillis = Math.max(0, nowEpochMillis - Math.max(0, previous));
		long elapsedMinutes = elapsedMillis / 60_000L;
		long cappedMinutes = Math.min(elapsedMinutes, catalog.offlineCapMinutes());
		Map<ResourceType, Integer> produced = productionService.updateAll(
			state,
			Math.max(0, previous) + cappedMinutes * 60_000L,
			cappedMinutes);
		if (cappedMinutes < elapsedMinutes)
		{
			// The interval beyond the cap is forfeited. Without this the surplus would
			// still sit between each site's clock and now, and the next uncapped update
			// would credit it, making the cap decorative.
			for (GatheringSiteState site : state.gatheringSiteStates())
			{
				site.setUpdatedAtEpochMillis(Math.max(0, nowEpochMillis));
			}
		}
		state.setLastOfflineProgressAtEpochMillis(Math.max(0, nowEpochMillis));
		return new OfflineProgressSummary(produced, cappedMinutes);
	}
}
