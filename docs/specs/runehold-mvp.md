# Spec: Runehold MVP

## Objective

Build the first playable vertical slice of **Runehold**, a RuneLite side game where normal Old School RuneScape progression produces mana that can be spent on a persistent village.

The target user is an OSRS player who enjoys long-term progression and base-building games. The first release succeeds when the player can gain mana from new XP, open the Runehold side panel, and spend that mana to construct or upgrade a small village that remains intact after restarting RuneLite.

This slice validates one product assumption: **earning mana while playing OSRS makes village progression compelling enough to revisit the panel.** It intentionally does not validate multiplayer attacks yet.

### User stories

- As a logged-in player, I gain mana from XP earned after Runehold starts observing a skill.
- As a player, I can see my mana balance and daily earning progress.
- As a player, I can construct and upgrade unlocked buildings when I can afford them.
- As a player, I receive a clear explanation when a building is locked or unaffordable.
- As a player, my village is stored separately for each RuneScape profile.

## Functional Rules

### Mana

- The first `StatChanged` event observed for each skill establishes an XP baseline and awards no mana.
- Later positive XP deltas award one mana per 100 XP.
- XP below the conversion threshold carries forward instead of being discarded.
- Negative or duplicate XP deltas award no mana and reset that skill's baseline safely.
- Mana earnings are capped at 10,000 per local calendar day for the MVP.
- A new village starts with 250 mana so the building loop can be tested immediately.
- Bank withdrawals, Grand Exchange purchases, player trades, inventory movement, and existing bank wealth award no mana.

### Village

- Every village begins with a level-1 Town Hall that cannot be removed.
- The MVP includes four upgradeable building types:
  - Town Hall: gates the maximum level of other buildings.
  - Mana Well: represents mana storage and future production systems.
  - Barracks: represents future attack units.
  - Workshop: represents future defenses.
- Construction and upgrades are deterministic transactions: validate unlock, validate cost, subtract mana, then increase the building level.
- A failed transaction never changes village state.
- Maximum levels and costs are declared in one catalog, not duplicated in UI code.

### Persistence

- State is serialized as JSON using RuneLite's injected `Gson` instance.
- State is saved through `ConfigManager#setRSProfileConfiguration` and loaded through `ConfigManager#getRSProfileConfiguration`.
- Corrupt or unsupported state falls back to a fresh village without crashing RuneLite.
- The persisted state includes a schema version to support future migrations.
- No bank contents, chat messages, credentials, or unrelated account data are stored.

### User Interface

- Runehold appears as a RuneLite navigation button and `PluginPanel`.
- The panel uses RuneLite's existing dark color scheme and standard Swing components.
- The header shows mana balance and daily cap progress.
- Each building shows its name, current level, next cost, lock state, and one explicit action button.
- Button state is conveyed with text as well as color, and all actions remain keyboard accessible.
- Panel mutations occur on Swing's Event Dispatch Thread.

## Tech Stack

- Java 11.
- Gradle project based on the official `runelite/example-plugin` template.
- RuneLite client dependency at `latest.release`, matching the official template.
- Standard Plugin Hub build with no new third-party runtime dependencies.
- JUnit 4.12 for unit tests, matching the official template.
- Gson, Guice, Lombok, and Swing only through dependencies already supplied by RuneLite or the template.

## Commands

Run from the repository root on Windows:

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat run
```

The `run` task launches a development RuneLite client. Only the user may log in and verify behavior in game; automated interaction with RuneScape is prohibited.

## Project Structure

```text
build.gradle
settings.gradle
runelite-plugin.properties
src/
  main/
    java/com/runehold/
      RuneholdPlugin.java           RuneLite lifecycle and event adapter
      RuneholdConfig.java           User-facing configuration
      domain/                        Pure village and mana domain objects
      service/                       Mana and construction use cases
      persistence/                   RuneLite profile-backed state store
      ui/                            Swing side panel and building components
    resources/com/runehold/
      icon.png                       Optimized toolbar icon
  test/
    java/com/runehold/
      domain/                        Pure unit tests
      service/                       Transaction and mana tests
      RuneholdPluginTest.java       Development client launcher
docs/specs/
tasks/
```

## Architecture

```text
RuneLite StatChanged events
          |
          v
    XP baseline adapter
          |
          v
      ManaService ---------> VillageState
                                 |
                    +------------+-------------+
                    |                          |
                    v                          v
             VillageStateStore           RuneholdPanel
             (ConfigManager)             (Swing/EDT)
