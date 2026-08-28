package com.memories;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.PluginDescriptor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings("unchecked")
public class MemoriesPluginTest
{
	@Test
	public void pluginHasRuneLiteDescriptor()
	{
		PluginDescriptor descriptor = MemoriesPlugin.class.getAnnotation(PluginDescriptor.class);

		assertNotNull(descriptor);
		assertEquals("Memories", descriptor.name());
	}

	@Test
	public void pluginCanBeRegisteredAsBuiltin() throws Exception
	{
		ExternalPluginManager.loadBuiltin(MemoriesPlugin.class);
	}

	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(MemoriesPlugin.class);
		RuneLite.main(args);
	}
}
