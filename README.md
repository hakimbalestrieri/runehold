# Runehold

Runehold is a local RuneLite side game for Old School RuneScape. XP earned while
the plugin is observing a skill becomes mana, and mana grows a small persistent
isometric village. The current release validates the local builder loop before
any multiplayer or remote service is introduced.

Continuing from another computer or a new AI conversation? Start with
[PROJECT_CONTEXT.md](PROJECT_CONTEXT.md), which records the active branch,
current implementation, constraints, commands and next recommended milestone.
Codex automatically reads the repository's `AGENTS.md`; the OSRS art-direction
skill is also committed under `.agents/skills`, so no chat transcript or manual
skill installation is required after cloning.

## Current MVP

- A fresh village starts with 250 mana and a level-1 Town Hall.
- The first stat event for each skill establishes a baseline and awards nothing.
- Every subsequent 100 XP awards 1 mana; sub-100 XP remainders carry per skill.
- Mana earned from XP is capped at 10,000 per local calendar day.
- Town Hall, Mana Well, Mana Grove, Barracks, Workshop, and Rune Banner can be
  placed in a dedicated isometric village window.
- The village window supports Build, Edit, Move, Confirm, Cancel, Recenter, zoom,
  panning, placement ghosts, valid/invalid footprints, and a category catalogue.
- Non-test mode uses one construction job at a time with persisted completion
  deadlines; test mode gives unlimited mana and instant construction.
- Mana Grove produces collectable mana over time.
- Costs, unlock requirements, affordability, and maximum levels are enforced by
  one deterministic domain catalog.
- State is saved as versioned JSON in RuneLite's profile-scoped configuration.
- Missing, malformed, or unsupported state safely opens a fresh village.

Runehold does not consume OSRS items. Bank withdrawals, trades, Grand Exchange
purchases, inventory movement, and existing wealth award no mana.

## OSRS visual identity

The side panel and village window combine RuneLite's game-native regular/bold
fonts and cache-backed item sprites with an original Runehold skin: compact
square stone borders, an earthy brown palette, flat carved buttons, and hard
one-pixel text shadows. It does not inherit modern rounded or gradient button
rendering from the host look and feel.

OSRS inventory icons keep their native 36 x 32 slots. They come from the user's
local game cache through RuneLite's `ItemManager`; extracted game textures and
font files are not packaged in the plugin JAR. `osrsbox-db` and `osrs-icons` were
used only to calibrate dimensions, alpha behavior, material ramps, and item
identity.

RuneLite runtime assets may decorate HUD controls, while terrain, building PNGs,
construction scaffolding, and village visuals remain original project artwork.
See [the complete asset inventory and Jagex attribution](THIRD_PARTY_NOTICES.md).
The active visual targets are the
[OSRS 2007 Runehold previews](docs/design/runehold-village-art-direction.md), and
the packaged sprite inventory is documented in
[Runehold Village Assets](docs/design/runehold-village-assets.md).

## Building progression

| Building | Levels | Upgrade costs by target level | Town Hall requirement |
| --- | ---: | --- | --- |
| Town Hall | 1-5 | L2 200, L3 600, L4 1,500, L5 4,000 | Always present |
| Mana Well | 0-5 | L1 100, L2 250, L3 750, L4 2,000, L5 5,000 | Same as target level |
| Mana Grove | 0-4 | L1 150, L2 450, L3 1,200, L4 3,200 | TH 1, 2, 3, 4 |
| Barracks | 0-4 | L1 300, L2 900, L3 2,400, L4 6,000 | TH 2, 3, 4, 5 |
| Workshop | 0-3 | L1 800, L2 2,500, L3 7,000 | TH 3, 4, 5 |
| Rune Banner | 0-1 | L1 25 | TH 1 |

Barracks and Workshop intentionally remain foundations for later troops and
defenses. Mana Grove already has local passive mana production and collection.

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
`ExternalPluginManager` with `-Drunehold.testing=true`, so the local builder has
unlimited mana and instant construction for testing. For Jagex Accounts, follow
RuneLite's
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
              RuneholdPanel / VillageWindow
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
- UI presentation states, isometric projection, canvas rendering, interaction
  state, visual sprite variants, RuneLite event mapping, and plugin registration.
- Village-builder placement, movement, cancellation, construction jobs, Mana
  Grove production, save/reload, and a full functional builder scenario.
- A representative final screenshot is generated at
  `build/reports/village-preview.png` during the visual canvas test.

After automated checks pass, complete the
[manual in-game checklist](docs/manual-test-checklist.md). A passing JVM build is
not a substitute for user confirmation inside RuneLite.

## Post-MVP roadmap

Multiplayer villages, asynchronous raids, matchmaking, defenses, replays, clans,
and leaderboards require a separate specification and server-authoritative threat
model. They must remain opt-in and cannot expose private player data or influence
the OSRS economy.

## License

BSD 2-Clause. See [LICENSE](LICENSE).

The code licence does not grant rights to Jagex intellectual property displayed
at runtime. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
