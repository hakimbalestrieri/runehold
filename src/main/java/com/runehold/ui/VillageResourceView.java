package com.runehold.ui;

import java.util.Objects;

public final class VillageResourceView
{
	public enum Kind
	{
		MANA,
		MANA_CAPACITY,
		GROVE_MANA,
		DAILY_XP_MANA,
		BUILDER,
		VILLAGE,
		WORKERS,
		RESOURCES
	}

	private final Kind kind;
	private final String title;
	private final String value;
	private final String tooltip;

	public VillageResourceView(Kind kind, String title, String value, String tooltip)
	{
		this.kind = Objects.requireNonNull(kind, "kind");
		this.title = Objects.requireNonNull(title, "title");
		this.value = Objects.requireNonNull(value, "value");
		this.tooltip = Objects.requireNonNull(tooltip, "tooltip");
	}

	public Kind getKind()
	{
		return kind;
	}

	public String getTitle()
	{
		return title;
	}

	public String getValue()
	{
		return value;
	}

	public String getTooltip()
	{
		return tooltip;
	}
}
