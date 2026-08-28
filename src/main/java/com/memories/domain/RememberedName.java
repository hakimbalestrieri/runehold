package com.memories.domain;

import java.util.Objects;

public final class RememberedName
{
	private final String name;
	private final long observedAtEpochMilli;

	public RememberedName(String name, long observedAtEpochMilli)
	{
		this.name = Objects.requireNonNull(name, "name");
		this.observedAtEpochMilli = observedAtEpochMilli;
	}

	public String getName()
	{
		return name;
	}

	public long getObservedAtEpochMilli()
	{
		return observedAtEpochMilli;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (!(other instanceof RememberedName))
		{
			return false;
		}
		RememberedName that = (RememberedName) other;
		return observedAtEpochMilli == that.observedAtEpochMilli && name.equals(that.name);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, observedAtEpochMilli);
	}
}
