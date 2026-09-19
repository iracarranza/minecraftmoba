"""Stage 5: receipts, refused reuse, deterministic packaging, separate install."""
import json
import shutil
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from terrain_harvest import lifecycle
from terrain_harvest.lifecycle import (RECEIPT, install, package, reuse_refusal, write_receipt)
from terrain_harvest.materialize import export_volume
from test_terrain_preservation import build_source, volume

class ReceiptTests(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory(prefix='harvest-lifecycle-')
        self.tmp = Path(self._tmp.name); self.addCleanup(self._tmp.cleanup)
        self.v = volume()
        self.src = build_source(self.tmp/'source', block_entity=(5, 1, 5))
        self.dest = self.tmp/'dest'
        record = export_volume(self.v, self.src, self.dest)
        from terrain_harvest.model import dumps
        (self.dest/'terrain_volume.json').write_text(dumps(self.v))
        write_receipt(self.dest, self.v, record, self.src)

    def test_complete_export_is_reusable(self):
        self.assertIsNone(reuse_refusal(self.dest, self.v, self.src))

    def test_interrupted_build_has_no_receipt(self):
        (self.dest/RECEIPT).unlink()
        self.assertIn('never completed', reuse_refusal(self.dest, self.v, self.src))

    def test_exporter_version_change_refuses_reuse(self):
        original = lifecycle.EXPORTER_VERSION
        lifecycle.EXPORTER_VERSION = original + '-next'
        self.addCleanup(setattr, lifecycle, 'EXPORTER_VERSION', original)
        self.assertIn('exporter version changed', reuse_refusal(self.dest, self.v, self.src))

    def test_source_mutation_refuses_reuse(self):
        f = next(iter(sorted((self.src/'region').glob('*.mca'))))
        f.write_bytes(f.read_bytes() + b'\0' * 4096)
        self.assertIn('changed since export', reuse_refusal(self.dest, self.v, self.src))

    def test_missing_source_file_refuses_reuse(self):
        next(iter(sorted((self.src/'region').glob('*.mca')))).unlink()
        self.assertIn('missing', reuse_refusal(self.dest, self.v, self.src))

    def test_output_tamper_refuses_reuse(self):
        f = next(iter(sorted((self.dest/'region').glob('*.mca'))))
        f.write_bytes(f.read_bytes() + b'\0' * 4096)
        self.assertIn('output differs', reuse_refusal(self.dest, self.v, self.src))

    def test_deleted_output_file_refuses_reuse(self):
        next(iter(sorted((self.dest/'region').glob('*.mca')))).unlink()
        self.assertIn('output differs', reuse_refusal(self.dest, self.v, self.src))

    def test_extra_output_file_refuses_reuse(self):
        (self.dest/'region'/'stray.mca').write_bytes(b'x')
        self.assertIn('output differs', reuse_refusal(self.dest, self.v, self.src))

    def test_manifest_tamper_refuses_reuse(self):
        m = self.dest/'terrain_volume.json'
        m.write_text(m.read_text().replace('"local_section"', '"large_section"'))
        self.assertIn('changed since export', reuse_refusal(self.dest, self.v, self.src))

    def test_receipt_is_not_included_in_its_own_hashes(self):
        receipt = json.loads((self.dest/RECEIPT).read_text())
        self.assertNotIn(RECEIPT, receipt['output_sha256'])

    def test_receipt_for_another_volume_is_refused(self):
        other = volume('ellipse')
        self.assertIn('different volume', reuse_refusal(self.dest, other, self.src))

class PackagingTests(unittest.TestCase):
    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory(prefix='harvest-package-')
        self.tmp = Path(self._tmp.name); self.addCleanup(self._tmp.cleanup)
        self.gallery = self.tmp/'gallery'
        (self.gallery/'dimensions').mkdir(parents=True)
        (self.gallery/'gallery.json').write_text('{"schema": "terrain_gallery/1"}\n')
        (self.gallery/'dimensions'/'a.bin').write_bytes(b'abc')

    def test_packaging_is_byte_identical_across_runs(self):
        a = package(self.gallery, self.tmp/'a.zip')
        b = package(self.gallery, self.tmp/'b.zip')
        self.assertEqual(a['sha256'], b['sha256'])
        self.assertEqual(a['files'], 2)

    def test_packaging_refuses_overwrite(self):
        package(self.gallery, self.tmp/'a.zip')
        with self.assertRaises(FileExistsError):
            package(self.gallery, self.tmp/'a.zip')

    def test_install_is_separate_and_marked(self):
        saves = self.tmp/'saves'; saves.mkdir()
        r = install(self.gallery, saves, 'Gallery')
        self.assertTrue((saves/'Gallery'/'INSTALLED.txt').exists())
        self.assertTrue((self.gallery/'gallery.json').exists())
        self.assertFalse((self.gallery/'INSTALLED.txt').exists())
        with self.assertRaises(FileExistsError):
            install(self.gallery, saves, 'Gallery')

    def test_install_refuses_incomplete_gallery(self):
        (self.gallery/'gallery.json').unlink()
        (self.gallery/'INCOMPLETE').write_text('x')
        with self.assertRaisesRegex(ValueError, 'incomplete'):
            install(self.gallery, self.tmp/'saves2', 'Gallery')

if __name__ == '__main__':
    unittest.main()
