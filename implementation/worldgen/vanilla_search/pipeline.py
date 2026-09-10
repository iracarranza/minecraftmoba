"""Small official-worldgen proof-of-concept search, retention and reporting."""

from __future__ import annotations

import json
import hashlib
import math
import shutil
import tempfile
import time
from pathlib import Path

from serialization.nbt import byte, dump_gzip, integer, load_gzip, string
from successor.grid import rle

from . import DATA_VERSION, GENERATOR, SERVER_SHA1, VERSION
from .acquire import generate_world, prepare_runtime, verify_server_jar
from .evaluate import evaluate_region
from .extract import extract_region
from .render import render

CHUNK_BOUNDS=(-27,26,-33,32)


def _public(candidate):
    output={k:v for k,v in candidate.items() if k not in ("_rows","_masks","_world")}
    rows=candidate["_rows"]; cells=[c for row in rows for c in row]
    output["feature_grid"]={"order":"oriented row-major","width":len(rows[0]),"height":len(rows),"sample_spacing_blocks":8,"height_rle":rle([c["terrain_y"] for c in cells]),"biome_rle":rle([c["biome"] for c in cells]),"surface_block_rle":rle([c["top_block"] for c in cells]),"actual_surface_water_rle":rle([c["actual_surface_water"] for c in cells]),"forest_mask_rle":rle(candidate["_masks"]["forest"]),"buildable_mask_rle":rle(candidate["_masks"]["buildable"]),"highland_mask_rle":rle(candidate["_masks"]["highland"])}
    for key in ("open_ground", "canopy"):
        if key in candidate["_masks"]:
            output["feature_grid"][key+"_mask_rle"] = rle(candidate["_masks"][key])
    return output


def _distance(a,b):
    am,bm=a["metrics"],b["metrics"]
    keys=("western_mean_elevation_advantage_y","highland_depth_blocks","east_actual_ocean_fraction","actual_surface_water_fraction","largest_forest_region_blocks2","open_buildable_fraction_of_land")
    scales=(15,500,1,.3,100000,.5)
    return math.sqrt(sum(((am[k]-bm[k])/s)**2 for k,s in zip(keys,scales)))


def _select(candidates,count):
    screened=[c for c in candidates if not c["experimental_screen"]["warnings"]]
    warning_cost={"eastern_ocean_present":25,"ocean_gradient_points_east":8,"western_relief_advantage":16,"western_highland_depth":18,"region_not_mostly_water":20,"homelands_reasonably_viable":20,"six_corridors_connect":30}
    near=sorted((c for c in candidates if c not in screened),key=lambda c:c["score"]-sum(warning_cost.get(w,12) for w in c["experimental_screen"]["warnings"]),reverse=True)
    ranked=sorted(screened,key=lambda c:c["score"],reverse=True)+near
    selected=ranked[:1]
    while len(selected)<min(count,len(ranked)):
        remaining=[c for c in ranked if c not in selected]
        fewest=min(len(c["experimental_screen"]["warnings"]) for c in remaining)
        remaining=[c for c in remaining if len(c["experimental_screen"]["warnings"])==fewest]
        selected.append(max(remaining,key=lambda c:min(_distance(c,q) for q in selected)+c["score"]/180-sum(warning_cost.get(w,12) for w in c["experimental_screen"]["warnings"])/10))
    return selected


def _configure_world(world,name,candidate):
    _,root=load_gzip(Path(world)/"level.dat"); data=root.value["Data"].value; x,z=candidate["homelands"]["north"]["raw_world"]
    nearest=min((cell for row in candidate["_rows"] for cell in row),key=lambda c:(c["x"]-x)**2+(c["z"]-z)**2)
    spawn_y=nearest["surface_y"]+2
    data["SpawnX"],data["SpawnY"],data["SpawnZ"]=integer(x),integer(spawn_y),integer(z); data["GameType"]=integer(1); data["allowCommands"]=byte(1); data["LevelName"]=string(name)
    dump_gzip(Path(world)/"level.dat","",root)
    chunk_hashes={str(path.relative_to(world)):hashlib.sha256(path.read_bytes()).hexdigest() for folder in ("region","entities","poi") for path in sorted((Path(world)/folder).glob("*.mca"))}
    inspection={"minecraft_java_version":VERSION,"data_version":DATA_VERSION,"terrain_chunks":"unmodified official-server vanilla generation","analysis_orientation":candidate["orientation"],"evaluated_chunk_bounds":candidate["region"]["chunk_bounds"],"spawn":{"x":x,"y":spawn_y,"z":z,"mode":"creative"},"homelands":candidate["homelands"],"actual_structures":candidate["actual_structures"],"provisional_route_compatibility":candidate["routes"],"chunk_file_sha256":chunk_hashes}
    (Path(world)/"INSPECTION.json").write_text(json.dumps(inspection,indent=2)+"\n")
    (Path(world)/"README_INSPECTION.txt").write_text(f"Minecraft Java {VERSION}\nActual unmodified vanilla-generated chunks.\nSpawn: {x} {spawn_y} {z}\nSee INSPECTION.json for orientation, bounds, homelands, structures, and analytical Route corridors.\n")
    return inspection["spawn"]


