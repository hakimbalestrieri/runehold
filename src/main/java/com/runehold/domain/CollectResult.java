package com.runehold.domain;

public final class CollectResult
{
	private final int manaCollected;

	CollectResult(int manaCollected)
	{
		this.manaCollected = manaCollected;
	}

	public int getManaCollected()
	{
		return manaCollected;
	}
}
