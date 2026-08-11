# Image-generation prompt rules

## Required positive language

Use phrases such as:

- "low-resolution 2007 Java MMORPG gameplay screenshot"
- "authentic OSRS-era technical limitations"
- "very low-poly, visibly angular geometry"
- "flat-shaded faces with two or three tones"
- "tiny nearest-neighbor textures"
- "hard one-pixel edges, no antialiasing"
- "36-by-32 OSRS inventory sprite slots with binary-alpha pixel edges"
- "dark material-aware contours, not uniform cartoon outlines"
- "compact brown-gray stone interface"
- "yellow-orange bitmap game text with a one-pixel dark shadow"
- "native 768x512 appearance, integer-upscaled"

## Forbidden positive language

Do not ask for "high fidelity", "cinematic", "polished AAA", "realistic
lighting", "hand-painted detail", "premium", "modern", "glossy", "atmospheric",
"volumetric", "smooth low-poly", or "mobile game". These phrases reliably cause
HD drift even when listed beside retro constraints.

## Base prompt

```text
Use case: ui-mockup
Asset type: OSRS-era RuneLite plugin gameplay preview
Primary request: [screen and gameplay state]
Scene/backdrop: [bounded village or plugin context]
Subject: [only required buildings, objects, and interactions]
Style/medium: low-resolution 2007 Java MMORPG gameplay screenshot; authentic
OSRS-era technical limitations; very low-poly angular 3D with flat-shaded faces;
tiny nearest-neighbor textures; hard pixel edges; no antialiasing
Composition/framing: native 768x512 appearance, displayed at 2x with nearest-
neighbor pixels; compact brown-gray stone UI and dark wood borders
Color palette: restricted desaturated earth tones, dull gold, leaf green, muted
rune blue; maximum four tones per world material
Typography: yellow-orange bitmap game font with one-pixel dark shadow
Icon treatment: compact 36x32 inventory-sprite slots, irregular transparent
silhouettes, binary-alpha-looking hard edges, material-aware dark contours;
production Jagex item icons will be loaded through RuneLite at runtime
Constraints: [exact functional state]; original Runehold buildings and characters;
no copied game assets; no extra text
Avoid: HD render, modern remaster, Clash of Clans art, Warcraft art, Fortnite art,
mobile-game UI, PBR, smooth geometry, realistic light, gradients, bloom, glow,
ambient occlusion, reflections, soft shadows, rounded panels, vector icons,
airbrushing, depth of field, cinematic framing, watermark
```

Generate from scratch after an HD direction has been rejected. Do not include the
rejected image as a visual reference because image conditioning can overpower the
textual constraints.

Generated item icons are placeholders for composition only. Do not ask the model
to reproduce a named Jagex sprite. Name the gameplay role (mana, build, move,
upgrade) and replace any required Jagex icon with `ItemManager` or `SpriteManager`
in production.
