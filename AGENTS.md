# Runehold Development Rules

Runehold follows the official RuneLite example-plugin conventions.

## Mandatory startup sequence

This repository is the complete handoff between computers and Codex tasks. Do
not depend on a previous chat for context. At the beginning of every new task,
before changing files:

1. Read this file and completely read `PROJECT_CONTEXT.md`.
2. Read `docs/specs/runehold-mvp.md`,
   `docs/specs/runehold-village-builder.md`,
   `docs/design/runehold-village-art-direction.md` and `tasks/todo.md`.
3. Inspect `git status --short --branch`, the current branch and recent commits.
   The handoff branch named in `PROJECT_CONTEXT.md` is the source of truth. On a
   clean fresh clone, switch to it and run `git pull --ff-only`. Never discard or
   overwrite uncommitted user work to switch branches.
4. Confirm a supported JDK is active, then run `./gradlew test` and
   `./gradlew build` (`.\gradlew.bat` on Windows) before implementation.
5. Summarize the current state, remaining work and verification result. If the
   user asked to continue without naming a feature, start the recommended next
   milestone from `PROJECT_CONTEXT.md` incrementally, with tests.

The repository-scoped `$osrs-2007-art-direction` skill lives in
`.agents/skills/osrs-2007-art-direction` and must be used for every visual, UI,
sprite, icon, asset or art-direction change. Follow all references required by
that skill. No separate installation or copying of the skill is necessary after
cloning this repository.

## Review agents

Four repository-scoped reviewer agents are committed under `.claude/agents`. They
review and never edit files. Use them as gates rather than as afterthoughts:

- `software-architect` — before starting a domain slice and again once it
  compiles. Enforces layering, RuneLite hard constraints and schema versioning.
  Emits `ARCHITECTURE VERDICT`.
- `ux-ui-reviewer` — whenever panel, window, view-model or HUD code changes.
  Enforces state legibility, refusal feedback, keyboard access and EDT
  correctness. Emits `UX VERDICT`.
- `osrs-art-director` — for every visual, sprite, icon, tile or asset change.
  Applies the committed `$osrs-2007-art-direction` skill reject-first. Emits
  `ART VERDICT`.
- `qa-reviewer` — once a slice compiles and before declaring anything done. Runs
  focused tests, the full suite, the build and the diff/secret check, and hunts
  missing coverage. Emits `QA VERDICT`.

A slice is not finished until the agents whose scope it touches have returned a
passing verdict backed by real command output. Never report a verdict that was
not actually produced.

Use branches named `feature/<description>`, `fix/<description>` or
`docs/<description>`. Never put `codex` in a branch name.

## Build and dependencies

- Keep all source compatible with Java 11.
- Match the official RuneLite example-plugin Gradle structure.
- Do not add runtime dependencies without explicit approval.
- Use RuneLite-injected Gson and OkHttp rather than constructing alternatives.
- Do not commit generated output, IDE state, or temporary files.

## Safety and Plugin Hub compliance

- Never automate game input or send game actions.
- Never use reflection, JNI/JNA, unsafe native access, dynamic code loading,
  Java serialization, or external processes.
- Never expose player information over HTTP or collect information about other
  players.
- Keep the MVP fully local and profile-scoped; it has no network behavior.
- Use RuneLite gameval constants instead of magic IDs when applicable.

## Runtime behavior

- Keep event handlers small and event-driven.
- Never perform blocking disk or network work on the client thread.
- Mutate Swing components only on the event dispatch thread.
- Remove toolbar entries, listeners, overlays, and scheduled work on shutdown.
- Use debug logging for frequent diagnostics, not info logging.

## Configuration and persistence

- Keep the `runehold` configuration group and existing keys stable after release.
- Store state through profile-scoped `ConfigManager` APIs.
- Persist versioned JSON and recover safely from malformed data.
- Ask before adding telemetry, authentication, remote services, or destructive
  reset behavior.

## Testing

- Follow red-green-refactor for behavior changes.
- Keep domain tests independent of RuneLite and Swing.
- Run focused tests, then the full suite and build after each slice.
- Review the staged diff and check for secrets before every commit.
- Commit successful increments with a descriptive conventional commit and push
  the active feature branch when the user asks to publish the work.
- Only the user may perform the final logged-in RuneLite test. Do not automate or
  interact with RuneScape on the user's behalf.
