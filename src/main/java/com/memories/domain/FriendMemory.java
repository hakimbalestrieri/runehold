package com.memories.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class FriendMemory
{
	static final int MAX_HISTORY = 50;

	private final String currentName;

	// Oldest first.
	private final List<RememberedName> previousNames;

	private FriendMemory(String currentName, List<RememberedName> previousNames)
	{
		this.currentName = currentName;
		this.previousNames = previousNames;
	}

	public static FriendMemory of(String currentName, List<RememberedName> previousNamesOldestFirst)
	{
		Objects.requireNonNull(currentName, "currentName");
		Objects.requireNonNull(previousNamesOldestFirst, "previousNamesOldestFirst");
		List<RememberedName> capped = cap(new ArrayList<>(previousNamesOldestFirst));
		return new FriendMemory(currentName, Collections.unmodifiableList(capped));
	}

	static FriendMemory renamed(
		FriendMemory historyAtOldName,
		FriendMemory historyAtNewName,
		String oldName,
		String newName,
		long observedAtEpochMilli)
	{
		List<RememberedName> history = new ArrayList<>();
		if (historyAtOldName != null)
		{
			history.addAll(historyAtOldName.previousNames);
		}
		if (historyAtNewName != null)
		{
			history.addAll(historyAtNewName.previousNames);
		}

		if (history.isEmpty() || !history.get(history.size() - 1).getName().equalsIgnoreCase(oldName))
		{
			history.add(new RememberedName(oldName, observedAtEpochMilli));
		}

		// Two independently tracked chains can merge at a shared destination name (eg. a freed
		// name reclaimed by someone else); re-sort so the combined history stays chronological.
		history.sort(Comparator.comparingLong(RememberedName::getObservedAtEpochMilli));

		return new FriendMemory(newName, Collections.unmodifiableList(cap(history)));
	}

	private static List<RememberedName> cap(List<RememberedName> history)
	{
		if (history.size() <= MAX_HISTORY)
		{
			return history;
		}
		return new ArrayList<>(history.subList(history.size() - MAX_HISTORY, history.size()));
	}

	public String getCurrentName()
	{
		return currentName;
	}

	public List<RememberedName> getPreviousNames()
	{
		return previousNames;
	}
}
