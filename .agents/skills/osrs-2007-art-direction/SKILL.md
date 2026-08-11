---
name: osrs-2007-art-direction
description: Enforce a corpus-grounded 2007-era Old School RuneScape visual language for RuneLite plugins, game UI mockups, inventory icons, sprites, low-poly buildings, environments, and visual reviews. Use whenever a request mentions OSRS, Old School RuneScape, RuneLite visuals, Runehold previews, RuneScape-style UI, 2007 MMORPG art, retro low-poly fantasy, OSRS inventory sprites, osrsbox-db, or osrs-icons. Reject modern-HD, mobile-game, glossy, smooth, cinematic, or contemporary cartoon drift while preserving RuneLite and Jagex asset boundaries.
---

# OSRS 2007 Art Direction

Create visuals that look native to a 2007 Java MMORPG client, not like a modern
remaster inspired by it. Treat period limitations as hard constraints.

## Required references

Read these files before producing work:

- `references/visual-spec.md` for measurable 2D, 3D, UI, palette, and legal rules.
- `references/osrs-corpus.md` before choosing icon dimensions, color budgets,
  reference assets, or RuneLite item IDs.
- `references/imagegen-prompt.md` before generating a raster preview or asset.
- `references/qa-checklist.md` before presenting or accepting any output.

For pixel sprites and icons, also use `$pixel-art-sprites`. For complete screens,
also use `$game-ui-design` and `$game-ui-ux`, but this skill overrides any modern,
HD, cinematic, touch-first, television, or console convention they suggest.

## Workflow

1. Classify the deliverable as interface preview, 2D icon/sprite, low-poly model,
   environment, or mixed scene.
2. Lock a native resolution and pixel scale before composing. Evaluate at 1x.
3. Ground icon work in three to five native-size inventory references from the
   audited corpus; never infer the style from enlarged Wiki thumbnails.
4. Build the silhouette and information hierarchy before adding texture.
5. Limit polygons, texture resolution, shading steps, and animation frames. Use
   corpus-calibrated color budgets instead of an arbitrary 8-bit cap.
6. Use square, compact, stone-or-wood UI components with game-native typography.
7. Run the reject-first QA checklist. If one hard failure is present, regenerate or
   revise instead of explaining the defect away.

## RuneLite boundary

- Use `FontManager` for RuneScape fonts in implemented RuneLite interfaces.
- Use `ItemManager` or `SpriteManager` for game-cache sprites at runtime.
- Use `osrsbox-db` metadata only as a design-time ID/name reference. Do not add its
  database, API, GPL code, or PNG corpus as a plugin runtime dependency.
- Treat `osrs-icons` as a design-time catalogue only. Its icon package is
  CC BY-NC-SA 3.0 and identifies the sprites as Jagex/Wiki property.
- Keep packaged buildings, characters, logos, and village art original to Runehold.
- Never copy extracted Jagex assets into the repository merely because an
  extraction tool is open source.

## Output rules

- State the intended native resolution and integer display scale.
- For generated previews, report which hard constraints were checked.
- Do not use a rejected HD image as a style reference in later generations.
- Keep exact UI text short; generated images are visual specifications, not final
  localized production assets.
