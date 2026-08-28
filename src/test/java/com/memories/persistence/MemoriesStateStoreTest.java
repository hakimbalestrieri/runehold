package com.memories.persistence;

import com.google.gson.Gson;
import com.memories.domain.MemoriesBook;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MemoriesStateStoreTest
{
	private FakeConfiguration configuration;
	private MemoriesStateStore store;

	@Before
	public void setUp()
	{
		configuration = new FakeConfiguration();
		store = new MemoriesStateStore(configuration, new MemoriesStateCodec(new Gson()));
	}

	@Test
	public void loadsAFreshBookWhenNothingIsStoredYet()
	{
		assertTrue(store.load().historyFor("Bob").isEmpty());
		assertEquals(1, configuration.readCount);
	}

	@Test
	public void savesAndLoadsThroughGlobalConfiguration()
	{
		MemoriesBook book = new MemoriesBook();
		book.recordRename("Alice", "Bob", 1_000L);

		store.save(book);
		MemoriesBook restored = store.load();

		assertEquals("memories", configuration.lastGroup);
		assertEquals("friendNameHistory", configuration.lastKey);
		assertEquals(1, restored.historyFor("Bob").size());
	}

	private static final class FakeConfiguration implements MemoriesStateStore.Configuration
	{
		private String value;
		private String lastGroup;
		private String lastKey;
		private int readCount;

		@Override
		public String get(String group, String key)
		{
			readCount++;
			lastGroup = group;
			lastKey = key;
			return value;
		}

		@Override
		public void set(String group, String key, String newValue)
		{
			lastGroup = group;
			lastKey = key;
			value = newValue;
		}
	}
}
