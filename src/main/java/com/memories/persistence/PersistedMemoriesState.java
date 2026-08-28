package com.memories.persistence;

import com.memories.domain.FriendMemory;
import com.memories.domain.MemoriesBook;
import com.memories.domain.RememberedName;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

final class PersistedMemoriesState
{
	static final int CURRENT_SCHEMA_VERSION = 1;

	int schemaVersion;
	Map<String, PersistedFriendMemory> friends;

	private PersistedMemoriesState()
	{
	}

	static PersistedMemoriesState fromDomain(MemoriesBook book)
	{
		PersistedMemoriesState persisted = new PersistedMemoriesState();
		persisted.schemaVersion = CURRENT_SCHEMA_VERSION;
		persisted.friends = new LinkedHashMap<>();

		for (Map.Entry<String, FriendMemory> entry : book.snapshot().entrySet())
		{
			FriendMemory memory = entry.getValue();

			PersistedFriendMemory persistedMemory = new PersistedFriendMemory();
			persistedMemory.currentName = memory.getCurrentName();
			persistedMemory.previousNames = new ArrayList<>();
			for (RememberedName remembered : memory.getPreviousNames())
			{
				PersistedRememberedName persistedName = new PersistedRememberedName();
				persistedName.name = remembered.getName();
				persistedName.observedAtEpochMilli = remembered.getObservedAtEpochMilli();
				persistedMemory.previousNames.add(persistedName);
			}

			persisted.friends.put(entry.getKey(), persistedMemory);
		}

		return persisted;
	}
}
