package com.runehold;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.PluginDescriptor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RuneholdPluginTest
{
	@Test
	public void pluginHasRuneLiteDescriptor()
	{
		PluginDescriptor descriptor = RuneholdPlugin.class.getAnnotation(PluginDescriptor.class);

		assertNotNull(descriptor);
		assertEquals("Runehold", descriptor.name());
	}

	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(RuneholdPlugin.class);
		RuneLite.main(args);
	}
}
