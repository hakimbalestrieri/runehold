# ADR-001: Use a local XP-derived economy for the Runehold MVP

## Status

Accepted

## Date

2026-08-11

## Context

Runehold is intended to become a village-building side game inspired by persistent
base builders. The original concept proposed turning resources accumulated in OSRS
into plugin mana and eventually allowing players to raid one another.

A RuneLite plugin cannot safely or verifiably consume OSRS items. Treating bank,
trade, inventory, or Grand Exchange activity as resource deposits would also make
ordinary transfers indistinguishable from genuine acquisition and would create an
easy duplication path. A multiplayer backend would add identity, privacy,
anti-cheat, moderation, availability, and server-authority requirements before the
basic progression loop had been validated.

The Plugin Hub also requires the plugin to remain informational and user-driven: it
must not automate game input, send game actions, or expose player information.

## Decision

The MVP uses only positive XP deltas observed through RuneLite `StatChanged` events:

- The first event for each skill establishes a zero-award baseline.
- Each 100 XP awards 1 mana, with a per-skill remainder.
- A local-calendar daily cap limits awards to 10,000 mana.
- `Skill.OVERALL`/null skill events are ignored to prevent double-counting.
- Existing wealth, items, bank activity, trades, and Grand Exchange activity do
  not affect mana.

The economy and village are pure local domain code. A versioned JSON state is saved
through RuneLite's profile-scoped `ConfigManager`; no global fallback is used when
a profile is unavailable. The MVP performs no network requests.

## Alternatives considered

### Count items or bank value

This would look closer to “spending resources,” but the plugin cannot remove items
from the game. Transfers, withdrawals, and purchases could repeatedly mint plugin
currency. Rejected for weak semantics and trivial duplication.

### Require manual item sacrifice declarations

The player could press a button claiming that items were sacrificed. This would be
entirely trust-based, disconnected from OSRS state, and confusing when the items
remain usable. Rejected for the MVP.

### Launch with a multiplayer backend

This would enable attacks immediately, but client-reported XP and village changes
cannot secure competitive rewards. It would also require opt-in data transmission,
identity mapping, abuse controls, operations, and a server-authoritative battle
engine. Deferred until the local loop demonstrates value.

### Store one global village

A global ConfigManager key is simpler but would mix RuneScape profiles and make
account switching unsafe. Rejected in favor of profile-scoped state.

## Consequences

- The MVP is deterministic, offline, private, and inexpensive to operate.
- Earning mana follows genuine gameplay progress without changing OSRS itself.
- The first snapshot after startup or profile change cannot award historical XP.
- The village is independently persisted for each RuneScape profile.
- Items cannot be “spent” in this version, which is less literal than the original
  concept but avoids pretending that the plugin controls the OSRS economy.
- Multiplayer raids require a new specification and ADR covering consent, identity,
  server authority, anti-cheat limits, moderation, and failure handling.
