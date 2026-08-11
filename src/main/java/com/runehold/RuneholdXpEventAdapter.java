package com.runehold;

import com.runehold.domain.ManaAward;
import com.runehold.domain.ManaLedger;
import java.util.Objects;
import net.runelite.api.events.StatChanged;

final class RuneholdXpEventAdapter
{
	private final ManaLedger ledger;

	RuneholdXpEventAdapter(ManaLedger ledger)
	{
		this.ledger = Objects.requireNonNull(ledger, "ledger");
	}

	int record(StatChanged event)
	{
		Objects.requireNonNull(event, "event");
		if (event.getSkill() == null)
		{
			return 0;
		}

		ManaAward award = ledger.recordXp(event.getSkill().name(), event.getXp());
		return award.getManaAwarded();
	}
}
