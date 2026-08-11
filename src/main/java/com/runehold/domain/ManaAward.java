package com.runehold.domain;

public final class ManaAward
{
	private final int manaAwarded;
	private final int xpRemainder;
	private final int dailyManaEarned;
	private final boolean baselineEstablished;

	private ManaAward(
		int manaAwarded,
		int xpRemainder,
		int dailyManaEarned,
		boolean baselineEstablished)
	{
		this.manaAwarded = manaAwarded;
		this.xpRemainder = xpRemainder;
		this.dailyManaEarned = dailyManaEarned;
		this.baselineEstablished = baselineEstablished;
	}

	static ManaAward baseline(int xpRemainder, int dailyManaEarned)
	{
		return new ManaAward(0, xpRemainder, dailyManaEarned, true);
	}

	static ManaAward result(int manaAwarded, int xpRemainder, int dailyManaEarned)
	{
		return new ManaAward(manaAwarded, xpRemainder, dailyManaEarned, false);
	}

	public int getManaAwarded()
	{
		return manaAwarded;
	}

	public int getXpRemainder()
	{
		return xpRemainder;
	}

	public int getDailyManaEarned()
	{
		return dailyManaEarned;
	}

	public boolean isBaselineEstablished()
	{
		return baselineEstablished;
	}
}