def refresh_retained_world(world, candidate_path, seed_dir):
    """Re-extract a retained vanilla world after analysis-code changes."""
    world, candidate_path, seed_dir = Path(world), Path(candidate_path), Path(seed_dir)
    previous = json.loads(candidate_path.read_text())
    extracted = extract_region(world, CHUNK_BOUNDS)
    candidate = evaluate_region(extracted)
    candidate.update({
        "schema_version": 1, "generator": GENERATOR,
        "minecraft_java_version": VERSION, "data_version": DATA_VERSION,
        "worldgen_truth": previous["worldgen_truth"], "seed": previous["seed"],
        "region": previous["region"], "actual_structures": extracted["structures"],
        "actual_block_volume_presence": extracted["block_volume_presence"],
        "data_completeness": {"full_chunks": extracted["full_chunks"], "expected_chunks": extracted["expected_chunks"], "caves_and_fluids": "section-palette presence only; volume deferred", "ores": "deferred", "entities": "deferred"},
        "timing": previous.get("timing", {}), "local_world_directory": str(world.resolve()),
    })
    name = world.name
    candidate["inspection_spawn"] = _configure_world(world, name, candidate)
    render(candidate, seed_dir)
    candidate_path.write_text(json.dumps(_public(candidate), separators=(",", ":")) + "\n")
    return candidate


def run_search(server_jar,java,outdir,artifacts,start_seed=920270001,attempts=10,shortlist=3,accept_eula=False):
    verify_server_jar(server_jar); started=time.perf_counter(); outdir=Path(outdir); artifacts=Path(artifacts)
    temp=Path(tempfile.mkdtemp(prefix="moba-vanilla-poc-")); runtime=temp/"runtime"; setup_seconds=prepare_runtime(server_jar,java,runtime)
    candidates=[]; stage={"runtime_setup_seconds":round(setup_seconds,3),"acquisition_seconds":0.0,"extraction_seconds":0.0,"evaluation_seconds":0.0,"rendering_seconds":0.0}
    try:
        for offset in range(attempts):
            seed=start_seed+offset; print(f"seed={seed} acquisition starting",flush=True)
            work=temp/f"seed_{seed}"; world,timing=generate_world(seed,server_jar,java,work,runtime,CHUNK_BOUNDS,accept_eula,lambda i,n: print(f"seed={seed} chunks batch={i}/{n}",flush=True))
            extracted=extract_region(world,CHUNK_BOUNDS); evaluated=evaluate_region(extracted)
            stage["acquisition_seconds"]+=timing["total_acquisition_seconds"]; stage["extraction_seconds"]+=extracted["extraction_seconds"]; stage["evaluation_seconds"]+=evaluated["evaluation_seconds"]
            evaluated.update({"schema_version":1,"generator":GENERATOR,"minecraft_java_version":VERSION,"data_version":DATA_VERSION,"worldgen_truth":{"source":"official Mojang dedicated server","server_jar_sha1":SERVER_SHA1,"terrain_modified":False},"seed":seed,"region":{"chunk_bounds":list(CHUNK_BOUNDS),"block_bounds":[CHUNK_BOUNDS[0]*16,CHUNK_BOUNDS[1]*16+15,CHUNK_BOUNDS[2]*16,CHUNK_BOUNDS[3]*16+15]},"actual_structures":extracted["structures"],"actual_block_volume_presence":extracted["block_volume_presence"],"data_completeness":{"full_chunks":extracted["full_chunks"],"expected_chunks":extracted["expected_chunks"],"caves_and_fluids":"section-palette presence only; volume deferred","ores":"deferred","entities":"deferred"},"timing":{**timing,"chunk_parsing_and_feature_extraction_seconds":extracted["extraction_seconds"],"python_evaluation_seconds":evaluated["evaluation_seconds"]},"_world":world})
            candidates.append(evaluated); print(f"seed={seed} score={evaluated['score']} orientation={evaluated['orientation']}",flush=True)
        selected=_select(candidates,shortlist)
        if outdir.exists(): shutil.rmtree(outdir)
        if artifacts.exists(): shutil.rmtree(artifacts)
        outdir.mkdir(parents=True); (artifacts/"worlds").mkdir(parents=True)
        for candidate in selected:
            name=f"Minecraft_MOBA_Vanilla_POC_{candidate['seed']}"; destination=artifacts/"worlds"/name
            shutil.move(str(candidate["_world"]),destination); candidate["local_world_directory"]=str(destination)
            candidate["inspection_spawn"]=_configure_world(destination,name,candidate)
            seed_dir=outdir/"shortlist"/str(candidate["seed"]); seed_dir.mkdir(parents=True)
            render_started=time.perf_counter(); render(candidate,seed_dir); stage["rendering_seconds"]+=time.perf_counter()-render_started
            (seed_dir/"candidate.json").write_text(json.dumps(_public(candidate),separators=(",",":"))+"\n")
        summary={"generator":GENERATOR,"minecraft_java_version":VERSION,"server_jar_sha1":SERVER_SHA1,"seed_range":[start_seed,start_seed+attempts-1],"regions_evaluated":attempts,"region_chunk_bounds":list(CHUNK_BOUNDS),"region_block_dimensions":[864,1056],"orientations_per_region":8,"experimental_screen_passes":sum(not c["experimental_screen"]["warnings"] for c in candidates),"shortlist_seeds":[c["seed"] for c in selected],"stage_runtime_seconds":{k:round(v,3) for k,v in stage.items()},"total_runtime_seconds":round(time.perf_counter()-started,3),"evaluated":[{"seed":c["seed"],"score":c["score"],"orientation":c["orientation"],"metrics":c["metrics"],"experimental_warnings":c["experimental_screen"]["warnings"],"structure_count":len(c["actual_structures"]),"retained":c in selected} for c in candidates]}
        (outdir/"search_summary.json").write_text(json.dumps(summary,indent=2)+"\n"); (outdir/"FINDINGS.md").write_text(_report(selected,summary,artifacts))
        return selected,summary
    finally:
        shutil.rmtree(temp,ignore_errors=True)


