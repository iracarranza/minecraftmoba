"""Verify the portable, paused Default catalogue review without starting workers."""
from pathlib import Path
import gzip
import hashlib
import json

ROOT = Path(__file__).resolve().parents[2]
REPORTS = ROOT / 'implementation/worldgen/reports'
REVIEW = REPORTS / 'default_catalogue_review_2026-10-15'


def load(path):
    with gzip.open(path, 'rt') as handle:
        return json.load(handle)


def rows(path):
    with gzip.open(path, 'rt') as handle:
        for line in handle:
            yield json.loads(line)


def main():
    snapshot = json.loads((REVIEW / 'snapshot.json').read_text())
    manifest = json.loads((REVIEW / 'file-manifest.json').read_text())
    for entry in manifest:
        path = ROOT / entry['path']
        assert path.stat().st_size == entry['bytes'], path
        assert hashlib.sha256(path.read_bytes()).hexdigest() == entry['sha256'], path
    for entry in snapshot['compressed_exact_sources']:
        digest = hashlib.sha256()
        with gzip.open(ROOT / entry['archive'], 'rb') as handle:
            for block in iter(lambda: handle.read(1024 * 1024), b''):
                digest.update(block)
        assert digest.hexdigest() == entry['source_sha256'], entry['source']
    native = list(rows(REPORTS / 'default_seed_catalogue_2026-10-14/seed-index.jsonl.gz'))
    assert [r['seed'] for r in native] == list(range(910000001, 910013917))
    assert all(r['orientations_measured'] == 400 and r['distinct_regions_measured'] == 100 for r in native)
    for seed in snapshot['native']['included_full_native_sample_seeds']:
        sample = list(rows(REPORTS / f'default_seed_catalogue_2026-10-14/windows/{seed}.jsonl.gz'))
        assert len(sample) == 400 and len({r['window_id'] for r in sample}) == 400
        assert all(r['seed'] == seed and r['qualified'] is False and r['ready'] is False for r in sample)
    actual_dir = REPORTS / 'default_actual_catalogue_2026-10-14'
    jobs = load(actual_dir / 'manifest.json.gz')
    supplements = {r['id']: r for r in rows(actual_dir / 'regional-vector-supplements.jsonl.gz')}
    seen = set()
    counts = {'records': 0, 'default_regional_passes': 0, 'homebase_partial_passes': 0, 'partial_front_end_passes': 0}
    snapshot_compiler = next(r['sha256'] for r in snapshot['compiler_and_evaluator_snapshots'] if r['source'].endswith('/map_compiler.py'))
    for record in rows(actual_dir / 'actual-partials.jsonl.gz'):
        index = counts['records']
        assert record['id'] == jobs['jobs'][index]['id'] and record['id'] not in seen
        seen.add(record['id'])
        assert record['qualified'] is False and record['ready'] is False and record['unknown_checks']
        assert record['compiler_sha256'] == snapshot_compiler
        source = jobs['sources'][record['source_index']]
        assert record['source_sha256'] == source['source_sha256']
        if 'metrics' not in record:
            supplement = supplements[record['id']]
            assert supplement['does_not_change_existing_acceptance'] is True
            assert all(supplement[k] == record[k] for k in ('seed', 'bounds', 'rotation', 'reflected', 'source_sha256'))
            assert supplement['metrics']
        if record['user_rejected_seed']:
            assert record['recommended_bank_eligible'] is False
        counts['records'] += 1
        for field in ('default_regional_pass', 'homebase_partial_pass', 'partial_front_end_pass'):
            counts[field + 'es'] += bool(record.get(field))
    checkpoint = snapshot['actual']
    assert checkpoint['errors'] == 0
    assert counts['records'] == checkpoint['actual_partial_compilations'] == checkpoint['next_job']
    for field in ('default_regional_passes', 'homebase_partial_passes', 'partial_front_end_passes'):
        assert counts[field] == checkpoint[field], field
    assert len(supplements) == 35 and set(supplements) <= seen
    print(json.dumps({'status': 'pass', 'verified_files': len(manifest), 'native_seed_summaries': len(native), 'actual': counts,
                      'scope': 'Portable evidence integrity and identity only; no terrain regeneration or full gameplay qualification.'}, indent=2))


if __name__ == '__main__':
    main()
