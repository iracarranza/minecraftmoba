"""Structural checks. Nothing here asserts anything about appearance."""
import json
import struct
import sys
import tempfile
import unittest
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from build_pack import NEGATIVE_SPACE_MAX, build, codepoint, placeholder_glyph, png_rgba
from validate import validate

REGISTRY = json.loads((Path(__file__).resolve().parents[1] / "registry.json").read_text())


def built(tmp):
    out = Path(tmp) / "pack"
    build(REGISTRY, out)
    return out


class PngTests(unittest.TestCase):
    def test_writes_a_readable_rgba_png(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "g.png"
            png_rgba(path, 16, 16, placeholder_glyph(3))
            data = path.read_bytes()
            self.assertTrue(data.startswith(b"\x89PNG\r\n\x1a\n"))
            w, h, depth, colour = struct.unpack(">IIBB", data[16:26])
            self.assertEqual((w, h, depth, colour), (16, 16, 8, 6))  # 6 = RGBA

    def test_placeholders_differ_by_index(self):
        self.assertNotEqual(placeholder_glyph(1), placeholder_glyph(2))

    def test_placeholders_are_monochrome(self):
        # A colour would imply a palette decision this tool must not make.
        for px in placeholder_glyph(5):
            r, g, b, a = px
            if a: self.assertEqual((r, g), (b, g), "placeholder pixel is not greyscale")


class BuildTests(unittest.TestCase):
    def test_every_registry_glyph_gets_a_texture_and_provider(self):
        with tempfile.TemporaryDirectory() as tmp:
            pack = built(tmp)
            font = json.loads((pack / "assets/moba/font/glyphs.json").read_text())
            declared = {c for p in font["providers"] for c in p["chars"]}
            expected = sum(len(g["ids"]) for g in REGISTRY["groups"].values())
            self.assertEqual(len(declared), expected)
            for group in REGISTRY["groups"].values():
                for glyph in group["ids"]:
                    self.assertTrue((pack / "assets/moba/textures/font" / f"{glyph}.png").exists(), glyph)

    def test_codepoints_are_unique_and_in_the_private_use_area(self):
        with tempfile.TemporaryDirectory() as tmp:
            manifest = json.loads((built(tmp) / "GLYPH_MANIFEST.json").read_text())["glyphs"]
            points = [int(i["codepoint"].removeprefix("U+"), 16) for i in manifest.values()]
            self.assertEqual(len(points), len(set(points)))
            for cp in points: self.assertTrue(0xE000 <= cp <= 0xF8FF)

    def test_negative_space_covers_the_full_range(self):
        with tempfile.TemporaryDirectory() as tmp:
            space = json.loads((built(tmp) / "assets/moba/font/space.json").read_text())
            advances = space["providers"][0]["advances"]
            self.assertEqual(advances[chr(0xF000 + 1)], -1)
            self.assertEqual(advances[chr(0xF000 + NEGATIVE_SPACE_MAX)], -NEGATIVE_SPACE_MAX)

    def test_refuses_to_overwrite_an_existing_pack(self):
        with tempfile.TemporaryDirectory() as tmp:
            pack = built(tmp)
            with self.assertRaises(FileExistsError): build(REGISTRY, pack)


class ValidationTests(unittest.TestCase):
    def test_a_clean_pack_passes(self):
        with tempfile.TemporaryDirectory() as tmp:
            self.assertTrue(validate(built(tmp), [])["pass"])

    def test_a_missing_texture_is_caught(self):
        with tempfile.TemporaryDirectory() as tmp:
            pack = built(tmp)
            next((pack / "assets/moba/textures/font").glob("*.png")).unlink()
            r = validate(pack, [])
            self.assertFalse(r["pass"])
            self.assertTrue(any("missing" in p for p in r["problems"]))

    def test_a_codepoint_the_plugin_uses_but_the_pack_lacks_is_caught(self):
        with tempfile.TemporaryDirectory() as tmp:
            pack = built(tmp)
            src = Path(tmp) / "src"; src.mkdir()
            (src / "Thing.java").write_text('String icon = "\\ue0ff";')
            r = validate(pack, [src])
            self.assertFalse(r["pass"])
            self.assertIn("U+E0FF", r["referenced_but_missing"])

    def test_placeholders_are_reported_so_they_cannot_be_forgotten(self):
        with tempfile.TemporaryDirectory() as tmp:
            r = validate(built(tmp), [])
            self.assertEqual(len(r["placeholders"]),
                             sum(len(g["ids"]) for g in REGISTRY["groups"].values()))
            self.assertIn("design question", " ".join(r["not_covered"]))


if __name__ == "__main__":
    unittest.main()
