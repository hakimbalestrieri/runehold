package com.memories.persistence;

import com.google.gson.Gson;
import com.memories.domain.MemoriesBook;
import com.memories.domain.RememberedName;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MemoriesStateCodecTest
{
	private MemoriesStateCodec codec;

	@Before
	public void setUp()
	{
		codec = new MemoriesStateCodec(new Gson());
	}

	@Test
	public void roundTripsThroughJson()
	{
		MemoriesBook book = new MemoriesBook();
		book.recordRename("Alice", "Bob", 1_000L);
		book.recordRename("Bob", "Charlie", 2_000L);

		MemoriesBook restored = codec.decode(codec.encode(book));

		List<String> names = restored.historyFor("Charlie").stream()
			.map(RememberedName::getName)
			.collect(Collectors.toList());
		assertEquals(2, names.size());
		assertEquals("Alice", names.get(0));
		assertEquals("Bob", names.get(1));
	}

	@Test
	public void missingStateProducesAFreshBook()
	{
		assertTrue(codec.decode(null).historyFor("Bob").isEmpty());
		assertTrue(codec.decode("").historyFor("Bob").isEmpty());
		assertTrue(codec.decode("   ").historyFor("Bob").isEmpty());
	}

	@Test
	public void malformedJsonProducesAFreshBookInsteadOfFailingStartup()
	{
		assertTrue(codec.decode("{not json").historyFor("Bob").isEmpty());
	}

	@Test
	public void unsupportedSchemaVersionProducesAFreshBook()
	{
		String json = "{\"schemaVersion\":999,\"friends\":{}}";

		assertTrue(codec.decode(json).historyFor("Bob").isEmpty());
	}

	@Test
	public void incompleteFriendEntryProducesAFreshBook()
	{
		String json = "{\"schemaVersion\":1,\"friends\":{\"bob\":{\"currentName\":\"Bob\"}}}";

		assertTrue(codec.decode(json).historyFor("Bob").isEmpty());
	}

	@Test
	public void oversizedStateProducesAFreshBookRatherThanBeingParsed()
	{
		StringBuilder oversized = new StringBuilder("{\"schemaVersion\":1,\"friends\":{");
		while (oversized.length() <= MemoriesStateCodec.MAX_STATE_JSON_LENGTH)
		{
			oversized.append('a');
		}
		oversized.append("}}");

		assertTrue(codec.decode(oversized.toString()).historyFor("Bob").isEmpty());
	}
}
