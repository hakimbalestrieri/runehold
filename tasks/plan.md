# Runehold MVP — Implementation Plan

Status: awaiting implementation approval  
Specification: `docs/specs/runehold-mvp.md`

## Outcome

Deliver a standard RuneLite Plugin Hub-compatible plugin that turns newly earned
OSRS XP into a local, capped mana currency and lets the player spend that mana to
upgrade a persistent village from a RuneLite side panel.

The MVP ends at the local loop:

`StatChanged -> XP delta -> mana -> building upgrade -> profile persistence -> UI refresh`

Multiplayer, raids, remote accounts, telemetry, and network services are excluded.

## Architecture

### Pure domain

The domain package has no RuneLite or Swing imports. It owns:

- XP baselines, remainders, daily mana cap, and date rollover.
- Village balance and building levels.
- Building definitions, unlock requirements, costs, and maximum levels.
- Atomic upgrade validation and results.
- The versioned persisted-state model.

Time is supplied through a small injectable date source so rollover behavior is
deterministic in tests.

### RuneLite adapters

- `RuneholdPlugin` owns lifecycle and subscribes to `StatChanged`.
- An event adapter converts RuneLite skills and XP values into domain commands.
- Profile state is stored as one versioned JSON value through `ConfigManager`.
- XP baselines are cleared when the plugin stops or the active game session/profile
  changes, preventing deltas from crossing accounts.
- If no RuneScape profile is available, play remains in memory; persistence resumes
  only when profile-scoped configuration is available.

### UI

- A `PluginPanel` displays mana, daily progress, and all four buildings.
- Buttons invoke domain commands through a controller callback and then repaint the
  complete view from current state.
- UI mutation is marshalled onto Swing's event dispatch thread.
- Locked, affordable, max-level, and unaffordable states use both text and color.
- Controls remain reachable and understandable by keyboard.

## Delivery sequence

### Task 1 — Establish a reproducible RuneLite plugin project

Files (target maximum: 5 logical additions plus official wrapper assets):

- `build.gradle`
- `settings.gradle`
- `runelite-plugin.properties`
- `.gitignore`
- `AGENTS.md`
- Official Gradle wrapper assets and a minimal test launcher

Actions:

1. Base the build on RuneLite's official example-plugin template.
2. Target Java 11 and use `compileOnly 'net.runelite:client:latest.release'`.
3. Add JUnit 4 only for tests and avoid runtime dependencies.
4. Set `build=standard` in plugin metadata.
5. Add a launcher that loads the plugin with `ExternalPluginManager`.
6. Record the project constraints from the official template in `AGENTS.md`.
7. Use `Runehold` as temporary local author metadata until submission identity is
   confirmed.

Acceptance:

- `gradlew test` succeeds with a smoke test.
- `gradlew build` produces the plugin artifact.
- The repository contains no generated build output or IDE state.

Verification:

- `./gradlew test` or `.\gradlew.bat test`
- `./gradlew build` or `.\gradlew.bat build`

### Task 2 — Implement the XP-to-mana domain with tests first

Expected files:

- `src/main/java/com/runehold/domain/ManaLedger.java`
- `src/main/java/com/runehold/domain/ManaAward.java`
- `src/main/java/com/runehold/domain/VillageState.java`
- `src/test/java/com/runehold/domain/ManaLedgerTest.java`

Actions:

1. Write failing tests for first-event baseline behavior.
2. Test positive deltas, the 100 XP conversion rate, and carried remainder.
3. Test duplicate and negative XP events awarding zero and resetting baseline.
4. Test the 10,000 mana local-day cap and date rollover.
5. Test the initial 250 mana balance.
6. Implement only enough domain code to satisfy each case, then simplify.

Acceptance:

- No mana is awarded for the first event for each skill.
- Award math is deterministic and preserves fractional XP remainder.
- The cap cannot be bypassed with a large single delta.
- Date rollover resets daily earnings but not stored mana.
- No RuneLite type appears in the domain package.

Verification:

- Focused `ManaLedgerTest`
- Full unit-test suite

### Task 3 — Implement village construction transactions with tests first

Expected files:

- `src/main/java/com/runehold/domain/BuildingType.java`
- `src/main/java/com/runehold/domain/BuildingCatalog.java`
- `src/main/java/com/runehold/domain/UpgradeResult.java`
- `src/main/java/com/runehold/domain/Village.java`
- `src/test/java/com/runehold/domain/VillageTest.java`

Actions:

1. Encode Town Hall, Mana Well, Barracks, and Workshop definitions centrally.
2. Write failing tests for costs, unlocks, maximum levels, and insufficient mana.
3. Implement one atomic `upgrade` transaction that validates before mutation.
4. Guarantee that Town Hall starts at level 1 and cannot be removed.
5. Expose read-only information required by the UI.

Acceptance:

- Successful upgrades debit the exact cost and increase one level.
- Invalid upgrades change neither mana nor levels.
- Every upgrade attempt returns a stable reason suitable for UI text.
- Costs and unlock rules have one source of truth.

Verification:

- Focused `VillageTest`
- Full unit-test suite

Checkpoint: the complete economy loop works without RuneLite, Swing, disk, or
network access.

### Task 4 — Add versioned, profile-scoped persistence

Expected files:

- `src/main/java/com/runehold/persistence/PersistedRuneholdState.java`
- `src/main/java/com/runehold/persistence/RuneholdStateCodec.java`
- `src/main/java/com/runehold/persistence/RuneholdStateStore.java`
- `src/test/java/com/runehold/persistence/RuneholdStateCodecTest.java`
- `src/test/java/com/runehold/persistence/RuneholdStateStoreTest.java`

