"""Actual-surface diagnostics and provisional, terrain-preserving topology."""
from __future__ import annotations
import base64
import json
import math
from collections import Counter
from html import escape

from .evaluate import _components, _distance, _route_analysis
from .render import _canvas, _png, SCALE

def enrich(c):
    rows=c['_rows']; h,w=len(rows),len(rows[0]); cells=sum(rows,[])
    land=[not a['actual_surface_water'] for a in cells]
    canopy=[a.get('canopy_overhead', 'leaves' in a['top_block'] or 'log' in a['top_block']) for a in cells]
    # Height differences are sampled surface roughness, not walkability proof.
    slopes=[max(abs(a['terrain_y']-rows[z][x]['terrain_y']) for x,z in ((max(0,i%w-1),i//w),(min(w-1,i%w+1),i//w),(i%w,max(0,i//w-1)),(i%w,min(h-1,i//w+1)))) for i,a in enumerate(cells)]
    open_mask=[land[i] and not canopy[i] and slopes[i]<=4 and any(v in a['ground_block'] for v in ('grass_block','dirt','sand','gravel')) for i,a in enumerate(cells)]
    highland=[land[i] and a['terrain_y']>=95 for i,a in enumerate(cells)]
    cold=[highland[i] and any(v in a['biome']+' '+a['top_block'] for v in ('snow','frozen','jagged','grove')) for i,a in enumerate(cells)]
    west=[i for i in range(len(cells)) if i%w<w//3]; east=[i for i in range(len(cells)) if i%w>=2*w//3]
    center=[i for i in range(len(cells)) if w//3<=i%w<2*w//3 and h//3<=i//w<2*h//3]
    def share(mask,indices): return sum(mask[i] for i in indices)/max(1,len(indices))
    ocean=[a['actual_surface_water'] and 'ocean' in a['biome'] for a in cells]
    forest=c['_masks']['forest']; open_components=_components(open_mask,w,h)
    dry_components=_components(land,w,h)
    west_high=[highland[i] and i%w<w//3 for i in range(len(cells))]
    heights=[a['terrain_y'] for i,a in enumerate(cells) if land[i]]
    high_depth=max(_distance([not v for v in west_high],w,h),default=0)*16
    m={'eastern_ocean_fraction':round(share(ocean,east),4),'western_ocean_fraction':round(share(ocean,west),4),'western_highland_fraction':round(share(highland,west),4),'western_highland_core_diameter_proxy_blocks':high_depth,'western_cold_highland_samples':sum(cold[i] for i in west),'actual_canopy_fraction':round(sum(canopy)/len(cells),4),'genuine_open_ground_fraction':round(sum(open_mask)/max(1,sum(land)),4),'largest_open_patch_blocks2':max(open_components,default=0)*64,'largest_dry_component_fraction_of_land':round(max(dry_components,default=0)/max(1,sum(land)),4),'center_open_fraction':round(share(open_mask,center),4),'forest_biome_fraction':round(sum(forest)/len(cells),4),'terrain_height_range': [min(heights),max(heights)],'sampled_surface_water_fraction':round(1-sum(land)/len(cells),4)}
    # Minimal absence checks; relative strength and construction needs rank later.
    failures=[]
    if not any(ocean[i] for i in east): failures.append('no_eastern_ocean')
    if share(ocean,east)<=share(ocean,west): failures.append('ocean_axis_reversed')
    if not any(west_high): failures.append('no_western_highland')
    if max(dry_components,default=0)<len(cells)*.25: failures.append('catastrophic_dry_land_fragmentation')
    if min(v['developable_fraction'] for v in c['homelands'].values())<.35: failures.append('no_comparable_development_sites')
    m['score']=round(25*(share(ocean,east)-share(ocean,west))+20*share(highland,west)+15*min(1,m['western_cold_highland_samples']/100)+25*m['genuine_open_ground_fraction']+10*m['center_open_fraction']+8*min(1,m['forest_biome_fraction']/.2)-100*len(failures),3)
    m['hard_failures']=failures
    m['limitations']=['8-block samples miss thin barriers and small clearings','open ground is sampled substrate and top-block evidence, not sightline measurement','highland core diameter uses Manhattan distance; internal mountain quality remains manual']
    c['stage_c']=m
    c['_masks'].update({'open_ground':open_mask,'canopy':canopy,'highland':highland})
    # Old score/screens are historical diagnostics and do not drive this funnel.
    c['experimental_screen']['status']='legacy diagnostic only; not Stage C acceptance'

def regions(c):
    rows=c['_rows'];h,w=len(rows),len(rows[0]);cells=sum(rows,[])
    kinds=[]
    for i,a in enumerate(cells):
        if a['actual_surface_water']: kind='ocean' if 'ocean' in a['biome'] else 'inland water'
        elif any(s in a['biome']+' '+a['top_block'] for s in ('snow','frozen','jagged')) and a['terrain_y']>=95: kind='alpine highland'
        elif c['_masks']['highland'][i]: kind='upland / mountain'
        elif c['_masks']['open_ground'][i]: kind='open country'
        elif c['_masks']['forest'][i]: kind='forest'
        elif 'beach' in a['biome'] or 'shore' in a['biome']: kind='coast'
        else: kind='transition'
        kinds.append(kind)
    output=[]
    for kind in sorted(set(kinds)):
        unseen={i for i,k in enumerate(kinds) if k==kind}; components=[]
        while unseen:
            start=unseen.pop(); stack=[start]; group=[start]
            while stack:
                i=stack.pop();x,z=i%w,i//w
                for xx,zz in ((x-1,z),(x+1,z),(x,z-1),(x,z+1)):
                    q=zz*w+xx
                    if 0<=xx<w and 0<=zz<h and q in unseen:
                        unseen.remove(q); stack.append(q);group.append(q)
            components.append(group)
        for n,group in enumerate(sorted(components,key=len,reverse=True)[:2]):
            if len(group)<12: continue
            cx=sum(i%w for i in group)/len(group);cz=sum(i//w for i in group)/len(group)
            i=min(group,key=lambda i:(i%w-cx)**2+(i//w-cz)**2);a=cells[i]
            neighbors=Counter()
            for j in group:
                x,z=j%w,j//w
                for xx,zz in ((x-1,z),(x+1,z),(x,z-1),(x,z+1)):
                    if 0<=xx<w and 0<=zz<h and kinds[zz*w+xx]!=kind: neighbors[kinds[zz*w+xx]]+=1
            output.append({'id':f'{kind}-{n+1}','role':kind,'area_blocks2':len(group)*64,'logical_sample':[i%w,i//w],'raw_world':[a['x'],a['z']],'neighbor_relationships':dict(neighbors),'evidence':'connected classified actual surface samples; regional interiors require manual inspection'})
    return output

def greybox(c):
    rows=c['_rows'];h,w=len(rows),len(rows[0]);cells=sum(rows,[])
    homes=tuple(tuple(c['homelands'][team]['logical_sample']) for team in ('north','south'))
    c['routes']=_route_analysis(rows,homes,c['_masks']['buildable'],uncapped_relief=True,flexible_anchors=True)
    nodes=[]; crossings=[]
    for b in c['routes']['branches']:
        path=b['sample_path']; risks=[];wet=[]
        for index,(x,z) in enumerate(path):
            a=rows[z][x]
            if a['actual_surface_water']: wet.append(index)
            if index:
                px,pz=path[index-1]
                if abs(a['terrain_y']-rows[pz][px]['terrain_y'])>8: risks.append(index)
        longest=run=0
        for index in range(len(path)):
            run=run+1 if index in wet else 0;longest=max(longest,run)
        b['construction_diagnostics']={'sample_edges_over_8_y':len(risks),'longest_water_run_samples':longest,'interpretation':'corridor band requiring field review; stairs/clearing/bridges may be needed, not a finished walkable road'}
        for rank,fraction in (('major',1),('minor',.55)):
            x,z=path[round((len(path)-1)*fraction)];a=rows[z][x]
            if rank=='major' and any(n['logical_sample']==[x,z] for n in nodes): continue
            nodes.append({'id':f"{b['id']}-{rank}",'rank':rank,'role':b['target_role'] if rank=='major' else 'staging / discovery opportunity','logical_sample':[x,z],'raw_world':[a['x'],a['z']],'route_relationship':b['id'],'status':'provisional authored opportunity; not a generated structure'})
        for kind,indices in (('water crossing',wet),('steep approach',risks)):
            # Keep distinct risk clusters, not hundreds of adjacent labels.
            last=-100
            for index in indices:
                if index-last<8: continue
                x,z=path[index];a=rows[z][x];last=index
                crossings.append({'type':kind,'route':b['id'],'logical_sample':[x,z],'raw_world':[a['x'],a['z']],'resolution_options':['detour','bridge / stairs','Minecraft-native off-Route solution'],'status':'sampled risk; not proven chokepoint'})
    middle=next(n for n in nodes if n['rank']=='major' and n['role']=='central_wilderness')
    home_details={}
    for team,home in c['homelands'].items():
        x,z=home['logical_sample']; footprint=[rows[zz][xx] for zz in range(z-4,z+5) for xx in range(x-4,x+5)]
        home_details[team]={**home,'prototype_footprint_blocks':[72,72],'raw_footprint_bounds':[min(a['x'] for a in footprint)-4,max(a['x'] for a in footprint)+3,min(a['z'] for a in footprint)-4,max(a['z'] for a in footprint)+3],'height_range':[min(a['terrain_y'] for a in footprint),max(a['terrain_y'] for a in footprint)],'canopy_sample_fraction':round(sum(a.get('canopy_overhead',False) for a in footprint)/81,3),'status':'development opportunity; Fountain/defenses/resources not placed'}
    midx,midz=middle['logical_sample']
    interior={'logical_sample':[midx,midz],'raw_world':middle['raw_world'],'radius_blocks':96,'role':'interior junction zone, not mandatory objective','three_endpoint_spread_blocks':round(math.dist(nodes[0]['raw_world'],[n for n in nodes if n['rank']=='major'][-1]['raw_world']))}
    def direction(a,b):
        dx=b['x']-a['x'];dz=b['z']-a['z']
        return ('+X' if dx>0 else '-X') if dx else ('+Z' if dz>0 else '-Z')
    axes={'East':direction(rows[0][0],rows[0][-1]),'West':direction(rows[0][-1],rows[0][0]),'South':direction(rows[0][0],rows[-1][0]),'North':direction(rows[-1][0],rows[0][0])}
    # Shared corridors are explicit rather than falsely called independent lanes.
    route_sets=[set(map(tuple,b['sample_path'][8:])) for b in c['routes']['branches']]
    overlaps=[]
    for a in range(6):
        for b in range(a+1,6):
            overlaps.append({'a':c['routes']['branches'][a]['id'],'b':c['routes']['branches'][b]['id'],'shared_sample_cells':len(route_sets[a]&route_sets[b])})
    c['greybox']={'status':'Prototype/test analytical fit; terrain unchanged','logical_axes_in_minecraft':axes,'playable_bounds':c['region']['block_bounds'],'homelands':home_details,'homeland_separation_blocks':round(math.dist(home_details['north']['raw_world'],home_details['south']['raw_world'])),'center':interior,'poi_nodes':nodes,'crossing_and_steep_approach_candidates':crossings,'landscape_regions':regions(c),'corridor_overlap':overlaps,'competitive_equivalence':'Unproven: terrain site fractions only; resource portfolios, practical travel and defenses remain review criteria','correction_burden':'Local access/vegetation/infrastructure expected. No macro terrain replacement proposed. Reject after manual review if corridor stairs, bridges or homeland grading require major repair.'}
    route_balance=[]
    for index in range(3):
        north=c['routes']['branches'][index];south=c['routes']['branches'][index+3]
        route_balance.append({'target':north['target_role'],'north_length':north['length_blocks'],'south_length':south['length_blocks'],'longer_over_shorter_ratio':round(max(north['length_blocks'],south['length_blocks'])/max(1,min(north['length_blocks'],south['length_blocks'])),3)})
    c['greybox']['paired_route_length_diagnostics']=route_balance
    m=c['stage_c'];weaknesses=[]
    water_run=max(b['construction_diagnostics']['longest_water_run_samples'] for b in c['routes']['branches'])
    if m['sampled_surface_water_fraction']>.45: weaknesses.append('Maritime area consumes almost half or more of the window, reducing dry Wilderness breadth')
    if m['genuine_open_ground_fraction']<.22: weaknesses.append('Open country is relatively scarce; inspect whether forest/alpine terrain leaves sufficient open connective space')
    if m['center_open_fraction']<.17: weaknesses.append('The interior is relatively enclosed; junction legibility depends on corridors and edges rather than a broad plain')
    if water_run>=8: weaknesses.append(f'The longest sampled water crossing spans about {water_run*8} blocks; consider a shore detour before proposing a bridge')
    worst_ratio=max(v['longer_over_shorter_ratio'] for v in route_balance)
    if worst_ratio>1.6: weaknesses.append(f'One paired regional corridor has a {worst_ratio:.2f}× length imbalance; endpoint or homeland placement needs review')
    if not weaknesses: weaknesses.append('Coarse ground samples cannot establish block-scale access, interior sightlines or homeland resource equivalence')
    c['finalist_assessment']={'survival_rationale':'Actual western cold highlands, eastern ocean, distinct open/forest geography and plausible separated development sites within the chosen bounds','principal_weakness':'; '.join(weaknesses),'correction_burden':'Elevated local access/topology review' if water_run>=8 or worst_ratio>1.6 else 'Modest local access/vegetation work provisionally plausible','macro_repair_proposed':False,'evidence':'automated actual-surface and analytical-fit assessment, subject to manual interior review'}

def presentation(c,d):
    """Self-contained labeled SVG so inspection never needs an altered world."""
    rows=c['_rows'];h,w=len(rows),len(rows[0]);scale=5;ox=80;oy=90
    def xy(p):return ox+(p[0]+.5)*scale,oy+(p[1]+.5)*scale
    def label(p,text,color='#fff',dy=-8):
        x,y=xy(p);return f'<text x="{x}" y="{y+dy}" fill="{color}" stroke="#17231e" stroke-width="3" paint-order="stroke" font-size="12">{escape(text)}</text>'
    def actual_cover(a,x,z):
        i=z*w+x
        if a['actual_surface_water']: return (40,90,170) if 'ocean' in a['biome'] else (55,137,197)
        if c['_masks']['canopy'][i]: return (35,95,51)
        if any(v in a['top_block'] for v in ('snow','ice')): return (231,240,243)
        if c['_masks']['open_ground'][i]: return (164,189,104)
        if any(v in a['ground_block'] for v in ('stone','andesite','granite','diorite')): return (143,144,142)
        return (107,140,81)
    _png(d/'05_actual_ground_canopy.png',w*SCALE,h*SCALE,_canvas(rows,actual_cover))
    encoded=base64.b64encode((d/'05_actual_ground_canopy.png').read_bytes()).decode()
    svg=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{w*scale+160}" height="{h*scale+210}" viewBox="0 0 {w*scale+160} {h*scale+210}">','<rect width="100%" height="100%" fill="#15221e"/>',f'<g font-family="sans-serif"><text x="24" y="30" fill="white" font-size="20">Vanilla {c["seed"]} · analytical competitive fit</text>',f'<text x="24" y="54" fill="#c8d4c9" font-size="12">{w*8} × {h*8} blocks · bounds {c["region"]["block_bounds"]} · no terrain edits</text>',f'<image x="{ox}" y="{oy}" width="{w*scale}" height="{h*scale}" href="data:image/png;base64,{encoded}"/>',f'<rect x="{ox}" y="{oy}" width="{w*scale}" height="{h*scale}" fill="none" stroke="white" stroke-width="3"/>']
    for side,p in [('North',[w//2,-3]),('South',[w//2,h+5]),('West',[-14,h//2]),('East',[w-11,h//2])]:
        svg.append(label(p,f'{side} ({c["greybox"]["logical_axes_in_minecraft"][side]})'))
    labeled_roles=set()
    for r in sorted(c['greybox']['landscape_regions'],key=lambda r:r['area_blocks2'],reverse=True):
        if r['area_blocks2']>=4000 and r['role'] not in labeled_roles:
            svg.append(label(r['logical_sample'],r['role'],'#e3e9cc',14));labeled_roles.add(r['role'])
    for b in c['routes']['branches']:
        points=' '.join(f'{x},{y}' for x,y in map(xy,b['sample_path']))
        color='#88c9ff' if b['team']=='north' else '#ffaaa1'
        svg.append(f'<polyline points="{points}" fill="none" stroke="{color}" stroke-width="4" stroke-opacity=".85"/>')
    for team,home in c['greybox']['homelands'].items():
        x,y=xy(home['logical_sample']);color='#3ba3ff' if team=='north' else '#ff6659'
        svg.append(f'<rect x="{x-22.5}" y="{y-22.5}" width="45" height="45" fill="none" stroke="{color}" stroke-width="4"/>');svg.append(label(home['logical_sample'],team.upper()+' HOME',color,-28))
    x,y=xy(c['greybox']['center']['logical_sample']);svg.append(f'<ellipse cx="{x}" cy="{y}" rx="60" ry="45" fill="none" stroke="#fff2a5" stroke-dasharray="7 5"/>');svg.append(label(c['greybox']['center']['logical_sample'],'INTERIOR','#fff2a5',-50))
    for i,node in enumerate(c['greybox']['poi_nodes'],1):
        x,y=xy(node['logical_sample']);svg.append(f'<circle cx="{x}" cy="{y}" r="{6 if node["rank"]=="major" else 3}" fill="#ffe080" stroke="#322d19"/>');svg.append(label(node['logical_sample'],f'P{i}','#fff2a5'))
    for crossing in c['greybox']['crossing_and_steep_approach_candidates']:
        x,y=xy(crossing['logical_sample']);svg.append(f'<path d="M{x-3},{y-3}l6,6m-6,0l6,-6" stroke="#ffedcc" stroke-width="1.5"/>')
    svg.extend([f'<text x="24" y="{h*scale+160}" fill="white" font-size="12">Blue/red: provisional corridors · P: POI opportunity · ×: crossing / steep-step review</text>',f'<text x="24" y="{h*scale+183}" fill="#c8d4c9" font-size="12">White rectangle: playable boundary · homeland boxes: 72 × 72 block test footprints</text>','</g></svg>'])
    (d/'04_labeled_greybox.svg').write_text('\n'.join(svg))
    m=c['stage_c'];g=c['greybox']
    lines=[f'# Finalist {c["seed"]}', '', '> Prototype/test: real vanilla substrate plus analytical fit, not a selected or balanced map.', '',f'World: `{c["local_world_directory"]}`. Java {c["minecraft_java_version"]}.',f'Bounds (inclusive xmin,xmax,zmin,zmax): `{c["region"]["block_bounds"]}`; source center `{c["region"]["source_center"]}`.',f'Logical axes: `{g["logical_axes_in_minecraft"]}`.', '', '![Labeled analytical fit](04_labeled_greybox.svg)', '', f'Survival rationale: eastern ocean {m["eastern_ocean_fraction"]:.1%}, western highland {m["western_highland_fraction"]:.1%}, {m["western_cold_highland_samples"]} western cold highland samples, open ground {m["genuine_open_ground_fraction"]:.1%}. These are actual chunk samples, not cheap biome predictions.', '',f'Macro composition and orientability: western highland core diameter proxy {m["western_highland_core_diameter_proxy_blocks"]} blocks; terrain Y {m["terrain_height_range"]}; canopy {m["actual_canopy_fraction"]:.1%}; largest dry component {m["largest_dry_component_fraction_of_land"]:.1%} of dry land. Snow/elevation and coast/water provide complementary W/E cues; homeland infrastructure supplies N/S identity later.', '',f'Center: {g["center"]["raw_world"]}, a provisional junction area. Three shared regional destinations span {g["center"]["three_endpoint_spread_blocks"]} blocks; this is a topology hypothesis, not evidence that the center must be a single objective.', '', 'Landscape and formation relationships (actual sampled adjacency):', '']
    for r in g['landscape_regions']:
        lines.append(f'- {r["id"]}: {r["area_blocks2"]:,} blocks² near {r["raw_world"]}; neighbors: {", ".join(r["neighbor_relationships"])}.')
    lines+=['','Homeland fit:','']
    for team,home in g['homelands'].items(): lines.append(f'- {team}: {home["raw_world"]}, footprint {home["raw_footprint_bounds"]}, developable {home["developable_fraction"]:.1%}, Y range {home["height_range"]}, canopy {home["canopy_sample_fraction"]:.1%}.')
    lines+=['',f'Homeland separation: {g["homeland_separation_blocks"]} blocks. {g["competitive_equivalence"]}.','','Route fit (six provisional corridor relationships, not finished roads):','']
    for b in c['routes']['branches']: lines.append(f'- {b["id"]} → {b["target_role"]}: {b["length_blocks"]} blocks, {b["water_steps"]} water samples, maximum 8-block sampled elevation step {b["max_step_y"]} Y; {b["construction_diagnostics"]["sample_edges_over_8_y"]} edges exceed 8 Y.')
    lines+=['','POI opportunities:','']
    for i,n in enumerate(g['poi_nodes'],1): lines.append(f'- P{i}: {n["rank"]}, {n["role"]}, {n["raw_world"]}; {n["route_relationship"]}.')
    lines+=['',f'Principal weakness: {c["finalist_assessment"]["principal_weakness"]}. Largest open patch: {m["largest_open_patch_blocks2"]:,} blocks².', '',f'Correction burden: {c["finalist_assessment"]["correction_burden"]}. {g["correction_burden"]}', '', 'Paired regional Route lengths (diagnostic, not fairness scores):','']
    for pair in g['paired_route_length_diagnostics']: lines.append(f'- {pair["target"]}: N {pair["north_length"]} / S {pair["south_length"]} blocks; ratio {pair["longer_over_shorter_ratio"]}.')
    lines+=['', 'Manual criteria: interior regional legibility and sightlines; mountain passes/valleys and exposed caves; crossing alternatives; homeland usable floor area and access; practical two-team Route equivalence; expedition travel. Structure starts and underground palettes are recorded separately and are not POI placements.', '', 'Open the clean world in Minecraft Java 1.21.11; use `INSPECTION.json` for spawn and raw coordinate navigation. The labeled SVG defines logical north at the top and the complete intended boundary. Adjacent generated chunks are outside the evaluated playable area.', '']
    (d/'REVIEW.md').write_text('\n'.join(lines))
