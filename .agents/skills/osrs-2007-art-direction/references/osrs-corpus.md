# Audited OSRS visual corpus

Use these repositories to calibrate measurable properties. Do not copy their
assets into Runehold.

## Sources

- `osrsbox/osrsbox-db` (`dae12e34` inspected 2026-08-11): item IDs, names,
  metadata, and inventory PNGs. Repository code/data is GPL-3.0 and the PNGs are
  derived from the game.
- `Dava96/osrs-icons` (`cb4a192` inspected 2026-08-11): a catalogue generated
  from OSRS Wiki inventory and category images. The package is CC BY-NC-SA 3.0
  and states that the icons belong to Jagex/Wiki contributors.

Primary URLs:

- https://github.com/osrsbox/osrsbox-db
- https://github.com/Dava96/osrs-icons

## Measured inventory-icon characteristics

The local `osrsbox-db` corpus was inspected without redistributing it:

- The first 2,000 item PNGs all used a 36x32 canvas.
- A deterministic sample of 500 contained no semi-transparent pixels in any of
  the 492 non-empty icons: alpha was binary (`0` or `255`).
- The same sample used a median of 56 opaque colors (p25 33, p75 78, p90 109).
- Median non-transparent occupancy was about 38% of the canvas.
- Representative simple tools used roughly 30-35 colors; a small rune variant
  used 69.

Treat these as calibration ranges, not targets to maximize. The signature is a
small irregular silhouette, hard binary edge, compact color clusters, and a
surprisingly nuanced material ramp, not an arbitrary 8-bit color count.

`osrs-icons` preserves source inventory PNG dimensions while palette-compressing
them. It rasterizes SVG category icons to 32x32. Consequently:

- Use inventory PNGs, not rasterized Wiki SVG category icons, as the main visual
  evidence for inventory-sprite style.
- Do not call all OSRS icons "32x32"; reserve a 36x32 item slot in RuneLite UI.
- Do not infer antialiasing from an enlarged browser thumbnail. Inspect native
  pixels and alpha values.

## Reference protocol

1. Identify the functional role: resource, tool, combat, building, selection, or
   status.
2. Use `osrsbox-db` names and IDs to locate three to five relevant inventory
   references with different materials.
3. Inspect each at native 36x32 on a dark-brown background.
4. Record silhouette occupancy, contour behavior, material ramps, and focal
   highlight placement. Do not trace the sprite.
5. Create an original Runehold icon from the shared rules.
6. If the interface needs an actual Jagex item icon, discard the placeholder and
   load the canonical sprite through RuneLite `ItemManager`/`SpriteManager`.

## Integration rules

- Do not add the osrsbox API, JSON files, Python package, npm icon package, base64
  exports, or downloaded PNGs as runtime dependencies.
- Do not make network calls to either project from the plugin.
- Prefer RuneLite `gameval.ItemID` constants over hard-coded numeric IDs.
- Record every runtime Jagex sprite role in `THIRD_PARTY_NOTICES.md`.
- Keep Runehold buildings, terrain, characters, logos, and village controls
  original and repository-owned.
