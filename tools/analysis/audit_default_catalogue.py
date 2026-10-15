"""Snapshot audit of the concurrently appended actual-surface seed catalogue.

No source world, producer, compiler threshold or canonical design is mutated.
"""
from __future__ import annotations
import argparse
from collections import Counter, defaultdict
import hashlib
import json
import math
from pathlib import Path
import statistics
import time

ROOT=Path(__file__).resolve().parents[2]
DEFAULT_SOURCE=ROOT/'implementation/worldgen/reports/default_actual_catalogue_2026-10-14'
DEFAULT_OUT=ROOT/'implementation/worldgen/reports/default_catalogue_audit_2026-10-14'
METRICS=set('western_mean_elevation_advantage_y highland_depth_blocks highland_fraction_west local_relief_p90_y steep_fraction western_approach_bands western_wall_concentration snow_cold_cells_west coastline_sample_edge_blocks east_coast_boundary_variation_blocks east_coast_headland_cove_turns coastal_buildable_sample_cells east_actual_ocean_fraction west_actual_ocean_fraction east_over_west_ocean_advantage actual_surface_water_fraction actual_river_water_cells actual_river_mouth_sample_cells actual_water_body_count largest_actual_water_body_blocks2 forest_region_count largest_forest_region_blocks2 forest_interior_depth_blocks open_buildable_fraction_of_land homeland_developability_bias'.split())


def read_json(path):
    return json.loads(path.read_text())


def snapshot_jsonl(path):
    if not path.exists():
        return [],{'bytes':0,'discarded_incomplete_tail_bytes':0}
    with path.open('rb') as handle:
        size=path.stat().st_size
        raw=handle.read(size)
    boundary=raw.rfind(b'\n')+1
    records=[json.loads(line) for line in raw[:boundary].splitlines() if line]
    return records,{'bytes':len(raw),'discarded_incomplete_tail_bytes':len(raw)-boundary}


def save(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2,allow_nan=False)+'\n')


