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


def state_glyph(shape: str, index: int, size: int = 16):
    """A placeholder that still distinguishes one STATE from another.

    The default placeholder is identical for every glyph by design, so a wrong
    codepoint reads as a wrong icon. That is right for plumbing and wrong for a
    readout: ten hunger glyphs drawn as ten identical outlines is a hunger bar
    you cannot read, which is how the first live pack presented -- the vanilla
    row correctly hidden and the replacement correctly drawn and completely
    illegible.

    So a glyph may declare a shape. These are still placeholders and still
    monochrome: they carry no palette, no icon language and no artwork, only
    the minimum difference needed for a state to be distinguishable from its
    neighbours.

    The exception is "blank". A state meaning "there is nothing here" -- a
    drumstick beyond the player's Capacity -- renders as nothing, and that is
    the finished appearance rather than something awaiting art. It keeps its
    advance, so the row stays ten drumsticks wide and Capacity fills it from
    the left the way the vanilla row does.
    """
    white = (255, 255, 255, 255)
    clear = (0, 0, 0, 0)
    pixels = []
    for y in range(size):
        for x in range(size):
            border = x in (0, size - 1) or y in (0, size - 1)
            interior = 1 < x < size - 2 and 1 < y < size - 2
            if shape == "blank":
                pixels.append(clear)
            elif border:
                pixels.append(white)
            elif y == 2 and 2 <= x < 2 + 8 and (index >> (x - 2)) & 1:
                pixels.append(white)              # index bits, as ever
            elif shape == "solid" and interior:
                pixels.append(white)
            elif shape == "half" and interior:
                pixels.append(white if x < size // 2 else clear)
            elif shape == "blank":
                pixels.append(clear)              # see blank_note in the registry
            else:
                pixels.append(clear)
    return pixels


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


def ability_panel_probe(size: int = 64):
    """Three outlined cells on a baseline. A ruler, not artwork.

    Drawn into the upper band of a square texture so the rendered quad reads as
    a wide strip: the transparent remainder does not render, and a square source
    avoids the non-square generated-model handling.
    """
    white = (255, 255, 255, 255)
    dim = (255, 255, 255, 70)
    clear = (0, 0, 0, 0)
    pixels = [clear] * (size * size)

    def put(x, y, colour):
        if 0 <= x < size and 0 <= y < size:
            pixels[y * size + x] = colour

    # Three 18x18 cells with a 2px gutter, left-aligned in a 24..42 band.
    for cell in range(3):
        x0 = 2 + cell * 20
        for i in range(18):
            put(x0 + i, 24, white); put(x0 + i, 41, white)      # top and bottom
            put(x0, 24 + i, white); put(x0 + 17, 24 + i, white)  # sides
        for ix in range(x0 + 2, x0 + 16):
            for iy in range(26, 40):
                put(ix, iy, dim)
        # cell index as bars along the inside top edge, so 1/2/3 is legible
        for bar in range(cell + 1):
            for iy in range(27, 30):
                put(x0 + 3 + bar * 3, iy, white)
    return pixels


def bar_segment(fill: str, size: int = 9):
    """One cell of a continuous bar, drawn into a native heart/drumstick sprite.

    The native rows are ten sprites side by side, so segments that tile read as
    a single bar in the HUD's own position -- with the client doing the filling,
    which means no plugin, no boss bar, no action-bar fade and nothing to
    contend over.

    Column 8 is left clear. The sprites are 9 wide but spaced 8 apart, so a
    segment that used its full width would overlap its neighbour by a pixel and
    the bar would show a seam every cell.
    """
    lit = (255, 255, 255, 255)
    rim = (255, 255, 255, 170)
    hollow = (0, 0, 0, 120)
    clear = (0, 0, 0, 0)
    pixels = []
    for y in range(size):
        for x in range(size):
            if x == size - 1 or y in (0, size - 1):
                pixels.append(clear)          # the spacing column and a margin
                continue
            edge = y in (1, size - 2)
            if fill == "full":
                pixels.append(lit)
            elif fill == "half_left":
                pixels.append(lit if x < 4 else (rim if edge else hollow))
            elif fill == "half_right":
                pixels.append(lit if x >= 4 else (rim if edge else hollow))
            else:
                pixels.append(rim if edge else hollow)
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
    shapes = registry.get("placeholder_shapes", {}).get("shapes", {})
    for group_name, group in registry["groups"].items():
        for glyph_id in group["ids"]:
            cp = codepoint(index)
            shape = shapes.get(glyph_id)
            png_rgba(assets / "textures" / "font" / f"{glyph_id}.png", 16, 16,
                     state_glyph(shape, index) if shape else placeholder_glyph(index))
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
                "shape": shape,
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

    # ---- ability panel probe -------------------------------------------
    #
    # A test of one question the documentation cannot answer: whether a GUI item
    # model can be scaled and pushed LEFT out of the offhand slot into the empty
    # HUD space beside it, and -- decisively -- whether doing that suppresses the
    # corner minimap, which renders because a FILLED_MAP is held in the offhand.
    #
    # If the minimap survives, the ability row can live exactly where the offhand
    # slot is. If it does not, the tome cannot carry it and the row moves to
    # glyphs in the action bar.
    #
    # Deliberately ugly: three outlined cells and a baseline, so alignment and
    # extent are readable and nobody mistakes it for a design.
    png_rgba(assets / "textures" / "item" / "ability_panel.png", 64, 64,
             ability_panel_probe())
    write_json(assets / "models" / "item" / "ability_panel.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "moba:item/ability_panel"},
        # scale and translation are the whole point of the probe. GUI item
        # rendering is not scissored to the slot, so this should overflow it.
        "display": {"gui": {"rotation": [0, 0, 0],
                            "translation": [-11, 0, 0],
                            "scale": [2.4, 2.4, 2.4]}}
    })
    # The 1.21.4+ item definition the minecraft:item_model component resolves.
    write_json(assets / "items" / "ability_panel.json", {
        "model": {"type": "minecraft:model", "model": "moba:item/ability_panel"}
    })

    # Repaint the native health and hunger rows as bar segments.
    #
    # These are vanilla sprite replacements, so they live under
    # assets/minecraft. The client keeps deciding which segments are filled;
    # the pack only decides what a segment looks like. That is why this needs
    # no plugin support at all, and why it cannot change how MANY segments are
    # drawn -- vanilla derives the health row's length from max health.
    for sprite, fill in registry.get("bar_segments", {}).get("sprites", {}).items():
        png_rgba(out / "assets" / "minecraft" / "textures" / "gui" / "sprites" / f"{sprite}.png",
                 9, 9, bar_segment(fill))

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
