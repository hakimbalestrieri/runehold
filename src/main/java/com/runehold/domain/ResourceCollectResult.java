package com.runehold.domain;

public final class ResourceCollectResult
{
	private final ResourceType resourceType;
	private final int collected;
	private final int remainingAtSite;
	private final boolean storageFull;

	public ResourceCollectResult(
		ResourceType resourceType,
		int collected,
		int remainingAtSite,
		boolean storageFull)
	{
		this.resourceType = resourceType;
		this.collected = collected;
		this.remainingAtSite = remainingAtSite;
		this.storageFull = storageFull;
	}

	public ResourceType getResourceType()
	{
		return resourceType;
	}

	public int getCollected()
	{
		return collected;
	}

	public int getRemainingAtSite()
	{
		return remainingAtSite;
	}

	public boolean isStorageFull()
	{
		return storageFull;
	}
}
