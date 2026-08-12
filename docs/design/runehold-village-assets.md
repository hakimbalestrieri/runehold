# Runehold Village Assets

The six packaged building PNGs in `src/main/resources/village/` are original
Runehold art: Town Hall, Mana Well, Mana Grove, Barracks, Workshop and Rune
Banner. They are displayed by `VillageSpriteAtlas` with nearest-neighbor scaling
and a shared bottom-centre ground anchor.

| Sprite | Native PNG bounds | Grid footprint | Anchor |
| --- | ---: | ---: | --- |
| Town Hall | 148 x 136 px | 4 x 4 | bottom centre |
| Mana Well | 100 x 84 px | 2 x 2 | bottom centre |
| Mana Grove | 126 x 120 px | 3 x 3 | bottom centre |
| Barracks | 132 x 112 px | 3 x 3 | bottom centre |
| Workshop | 132 x 116 px | 3 x 3 | bottom centre |
| Rune Banner | 56 x 100 px | 1 x 1 | bottom centre |

They were generated from an original low-resolution, fixed-isometric brief and
post-processed locally only to remove the temporary chroma-key background and
split the atlas. The source atlas is not shipped. Higher building levels add
small cached pixel details, such as trim, fanions or mana glow, on top of the
same original sprite family. No Jagex, RuneScape, Clash of Clans, Wiki,
osrsbox-db or osrs-icons visual asset is packaged.

RuneLite `ItemManager` still supplies the side-panel's runtime item icons; their
roles and provenance remain documented in `THIRD_PARTY_NOTICES.md`.
