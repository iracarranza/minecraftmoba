# Resource pack — structural work only

**No styling has been done, deliberately.** This generates the plumbing a glyph
HUD needs and emits placeholders for everything that requires a visual
decision. Every placeholder is meant to be replaced.

## The split

A glyph's **identity, codepoint, size and alignment** are plumbing with one
correct answer. Its **appearance** is a design decision. This tooling produces
the first and refuses to make the second.

| Generated here | Left open |
| --- | --- |
| pack.mcmeta at the right pack_format | palette, shape language, line weight |
| Negative-space font, `U+F000+n` advances `-n` px | what any icon depicts |
| One bitmap provider per glyph | glyph silhouettes |
| Codepoint assignment and the manifest both sides read | HUD layout and composition |
| Placeholder PNGs, monochrome, index-marked | whether the drawn readout replaces the sidebar |
| Item model stubs for the sentinel and locked-slot marker | their textures |

## Why the placeholders look like that

A 16×16 hollow square, greyscale, with the glyph's index in binary along the
top edge. Monochrome because a colour would imply a palette; a square because a
drawn shape would imply an icon language. The index marks are there so a wrong
codepoint is visible in game as the wrong number rather than passing for a
different icon.

A test asserts they stay greyscale, so styling cannot leak in here by accident.

## The contract

`registry.json` is the agreement between the plugin and the pack: 23 glyphs
across status, ability, capacity, panel and infrastructure groups. The build
writes `GLYPH_MANIFEST.json`, which both sides read.

This matters because a mismatch **does not degrade gracefully**. The client
renders a tofu box, which reads as a broken font rather than as a missing
feature. `validate.py` scans plugin source for private-use escapes and fails on
any codepoint the pack does not provide, any provider without a texture, and
any duplicate assignment.

## Use

```sh
python3 implementation/resourcepack/build_pack.py --output build/moba_pack
python3 implementation/resourcepack/validate.py --pack build/moba_pack \
  --source implementation/plugin/src/main/java
python3 -m unittest discover -s tests -t .      # from implementation/resourcepack
```

The build refuses to overwrite an existing directory.

## What this does not do

It does not decide whether the hunger bar should be hidden, what a status icon
depicts, how the readout is laid out, or whether the drawn layer replaces the
sidebar. `features.hud.glyphs.enabled` in the plugin gates the drawn layer
separately from the plain-text baseline precisely so those can be tried without
disturbing something that already works.

Glyph alignment is also unverified: `ascent` and `height` are set to sensible
defaults and will need adjusting once real artwork exists, because alignment
depends on what the artwork puts where.
