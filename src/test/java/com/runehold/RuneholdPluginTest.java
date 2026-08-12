package com.runehold;

import com.runehold.domain.AssignmentResult;
import com.runehold.domain.ResourceCollectResult;
import com.runehold.domain.ResourceType;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.PluginDescriptor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings("unchecked")
public class RuneholdPluginTest
{
	@Test
	public void pluginHasRuneLiteDescriptor()
	{
		PluginDescriptor descriptor = RuneholdPlugin.class.getAnnotation(PluginDescriptor.class);

		assertNotNull(descriptor);
		assertEquals("Runehold", descriptor.name());
	}

	@Test
	public void pluginCanBeRegisteredAsBuiltin() throws Exception
	{
		ExternalPluginManager.loadBuiltin(RuneholdPlugin.class);
	}

	@Test
	public void refusedGatheringCommandsProduceAPlayerFacingNotice()
	{
		assertNull(RuneholdPlugin.noticeFor(AssignmentResult.success()));
		assertEquals("No worker slot available", RuneholdPlugin.noticeFor(
			AssignmentResult.failure(
				AssignmentResult.Status.WORKER_LIMIT,
				"No worker slot available")));
		assertNull(RuneholdPlugin.noticeFor(
			new ResourceCollectResult(ResourceType.ORE, 30, 0, false)));
		assertEquals("Nothing to collect yet.", RuneholdPlugin.noticeFor(
			new ResourceCollectResult(ResourceType.ORE, 0, 0, false)));
		assertEquals("Village storage is full. 30 ore stayed at the site.",
			RuneholdPlugin.noticeFor(
				new ResourceCollectResult(ResourceType.ORE, 0, 30, true)));
	}

	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(RuneholdPlugin.class);
		RuneLite.main(args);
	}
}
