# Runehold sprite pipeline

How a building sprite gets from a PNG on disk onto its tiles. Written after two
rendering bugs whose shared root cause was a second, hand-maintained dimension
table that disagreed with the artwork.

## Single source of truth

`VillageSpriteAtlas` loads every sprite and measures it once. `VillageSpriteMetadata`
carries those measurements. The renderer asks the atlas; it declares no dimension of
its own.

There is no dimension table in `VillageCanvas`. Reintroducing one is a regression:
the hand-typed widths drifted from the artwork by up to 15%, which scaled buildings
wrongly and floated them off their footprints.

## Native scale

Sprites are authored against `VillageSpriteMetadata.NATIVE_TILE_WIDTH` (36 px). At
that tile width a sprite draws unscaled; every other zoom applies the same factor.

**A sprite's drawn size depends on zoom and nothing else.** It must never depend on
where the building stands. A previous "keep tall sprites inside the plot" heuristic
shrank sprites by up to 42% based on their distance from the map's north corner,
which made the same Rune Banner visibly smaller in one region of the map than in
another.

## Anchoring

The renderer anchors the sprite's **opaque content**, not its canvas:

- content centre X sits on the footprint diamond's centre X;
- content bottom sits on the diamond's front corner, raised by
  `baselineInset(tileHeight)` = a quarter tile, so the building reads as standing
  on the plot rather than in front of it.

This is what makes an unevenly padded sprite land correctly.

## Measured artwork

Canvas size and opaque content bounds, measured from the committed PNGs. Canvas
sizes are what the renderer uses for scaling; the content box is what it anchors.

| Sprite | Canvas | Opaque box | Content centre X | Bottom gap |
| --- | --- | --- | --- | --- |
| `town_hall.png` | 119 x 136 | (4,4)-(114,131) | 59.5 (centred) | 4 |
| `mana_well.png` | 96 x 84 | (4,4)-(91,79) | 48 (centred) | 4 |
| `mana_grove.png` | 108 x 120 | (4,4)-(103,115) | 54 (centred) | 4 |
| `barracks.png` | 108 x 112 | (22,4)-(103,87) | 63 (**+9 off centre**) | **24** |
| `workshop.png` | 121 x 116 | (4,4)-(116,111) | 60.5 (centred) | 4 |
| `rune_banner.png` | 54 x 100 | (4,4)-(49,95) | 27 (centred) | 4 |

`barracks.png` is the outlier: its artwork sits nine pixels right of centre with
twenty-four empty pixels underneath, where every other sprite has a uniform
four-pixel margin. The artwork is not wrong — the renderer was, for assuming the
canvas was the anchor. Content-based anchoring absorbs this, so the PNG is left
untouched rather than re-cut.

## Gathering site placeholders

The eight gathering sites have no packaged artwork yet. `VillageSpriteAtlas`
generates a flat footprint-sized plot per site, authored at the native tile width so
it lands exactly on its tiles. These are interim, not shippable art: producing real
site sprites is an open art task.

## QA

`VillageSpriteQaTest` enforces the invariants above:

- every `BuildingType` has loadable artwork and metadata;
- metadata equals the image on disk;
- alpha is binary, with no translucent fringe;
- a building's drawn size is identical at every map position;
- content centre and content baseline land on the footprint diamond;
- every `BuildingType` can be painted without throwing.

A new sprite that fails any of these is not ready to ship.
