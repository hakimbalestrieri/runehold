# Runehold project context

Last updated: 2026-08-11

This file is the handoff document for continuing Runehold from another
computer or a new AI conversation. Read it together with `AGENTS.md` before
changing the project.

## Repository and active branch

- GitHub: <https://github.com/hakimbalestrieri/runehold>
- Latest development branch: `feature/village-builder`
- `main` contains the validated MVP before the OSRS visual pass.
- Do not assume a fresh clone is on the latest branch; switch explicitly.

```powershell
git clone https://github.com/hakimbalestrieri/runehold.git
cd runehold
git switch feature/village-builder
git pull --ff-only
```

## Product direction

Runehold is a RuneLite side game inspired by Clash of Clans and rendered in an
authentic 2007-era OSRS visual language. XP earned while the plugin is active is
converted into local plugin mana. Mana builds and upgrades a persistent
village. The intended long-term game adds a bounded village map, placement,
construction, defenses, troops and asynchronous attacks against other Runehold
villages.

The current release is deliberately local-only. It validates the economy,
persistence and RuneLite integration before multiplayer or a backend is added.

## Implemented and validated

- XP baselines prevent previously earned XP from granting mana.
- Every 100 newly earned XP grants 1 mana, with per-skill remainders.
- Daily XP mana is capped at 10,000.
- Town Hall, Mana Well, Barracks and Workshop have deterministic costs, locks
  and maximum levels.
- Versioned state is stored with RuneLite's profile-scoped `ConfigManager`.
- Corrupt or unsupported state safely falls back to a fresh village.
- The RuneLite side panel uses game-native fonts and cache-backed item sprites.
- The UI uses an original square stone/wood OSRS-style skin.
- The toolbar lifecycle, profile switching and shutdown cleanup are implemented.
- The automated suite currently contains 41 passing tests.
- The normal Gradle build and the Plugin Hub `standard` build both pass.
- The official API recorder reported no disallowed APIs.

The user still needs to complete and confirm the logged-in development-client
checklist in `docs/manual-test-checklist.md`.

## Not implemented yet

- A dedicated visual village map or bounded placement grid.
- Drag/place/move controls for buildings.
- Construction timers, builders or building animations.
- Troops, defenses, combat simulation or raid replays.
- Accounts, backend, matchmaking, clans or leaderboards.
- Any network communication or upload of player information.

The recommended next milestone is the local village canvas and placement model.
Keep it offline and deterministic; design the multiplayer protocol only after
the local interaction is proven.

## Architecture map

- `src/main/java/com/runehold/RuneholdPlugin.java`: RuneLite lifecycle, events,
  profile changes, toolbar registration and client-thread commands.
- `src/main/java/com/runehold/domain`: pure Java economy and village rules.
- `src/main/java/com/runehold/persistence`: versioned JSON validation and
  profile-scoped storage.
- `src/main/java/com/runehold/ui`: immutable view models and Swing rendering.
- `src/test/java/com/runehold`: domain, persistence, UI and registration tests.
- `docs/specs/runehold-mvp.md`: approved current behavior.
- `docs/specs/runehold-village-builder.md`: larger village-builder direction.
- `docs/design/runehold-village-art-direction.md`: active visual target.
- `THIRD_PARTY_NOTICES.md`: asset provenance and Jagex attribution.
- `skills/osrs-2007-art-direction`: reusable OSRS visual review workflow.

## Non-negotiable RuneLite constraints

- Target Java 11 and retain `build=standard`.
- Never call `Thread.sleep()`, `Thread.interrupt()` or create custom threads or
  executors for gameplay/UI sequencing.
- Never use reflection, JNI/JNA, unsafe native access, dynamic code loading,
  Java serialization or external processes.
- Never inject mouse/keyboard input or send game actions.
- Never perform blocking network or disk I/O on the client thread.
- Keep game-state mutations on RuneLite's `ClientThread` and Swing mutations on
  the event dispatch thread.
- Keep the existing `runehold` config group and keys stable or add a migration.
- Do not bundle extracted Jagex fonts, textures or item sprites. Obtain approved
  runtime assets through RuneLite APIs and keep original Runehold artwork for
  custom buildings and terrain.
- Multiplayer requires a new security/privacy review, explicit opt-in and the
  Plugin Hub third-party-server warning. Do not upload OSRS names, locations,
  equipment or other players' data.

## Local verification

The wrapper is Gradle 8.10. The source targets Java 11; Temurin JDK 21 is the
recommended local runtime because JDK 25 cannot currently run this wrapper.

```powershell
# Point JAVA_HOME to the JDK 21 installation on the new computer if needed.
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat run
```

Only the user may log in and test RuneScape interactions. Do not automate the
game client. Follow the manual checklist after automated checks pass.

## Prompt for a new Codex conversation

Copy this prompt into the new task and append the feature to implement:

> Open and completely read `AGENTS.md`, `PROJECT_CONTEXT.md`,
> `docs/specs/runehold-mvp.md`, `docs/specs/runehold-village-builder.md`,
> `docs/design/runehold-village-art-direction.md` and `tasks/todo.md`. Inspect
> the current Git branch and run the existing tests before changing anything.
> Continue Runehold from `feature/village-builder`, preserving the current
> Plugin Hub compliance constraints and OSRS 2007 art direction. Implement the
> next change incrementally with tests, then commit and push it. The feature I
> want next is: [describe the next feature here].
