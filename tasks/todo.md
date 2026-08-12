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

## Gathering and workers

- [x] Add the pure gathering domain: resource types, inventory, site catalog and
      site state.
- [x] Add workers, assignment, pathfinding, production and offline progress
      services.
- [x] Persist resources, sites, workers and the offline timestamp at schema v5.
- [x] Migrate schema v4 payloads to default population and sites without data loss.
- [x] Add the side-panel gathering rows with assign, recall and collect actions.
- [x] Wire the gathering commands through the client thread and refresh the panel.
- [x] Run the focused gathering tests, the full suite and the build.
- [x] Load canonical OSRS resource sprites for the eight sites through
      `ItemManager` and record them in `THIRD_PARTY_NOTICES.md`.
- [x] Run the four reviewer agents on this slice.
- [x] Fix the reviewed blockers introduced by this slice: surface refused
      commands in the panel, ASCII-only button labels, non-clipping site names
      and shortened status text.
- [ ] User confirms gathering behaviour in the logged-in RuneLite client, and
      adds the gathering section to `docs/manual-test-checklist.md`.

## Gathering sites as placeable buildings

- [x] Make every gathering site a `BuildingType` so one layout owns every
      footprint; `GatheringSiteType` is gone.
- [x] Give each site a build cost, duration, level cap and Town Hall requirement
      in `BuildingCatalog`, under the new `GATHERING` category.
- [x] Derive worker access tiles from the placed footprint instead of fixed
      catalog coordinates, and recall workers when a site moves.
- [x] Start a site's production clock at construction completion.
- [x] Persist at schema v6 and migrate v5 sites to unplaced.
- [x] Give unplaced sites a place action in the panel instead of worker actions.
- [x] Drop the duplicate occupancy model from `PathfindingService`.
- [x] Make the offline cap forfeit the surplus interval.
- [x] Validate persisted stock, worker counts and worker/site cross-references.
- [x] Make `GatheringSiteState` mutators package-private.

## Renderer alignment and scale

- [x] Replace the hand-maintained sprite dimension table in `VillageCanvas` with
      measurements taken from the artwork (`VillageSpriteMetadata`).
- [x] Remove the position-dependent sprite shrink; drawn size now depends on zoom
      only.
- [x] Anchor sprites on their opaque content instead of their canvas, so
      `barracks.png` (nine pixels off centre, twenty-four empty pixels below)
      stands on its own footprint.
- [x] Declare `BuildingType.Kind` per constant instead of inferring it from
      `ordinal()`.
- [x] Keep gathering sites in `getBuildings()` so the village window can place
      them; the side panel filters them out via `getStructures()`.
- [x] Add `VillageSpriteQaTest` covering artwork presence, metadata truth, binary
      alpha, position-independent size, footprint anchoring and drawability.
- [x] Document real PNG dimensions in `docs/design/runehold-sprite-pipeline.md`.

### Still open

- [ ] Real gathering-site artwork. The canvas renders a flat coloured plot per
      site as an interim; `src/main/resources/village/` has no site PNG.
- [ ] Villager sprite sheet, worker transit simulation and the gathering-site
      selection panel described in the living-village brief.
- [ ] The specialised skills named in the living-village brief
      (`runehold-game-systems`, `runehold-deterministic-simulation`,
      `runehold-isometric-sprite-pipeline`, `runehold-character-animation`,
      `runehold-runelite-swing-ui`, `runehold-persistence-migrations`).
- [ ] Gathering economy design document: sources, sinks, capacities, worker
      scarcity and unlock pacing, before any balance number is changed.
- [ ] Decide the panel font policy: the RuneScape faces are drawn on a 16px grid
      and the whole panel derives fractional sizes (11f-14f), not only this slice.
- [ ] Re-run the four reviewer agents on the placeable-site slice.
- [ ] Remaining untested paths: production time boundaries, collect overflow,
      truncated v6 payload.
