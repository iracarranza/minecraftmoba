"""Generate the structural parts of the resource pack. No visual design.

The split this tool enforces: a glyph's **identity, codepoint, size and
alignment** are plumbing with one correct answer; its **appearance** is a design
decision. This generates the first and emits deliberately provisional
placeholders for the second.

What is generated:

  - pack.mcmeta at the right pack_format
  - a negative-space font, which is fully determined by arithmetic
  - a bitmap font provider per glyph, from registry.json
  - one placeholder PNG per glyph, monochrome and index-marked so it is legible
    in game and obviously not artwork
  - item model stubs for the sentinel and the locked-slot marker

Every placeholder is meant to be replaced. None encodes a palette, a style or a
shape language, because those are not mine to choose.
"""
from __future__ import annotations
import argparse
import json
import struct
import zlib
from pathlib import Path

PACK_FORMAT = [64, 0]          # 1.21.11 resource pack format
NEGATIVE_SPACE_MAX = 256


def png_rgba(path: Path, width: int, height: int, pixels: list[tuple[int, int, int, int]]):
    """Minimal RGBA PNG. Glyphs need alpha, so the worldgen RGB writer will not do."""
    def chunk(kind: bytes, data: bytes) -> bytes:
        return (struct.pack(">I", len(data)) + kind + data
                + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF))
    raw = b"".join(
        b"\x00" + b"".join(bytes(pixels[y * width + x]) for x in range(width))
        for y in range(height))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b"\x89PNG\r\n\x1a\n"
                     + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
                     + chunk(b"IDAT", zlib.compress(raw, 9))
                     + chunk(b"IEND", b""))


def placeholder_glyph(index: int, size: int = 16):
    """A hollow square with the glyph's index in binary along the top edge.

    Monochrome on purpose. A colour would imply a palette, and a drawn shape
    would imply an icon language; neither is a decision this tool should make.
    The index marks exist so a wrong codepoint is visible in game rather than
    looking like a different icon.
    """
    white = (255, 255, 255, 255)
    dim = (255, 255, 255, 90)
    clear = (0, 0, 0, 0)
    pixels = []
    for y in range(size):
        for x in range(size):
            border = x in (0, size - 1) or y in (0, size - 1)
            if border:
                pixels.append(white)
            elif y == 2 and 2 <= x < 2 + 8 and (index >> (x - 2)) & 1:
                pixels.append(white)           # index bits, low bit leftmost
            elif 4 <= x < size - 4 and 6 <= y < size - 4:
                pixels.append(dim)             # body, so the cell reads as filled
            else:
                pixels.append(clear)
    return pixels


def codepoint(index: int) -> str:
    return chr(0xE000 + index)


def build(registry: dict, out: Path):
    if out.exists():
        raise FileExistsError(f"{out} exists; build to a fresh directory")
    assets = out / "assets" / "moba"

    (out / "pack.mcmeta").parent.mkdir(parents=True, exist_ok=True)
    (out / "pack.mcmeta").write_text(json.dumps({
        "pack": {
            "pack_format": PACK_FORMAT[0],
            "description": "MOBA structural pack: glyph plumbing and placeholders. No styling."
        }
    }, indent=2) + "\n")

    # Negative space: pure arithmetic, no appearance at all.
    advances = {" ": 4}
    for n in range(1, NEGATIVE_SPACE_MAX + 1):
        advances[chr(0xF000 + n)] = -n
    write_json(assets / "font" / "space.json", {
        "providers": [{"type": "space", "advances": advances}]
    })

    index = 0
    providers = []
    manifest = {}
    for group_name, group in registry["groups"].items():
        for glyph_id in group["ids"]:
            cp = codepoint(index)
            png_rgba(assets / "textures" / "font" / f"{glyph_id}.png", 16, 16,
                     placeholder_glyph(index))
            providers.append({
                "type": "bitmap",
                "file": f"moba:font/{glyph_id}.png",
                "ascent": registry["glyph_ascent"],
                "height": registry["glyph_height"],
                "chars": [cp],
            })
            manifest[glyph_id] = {
                "codepoint": f"U+{0xE000 + index:04X}",
                "escape": f"\\u{0xE000 + index:04x}",
                "group": group_name,
                "index": index,
                "placeholder": True,
            }
            index += 1
    write_json(assets / "font" / "glyphs.json", {"providers": providers})

    # Item model stubs. Structural: they bind an item to a model path. The
    # textures they point at are placeholders and are meant to be replaced.
    for item_id, texture in (("moba_sentinel", "item/moba_sentinel"),
                             ("moba_locked_slot", "item/moba_locked_slot")):
        png_rgba(assets / "textures" / "item" / f"{item_id}.png", 16, 16,
                 placeholder_glyph(index)); index += 1
        write_json(assets / "models" / "item" / f"{item_id}.json", {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"moba:{texture}"}
        })

    # Hide the vanilla hunger row.
    #
    # A resource pack can only change a sprite, never decide per-icon whether a
    # drumstick is empty-but-available or beyond the player's Capacity: both are
    # the same client-side state. So the vanilla row is blanked entirely and the
    # plugin draws the readout from the hunger glyphs above, which is the only
    # way the three states can differ. These overrides live under
    # assets/minecraft, not assets/moba, because they replace vanilla sprites.
    hidden = registry.get("hidden_vanilla_sprites", {}).get("paths", [])
    for sprite in hidden:
        png_rgba(out / "assets" / "minecraft" / "textures" / "gui" / "sprites" / f"{sprite}.png",
                 9, 9, [(0, 0, 0, 0)] * 81)

    write_json(out / "GLYPH_MANIFEST.json", {
        "schema": "moba_glyph_manifest/1",
        "note": "Generated. The plugin must emit exactly these codepoints; a mismatch renders "
                "as tofu rather than as a wrong icon, so both sides read this file.",
        "glyphs": manifest,
        "negative_space": {"base": "U+F001", "range": f"1..{NEGATIVE_SPACE_MAX} px",
                           "usage": "U+F000+n advances -n pixels"},
        "hidden_vanilla_sprites": hidden,
    })
    return manifest


def write_json(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n")


if __name__ == "__main__":
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--registry", type=Path,
                   default=Path(__file__).parent / "registry.json")
    p.add_argument("--output", type=Path, required=True)
    a = p.parse_args()
    m = build(json.loads(a.registry.read_text()), a.output.resolve())
    print(f"built {len(m)} placeholder glyphs into {a.output}")
    print("every glyph is a placeholder; none encodes a palette or shape language")
