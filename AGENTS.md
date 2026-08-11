# Runehold Development Rules

Runehold follows the official RuneLite example-plugin conventions.

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
- Only the user may perform the final logged-in RuneLite test. Do not automate or
  interact with RuneScape on the user's behalf.