Actions:

1. Define schema version 1 with village, ledger, baselines, remainders, daily
   earnings, and earning date.
2. Serialize with RuneLite's injected Gson.
3. Read and write one profile-scoped ConfigManager key.
4. Treat missing, malformed, unsupported, or incomplete data as a fresh state.
5. Verify that no write is attempted when profile-scoped storage is unavailable.

Acceptance:

- Valid state round-trips without loss.
- Corrupt data cannot prevent plugin startup.
- Unsupported schema versions fail closed to a fresh village.
- State is never stored globally as a substitute for a missing profile.

Verification:

- Focused codec/store tests with a mocked or test-double configuration boundary
- Full unit-test suite

### Task 5 — Build the native RuneLite side panel

Expected files:

- `src/main/java/com/runehold/ui/RuneholdPanel.java`
- `src/main/java/com/runehold/ui/BuildingRow.java`
- `src/main/java/com/runehold/ui/RuneholdViewModel.java`
- `src/main/java/com/runehold/ui/RuneholdController.java`
- `src/test/java/com/runehold/ui/RuneholdViewModelTest.java`

Actions:

1. Add the mana balance and `earned / 10,000 today` header.
2. Render all buildings with name, level, next cost, lock reason, and action.
3. Disable unavailable actions while retaining explanatory text.
4. Route upgrades through a controller and rebuild the view from domain state.
5. Add accessible names/tooltips and preserve normal keyboard button behavior.
6. Keep visual updates on the Swing event dispatch thread.

Acceptance:

- Every domain state has an unambiguous visual representation.
- Button states match domain rules and never duplicate rule calculations.
- A successful upgrade updates mana and the building row immediately.
- Locked and unaffordable states remain understandable without color.

Verification:

- View-model tests for all action states
- Headless-safe construction smoke test where practical
- Full unit-test suite

### Task 6 — Connect RuneLite lifecycle and OSRS events

Expected files:

- `src/main/java/com/runehold/RuneholdPlugin.java`
- `src/main/java/com/runehold/RuneholdConfig.java`
- `src/main/java/com/runehold/RuneholdModule.java` if injection bindings require it
- `src/test/java/com/runehold/RuneholdPluginTest.java`
- `src/main/resources/runehold_icon.png`

Actions:

1. Load profile state during startup and construct the controller/panel.
2. Add a navigation button through `ClientToolbar`.
3. Subscribe to `StatChanged`, map skill identity to a stable key, and pass current
   XP to the ledger.
4. Persist after awarded mana and successful upgrades.
5. Refresh the panel after state changes on the event dispatch thread.
6. Remove the navigation button and clear transient baselines on shutdown/session
   change.
7. Use `ImageUtil.loadImageResource` for an original toolbar icon.

Acceptance:

- Plugin startup with missing or corrupt state produces a usable fresh village.
- First stat events after startup/session change establish baselines only.
- Positive XP events update mana and UI according to the tested domain rules.
- Restarting the same RuneScape profile restores the state.
- Shutdown removes all UI registrations cleanly.

Verification:

- Plugin load/lifecycle test through `ExternalPluginManager`
- Full unit-test suite
- Full build

Checkpoint: the automated local vertical slice is complete and buildable.

### Task 7 — Document and manually validate the playable slice

Expected files:

- `README.md`
- `docs/manual-test-checklist.md`
- Existing files changed only for defects found during review

Actions:

1. Document features, non-features, privacy, local storage, and install/run steps.
2. Add a manual checklist for login, initial baselines, XP gain, carry, upgrades,
   persistence, profile switching, date rollover, and corrupt-state recovery.
3. Run tests, build, static diff checks, and a multi-axis code review.
4. Offer `.\gradlew.bat run` for the user to log in and verify in-game behavior.
5. Record any manual failures as new failing tests before fixing them.

Acceptance:

- Automated checks pass from a clean checkout.
- Documentation does not imply multiplayer, item consumption, or OSRS rewards.
- The user can reproduce the manual validation flow.
- Completion is claimed only after the user confirms the in-game run.

Verification:

- `.\gradlew.bat test`
- `.\gradlew.bat build`
- `git diff --check`
- User-run `.\gradlew.bat run`

## Test strategy

- Unit tests cover all rules and edge cases in pure Java.
- Persistence tests cover round-trips and hostile/corrupt input.
- UI tests focus on presentation state rather than pixel rendering.
- Plugin tests cover dependency injection, lifecycle, and event delegation.
- Manual RuneLite validation is reserved for integration behavior that cannot be
  proven safely outside a logged-in client.

## Risks and controls

| Risk | Control |
| --- | --- |
| Startup `StatChanged` events look like earned XP | Baseline each skill before awarding |
| Profile/account switch creates a huge delta | Clear transient baselines on session/profile change |
| Clock rollover makes tests flaky | Inject the local date source |
| Corrupt JSON bricks startup | Versioned codec with fresh-state fallback |
| UI and client threads race | Keep domain commands serialized and Swing mutation on EDT |
| Build changes under `latest.release` | Keep official API usage minimal and run full build at every checkpoint |
| Client-reported data cannot secure PvP | Keep networking and competitive rewards outside the MVP |

## Approval gate

Implementation starts only after this plan is approved. Each task will follow
red-green-refactor, end with its stated verification, and be committed as a small
passing slice. Any change to network behavior, economy formulas, persistence schema,
or data deletion requires a separate decision before implementation.
