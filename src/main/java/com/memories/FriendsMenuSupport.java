package com.memories;

import net.runelite.api.gameval.InterfaceID;

final class FriendsMenuSupport
{
	static final String MEMORIES_OPTION = "Memories";
	private static final String FRIENDS_LIST_ANCHOR_OPTION = "Message";

	private FriendsMenuSupport()
	{
	}

	/**
	 * "Message" is the one default option present for every row on the friends list
	 * (online or offline), so it anchors exactly one "Memories" entry per right-click.
	 */
	static boolean isFriendsListMessageEntry(String option, int widgetInterfaceId)
	{
		return FRIENDS_LIST_ANCHOR_OPTION.equals(option) && widgetInterfaceId == InterfaceID.FRIENDS;
	}
}
