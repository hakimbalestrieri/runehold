package com.runehold.persistence;

import com.runehold.domain.VillageState;
import java.time.LocalDate;
import java.util.Objects;
import net.runelite.client.config.ConfigManager;

public final class RuneholdStateStore
{
	public static final String CONFIG_GROUP = "runehold";
	public static final String STATE_KEY = "state";

	private final ProfileConfiguration configuration;
	private final RuneholdStateCodec codec;

	public RuneholdStateStore(ConfigManager configManager, RuneholdStateCodec codec)
	{
		this(new RuneLiteProfileConfiguration(configManager), codec);
	}

	RuneholdStateStore(ProfileConfiguration configuration, RuneholdStateCodec codec)
	{
		this.configuration = Objects.requireNonNull(configuration, "configuration");
		this.codec = Objects.requireNonNull(codec, "codec");
	}

	public VillageState load(LocalDate today)
	{
		if (!hasProfile())
		{
			return VillageState.fresh(today);
		}

		VillageState state = codec.decode(configuration.get(CONFIG_GROUP, STATE_KEY), today);
		state.rollEarningDayIfNeeded(today);
		return state;
	}

	public boolean save(VillageState state)
	{
		if (!hasProfile())
		{
			return false;
		}

		configuration.set(CONFIG_GROUP, STATE_KEY, codec.encode(state));
		return true;
	}

	private boolean hasProfile()
	{
		String profileKey = configuration.getProfileKey();
		return profileKey != null && !profileKey.trim().isEmpty();
	}

	interface ProfileConfiguration
	{
		String getProfileKey();

		String get(String group, String key);

		void set(String group, String key, String value);
	}

	private static final class RuneLiteProfileConfiguration implements ProfileConfiguration
	{
		private final ConfigManager configManager;

		private RuneLiteProfileConfiguration(ConfigManager configManager)
		{
			this.configManager = Objects.requireNonNull(configManager, "configManager");
		}

		@Override
		public String getProfileKey()
		{
			return configManager.getRSProfileKey();
		}

		@Override
		public String get(String group, String key)
		{
			return configManager.getRSProfileConfiguration(group, key);
		}

		@Override
		public void set(String group, String key, String value)
		{
			configManager.setRSProfileConfiguration(group, key, value);
		}
	}
}