```

The domain layer has no RuneLite imports. RuneLite events, storage, and Swing are adapters around deterministic, testable logic. A future multiplayer backend must integrate through a separate gateway rather than entering the domain model directly.

## Code Style

Follow the official RuneLite template: tabs for indentation, braces on the next line, descriptive names, no magic game IDs, and event subscriptions as thin adapters.

```java
public BuildResult upgrade(BuildingType type)
{
	BuildingDefinition definition = catalog.get(type);
	int cost = definition.costForNextLevel(state.levelOf(type));

	if (state.getMana() < cost)
	{
		return BuildResult.insufficientMana(cost);
	}

	state.spendMana(cost);
	state.upgrade(type);
	return BuildResult.success(type, state.levelOf(type));
}
```

## Testing Strategy

- Use test-driven development for all mana conversion and construction rules.
- Unit tests cover XP baselines, carry XP, daily caps, invalid deltas, costs, unlocks, maximum levels, and atomic failure behavior.
- Persistence tests use a fake store or direct codec tests; they do not require a RuneLite login.
- UI logic is kept thin and verified through presenter/state tests where practical.
- `gradlew test` and `gradlew build` must pass before each implementation checkpoint.
- Runtime verification requires the user to launch the development client and confirm the panel in game.

## Boundaries

### Always

- Keep OSRS event handlers non-blocking and event-driven.
- Run Swing updates on the Event Dispatch Thread.
- Use RuneLite APIs and `gameval` constants instead of reflection or magic game IDs.
- Load packaged images with `ImageUtil.loadImageResource`.
- Persist only Runehold state and tolerate corrupt data.
- Add a failing test before implementing new domain behavior.

### Ask first

- Add any runtime dependency not present in the official template.
- Introduce a network service, account linking, telemetry, or remote persistence.
- Change the mana formula, daily cap, or persisted schema after release.
- Reset, migrate, or delete player village data.
- Add real competitive rankings or resource loss on defense.

### Never

- Inject mouse or keyboard input or automate OSRS actions.
- Add menu entries that send actions to the OSRS server.
- Simulate or assist OSRS boss mechanics.
- Treat bank withdrawals, trades, or GE purchases as earned resources.
- Transmit bank contents, player locations, credentials, or other players' information.
- Use Java serialization, reflection, native code, dynamic code loading, or external processes.
- Make Runehold currency convertible to OSRS GP, items, or real money.

## Success Criteria

- [ ] The project matches the official RuneLite template and builds on Java 11.
- [ ] The development client loads `RuneholdPlugin` through `ExternalPluginManager`.
- [ ] Initial skill snapshots award zero mana.
- [ ] Subsequent XP gains award deterministic mana with carry and a daily cap.
- [ ] The side panel displays current mana and all four MVP buildings.
- [ ] A player can construct or upgrade a building only when unlocked and affordable.
- [ ] Village state survives plugin restart and is isolated by RuneScape profile.
- [ ] Corrupt persisted state recovers to a fresh village without an uncaught exception.
- [ ] Unit tests and the Gradle build pass.
- [ ] The user confirms the golden path in the RuneLite development client.

## Not in the MVP

- Multiplayer attacks, matchmaking, replays, clans, or leaderboards.
- Loot-value mana, bank conversion, item sacrifice, or multiple currencies.
- Troop training and battle simulation.
- Remote APIs, authentication, telemetry, or cross-device synchronization.
- Custom art packs beyond one toolbar icon and simple building visuals.
- Mobile layouts or a standalone web client.

## Open Questions

- Confirm the public plugin name: `Runehold` is the working name.
- Confirm the author string for `runelite-plugin.properties` before submission.
- Decide after the local loop is tested whether the first multiplayer mode should be non-destructive friendly raids or trophy-based competitive raids.
- Decide where a future authoritative backend would be hosted and how profiles would be identified without trusting an RSN alone.

## Official Sources

- RuneLite example plugin: https://github.com/runelite/example-plugin
- Official build template: https://raw.githubusercontent.com/runelite/example-plugin/master/build.gradle
- Official agent and Plugin Hub restrictions: https://raw.githubusercontent.com/runelite/example-plugin/master/AGENTS.md
- RuneLite `StatChanged` event: https://raw.githubusercontent.com/runelite/runelite/master/runelite-api/src/main/java/net/runelite/api/events/StatChanged.java
- RuneLite `PluginPanel`: https://raw.githubusercontent.com/runelite/runelite/master/runelite-client/src/main/java/net/runelite/client/ui/PluginPanel.java
- RuneLite navigation APIs: https://raw.githubusercontent.com/runelite/runelite/master/runelite-client/src/main/java/net/runelite/client/ui/NavigationButton.java
- RuneLite profile configuration APIs: https://raw.githubusercontent.com/runelite/runelite/master/runelite-client/src/main/java/net/runelite/client/config/ConfigManager.java
- Jagex third-party client guidelines: https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1
