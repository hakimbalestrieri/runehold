package com.memories.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MemoriesBook
{
	private final Map<String, FriendMemory> byNormalizedName;

	public MemoriesBook()
	{
		this.byNormalizedName = new LinkedHashMap<>();
	}

	private MemoriesBook(Map<String, FriendMemory> initial)
	{
		this.byNormalizedName = new LinkedHashMap<>(initial);
	}

	public static MemoriesBook restore(Map<String, FriendMemory> byNormalizedName)
	{
		return new MemoriesBook(Objects.requireNonNull(byNormalizedName, "byNormalizedName"));
	}

	/**
	 * Records that {@code oldName} is now known as {@code newName}.
	 *
	 * @return true if this changed the stored state (so the caller knows to persist)
	 */
	public boolean recordRename(String oldName, String newName, long observedAtEpochMilli)
	{
		String trimmedOld = oldName == null ? "" : oldName.trim();
		String trimmedNew = newName == null ? "" : newName.trim();
		if (trimmedOld.isEmpty() || trimmedNew.isEmpty())
		{
			return false;
		}

		String oldKey = NameKey.normalize(trimmedOld);
		String newKey = NameKey.normalize(trimmedNew);
		if (oldKey.equals(newKey))
		{
			return false;
		}

		FriendMemory historyAtOldName = byNormalizedName.remove(oldKey);
		FriendMemory historyAtNewName = byNormalizedName.get(newKey);
		FriendMemory result = FriendMemory.renamed(
			historyAtOldName, historyAtNewName, trimmedOld, trimmedNew, observedAtEpochMilli);
		byNormalizedName.put(newKey, result);
		return true;
	}

	/**
	 * @return previous names for the account currently known as {@code currentName}, oldest first,
	 * or an empty list if none are known.
	 */
	public List<RememberedName> historyFor(String currentName)
	{
		FriendMemory memory = byNormalizedName.get(NameKey.normalize(currentName));
		return memory == null ? Collections.emptyList() : memory.getPreviousNames();
	}

	public Map<String, FriendMemory> snapshot()
	{
		return Collections.unmodifiableMap(byNormalizedName);
	}
}
