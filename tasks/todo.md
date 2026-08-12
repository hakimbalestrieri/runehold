# Runehold MVP - Task Checklist

Status: MVP, OSRS panel, and playable local village-builder slice implemented;
automated validation passed, awaiting the user's manual RuneLite launcher
confirmation.

- [x] Define and approve `docs/specs/runehold-mvp.md`.
- [x] Approve `tasks/plan.md`.
- [x] Task 1: scaffold the official Java 11 RuneLite plugin project.
  - [x] Add Gradle build, settings, wrapper, metadata, and ignore rules.
  - [x] Add project constraints and test launcher.
  - [x] Make smoke test and full build pass.
- [x] Task 2: implement XP-to-mana rules with tests first.
  - [x] Baseline behavior and invalid deltas.
  - [x] Conversion and remainder carry.
  - [x] Daily cap and date rollover.
  - [x] Initial village balance.
- [x] Task 3: implement village upgrades with tests first.
  - [x] Four-building catalog.
  - [x] Unlocks, costs, and maximum levels.
  - [x] Atomic success/failure transactions.
- [x] Checkpoint: run all pure-domain tests.
- [x] Task 4: implement versioned profile persistence.
  - [x] JSON round-trip.
  - [x] Corrupt and unsupported schema recovery.
  - [x] Missing-profile behavior.
- [x] Task 5: implement the RuneLite side panel.
  - [x] Mana and daily-cap header.
  - [x] Building rows and action states.
  - [x] Keyboard/accessibility and EDT behavior.
- [x] Task 6: integrate lifecycle and `StatChanged` events.
  - [x] Toolbar navigation and clean teardown.
  - [x] XP event adapter and session baseline reset.
  - [x] Save after mana awards and upgrades.
  - [x] Original toolbar icon.
- [x] Checkpoint: run plugin tests and full build.
- [ ] Task 7: document and validate the playable slice.
  - [x] README and privacy/non-feature boundaries.
  - [x] Manual in-game test checklist.
  - [x] Full tests, build, diff check, and code review.
  - [ ] User confirms the RuneLite launcher test.
- [ ] Decide final author string before Plugin Hub submission.
- [ ] Keep multiplayer/raids/backend work in a separate post-MVP specification.

## Village Builder

- [x] Add the pure 18 x 18 village layout model.
  - [x] Centralize the four building footprints in the catalog.
  - [x] Place the initial Town Hall at (7, 7).
  - [x] Reject out-of-bounds and colliding placements without mutation.
  - [x] Support atomic building movement while ignoring self-collision.
- [x] Integrate layout and construction jobs into versioned village state.
- [x] Migrate persisted schema v1 to newer layout schemas without losing economy data.
- [x] Add the isometric village window, canvas and accessible interactions.
- [x] Add Build catalogue, categories, placement ghost, Confirm/Cancel and global Edit mode.
- [x] Add original building PNGs, cached nearest-neighbor scaling and level variants.
- [x] Add functional builder scenario, projection, collision, resize and depth-order tests.
