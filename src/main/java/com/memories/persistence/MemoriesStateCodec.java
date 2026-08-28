package com.memories.persistence;

import com.google.gson.Gson;
import com.memories.domain.FriendMemory;
import com.memories.domain.MemoriesBook;
import com.memories.domain.RememberedName;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MemoriesStateCodec
{
	static final int MAX_STATE_JSON_LENGTH = 256 * 1024;
	static final int MAX_TRACKED_FRIENDS = 2_000;

	private final Gson gson;

	public MemoriesStateCodec(Gson gson)
	{
		this.gson = Objects.requireNonNull(gson, "gson");
	}

	public String encode(MemoriesBook book)
	{
		Objects.requireNonNull(book, "book");
		return gson.toJson(PersistedMemoriesState.fromDomain(book));
	}

	public MemoriesBook decode(String json)
	{
		if (json == null || json.length() > MAX_STATE_JSON_LENGTH || json.trim().isEmpty())
		{
			return new MemoriesBook();
		}

		try
		{
			PersistedMemoriesState persisted = gson.fromJson(json, PersistedMemoriesState.class);
			return restoreValidated(persisted);
		}
		catch (RuntimeException ex)
		{
			return new MemoriesBook();
		}
	}

	private MemoriesBook restoreValidated(PersistedMemoriesState persisted)
	{
		if (persisted == null
			|| persisted.schemaVersion != PersistedMemoriesState.CURRENT_SCHEMA_VERSION
			|| persisted.friends == null
			|| persisted.friends.size() > MAX_TRACKED_FRIENDS)
		{
			throw new IllegalArgumentException("incomplete Memories state");
		}

		Map<String, FriendMemory> byNormalizedName = new LinkedHashMap<>();
		for (Map.Entry<String, PersistedFriendMemory> entry : persisted.friends.entrySet())
		{
			String key = entry.getKey();
			PersistedFriendMemory value = entry.getValue();
			if (key == null || value == null || value.currentName == null || value.previousNames == null)
			{
				throw new IllegalArgumentException("invalid Memories friend entry");
			}

			List<RememberedName> previousNames = new ArrayList<>();
			for (PersistedRememberedName remembered : value.previousNames)
			{
				if (remembered == null || remembered.name == null)
				{
					throw new IllegalArgumentException("invalid Memories name entry");
				}
				previousNames.add(new RememberedName(remembered.name, remembered.observedAtEpochMilli));
			}

			byNormalizedName.put(key, FriendMemory.of(value.currentName, previousNames));
		}

		return MemoriesBook.restore(byNormalizedName);
	}
}
