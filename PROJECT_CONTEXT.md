# Runehold project context

Last updated: 2026-08-12

This file is the handoff document for continuing Runehold from another
computer or a new AI conversation. Read it together with `AGENTS.md` before
changing the project.

## Automatic Codex handoff

No previous conversation or long bootstrap prompt is required. When the cloned
repository root is opened as the Codex workspace, Codex automatically reads the
root `AGENTS.md`. That file requires the agent to read this context, inspect the
handoff branch, run the baseline verification and continue from the recommended
milestone.

The custom OSRS art-direction skill is committed at
`.agents/skills/osrs-2007-art-direction`, the repository-scoped discovery path.
It is therefore available after cloning without copying it into the user's
profile. For visual work, invoke or allow Codex to invoke
`$osrs-2007-art-direction`.

On a fresh computer:

1. Install Git, Codex Desktop and Temurin JDK 21.
2. Clone the repository and switch to the handoff branch with the commands
   below.
3. Open the cloned `runehold` directory as the Codex workspace.
4. Start a task with: `Continue le developpement de Runehold.`

Codex must then perform the startup sequence in `AGENTS.md` before editing.

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
- Town Hall, Mana Well, Mana Grove, Barracks, Workshop and Rune Banner have
  deterministic costs, locks, footprints and maximum levels.
- Versioned state is stored with RuneLite's profile-scoped `ConfigManager`.
- Corrupt or unsupported state safely falls back to a fresh village.
- The RuneLite side panel uses game-native fonts and cache-backed item sprites.
- The UI uses an original square stone/wood OSRS-style skin.
- The toolbar lifecycle, profile switching and shutdown cleanup are implemented.
- The dedicated village window is implemented with an isometric canvas, Build
  catalogue, placement ghosts, valid/invalid footprints, Confirm/Cancel, Edit
  mode, Move, Recenter, zoom and panning.
- Construction jobs, persisted deadlines, Mana Grove production/collection, mana
  capacity, unlimited test mana and versioned layout persistence are implemented.
- Original packaged building PNGs live under `src/main/resources/village/`; the
  atlas uses nearest-neighbor scaling and cached level variants.
- The automated suite currently contains 78 passing tests.
- The normal Gradle test and build tasks pass locally.
- The first village-builder domain slice defines an 18 x 18 layout, deterministic
  Town Hall placement, centralized building footprints, collision checks and
  atomic placement/movement commands.

The user still needs to complete and confirm the logged-in development-client
checklist in `docs/manual-test-checklist.md`.

## Not implemented yet

- Troops, defenses, combat simulation or raid replays.
- Accounts, backend, matchmaking, clans or leaderboards.
- Any network communication or upload of player information.

The recommended next milestone is a manual RuneLite pass on the development
client, followed by deeper polish for multiple builders, richer construction
animation, and later troop/defense systems. Keep it offline and deterministic;
design the multiplayer protocol only after the local interaction is proven.

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
- `.agents/skills/osrs-2007-art-direction`: auto-discovered reusable OSRS visual
  review workflow.

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

## Starting a new Codex task

The minimum message is sufficient because `AGENTS.md` contains the mandatory
bootstrap sequence:

> Continue le developpement de Runehold.

To select a particular increment, append it to that sentence. If no increment
is specified, continue with manual RuneLite validation and builder polish.
