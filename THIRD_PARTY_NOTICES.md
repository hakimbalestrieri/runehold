# Third-party assets and attribution

Runehold is an unofficial fan project for Old School RuneScape.

> Created using intellectual property belonging to Jagex Limited under the
> terms of Jagex's Fan Content Policy. This content is not endorsed by or
> affiliated with Jagex.

Old School RuneScape and RuneScape are trademarks of Jagex Limited. This notice
does not replace the terms of the
[Jagex Fan Content Policy](https://legal.jagex.com/docs/policies/fan-content-policy).

## Runtime OSRS assets

Runehold does not contain copied OSRS font or item-image files. The running
RuneLite client supplies these presentation assets at runtime:

| Use in Runehold | Runtime source | RuneLite API |
| --- | --- | --- |
| All current UI text | RuneScape regular and bold fonts | [`FontManager`](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/ui/FontManager.java) |
| Mana | Water rune sprite | [`ItemManager`](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/game/ItemManager.java) |
| Town Hall | Construction skillcape sprite | `ItemManager` |
| Mana Well | Water rune sprite | `ItemManager` |
| Barracks | Bronze sword sprite | `ItemManager` |
| Workshop and upgrade actions | Hammer sprite | `ItemManager` |

The asynchronous images are created from the user's local game cache by the
RuneLite client. The plugin JAR only contains the code that requests them. The
Runehold navigation icon and village concept artwork are original project
assets, not extracted game files.

## Repositories audited on 2026-08-11

| Repository or branch | Revision | Finding | Included in Runehold |
| --- | --- | --- | --- |
| [`melkypie/resource-packs`](https://github.com/melkypie/resource-packs) `master` | `bd269eda1d4b6f50eba3b8cd84939a20b3b2bd29` | Plugin source is BSD-2-Clause. | No code or files copied. |
| [`melkypie/resource-packs`](https://github.com/melkypie/resource-packs/tree/sample-vanilla) `sample-vanilla` | `92e70091caf059b55e7696136ad04e10530a13aa` | README says the pack includes default RuneScape textures; that branch has no licence file clearing redistribution. | Rejected; no textures copied. |
| [`RuneStar/fonts`](https://github.com/RuneStar/fonts) | `c07e1426be82c759eb232fdaadc670e2d8f47f85` | Repository is CC0, but describes exact in-game glyphs and its CC0 disclaimer does not clear third-party rights. | Rejected as bundled files; RuneLite runtime fonts are used instead. |
| [`Dava96/osrs-icons`](https://github.com/Dava96/osrs-icons) | `cb4a192` | Package is CC BY-NC-SA 3.0; its licence identifies the icons as Jagex/Wiki property. Inventory PNGs preserve source dimensions, while Wiki SVG category icons are rasterised to 32 x 32. | Design-time calibration only; no package, base64 data, or icons copied. |
| [`osrsbox/osrsbox-db`](https://github.com/osrsbox/osrsbox-db) | `dae12e34` | Repository is GPL-3.0 and exposes game-derived 36 x 32 inventory PNGs plus item metadata. The repository licence does not transfer ownership of Jagex assets. | Design-time measurements and ID/name lookup only; no code, JSON, API, or images copied. |
| [`runelite/runelite`](https://github.com/runelite/runelite) | RuneLite dependency `1.12.35` | Official client APIs expose OSRS fonts and cache-backed item sprites at runtime. | API calls only; no RuneLite source copied. |

An open-source licence on extraction tooling does not automatically licence the
underlying Jagex assets processed by that tooling. New third-party visual files
must be added to this inventory with their exact source, revision and licence
before they enter the repository.