def audit(source,out):
    started=time.time(); before=read_json(source/'checkpoint.json')
    records,snapshot=snapshot_jsonl(source/'actual-partials.jsonl')
    original_missing=sum('metrics' not in record for record in records)
    supplements,supplement_snapshot=snapshot_jsonl(source/'regional-vector-supplements.jsonl')
    supplement_map={r['id']:r for r in supplements}
    duplicate_supplements={key:value for key,value in Counter(r['id'] for r in supplements).items() if value>1}
    applied=0; supplement_failures=[]
    for record in records:
        supplement=supplement_map.get(record['id'])
        if supplement is None or 'metrics' in record:
            continue
        if not supplement.get('does_not_change_existing_acceptance') or supplement.get('qualified') is not False or any(supplement.get(k)!=record.get(k) for k in ('seed','bounds','rotation','reflected','source_surface','source_sha256')):
            supplement_failures.append({'id':record['id'],'problem':'invalid_original_record_binding'}); continue
        for key in ('metrics','unavailable_metrics','measurement_scope','provenance'):
            record[key]=supplement[key]
        applied+=1
    errors,error_snapshot=snapshot_jsonl(source/'errors.jsonl')
    after=read_json(source/'checkpoint.json'); manifest=read_json(source/'manifest.json')
    ids=[r['id'] for r in records]; duplicates={k:v for k,v in Counter(ids).items() if v>1}
    error_ids={r['job']['id'] for r in errors}
    expected_prefix=[j['id'] for j in manifest['jobs'][:len(records)+len(errors)] if j['id'] not in error_ids]
    measured=[r for r in records if 'metrics' in r]
    no_metrics=[r for r in records if 'metrics' not in r]
    missing_metric_keys=[{'id':r['id'],'missing':sorted(METRICS-set(r.get('metrics',{}))-set(r.get('unavailable_metrics',[])))} for r in measured if METRICS-set(r.get('metrics',{}))-set(r.get('unavailable_metrics',[]))]
    invariants=[]; schema_violations=[]
    for record in records:
        problems=[]
        for key in ('seed','bounds','rotation','reflected','source_surface','source_sha256','qualified','ready','unknown_checks','rejections','user_rejected_seed','recommended_bank_eligible'):
            if key not in record: problems.append('missing:'+key)
        if record.get('qualified') is not False or record.get('ready') is not False:
            problems.append('partial_record_claims_qualification')
        if record.get('user_rejected_seed') and record.get('recommended_bank_eligible'):
            problems.append('user_rejected_seed_recommended')
        if not record.get('unknown_checks'):
            problems.append('unknown_checks_not_preserved')
        source_entry=manifest['sources'][record['source_index']]
        if record.get('source_sha256')!=source_entry['source_sha256'] or record['source_surface']!=source_entry['source_surface']:
            problems.append('record_source_reference_mismatch')
        b=record['bounds']; sb=source_entry['bounds']
        if not (sb[0]<=b[0]<=b[1]<=sb[1] and sb[2]<=b[2]<=b[3]<=sb[3]):
            problems.append('window_outside_complete_source')
        identity=json.dumps([record['seed'],b,record['rotation'],record['reflected']],separators=(',',':'))
        if hashlib.sha256(identity.encode()).hexdigest()[:20]!=record['id']:
            problems.append('identity_does_not_match_seed_window_orientation')
        if 'metrics' in record and 'NO_COARSE_HOME_PAIR' not in record['rejections']:
            for key in ('recognition_rejections','homebase_rejections','fitted_homelands','actual_symmetry','stage_c'):
                if key not in record: problems.append('missing_measured_evidence:'+key)
            expected=all(record.get(k) is True for k in ('default_regional_pass','homebase_partial_pass','fountain_pair_minimum_175_pass','actual_symmetry_pass'))
            if expected!=record.get('partial_front_end_pass'):
                problems.append('partial_front_end_flag_inconsistent')
        elif 'metrics' in record:
            if len(record['metrics'])!=24 or record.get('unavailable_metrics')!=['homeland_developability_bias']:
                problems.append('independent_regional_vector_not_explicit')
            if record.get('partial_front_end_pass') is not False or record.get('provenance',{}).get('acceptance_modified') is not False:
                problems.append('diagnostic_measurements_changed_acceptance')
        if problems: schema_violations.append({'id':record['id'],'problems':problems})
    # Stratify independent coordinate checks across orientation/reflection groups.
    samples=[]; by_orientation=defaultdict(list)
    for record in measured:
        by_orientation[(record['rotation'],record['reflected'])].append(record)
    for group in by_orientation.values():
        samples.extend(group[::max(1,len(group)//8)][:8])
    samples_by_source=defaultdict(list)
    for record in samples: samples_by_source[record['source_surface']].append(record)
    sources=[]; source_failures=[]; coordinate_failures=[]; fountains_checked=0
    for entry in manifest['sources']:
        path=Path(entry['source_surface']); raw=path.read_bytes(); extracted=json.loads(raw)
        rows=extracted['rows']; width=extracted['width']; height=extracted['height']
        bounds=[rows[0][0]['x']-4,rows[0][-1]['x']+3,rows[0][0]['z']-4,rows[-1][0]['z']+3]
        cells=[cell for row in rows for cell in row]; count=len(cells)
        problems=[]
        if hashlib.sha256(raw).hexdigest()!=entry['source_sha256']: problems.append('source_hash_changed')
        if not extracted.get('full_chunks') or extracted['full_chunks']!=extracted.get('expected_chunks'): problems.append('incomplete_chunks')
        if extracted.get('sample_spacing')!=8: problems.append('not_eight_block_surface_samples')
        if len(rows)!=height or any(len(row)!=width for row in rows): problems.append('ragged_dimensions')
        if extracted.get('expected_chunks')!=width*height//4: problems.append('sample_chunk_area_mismatch')
        if bounds!=entry['bounds']: problems.append('bounds_mismatch')
        if not all('actual_surface_water' in c and 'terrain_y' in c and 'biome' in c and 'ground_block' in c for c in cells): problems.append('missing_actual_column_fields')
        if problems: source_failures.append({'source_surface':str(path),'problems':problems})
        water=sum(c['actual_surface_water'] for c in cells)/count
        ocean=sum(c['actual_surface_water'] and 'ocean' in c['biome'] for c in cells)/count
        family='archipelago' if '/archipelago_' in str(path) else 'pale' if '/pale_' in str(path) else 'other'
        sources.append({**entry,'source_family':family,'actual_water_fraction':water,'actual_ocean_fraction':ocean,
                        'full_chunks':extracted['full_chunks'],'expected_chunks':extracted['expected_chunks'],
                        'complete_actual_surface':not problems,'ground_scan':extracted.get('ground_scan'),
                        'columns':count})
        for record in samples_by_source[str(path)]:
            for team,point in record.get('fountains',{}).items():
                x,y,z=point; sx=(x-(bounds[0]+4))//8; sz=(z-(bounds[2]+4))//8
                if not (0<=sx<width and 0<=sz<height):
                    coordinate_failures.append({'id':record['id'],'team':team,'reason':'outside_source'}); continue
                cell=rows[sz][sx]; fountains_checked+=1
                if [x,z]!=[cell['x'],cell['z']] or y!=cell['terrain_y']+1 or cell['actual_surface_water']:
                    coordinate_failures.append({'id':record['id'],'team':team,'reason':'source_column_mismatch'})
    metrics_summary={}
    for key in sorted(METRICS):
        values=[r['metrics'][key] for r in measured if isinstance(r['metrics'].get(key),(int,float))]
        if values: metrics_summary[key]={'count':len(values),'min':min(values),'median':statistics.median(values),'max':max(values)}
    per_seed=[]
    for seed in sorted({r['seed'] for r in records}):
        own=[r for r in records if r['seed']==seed]
        per_seed.append({'seed':seed,'measured_orientations':len(own),
                         'distinct_windows':len({tuple(r['bounds']) for r in own}),
                         'default_regional_passes':sum(r.get('default_regional_pass',False) for r in own),
                         'homebase_partial_passes':sum(r.get('homebase_partial_pass',False) for r in own),
                         'combined_partial_passes':sum(r.get('partial_front_end_pass',False) for r in own),
                         'user_rejected_seed':any(r['user_rejected_seed'] for r in own)})
    low=before['actual_partial_compilations']; high=after['actual_partial_compilations']+1
    issues=[]
    if no_metrics:
        issues.append({'severity':'measurement_gap','code':'NO_COARSE_HOME_PAIR_HAS_NO_REGIONAL_VECTOR',
                       'count':len(no_metrics),'fraction':len(no_metrics)/max(1,len(records)),
                       'reason':'evaluate_orientation returns before regional metrics when no initial Homebase pair is found. Rejection and source reference survive, but later looser regional policies require remeasurement.',
                       'examples':[{'id':r['id'],'seed':r['seed'],'bounds':r['bounds'],'source_surface':r['source_surface'],'rejections':r['rejections']} for r in no_metrics[:3]]})
    if source_failures or schema_violations or duplicates or missing_metric_keys or coordinate_failures or duplicate_supplements or supplement_failures:
        issues.append({'severity':'error','code':'AUDIT_INVARIANT_FAILURE','see_detail_fields':True})
    result={'status':'measurement_gap' if issues and not any(i['severity']=='error' for i in issues) else 'fail' if issues else 'pass',
            'snapshot_unix':started,'snapshot':snapshot,'error_snapshot':error_snapshot,'supplement_snapshot':supplement_snapshot,
            'checkpoint_before':before,'checkpoint_after':after,
            'counts':{'actual_partial_records':len(records),'measured_vectors':len(measured),'missing_vectors':len(no_metrics),
                      'full_25_value_vectors':sum(METRICS<=set(r['metrics']) for r in measured),
                      'independent_24_value_vectors':sum(len(r['metrics'])==24 for r in measured),
                      'legacy_missing_vector_records':original_missing,'supplements_applied':applied,
                      'supplement_records_not_counted_as_observations':len(supplements),
                      'distinct_sampled_seeds':len(per_seed),'distinct_sampled_windows':len({(r['seed'],*r['bounds']) for r in records}),
                      'distinct_window_orientations':len(set(ids)),'duplicate_ids':len(duplicates),
                      'source_windows':len(sources),'distinct_source_seeds':len({s['seed'] for s in sources}),
                      'queued_window_orientations':len(manifest['jobs']),
                      'default_regional_pass_records':sum(r.get('default_regional_pass',False) for r in records),
                      'distinct_seeds_with_default_regional_pass':sum(s['default_regional_passes']>0 for s in per_seed),
                      'homebase_partial_pass_records':sum(r.get('homebase_partial_pass',False) for r in records),
                      'combined_partial_pass_records':sum(r.get('partial_front_end_pass',False) for r in records),
                      'distinct_seeds_with_combined_partial_pass':sum(s['combined_partial_passes']>0 for s in per_seed),
                      'fully_qualified_new_seeds':0,'sampled_coordinate_records':len(samples),'fountains_independently_checked':fountains_checked},
            'progress_integrity':{'catalogue_is_exact_manifest_prefix_excluding_errors':ids==expected_prefix,
                                  'record_count_bracketed_by_concurrent_checkpoint':low<=len(records)<=high,
                                  'before_count':low,'snapshot_count':len(records),'after_count':after['actual_partial_compilations'],
                                  'explanation':'Producer appends one record before atomically saving its next cursor; one in-flight record may be ahead of a checkpoint.'},
            'source_bias':{'families':dict(Counter(s['source_family'] for s in sources)),
                           'sources_with_water_gt_0431':sum(s['actual_water_fraction']>.431 for s in sources),
                           'median_source_water_fraction':statistics.median(s['actual_water_fraction'] for s in sources),
                           'source_scope':'Earlier selected archipelago/Pale windows, not a representative random sample of Default terrain. Existing worlds only; new windows do not count as newly found seeds.'},
            'truth_limits':['Complete official-vanilla extracted surface grids and their hashes were checked; no native approximate probes are used in this lane.',
                            'Source completeness and hashes are validated; this audit does not independently regenerate worlds or prove every source block.',
                            'Eight-block actual surface sampling is partial evidence, never exact block-column physical or full gameplay qualification.'],
            'issues':issues,'duplicates':duplicates,'duplicate_supplements':duplicate_supplements,'supplement_failures':supplement_failures,'source_failures':source_failures,'schema_violations':schema_violations,
            'missing_metric_keys':missing_metric_keys,'coordinate_failures':coordinate_failures,
            'metric_ranges':metrics_summary,'sources':sources,'per_seed':per_seed,
            'rejection_counts':dict(Counter(code for r in records for code in r.get('rejections',[]))),
            'elapsed_seconds':round(time.time()-started,3)}
    if not all(result['progress_integrity'][key] for key in ('catalogue_is_exact_manifest_prefix_excluding_errors','record_count_bracketed_by_concurrent_checkpoint')):
        result['status']='fail'; result['issues'].append({'severity':'error','code':'PROGRESS_INTEGRITY_FAILURE'})
    out.mkdir(parents=True,exist_ok=True); save(out/'audit.json',result)
    count=result['counts']; bias=result['source_bias']; progress=result['progress_integrity']
    text=f'''# Default actual catalogue audit — 14 October 2026

Snapshot: **{count['actual_partial_records']:,} actual partial records**, **{count['distinct_sampled_windows']:,} windows**, **{count['distinct_sampled_seeds']} distinct existing seeds**. The queue contains {count['queued_window_orientations']:,} window/orientation jobs, not that many seeds. None is fully qualified.

**Progress integrity:** {count['duplicate_ids']} duplicate job IDs; exact manifest prefix: {progress['catalogue_is_exact_manifest_prefix_excluding_errors']}; snapshot count bracketed by concurrent checkpoint: {progress['record_count_bracketed_by_concurrent_checkpoint']}. A concurrently appended incomplete JSONL tail is excluded from the snapshot.

**Evidence retained:** {count['full_25_value_vectors']:,} complete 25-value regional vectors plus {count['independent_24_value_vectors']} Homebase-independent 24-value vectors, named rejection details and explicit unknown checks. {count['legacy_missing_vector_records']} original records lacked a vector; {count['supplements_applied']} append-only supplements restored their raw measurements by original job ID. Supplements add **zero** observations. Homebase bias remains explicitly unavailable without real Homebases; diagnostic placeholders never change acceptance. {count['missing_vectors']} records still lack a regional vector.

**Actual source audit:** {count['source_windows']} source windows / {count['distinct_source_seeds']} seeds; {len(source_failures)} completeness/hash/shape failures. {count['fountains_independently_checked']} Fountain positions were checked against actual dry source columns across {count['sampled_coordinate_records']} sampled records; {len(coordinate_failures)} mismatches. No approximate native probes enter this lane. Actual 8-block surface evidence remains distinct from exact block fit and full gameplay qualification.

**Selection bias:** source families {bias['families']}; {bias['sources_with_water_gt_0431']}/{count['source_windows']} windows exceed 43.1% actual surface water; median water {bias['median_source_water_fraction']:.1%}. Cached source supply is strongly shaped by earlier maritime/Pale searches and cannot estimate Default seed rarity.

**Strict Default supply in this snapshot:** {count['default_regional_pass_records']} regional passes across {count['distinct_seeds_with_default_regional_pass']} seeds; {count['homebase_partial_pass_records']} Homebase partial passes. {count['combined_partial_pass_records']} combined front-end passes across {count['distinct_seeds_with_combined_partial_pass']} seeds meet regional recognition, sampled Homebase, Fountain-pair 175 spacing and sampled symmetry only. Defensive ordering/all-nine spacing, Routes, Lair, exact columns, resources and readiness remain unknown.

See [audit.json](audit.json) for full source measurements, per-seed counts, metric ranges and concrete gap examples. Producers and canonical filters were not changed.
'''
    (out/'README.md').write_text(text)
    return result


def main():
    parser=argparse.ArgumentParser(); parser.add_argument('--source',type=Path,default=DEFAULT_SOURCE)
    parser.add_argument('--out',type=Path,default=DEFAULT_OUT); args=parser.parse_args()
    result=audit(args.source,args.out)
    print(json.dumps({'status':result['status'],'counts':result['counts'],'source_bias':result['source_bias'],
                      'progress_integrity':result['progress_integrity'],'issues':result['issues']}))


if __name__=='__main__':main()
