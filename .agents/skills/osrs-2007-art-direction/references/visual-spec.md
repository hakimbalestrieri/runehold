# OSRS 2007 visual specification

## 1. Period target

Target a low-resolution screenshot from a 2007 Java MMORPG client. The result
must feel deliberately constrained, slightly crude, compact, angular, and clean.
Do not interpret "OSRS-inspired" as fantasy mobile art or a modern remaster.

## 2. 3D world and buildings

- Use intentionally low polygon counts and visibly angular silhouettes.
- Prefer flat-shaded faces or simple vertex color. One face has one dominant tone.
- Use tiny nearest-neighbor textures: roughly 32-128 pixels per material family.
- Keep roofs, walls, trees, rocks, characters, and terrain faceted.
- Use a fixed elevated camera with an orthographic-like perspective.
- Use hard simple shadows or small blob shadows only.
- Limit each world material to 2-4 obvious tonal steps.
- Keep proportions plausible and slightly awkward rather than heroic or cute.
- Keep contours material-aware. Do not wrap all world geometry in a uniform comic
  outline.

Never use PBR materials, normal maps, smooth subdivision, ambient occlusion,
screen-space reflections, bloom, depth of field, volumetric light, realistic fog,
ray tracing, rim lighting, glossy highlights, cinematic color grading, or soft
contact shadows.

## 3. 2D sprites and icons

- Design Runehold inventory-style icons on the native OSRS item canvas: 36x32
  pixels with transparency. Keep important mass inside roughly 28x30 pixels and
  allow irregular negative space around the silhouette.
- Use binary alpha only (`0` or `255`) for production inventory-style PNGs. Never
  create translucent fringe pixels.
- Use dark material-aware contour clusters where they improve separation. Do not
  force a uniform comic-book outline around every internal or external edge.
- Use a corpus-calibrated palette. Simple tools commonly need around 24-40 opaque
  colors; runes, gems, or detailed items may need 40-80. Preserve hard pixel
  clusters instead of reducing every sprite to an artificial 8-color palette.
- Use hard edges and nearest-neighbor scaling only. Judge the icon at 1x on a
  dark-brown inventory-like background before viewing an enlarged copy.
- Prefer readable silhouette and exaggerated tool shape over internal detail.
- Light broadly from the upper-left, but allow several discrete ramps for curved
  metal, wood, stone, cloth, and magical material within the same icon.
- Export as lossless PNG; never JPEG.

Do not use vector-smooth curves, subpixel strokes, blurred shadows, automatic
antialiasing, high-resolution painterly detail, airbrushing, or gradients.

## 4. Interface language

- Build compact rectangular panels from mottled brown-gray stone, aged wood, or
  dull parchment.
- Use square corners and stepped/beveled 1-3 pixel borders. No rounded cards.
- Use the RuneScape regular/bold fonts supplied by RuneLite in implementation.
- Use yellow-orange labels (`#FF981F` family), pale yellow emphasis, and dark
  brown/black one-pixel text shadows.
- Reserve 36x32 slots for inventory sprites beside text; do not stretch them to a
  square or enlarge them as mobile illustrations.
- Keep controls dense, practical, mouse-and-keyboard oriented, and visually tied
  to the classic client frame.
- Use a clear selected state with a hard border, icon, or inset, not a soft glow.
- Encode valid/invalid placement with color plus border pattern and a text label.

Never use glassmorphism, flat-design cards, large whitespace, floating pills,
smooth rounded corners, thin modern sans-serif fonts, neon accents, soft shadows,
motion blur, elastic animation, or oversized mobile resource bars.

## 5. Palette

Use a small, slightly desaturated world/UI palette:

- deep outline brown-black: `#1B1711`
- dark wood: `#382C20`
- weathered brown: `#5A4631`
- stone shadow: `#4B4841`
- stone midtone: `#6D695E`
- dull parchment: `#B9A77A`
- UI orange: `#FF981F`
- muted gold: `#C7A640`
- leaf green: `#4E6235`
- dry grass: `#6D6A38`
- rune blue: `#3F6FA6`

For world geometry and interface chrome, add a color only when it communicates a
distinct material or game state. Inventory sprites may use compact local ramps as
defined above; do not apply the world-material four-tone limit to the whole icon.

## 6. Composition

- Start from a 765x503 or similarly compact 3:2-ish native canvas.
- Display previews at exactly 2x or 3x using nearest-neighbor scaling.
- Keep the village canvas central and reserve compact edge panels for controls.
- Avoid a full-width premium-game HUD unless the feature genuinely requires it.
- Keep every important label readable at native size.

## 7. Intellectual-property boundary

Use OSRS as a period/style reference and consume OSRS fonts/item sprites through
RuneLite at runtime. Do not package copied Jagex textures, models, characters,
logos, or UI atlases. Runehold buildings and characters must be original designs
that obey this technical style specification.

The `osrsbox-db` and `osrs-icons` corpora are calibration references, not an asset
bundle for Runehold. See `osrs-corpus.md` for the measured rules and licences.
