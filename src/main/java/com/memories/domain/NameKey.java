package com.memories.domain;

import java.util.Locale;

final class NameKey
{
	private NameKey()
	{
	}

	static String normalize(String name)
	{
		if (name == null)
		{
			return "";
		}

		return name.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
	}
}
