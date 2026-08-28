# Runehold

Runehold is a local RuneLite side game for Old School RuneScape. XP earned while
the plugin is observing a skill becomes mana, and mana grows a small persistent
village. The first release validates the progression loop before any multiplayer
or remote service is introduced.

## Current MVP

- A fresh village starts with 250 mana and a level-1 Town Hall.
- The first stat event for each skill establishes a baseline and awards nothing.
- Every subsequent 100 XP awards 1 mana; sub-100 XP remainders carry per skill.
- Mana earned from XP is capped at 10,000 per local calendar day.
- Town Hall, Mana Well, Barracks, and Workshop can be built or upgraded.
- Costs, unlock requirements, affordability, and maximum levels are enforced by
  one deterministic domain catalog.
- State is saved as versioned JSON in RuneLite's profile-scoped configuration.
- Missing, malformed, or unsupported state safely opens a fresh village.

Runehold does not consume OSRS items. Bank withdrawals, trades, Grand Exchange
purchases, inventory movement, and existing wealth award no mana.

## Building progression

| Building | Levels | Upgrade costs by target level | Town Hall requirement |
| --- | ---: | --- | --- |
| Town Hall | 1–5 | L2 200, L3 600, L4 1,500, L5 4,000 | — |
| Mana Well | 0–5 | L1 100, L2 250, L3 750, L4 2,000, L5 5,000 | Same as target level |
| Barracks | 0–4 | L1 300, L2 900, L3 2,400, L4 6,000 | TH 2, 3, 4, 5 |
| Workshop | 0–3 | L1 800, L2 2,500, L3 7,000 | TH 3, 4, 5 |

The non-Town-Hall buildings intentionally represent foundations for later game
systems. They do not yet produce troops, defenses, passive mana, or combat power.

## Privacy and safety

- The MVP has no network requests, accounts, authentication, telemetry, or remote
  backend.
- No player information is transmitted.
- Runehold never injects mouse or keyboard input and never sends game actions.
- Plugin currency cannot be exchanged for OSRS GP, items, or real money.
- Competitive raids and leaderboards are explicitly outside this release.

## Development quick start

Requirements:

- A JDK supported by Gradle 8.10, preferably Temurin JDK 21.
- Internet access on the first build so Gradle can resolve RuneLite artifacts.

The source targets Java 11. JDK 25 cannot currently run the official Gradle 8.10
wrapper; select JDK 21 with `JAVA_HOME` if JDK 25 is your system default.

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat run
```

The `run` task opens a RuneLite development client through
`ExternalPluginManager`. For Jagex Accounts, follow RuneLite's
[development-client login instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).
Only the user should log in and interact with the game.

## Architecture

```text
RuneLite StatChanged
        |
        v
RuneholdXpEventAdapter -> ManaLedger -> VillageState
                                           |
                         +-----------------+-----------------+
                         |                                   |
                         v                                   v
                  RuneholdController                 RuneholdStateStore
                         |                             profile JSON only
                         v
                    RuneholdPanel
```

- `com.runehold.domain` is pure Java and has no RuneLite or Swing imports.
- `com.runehold.persistence` validates the versioned JSON boundary.
- `com.runehold.ui` renders domain previews and never duplicates economy rules.
- `RuneholdPlugin` owns lifecycle, profile changes, toolbar registration, and
  RuneLite event delegation.
- Mutable economy commands are serialized on RuneLite's `ClientThread`; the Swing
  EDT receives immutable view models only.

The design rationale is recorded in
[ADR-001](docs/decisions/001-local-xp-economy.md), and the approved MVP behavior is
in [the specification](docs/specs/runehold-mvp.md).

## Verification

Automated coverage includes:

- XP baselines, invalid deltas, per-skill remainders, daily cap, and rollover.
- Building costs, unlocks, maximum levels, previews, and atomic failures.
- Persistence round-trips, corrupt state, unsupported schemas, and missing
  profiles.
- UI presentation states, event-dispatch-thread construction, RuneLite event
  mapping, and plugin registration.

After automated checks pass, complete the
[manual in-game checklist](docs/manual-test-checklist.md). A passing JVM build is
not a substitute for user confirmation inside RuneLite.

## Memories plugin

This repository also hosts a second, unrelated RuneLite plugin: **Memories**,
which remembers the previous names of people on your friends list and adds a
right-click "Memories" option to view them. It does not share any code,
configuration, or state with Runehold. See [MEMORIES.md](MEMORIES.md) for
details, including how it's packaged alongside Runehold in this repository.

## Post-MVP roadmap

Multiplayer villages, asynchronous raids, matchmaking, defenses, replays, clans,
and leaderboards require a separate specification and server-authoritative threat
model. They must remain opt-in and cannot expose private player data or influence
the OSRS economy.

## License

BSD 2-Clause. See [LICENSE](LICENSE).
