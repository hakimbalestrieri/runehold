package com.memories.persistence;

import com.memories.domain.MemoriesBook;
import java.util.Objects;
import net.runelite.client.config.ConfigManager;

/**
 * Stores name history globally rather than per RuneScape profile: a friend's identity and
 * naming history do not depend on which of the local user's own accounts is logged in.
 */
public final class MemoriesStateStore
{
	public static final String CONFIG_GROUP = "memories";
	public static final String STATE_KEY = "friendNameHistory";

	private final Configuration configuration;
	private final MemoriesStateCodec codec;

	public MemoriesStateStore(ConfigManager configManager, MemoriesStateCodec codec)
	{
		this(new RuneLiteConfiguration(configManager), codec);
	}

	MemoriesStateStore(Configuration configuration, MemoriesStateCodec codec)
	{
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.codec = Objects.requireNonNull(codec, "codec");
	}

	public MemoriesBook load()
	{
		return codec.decode(configuration.get(CONFIG_GROUP, STATE_KEY));
	}

	public void save(MemoriesBook book)
	{
		configuration.set(CONFIG_GROUP, STATE_KEY, codec.encode(book));
	}

	interface Configuration
	{
		String get(String group, String key);

		void set(String group, String key, String value);
	}

	private static final class RuneLiteConfiguration implements Configuration
	{
		private final ConfigManager configManager;

		private RuneLiteConfiguration(ConfigManager configManager)
		{
			this.configManager = Objects.requireNonNull(configManager, "configManager");
		}

		@Override
		public String get(String group, String key)
		{
			return configManager.getConfiguration(group, key);
		}

		@Override
		public void set(String group, String key, String value)
		{
			configManager.setConfiguration(group, key, value);
		}
	}
}
