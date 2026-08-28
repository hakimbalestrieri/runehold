package com.memories;

import com.google.gson.Gson;
import com.memories.domain.MemoriesBook;
import com.memories.domain.RememberedName;
import com.memories.persistence.MemoriesStateCodec;
import com.memories.persistence.MemoriesStateStore;
import com.memories.ui.MemoriesDialog;
import java.awt.Canvas;
import java.util.List;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Friend;
import net.runelite.api.FriendContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.Nameable;
import net.runelite.api.ScriptID;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.NameableNameChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Memories",
	description = "Remembers the previous names of people on your friends list"
)
public class MemoriesPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Gson gson;

	private MemoriesStateStore stateStore;
	private MemoriesBook book;

	@Override
	protected void startUp()
	{
		stateStore = new MemoriesStateStore(configManager, new MemoriesStateCodec(gson));
		book = stateStore.load();
		log.debug("Memories started");
	}

	@Override
	protected void shutDown()
	{
		book = null;
		stateStore = null;
		log.debug("Memories stopped");
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == ScriptID.FRIENDS_UPDATE)
		{
			reconcileAll();
		}
	}

	@Subscribe
	public void onNameableNameChanged(NameableNameChanged event)
	{
		Nameable nameable = event.getNameable();
		if (!(nameable instanceof Friend))
		{
			return;
		}

		if (reconcile(nameable.getPrevName(), nameable.getName()))
		{
			stateStore.save(book);
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		int widgetInterfaceId = WidgetUtil.componentToInterface(event.getActionParam1());
		if (!FriendsMenuSupport.isFriendsListMessageEntry(event.getOption(), widgetInterfaceId))
		{
			return;
		}

		String target = event.getTarget();
		String name = Text.toJagexName(Text.removeTags(target));

		FriendContainer friends = client.getFriendContainer();
		Friend friend = friends == null ? null : friends.findByName(name);
		if (friend != null && reconcile(friend.getPrevName(), friend.getName()))
		{
			stateStore.save(book);
		}

		Canvas canvas = client.getCanvas();
		client.getMenu().createMenuEntry(-1)
			.setOption(FriendsMenuSupport.MEMORIES_OPTION)
			.setType(MenuAction.RUNELITE)
			.setTarget(target)
			.onClick(e -> showMemories(canvas, name));
	}

	private void showMemories(Canvas canvas, String name)
	{
		List<RememberedName> history = book.historyFor(name);
		SwingUtilities.invokeLater(() ->
			MemoriesDialog.show(SwingUtilities.getWindowAncestor(canvas), name, history));
	}

	private void reconcileAll()
	{
		FriendContainer friends = client.getFriendContainer();
		if (friends == null)
		{
			return;
		}

		boolean changed = false;
		for (Friend friend : friends.getMembers())
		{
			if (reconcile(friend.getPrevName(), friend.getName()))
			{
				changed = true;
			}
		}

		if (changed)
		{
			stateStore.save(book);
		}
	}

	private boolean reconcile(String prevName, String currentName)
	{
		if (prevName == null || prevName.isEmpty() || prevName.equalsIgnoreCase(currentName))
		{
			return false;
		}

		return book.recordRename(prevName, currentName, System.currentTimeMillis());
	}
}
