from __future__ import annotations

import hashlib
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

WORLDGEN = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(WORLDGEN))

from serialize_default_worlds import SERVER_JAR_SHA1, package_world, verify_server_jar
from serialization.nbt import (
    COMPOUND,
    INT,
    Tag,
    compound,
    dumps,
    integer,
    list_tag,
    loads,
    long_array,
    plain,
    string,
)
from serialization.region import read_region, write_region


class SerializationPrimitiveTests(unittest.TestCase):
    def test_nbt_round_trip_preserves_signed_long_arrays_and_lists(self):
        root = compound(
            name=string("modern-anvil"),
            values=list_tag(INT, [integer(1), integer(-2), integer(3)]),
            packed=long_array([0, (1 << 64) - 1, 1 << 63]),
            nested=Tag(COMPOUND, {"answer": integer(42)}),
        )
        name, decoded = loads(dumps("", root))
        self.assertEqual("", name)
        self.assertEqual("modern-anvil", plain(decoded)["name"])
        self.assertEqual([1, -2, 3], plain(decoded)["values"])
        self.assertEqual([0, -1, -(1 << 63)], plain(decoded)["packed"])
        self.assertEqual(42, plain(decoded)["nested"]["answer"])

    def test_region_round_trip_handles_negative_chunk_coordinates(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "r.-1.0.mca"
            chunks = {(-1, 0): ("", compound(xPos=integer(-1), zPos=integer(0)))}
            write_region(path, chunks)
            decoded = list(read_region(path))
        self.assertEqual(1, len(decoded))
        cx, cz, name, root = decoded[0]
        self.assertEqual((-1, 0, ""), (cx, cz, name))
        self.assertEqual({"xPos": -1, "zPos": 0}, plain(root))

    def test_packages_are_byte_reproducible_and_rooted_by_world_name(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            world = root / "Example_World"
            world.mkdir()
            (world / "level.dat").write_bytes(b"level")
            (world / "note.txt").write_text("inspection\n")
            first, second = root / "first.zip", root / "second.zip"
            package_world(world, first)
            package_world(world, second)
            self.assertEqual(hashlib.sha256(first.read_bytes()).digest(), hashlib.sha256(second.read_bytes()).digest())
            with zipfile.ZipFile(first) as archive:
                self.assertEqual(["Example_World/level.dat", "Example_World/note.txt"], archive.namelist())

    def test_wrong_server_jar_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "server.jar"
            path.write_bytes(b"not the selected Mojang server")
            with self.assertRaisesRegex(ValueError, SERVER_JAR_SHA1):
                verify_server_jar(path)


if __name__ == "__main__":
    unittest.main()
