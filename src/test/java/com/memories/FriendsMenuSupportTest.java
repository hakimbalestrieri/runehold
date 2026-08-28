package com.memories;

import net.runelite.api.gameval.InterfaceID;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FriendsMenuSupportTest
{
	@Test
	public void matchesTheMessageOptionOnTheFriendsListWidget()
	{
		assertTrue(FriendsMenuSupport.isFriendsListMessageEntry("Message", InterfaceID.FRIENDS));
	}

	@Test
	public void ignoresOtherOptionsOnTheFriendsListWidget()
	{
		assertFalse(FriendsMenuSupport.isFriendsListMessageEntry("Delete", InterfaceID.FRIENDS));
		assertFalse(FriendsMenuSupport.isFriendsListMessageEntry("Report", InterfaceID.FRIENDS));
	}

	@Test
	public void ignoresTheMessageOptionOutsideTheFriendsListWidget()
	{
		assertFalse(FriendsMenuSupport.isFriendsListMessageEntry("Message", InterfaceID.IGNORE));
	}
}
