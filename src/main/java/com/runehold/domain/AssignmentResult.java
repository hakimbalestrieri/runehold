package com.runehold.domain;

public final class AssignmentResult
{
	public enum Status
	{
		SUCCESS,
		UNKNOWN_WORKER,
		UNKNOWN_SITE,
		SITE_NOT_BUILT,
		ALREADY_ASSIGNED,
		WORKER_LIMIT,
		NO_ACCESSIBLE_PATH
	}

	private final Status status;
	private final String message;

	private AssignmentResult(Status status, String message)
	{
		this.status = status;
		this.message = message;
	}

	public static AssignmentResult success()
	{
		return new AssignmentResult(Status.SUCCESS, "Assigned");
	}

	public static AssignmentResult failure(Status status, String message)
	{
		return new AssignmentResult(status, message);
	}

	public boolean isSuccess()
	{
		return status == Status.SUCCESS;
	}

	public Status getStatus()
	{
		return status;
	}

	public String getMessage()
	{
		return message;
	}
}