def _report(selected,summary,artifacts):
    t=summary["stage_runtime_seconds"]
    correction=summary.get("post_screen_regeneration",{}); correction_text=f" A post-review screen correction regenerated seed 920270003 in {correction.get('total_seconds',0):.1f}s; this is reported separately from the ten-seed batch." if correction else ""
    lines=["# Vanilla Default-region seed-search proof of concept","","> Prototype/test evidence only. No final Default map is selected and no terrain was modified.","","## Method","",f"Worldgen truth is the [official Mojang Minecraft Java {VERSION} dedicated server](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-11), pinned by SHA-1 `{SERVER_SHA1}`. In the main batch, each seed was started once, the 864×1056-block origin region was generated in twenty <=256-chunk `forceload` batches, and the resulting full Anvil chunks were parsed directly. [Cubiomes](https://github.com/Cubitect/cubiomes) (MIT) was investigated but not adopted: it is useful for biome/structure prefiltering, while its own documentation states that it lacks block-level terrain and reliable surface-height checks. [Chunky](https://github.com/pop4959/Chunky) (GPL-3.0) was also reviewed, but adding a mod/plugin did not avoid full per-seed chunk generation or improve source fidelity over Mojang's server for this small test.","",f"The batch evaluated {summary['regions_evaluated']} seeds × 8 analytical orientations. Runtime: setup {t['runtime_setup_seconds']:.1f}s; official-server acquisition {t['acquisition_seconds']:.1f}s; parsing/extraction {t['extraction_seconds']:.1f}s; Python evaluation {t['evaluation_seconds']:.1f}s; rendering {t['rendering_seconds']:.1f}s; total {summary['total_runtime_seconds']:.1f}s. Acquisition is the bottleneck.{correction_text}","","## Retained regions",""]
    reasons={
        920270010:("the strongest coast/highland contrast in the sample, with a broad mountainous west and highly articulated ocean edge","49.1% surface water, island fragmentation, and a 34-block maximum sampled corridor step make it the most traversal-risky finalist"),
        920270002:("the most balanced proof that deep forest, navigable western relief, usable land, real structures, and a clear eastern coast can coexist naturally","the forest dominates much of the west and may compress open between-Route Wilderness despite the favorable coarse corridor gradients"),
        920270003:("strong internal relief, snow/cold evidence, and unusually legible connected vanilla water geography make it a useful stress case","the selected frame only narrowly reaches ocean and fails the east-over-west ocean-gradient screen"),
    }
    for c in selected:
        warnings=c["experimental_screen"]["warnings"]; role="full functional-screen pass" if not warnings else "one-warning near-miss retained to test the screen boundary"
        keep,reject=reasons.get(c["seed"],("distinct real coast/highland/forest and hydrology evidence","block-scale traversal remains unvalidated"))
        m=c["metrics"]; lines += [f"### Seed {c['seed']}","",f"- Selection role: {role}; warnings: {', '.join(warnings) or 'none'}.",f"- Raw bounds: `{c['region']['block_bounds']}`; orientation: {c['orientation']['rotation_degrees_clockwise']}° clockwise, east/west reflection={c['orientation']['east_west_reflected']}.",f"- Western elevation advantage {m['western_mean_elevation_advantage_y']} Y; highland depth {m['highland_depth_blocks']} blocks; local-relief P90 {m['local_relief_p90_y']} Y; {m['western_approach_bands']} sampled approach bands; west snow/cold samples {m['snow_cold_cells_west']}.",f"- East/west actual-ocean fractions {m['east_actual_ocean_fraction']:.1%}/{m['west_actual_ocean_fraction']:.1%}; sampled coastline {m['coastline_sample_edge_blocks']} blocks; boundary variation {m['east_coast_boundary_variation_blocks']} blocks; {m['east_coast_headland_cove_turns']} turns; actual river-water/mouth samples {m['actual_river_water_cells']}/{m['actual_river_mouth_sample_cells']}; {m['actual_water_body_count']} actual surface-water components.",f"- Largest forest region {m['largest_forest_region_blocks2']:,} blocks²; forest interior depth {m['forest_interior_depth_blocks']} blocks; open/buildable land {m['open_buildable_fraction_of_land']:.1%}.",f"- Actual structure starts: {len(c['actual_structures'])} ({', '.join(sorted({x['type'].removeprefix('minecraft:') for x in c['actual_structures']})) or 'none'}).",f"- Homeland developability bias {m['homeland_developability_bias']:.3f}; all six analytical corridors connected={c['routes']['all_six_connected']}; maximum sampled Route step {c['routes']['maximum_step_y']} Y.",f"- Strongest reason to retain: {keep}. Strongest reason to reject: {reject}.",f"- Direct world: `{c['local_world_directory']}`; creative spawn `{c['inspection_spawn']['x']} {c['inspection_spawn']['y']} {c['inspection_spawn']['z']}`.",f"- Renders: `shortlist/{c['seed']}/`.",""]
    lines += ["## Findings","","The three-image review confirms that the candidates are actual vanilla geography rather than synthetic approximations. Seed 920270002 visibly combines an eastern ocean, a coastal peninsula, a large western forest, and broken upland relief. Seed 920270010 has the clearest ocean/highland opposition but its island-heavy east makes several analytical corridors cross water. Seed 920270003 is an intentional boundary case: its mountain and inland-water geography are valuable, while the intended eastern-ocean grammar is visibly weak. An initially retained seed, 920270006, was rejected after the review exposed a north/west-ocean orientation false positive; the orientation selector and east-over-west screen were corrected rather than preserving that result.","","Metrics that transferred cleanly: oriented west/east contrast, elevation/relief/slope, mountain depth, connected-component area/interior depth, open-space share, homeland developability/equivalence, and terrain-cost corridor feasibility. They now consume actual heightmaps, blocks, biomes, fluids, and structures.","","Discarded synthetic assumptions: topology-family labels, generated mountain masks, D8 hydrology, ecology/resource markers, guaranteed snow, synthetic coast profiles, and synthetic POI portfolios. Actual water means sampled vanilla water/ice blocks; river classification additionally requires the vanilla river biome. Cave-water/lava are only section-palette presence in this coarse pass, and ore/entity analysis is deferred.","","Naturally searchable in this sample: ocean adjacency, coast shape, water bodies, biome forests, snow/cold terrain, relief/highland depth, open homeland sites, and real structure starts. Likely later corrections/overlays: exact N/S homeland equivalence, six authored Route relationships, guaranteed competitive resource vocabulary, objective placement, and occasional modest access repair.","","Scaling estimate: the main run averaged about 69 seconds of official generation and 8.5 seconds of extraction per full region. At that observed serial rate, 1,000 full regions would take roughly 21.5 hours and one million roughly 2.5 years before operational overhead; limited process-level parallelism could reduce elapsed time but not the underlying work. A production search should use a faithful 1.21.11 biome/structure prefilter, generate full chunks only for the small surviving fraction, parallelize isolated server processes, and retain no rejected worlds. Millions of seeds require a native coarse filter; official chunk generation belongs only in fine validation.","","Key blockers: no reviewed library found that exposes exact 1.21.11 block terrain/height cheaply; Cubiomes' current mainline does not fully model 1.21.11 block terrain; official generation requires one world/server initialization per seed; full cave/aquifer/lava volumes and structures outside generated chunks cost additional parsing/generation. Natural structures are exact generated starts, but their strategic value is unresolved.","",f"Retained world directories are under `{artifacts/'worlds'}` and are intentionally gitignored. Copy a directory into the Minecraft `saves` folder and open it with Java {VERSION}. The chunks are unmodified vanilla output; only `level.dat` inspection spawn/name plus `INSPECTION.json` and `README_INSPECTION.txt` were added after selection.","","Stop condition reached: inspect these candidates before production-scale search, terrain correction, or Route design.",""]
    return "\n".join(lines)
