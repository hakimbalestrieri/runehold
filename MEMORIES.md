# Memories

Memories is a RuneLite plugin that remembers the previous names of people on
your friends list, so a rename doesn't erase your history with someone.

It lives in this repository as a second, independent plugin alongside
Runehold — see [Status](#status) below for what that means in practice.

## What it does

- Watches your friends list for name changes.
- Right-click a friend, choose **Memories**, and see every previous name
  recorded for that account, newest first, with the date each one was noted.
- Follows multi-hop renames: if someone goes A -> B -> C, the full chain is
  kept, not just the last hop.

## How it works

- OSRS gives friends a single "previous name" slot; RuneLite exposes it as
  `Friend.getPrevName()` and fires a `NameableNameChanged` event the moment a
  rename is detected while you're online.
- Memories listens for that event, and also reconciles the whole friends list
  whenever it refreshes (on login, and on every subsequent friends-list
  update), so history is captured even for renames that happened before the
  plugin was installed.
- Identity is tracked by current display name (case/whitespace-insensitive),
  since the client does not expose a stable account ID for friends. If a name
  is freed and later claimed by an unrelated player, that player would
  inherit the name's recorded history — the same limitation the game's own
  single-slot "previous name" has.
- History is capped at 50 previous names per person to keep storage bounded.

## Privacy and storage

- Everything is stored locally in RuneLite's own configuration (the
  `memories` config group) and is never sent over the network.
- Storage is global rather than tied to one RuneScape profile, since a
  friend's naming history doesn't depend on which of your own accounts is
  logged in when you notice the change.
- Removing someone from your friends list does **not** delete their recorded
  history. The point of the plugin is to still recognize them if they add you
  again under a new name.

## Try it locally

```
./gradlew test
./gradlew runMemories
```

`runMemories` opens a RuneLite development client with Memories loaded, the
same way `./gradlew run` does for Runehold. Only you should log in and
interact with the game in that window.

## Project layout

```
com.memories.domain        pure Java name-history model (no RuneLite/Swing imports)
com.memories.persistence   versioned JSON codec + global ConfigManager storage
com.memories.ui            the "Memories" popup dialog
com.memories               plugin lifecycle and RuneLite event wiring
```

## Status

Implemented and unit-tested; not yet submitted to the Plugin Hub.
`runelite-plugin.properties` at the repo root still declares only Runehold,
so Memories currently ships as a second plugin class in this repository,
buildable and loadable locally via `runMemories`. A Plugin Hub listing is one
plugin per manifest, so publishing Memories there will need its own manifest
— either its own repository, or a deliberate multi-plugin submission setup —
rather than being folded into Runehold's existing listing.
