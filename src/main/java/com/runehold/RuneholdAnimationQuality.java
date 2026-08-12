package com.runehold;

public enum RuneholdAnimationQuality
{
	LOW("Low", 500),
	STANDARD("Standard", 250),
	HIGH("High", 125);

	private final String name;
	private final int frameMillis;

	RuneholdAnimationQuality(String name, int frameMillis)
	{
		this.name = name;
		this.frameMillis = frameMillis;
	}

	public int getFrameMillis()
	{
		return frameMillis;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
