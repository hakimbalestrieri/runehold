package com.memories.domain;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MemoriesBookTest
{
	private MemoriesBook book;

	@Before
	public void setUp()
	{
		book = new MemoriesBook();
	}

	@Test
	public void unknownNameHasNoHistory()
	{
		assertTrue(book.historyFor("Nobody").isEmpty());
	}

	@Test
	public void singleRenameIsRemembered()
	{
		assertTrue(book.recordRename("Alice", "Bob", 1_000L));

		List<String> names = namesOf(book.historyFor("Bob"));
		assertEquals(1, names.size());
		assertEquals("Alice", names.get(0));
		assertTrue(book.historyFor("Alice").isEmpty());
	}

	@Test
	public void renameChainIsPreservedOldestFirst()
	{
		book.recordRename("Alice", "Bob", 1_000L);
		book.recordRename("Bob", "Charlie", 2_000L);

		List<String> names = namesOf(book.historyFor("Charlie"));
		assertEquals(2, names.size());
		assertEquals("Alice", names.get(0));
		assertEquals("Bob", names.get(1));
	}

	@Test
	public void mergesHistoryWhenRenamingIntoAnAlreadyTrackedName()
	{
		book.recordRename("Xylo", "Zed", 500L);
		book.recordRename("Alice", "Bob", 1_000L);
		book.recordRename("Bob", "Zed", 2_000L);

		List<String> names = namesOf(book.historyFor("Zed"));
		assertEquals(3, names.size());
		assertEquals("Xylo", names.get(0));
		assertEquals("Alice", names.get(1));
		assertEquals("Bob", names.get(2));
	}

	@Test
	public void repeatingTheSameRenameEventDoesNotDuplicateTheEntry()
	{
		book.recordRename("Alice", "Bob", 1_000L);
		book.recordRename("Alice", "Bob", 1_000L);

		assertEquals(1, book.historyFor("Bob").size());
	}

	@Test
	public void nameMatchingIsCaseAndWhitespaceInsensitive()
	{
		book.recordRename("Alice", "Bob", 1_000L);

		assertEquals(1, book.historyFor("  bOB  ").size());
	}

	@Test
	public void blankOrNullNamesAreIgnored()
	{
		assertFalse(book.recordRename(null, "Bob", 1_000L));
		assertFalse(book.recordRename("Alice", null, 1_000L));
		assertFalse(book.recordRename("  ", "Bob", 1_000L));
		assertFalse(book.recordRename("Alice", "", 1_000L));
	}

	@Test
	public void renamingToTheSameNameIsANoOp()
	{
		assertFalse(book.recordRename("Alice", "alice", 1_000L));
		assertTrue(book.historyFor("Alice").isEmpty());
	}

	@Test
	public void historyIsCappedToTheMostRecentEntries()
	{
		String current = "Start";
		for (int i = 0; i < FriendMemory.MAX_HISTORY + 5; i++)
		{
			String next = "Name" + i;
			book.recordRename(current, next, i);
			current = next;
		}

		List<RememberedName> history = book.historyFor(current);
		assertEquals(FriendMemory.MAX_HISTORY, history.size());
		assertEquals("Name4", history.get(0).getName());
	}

	private static List<String> namesOf(List<RememberedName> history)
	{
		return history.stream().map(RememberedName::getName).collect(Collectors.toList());
	}
}
