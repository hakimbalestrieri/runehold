# Reject-first visual QA

Inspect the output at native size. Reject it immediately if any hard failure is
present.

## Hard failures

- [ ] Looks like a modern remaster, mobile strategy game, or contemporary cartoon.
- [ ] Uses smooth geometry, PBR materials, realistic lighting, bloom, or soft fog.
- [ ] Uses gradients, blurred shadows, glossy surfaces, or antialiased icon edges.
- [ ] Uses rounded cards, pills, oversized resource bars, or large modern spacing.
- [ ] Contains detailed painterly textures that cannot read at native resolution.
- [ ] Mixes inconsistent pixel scales or non-integer scaling.
- [ ] Uses uniformly thick black cartoon outlines around all world geometry.
- [ ] Copies a recognizable RuneScape/Clash building, character, logo, or layout.

## Required passes

- [ ] Geometry is visibly angular and intentionally low-poly.
- [ ] Each world material uses no more than four obvious tonal steps.
- [ ] UI panels are compact brown/gray stone, dark wood, or dull parchment.
- [ ] Text resembles yellow/orange bitmap game text with a hard dark shadow.
- [ ] Inventory icons occupy 36x32 slots, retain irregular negative space, and
      read at 1x without being stretched square.
- [ ] Production inventory-style PNGs use binary alpha with no translucent fringe.
- [ ] Icon palettes use clustered material ramps comparable to the audited corpus,
      rather than an arbitrary global 8/12-color cap.
- [ ] Important game states use shape/border/text as well as color.
- [ ] Village buildings remain original Runehold designs.
- [ ] The image remains readable when viewed at its stated native resolution.

## Acceptance rule

Accept only when every required pass is true and every hard failure is false.
When uncertain, reject and simplify: fewer polygons, fewer colors, smaller
textures, harder edges, flatter light, denser UI. For inventory icons, simplify
the silhouette and clusters first; do not flatten valid material ramps merely to
hit a low color count.
