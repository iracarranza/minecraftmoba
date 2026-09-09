import os, math, json, base64, io, gc, hashlib, time, argparse
from dataclasses import dataclass
from pathlib import Path
import numpy as np
from scipy.ndimage import gaussian_filter, distance_transform_edt, label, binary_dilation
from PIL import Image, ImageDraw, ImageFont
from numba import njit, prange

# -----------------------------------------------------------------------------
# WORLD / DISPLAY CONSTANTS
# -----------------------------------------------------------------------------
X0,X1=-336,335
Z0,Z1=-416,415
Y0,Y1=48,111
NX=X1-X0+1
NZ=Z1-Z0+1
NY=Y1-Y0+1
SEA_LEVEL=64

# Java 1.12 numeric IDs used by the greybox family.
AIR=0; STONE=1; GRASS=2; DIRT=3; COBBLE=4; WATER=9; LAVA=11; SAND=12; GRAVEL=13
GOLD_ORE=14; IRON_ORE=15; COAL_ORE=16; LOG=17; LEAVES=18; MUSHROOM_BROWN=39; MUSHROOM_RED=40
DIAMOND_ORE=56; CLAY=82; EMERALD_ORE=129; CONCRETE=251

# Material map for 2D generation.
M_GRASS=1; M_STONE=2; M_SAND=3; M_GRAVEL=4; M_WATER=5; M_LAVA=6; M_ROUTE=7; M_HOME_N=8; M_HOME_S=9
M_VILLAGE=10; M_POI=11; M_MINOR=12; M_FOREST=13

TOPOLOGIES=['twin_massif','split_spine','horseshoe','broken_peaks','long_ridge','highland_ravines','offset_basin','sawtooth_valleys']

# -----------------------------------------------------------------------------
# UTILS
# -----------------------------------------------------------------------------
def xi(x): return int(x-X0)
def zi(z): return int(z-Z0)
def wx(ix): return int(ix+X0)
def wz(iz): return int(iz+Z0)

def norm01(a):
    lo=float(a.min()); hi=float(a.max())
    return (a-lo)/(hi-lo+1e-9)

def smooth_noise(rng, sigma, amp=1.0):
    a=rng.normal(size=(NZ,NX)).astype(np.float32)
    a=gaussian_filter(a,sigma=sigma,mode='reflect')
    a=(a-a.mean())/(a.std()+1e-6)
    return a*amp

def disk_mask(cx,cz,r):
    yy,xx=np.ogrid[:NZ,:NX]
    return (xx-xi(cx))**2+(yy-zi(cz))**2 <= r*r

def draw_polyline_mask(points,width=3):
    mask=np.zeros((NZ,NX),bool)
    for (x0,z0),(x1,z1) in zip(points[:-1],points[1:]):
        n=max(abs(x1-x0),abs(z1-z0))*2+1
        xs=np.linspace(x0,x1,n); zs=np.linspace(z0,z1,n)
        for x,z in zip(xs,zs):
            ix0=max(0,int(round(x))-X0-width); ix1=min(NX,int(round(x))-X0+width+1)
            iz0=max(0,int(round(z))-Z0-width); iz1=min(NZ,int(round(z))-Z0+width+1)
            if ix0>=ix1 or iz0>=iz1: continue
            y,xg=np.ogrid[iz0:iz1,ix0:ix1]
            mask[iz0:iz1,ix0:ix1] |= (xg-(x-X0))**2+(y-(z-Z0))**2 <= width*width
    return mask

def polyline(points,steps=180):
    out=[]
    for a,b in zip(points[:-1],points[1:]):
        x0,z0=a; x1,z1=b
        n=max(2,int(math.hypot(x1-x0,z1-z0)/2))
        out += [(float(x),float(z)) for x,z in zip(np.linspace(x0,x1,n,endpoint=False),np.linspace(z0,z1,n,endpoint=False))]
    out.append(tuple(map(float,points[-1])))
    return out

def point_to_mask_distance(mask, point):
    z,x=point[1]-Z0,point[0]-X0
    dist=distance_transform_edt(~mask)
    if 0<=z<NZ and 0<=x<NX: return float(dist[int(z),int(x)])
    return 999.0

# -----------------------------------------------------------------------------
# GENERATION
# -----------------------------------------------------------------------------
def build_routes(rng):
    # Homelands are symmetric. Curvature varies seed-to-seed but south mirrors north.
    ends=[(-170+rng.integers(-22,23),-72+rng.integers(-16,17)),
          (rng.integers(-35,36),-48+rng.integers(-15,16)),
          (160+rng.integers(-25,26),-76+rng.integers(-18,19))]
    start=(0,-292); junction=(rng.integers(-18,19),-244+rng.integers(-8,9))
    north=[]
    for bx,(ex,ez) in enumerate(ends):
        bendz=-165+rng.integers(-24,25)
        midx=int(round((junction[0]+ex)*.48+rng.integers(-26,27)))
        pts=[start,junction,(midx,bendz),(int(ex),int(ez))]
        north.append(pts)
    south=[[(x,-z) for x,z in pts] for pts in north]
    mask=np.zeros((NZ,NX),bool)
    centerlines=[]
    for pts in north+south:
        mask |= draw_polyline_mask(pts,width=3)
        centerlines.append(polyline(pts))
    return mask,north,south,centerlines,ends

def mountain_component(rng, topology):
    zgrid,xgrid=np.mgrid[Z0:Z1+1,X0:X1+1]
    # Wavy eastern boundary; topography begins as foothills around -135..-175.
    coastnoise=gaussian_filter(rng.normal(size=NZ),sigma=28)
    coastnoise=(coastnoise-coastnoise.mean())/(coastnoise.std()+1e-6)
    east_boundary=-150 + 14*np.sin((zgrid[:,0]+rng.uniform(-80,80))/95) + 7*coastnoise
    east_boundary=east_boundary[:,None]
    westness=np.clip((east_boundary-xgrid)/170.0,0,1)
    mask=(westness>0) & (np.abs(zgrid)<285+rng.integers(-20,21))
    base=(5*westness + 22*westness**1.55).astype(np.float32)
    comp=base.copy()
    def G(cx,cz,sx,sz,a):
        return a*np.exp(-(((xgrid-cx)/sx)**2+((zgrid-cz)/sz)**2))
    # Each family produces materially different internal topology.
    if topology=='twin_massif':
        comp += G(-270,-135,52,72,16)+G(-270,115,58,75,14)-G(-245,-5,70,42,9)
    elif topology=='split_spine':
        comp += G(-255,-10,42,235,13)+G(-295,-180,38,58,10)-G(-255,55,42,70,11)
    elif topology=='horseshoe':
        comp += G(-280,-105,58,58,13)+G(-300,105,58,60,14)+G(-310,5,42,125,9)-G(-270,8,58,66,12)
    elif topology=='broken_peaks':
        for cx,cz,a in [(-240,-190,13),(-294,-115,17),(-245,-25,11),(-300,75,16),(-235,170,13)]:
            comp += G(cx+rng.integers(-8,9),cz+rng.integers(-12,13),35+rng.integers(0,14),40+rng.integers(0,18),a)
        comp -= G(-255,40,65,38,8)
    elif topology=='long_ridge':
        comp += G(-278,0,42,240,16)
        for cz in [-150,-20,125]: comp -= G(-255,cz+rng.integers(-15,16),58,30,8)
    elif topology=='highland_ravines':
        comp += G(-278,0,90,225,12)+G(-315,-80,45,95,9)
        # several sinuous ravines
        for phase in [rng.uniform(-2,2),rng.uniform(-2,2)]:
            center=-15+80*np.sin((xgrid+phase*20)/70)+phase*22
            comp -= 7*np.exp(-((zgrid-center)/18)**2)*np.clip(westness*.9,0,1)
    elif topology=='offset_basin':
        comp += G(-285,-65,78,150,15)+G(-245,145,48,70,12)-G(-286,-45,43,55,14)
    elif topology=='sawtooth_valleys':
        comp += G(-285,0,60,230,13)
        for cz in [-190,-95,5,105,190]:
            comp -= G(-250,cz+rng.integers(-10,11),62,24,7+rng.uniform(0,3))
            comp += G(-305,cz+42,38,30,6)
    # Natural roughness grows with commitment but stays smoothed enough to avoid walls.
    rough=smooth_noise(rng,8,1.4)+smooth_noise(rng,22,2.1)
    comp += rough*westness*(.4+westness)
    comp=np.where(mask,comp,0)
    # soften only visible component boundaries; keeps macro topology but removes math seams
    comp=gaussian_filter(comp,sigma=2.2)
    comp=np.where(mask,comp,0)
    return comp,mask,westness,east_boundary[:,0]

def carve_entrances(rng, h, mountain_mask, westness, count):
    zgrid,xgrid=np.mgrid[Z0:Z1+1,X0:X1+1]
    zs=np.linspace(-190,175,count)+rng.integers(-28,29,size=count)
    entrances=[]
    for j,cz in enumerate(zs):
        # Winding corridor from foothill edge into interior; broad enough to be a region route.
        phase=rng.uniform(-1,1)
        pathz=cz + 17*np.sin((xgrid+250)/50+phase)
        width=rng.uniform(17,27)
        corridor=np.exp(-((zgrid-pathz)/width)**2)*np.clip((westness-.03)/.65,0,1)
        depth=rng.uniform(5.5,9.5)
        h -= depth*corridor
        entrances.append((int(-180),int(round(cz))))
    return h,entrances

def place_lake(rng,h,allowed,kind='water'):
    # Hydrology: choose a low point then fill every connected neighbor at/below a spill
    # level inside a local window. This intentionally expands into adjacent low blocks.
    ys,xs=np.where(allowed)
    if len(xs)==0: return np.zeros_like(allowed),None,None
    for _ in range(60):
        i=int(rng.integers(0,len(xs))); cz=int(ys[i]); cx=int(xs[i])
        radius=int(rng.integers(10,24) if kind=='water' else rng.integers(7,16))
        z0=max(0,cz-radius); z1=min(NZ,cz+radius+1); x0=max(0,cx-radius); x1=min(NX,cx+radius+1)
        sub=h[z0:z1,x0:x1]
        if sub.size<80: continue
        local_min=float(h[cz,cx])
        level=int(math.floor(local_min+rng.uniform(1.4,3.4)))
        eligible=(sub<level) & allowed[z0:z1,x0:x1]
        # connected component containing center
        lab,n=label(eligible)
        l=lab[cz-z0,cx-x0]
        if l==0: continue
        comp=(lab==l)
        ncell=int(comp.sum())
        if 30<=ncell<=900:
            mask=np.zeros_like(allowed); mask[z0:z1,x0:x1]=comp
            return mask,level,(wx(cx),wz(cz))
    return np.zeros_like(allowed),None,None

def choose_positions(rng, valid, n, mindist, avoid=None):
    if avoid is None: avoid=[]
    ys,xs=np.where(valid)
    if len(xs)==0: return []
    order=rng.permutation(len(xs))
    out=[]
    for i in order:
        x,z=wx(xs[i]),wz(ys[i])
        if all(math.hypot(x-a,z-b)>=mindist for a,b in out+avoid):
            out.append((x,z))
            if len(out)>=n: break
    return out

def generate_candidate(seed):
    rng=np.random.default_rng(seed)
    topology=TOPOLOGIES[int(seed)%len(TOPOLOGIES)]
    zgrid,xgrid=np.mgrid[Z0:Z1+1,X0:X1+1]
    # Ordinary Overworld base
    h=64.0 + smooth_noise(rng,14,1.6)+smooth_noise(rng,42,1.5)
    # broad positive/negative wilderness events
    for _ in range(int(rng.integers(8,14))):
        cx=rng.uniform(-130,170); cz=rng.uniform(-235,235); amp=rng.uniform(-5.5,6.5)
        sx=rng.uniform(18,55); sz=rng.uniform(18,55)
        h += amp*np.exp(-(((xgrid-cx)/sx)**2+((zgrid-cz)/sz)**2))
    route_mask,north_routes,south_routes,centerlines,route_ends=build_routes(rng)
    mcomp,mountain_mask,westness,mountain_edge=mountain_component(rng,topology)
    h += mcomp
    entrance_count=int(rng.integers(2,5))
    h,entrances=carve_entrances(rng,h,mountain_mask,westness,entrance_count)
    # East coastline/ocean: variable macro-shape, not a fixed vertical strip.
    coast_noise=gaussian_filter(rng.normal(size=NZ),sigma=35)
    coast_noise=(coast_noise-coast_noise.mean())/(coast_noise.std()+1e-6)
    coast_x=205 + rng.integers(-18,19) + 24*np.sin((np.arange(NZ)+rng.uniform(-100,100))/120)+12*coast_noise
    ocean=np.zeros((NZ,NX),bool)
    for iz in range(NZ): ocean[iz, np.arange(NX)+X0 > coast_x[iz]]=True
    # lower seabed and beaches; smooth transition
    h[ocean]=np.minimum(h[ocean], SEA_LEVEL-rng.uniform(4,8))
    coastdist=distance_transform_edt(~ocean)
    beach=(~ocean)&(coastdist<7)&(h<=SEA_LEVEL+3)
    # Homes: mirrored, protected baseline flats.
    home_n=((xgrid/54)**2+((zgrid+330)/52)**2<1)
    home_s=((xgrid/54)**2+((zgrid-330)/52)**2<1)
    h[home_n|home_s]=64
    # Authored routes: carve/raise a reliable corridor to a smoothed reference surface.
    href=gaussian_filter(h,sigma=5)
    h[route_mask]=np.round(href[route_mask])
    # Local transition apron prevents route retaining walls.
    apron=binary_dilation(route_mask,iterations=4)&~route_mask
    blend=gaussian_filter(route_mask.astype(float),sigma=2.0)
    h[apron]=h[apron]*(1-.45*blend[apron])+href[apron]*(.45*blend[apron])
    h=np.clip(np.rint(h),54,109).astype(np.int16)
    # Material regions
    material=np.full((NZ,NX),M_GRASS,np.uint8)
    material[(mountain_mask)&(westness>.18)]=M_STONE
    gravel_noise=smooth_noise(rng,10,1)
    material[mountain_mask&(westness>.10)&(gravel_noise>.75)]=M_GRAVEL
    material[beach]=M_SAND
    material[ocean]=M_WATER
    # Forest footprint uses regional moisture and avoids high mountain / ocean / routes.
    moisture=smooth_noise(rng,24,1)+.55*smooth_noise(rng,8,1)
    # vary forest's broad center to make ordinary wilderness layouts dissimilar
    fcx=rng.uniform(-70,135); fcz=rng.uniform(-165,165)
    moisture += 1.3*np.exp(-(((xgrid-fcx)/rng.uniform(70,130))**2+((zgrid-fcz)/rng.uniform(90,170))**2))
    forest=(moisture>np.quantile(moisture[~ocean],rng.uniform(.74,.82))) & (~ocean) & (~route_mask) & (westness<.45) & (~home_n) & (~home_s)
    # clean tiny speckles
    lab,n=label(forest)
    counts=np.bincount(lab.ravel())
    forest=np.isin(lab,np.where(counts>140)[0]) & (lab>0)
    # Lakes / lava follow connected low terrain.
    water_allowed=(~ocean)&(~route_mask)&(~home_n)&(~home_s)&(h<76)
    lakes=[]; lava=[]
    for _ in range(int(rng.integers(2,4))):
        lm,lev,ctr=place_lake(rng,h,water_allowed&~binary_dilation(forest,iterations=2),'water')
        if lev is not None:
            lakes.append((lm,lev,ctr)); water_allowed &= ~binary_dilation(lm,iterations=8)
    lava_allowed=mountain_mask&(westness>.3)&(~route_mask)&(h>68)&(h<98)
    for _ in range(int(rng.integers(1,3))):
        lm,lev,ctr=place_lake(rng,h,lava_allowed,'lava')
        if lev is not None:
            lava.append((lm,lev,ctr)); lava_allowed &= ~binary_dilation(lm,iterations=10)
    # POIs/villages. Ensure spread and no literal mirroring.
    valid_poi=(~ocean)&(~mountain_mask | (westness<.55))&(~home_n)&(~home_s)&(~route_mask)&(np.abs(zgrid)<270)
    villages=choose_positions(rng,valid_poi,int(rng.integers(3,6)),110)
    if len(villages)<3:
        villages=choose_positions(rng,(~ocean)&(~home_n)&(~home_s)&(np.abs(zgrid)<280),3,90)
    major=choose_positions(rng,valid_poi,int(rng.integers(3,6)),95,avoid=villages)
    minor=choose_positions(rng,valid_poi,int(rng.integers(3,7)),65,avoid=villages+major)
    # Resource subregions in mountain, represented by cluster centers and value profiles.
    deep=mountain_mask&(westness>.38)&(h>72)
    resource_centers=choose_positions(rng,deep,int(rng.integers(5,8)),55)
    resource_types=['iron_exposure','coal_shelf','gold_fault','emerald_highland','deep_cave','stone_quarry','rare_ore']
    resources=[]
    for i,p in enumerate(resource_centers):
        resources.append({'pos':p,'type':resource_types[(i+int(seed))%len(resource_types)],'value':round(float(rng.uniform(.65,1.0)),2)})
    # Landmarks for legibility.
    high=mout=mountain_mask&(h>=np.percentile(h[mountain_mask],82))
    landmark_positions=choose_positions(rng,high,int(rng.integers(4,7)),50)
    landmarks=[{'pos':p,'kind':rng.choice(['peak','cliff','saddle','stone_needle','high_shelf','ravine_head']).item()} for p in landmark_positions]
    return {
        'seed':int(seed),'topology':topology,'h':h,'material':material,'ocean':ocean,'beach':beach,
        'mountain_mask':mountain_mask,'westness':westness,'route_mask':route_mask,'north_routes':north_routes,'south_routes':south_routes,
        'centerlines':centerlines,'route_ends':route_ends,'home_n':home_n,'home_s':home_s,'forest':forest,
        'lakes':lakes,'lava':lava,'villages':villages,'major':major,'minor':minor,'resources':resources,'landmarks':landmarks,
        'entrances':entrances,'entrance_count':entrance_count,'forest_center':(int(round(fcx)),int(round(fcz)))
    }

# -----------------------------------------------------------------------------
# VALIDATION
# -----------------------------------------------------------------------------
def local_step_stats(h,mask):
    vals=[]
    for dz,dx in [(0,1),(1,0)]:
        a=h[max(0,dz):NZ, max(0,dx):NX]
        b=h[:NZ-dz if dz else NZ, :NX-dx if dx else NX]
        m=mask[max(0,dz):NZ,max(0,dx):NX]&mask[:NZ-dz if dz else NZ,:NX-dx if dx else NX]
        if m.any(): vals.append(np.abs(a-b)[m])
    if not vals: return 0,0,0
    v=np.concatenate(vals)
    return float(np.percentile(v,95)),int((v>8).sum()),int((v>12).sum())

def validation_metrics(v):
    h=v['h']; mountain=v['mountain_mask']; route=v['route_mask']; forest=v['forest']; ocean=v['ocean']
    free=(~ocean)&(~route)&(~v['home_n'])&(~v['home_s'])
    lab,n=label(free); counts=np.bincount(lab.ravel()); largest=int(counts[1:].max()) if len(counts)>1 else 0
    mdepth=float(distance_transform_edt(mountain).max())
    fdepth=float(distance_transform_edt(forest).max()) if forest.any() else 0
    p95,big,huge=local_step_stats(h,mountain)
    # route roughness only on route-neighbor pairs
    rp95,rbig,rhuge=local_step_stats(h,route)
    # mountain east vs deep elevation differential = intentionality proxy
    west=v['westness']
    approach=mountain&(west>.04)&(west<.18); deep=mountain&(west>.62)
    approach_y=float(np.median(h[approach])) if approach.any() else 64
    deep_y=float(np.median(h[deep])) if deep.any() else 64
    # Resource logistics: mean Euclidean distance to route.
    rdist=distance_transform_edt(~route)
    rd=[]
    for r in v['resources']:
        x,z=r['pos']; rd.append(float(rdist[zi(z),xi(x)]))
    # Village spacing
    vd=[]
    for i,a in enumerate(v['villages']):
        for b in v['villages'][i+1:]: vd.append(math.hypot(a[0]-b[0],a[1]-b[1]))
    # mountain internally walkable network: cells participating in at least one <=1 step adjacency
    walk=np.zeros_like(mountain)
    for dz,dx in [(0,1),(0,-1),(1,0),(-1,0)]:
        hh=np.roll(h,(dz,dx),(0,1)); mm=np.roll(mountain,(dz,dx),(0,1))
        walk |= mountain&mm&(np.abs(h-hh)<=1)
    wl,nw=label(walk); wc=np.bincount(wl.ravel()); largest_walk=int(wc[1:].max()) if len(wc)>1 else 0
    return {
        'villages':len(v['villages']),'min_village_spacing':round(min(vd) if vd else 0,1),
        'free_wilderness_blocks':int(free.sum()),'largest_free_component':largest,
        'mountain_blocks':int(mountain.sum()),'mountain_depth':round(mdepth,1),
        'mountain_y_min':int(h[mountain].min()),'mountain_y_max':int(h[mountain].max()),
        'mountain_vertical_range':int(h[mountain].max()-h[mountain].min()),
        'mountain_p95_step':round(p95,1),'mountain_edges_gt8':big,'mountain_edges_gt12':huge,
        'mountain_walk_component':largest_walk,'entrances':len(v['entrances']),'landmarks':len(v['landmarks']),
        'resource_regions':len(v['resources']),'mean_resource_route_distance':round(float(np.mean(rd)) if rd else 0,1),
        'forest_blocks':int(forest.sum()),'forest_depth':round(fdepth,1),
        'route_blocks':int(route.sum()),'route_p95_step':round(rp95,1),'route_edges_gt8':rbig,
        'approach_median_y':round(approach_y,1),'deep_median_y':round(deep_y,1),'commitment_gain_y':round(deep_y-approach_y,1),
        'water_features':len(v['lakes']),'lava_features':len(v['lava'])
    }

def validate(v):
    m=validation_metrics(v)
    checks={
      'three_to_five_villages': 3<=m['villages']<=5,
      'villages_distributed': m['min_village_spacing']>=90,
      'large_connected_wilderness': m['largest_free_component']>=250000,
      'mountain_is_region': m['mountain_blocks']>=65000 and m['mountain_depth']>=45,
      'mountain_has_vertical_commitment': m['mountain_vertical_range']>=34 and m['commitment_gain_y']>=16,
      'mountain_not_heightmap_wall': m['mountain_edges_gt12']<=20 and m['mountain_edges_gt8']<=800,
      'mountain_internal_network': m['mountain_walk_component']>=18000,
      'multiple_mountain_approaches': m['entrances']>=2,
      'legible_landmarks': m['landmarks']>=4,
      'distinct_resource_regions': m['resource_regions']>=5 and m['mean_resource_route_distance']>=55,
      'forest_has_interior': m['forest_blocks']>=9000 and m['forest_depth']>=12,
      'routes_are_reliable': m['route_p95_step']<=2 and m['route_edges_gt8']==0,
      'surface_hydrology_exists': m['water_features']>=1,
    }
    return all(checks.values()),m,checks

def diversity_vector(v,m):
    # Normalized strategic/topological fingerprint, not merely raw RNG params.
    vc=v['villages']; rc=v['resources']; lm=v['landmarks']
    def centroid(points):
        if not points:return (0,0)
        return (np.mean([p[0] if isinstance(p,tuple) else p['pos'][0] for p in points]),np.mean([p[1] if isinstance(p,tuple) else p['pos'][1] for p in points]))
    vx,vz=centroid(vc); rx,rz=centroid(rc); lx,lz=centroid(lm)
    topo=TOPOLOGIES.index(v['topology'])
    return np.array([topo/7,m['mountain_blocks']/110000,m['mountain_depth']/110,m['mountain_vertical_range']/60,
                     m['forest_blocks']/80000,v['forest_center'][0]/250,v['forest_center'][1]/250,
                     vx/300,vz/350,rx/300,rz/300,lx/300,lz/300,len(v['lakes'])/4,len(v['lava'])/3,
                     len(v['entrances'])/4],dtype=float)

def diverse_enough(vec, accepted, topology):
    if not accepted: return True,999
    all_ds=[float(np.linalg.norm(vec-a['diversity_vec'])) for a in accepted]
    same_ds=[float(np.linalg.norm(vec-a['diversity_vec'])) for a in accepted if a['topology']==topology]
    md=min(all_ds)
    same=min(same_ds) if same_ds else 999.0
    # Reject visually/strategically near-neighbors even when both satisfy the contract.
    # Same-family variants face a stronger threshold so each family explores its own range.
    return (md>=0.48 and same>=0.70),md

# -----------------------------------------------------------------------------
# VOXELIZATION
# -----------------------------------------------------------------------------
def voxelize(v,rng):
    h=v['h']; mat=v['material']; ids=np.zeros((NY,NZ,NX),np.uint8); meta=np.zeros_like(ids)
    ys=np.arange(Y0,Y1+1,dtype=np.int16)[:,None,None]
    solid=ys<=h[None,:,:]
    ids[solid]=STONE
    # Dirt/grass cap on non-rock, non-beach, non-ocean.
    grasslike=(mat==M_GRASS)|(v['forest'])|(v['home_n'])|(v['home_s'])
    for d in range(3):
        yy=h-d; valid=grasslike&(yy>=Y0)&(yy<=Y1)
        zz,xx=np.where(valid); ids[(yy[valid]-Y0,zz,xx)]=DIRT
    valid=grasslike&(h>=Y0)&(h<=Y1); zz,xx=np.where(valid); ids[(h[valid]-Y0,zz,xx)]=GRASS
    for m,bid in [(M_GRAVEL,GRAVEL),(M_SAND,SAND),(M_STONE,STONE)]:
        valid=(mat==m)&(~v['ocean'])&(h>=Y0)&(h<=Y1); zz,xx=np.where(valid); ids[(h[valid]-Y0,zz,xx)]=bid
    # Ocean water fills to sea level over seabed.
    for y in range(Y0,SEA_LEVEL+1):
        valid=v['ocean']&(h<y)
        ids[y-Y0][valid]=WATER
    # Lakes/lava fill physically connected footprints to their spill levels.
    for feats,bid in [(v['lakes'],WATER),(v['lava'],LAVA)]:
        for mask,level,ctr in feats:
            for y in range(Y0,min(Y1,level)+1):
                valid=mask&(h<y)
                ids[y-Y0][valid]=bid
    # Routes rendered on top and clear 2-block player volume above.
    route=v['route_mask']; zz,xx=np.where(route); yh=h[route]
    ids[(yh-Y0,zz,xx)]=CONCRETE; meta[(yh-Y0,zz,xx)]=4
    for off in [1,2,3]:
        y2=yh+off; ok=y2<=Y1
        ids[(y2[ok]-Y0,zz[ok],xx[ok])]=AIR
    # Homes as blue/red concrete top surfaces.
    for mask,color in [(v['home_n'],11),(v['home_s'],14)]:
        zz,xx=np.where(mask); yh=h[mask]; ids[(yh-Y0,zz,xx)]=CONCRETE; meta[(yh-Y0,zz,xx)]=color
        for off in [1,2]:
            y2=yh+off; ok=y2<=Y1; ids[(y2[ok]-Y0,zz[ok],xx[ok])]=AIR
    # POI markers are compact plazas, not giant cubes.
    for points,color,r in [(v['villages'],1,5),(v['major'],10,4),(v['minor'],5,3)]:
        for x,z in points:
            mask=disk_mask(x,z,r)&(~v['ocean']); zz,xx=np.where(mask); yh=h[mask]
            ids[(yh-Y0,zz,xx)]=CONCRETE; meta[(yh-Y0,zz,xx)]=color
            for off in [1,2]:
                y2=yh+off; ok=y2<=Y1; ids[(y2[ok]-Y0,zz[ok],xx[ok])]=AIR
    # Forest: irregular Poisson-ish tree placement using randomized candidate order.
    f=v['forest']; ys0,xs0=np.where(f)
    occupied=np.zeros_like(f,bool); tree_count=0
    if len(xs0):
        for idx in rng.permutation(len(xs0)):
            iz0=int(ys0[idx]); ix0=int(xs0[idx]); x=wx(ix0); z=wz(iz0)
            if occupied[max(0,iz0-3):min(NZ,iz0+4),max(0,ix0-3):min(NX,ix0+4)].any(): continue
            if rng.random()>.23: continue
            gy=int(h[iz0,ix0]); ht=int(rng.integers(4,8));
            if gy+ht+2>Y1: continue
            # ensure trunk base on soil and free route margin
            if v['route_mask'][iz0,ix0]: continue
            for y in range(gy+1,gy+ht+1): ids[y-Y0,iz0,ix0]=LOG
            crown=gy+ht
            for yy in range(crown-2,crown+2):
                rr=2 if yy<=crown else 1
                for dz in range(-rr,rr+1):
                    for dx in range(-rr,rr+1):
                        iz2=iz0+dz; ix2=ix0+dx
                        if 0<=iz2<NZ and 0<=ix2<NX and abs(dx)+abs(dz)<=rr+1 and rng.random()>.1:
                            if ids[yy-Y0,iz2,ix2]==AIR: ids[yy-Y0,iz2,ix2]=LEAVES
            occupied[iz0,ix0]=True; tree_count+=1
            if tree_count>500: break
    # Mountain resources: small exposed/internal patches around metadata centers.
    oreids={'iron_exposure':IRON_ORE,'coal_shelf':COAL_ORE,'gold_fault':GOLD_ORE,'emerald_highland':EMERALD_ORE,'deep_cave':COAL_ORE,'stone_quarry':IRON_ORE,'rare_ore':DIAMOND_ORE}
    for r in v['resources']:
        x,z=r['pos']; iz0=zi(z); ix0=xi(x); gy=int(h[iz0,ix0]); bid=oreids[r['type']]
        for _ in range(18):
            dx=int(rng.integers(-5,6)); dz=int(rng.integers(-5,6)); dy=int(rng.integers(-9,1))
            ix2=ix0+dx; iz2=iz0+dz; y=gy+dy
            if 0<=ix2<NX and 0<=iz2<NZ and Y0<=y<=Y1 and ids[y-Y0,iz2,ix2]==STONE:
                ids[y-Y0,iz2,ix2]=bid
    return ids,meta,tree_count

# -----------------------------------------------------------------------------
# RENDERER — corrected player standing position + progressive visual passes
# -----------------------------------------------------------------------------
BASE=np.zeros((256,3),np.uint8)
BASE[STONE]=(112,112,112); BASE[GRASS]=(91,145,58); BASE[DIRT]=(127,91,57); BASE[COBBLE]=(104,104,104)
BASE[WATER]=(55,103,190); BASE[LAVA]=(236,89,17); BASE[SAND]=(218,205,158); BASE[GRAVEL]=(128,124,119)
BASE[LOG]=(105,81,50); BASE[LEAVES]=(54,108,43); BASE[MUSHROOM_BROWN]=(126,83,45); BASE[MUSHROOM_RED]=(175,48,43)
BASE[COAL_ORE]=(74,74,74); BASE[IRON_ORE]=(178,139,111); BASE[GOLD_ORE]=(220,184,50); BASE[DIAMOND_ORE]=(60,197,200); BASE[EMERALD_ORE]=(46,181,94)
BASE[CLAY]=(158,164,176)
CONCRETE_RGB=np.array([(244,244,244),(235,128,38),(184,62,174),(79,168,211),(238,170,17),(87,163,22),(210,94,137),(53,56,60),(121,121,111),(18,133,141),(95,29,151),(42,45,139),(91,55,29),(69,87,33),(136,29,29),(8,10,15)],np.uint8)

@njit
def ihash(x,y,z,k=0):
    n=(x*374761393+y*668265263+z*2147483647+k*1274126177)&0xffffffff
    n=(n^(n>>13))*1274126177 & 0xffffffff
    return n^(n>>16)

@njit
def block_at(ids,x,y,z):
    if x<X0 or x>X1 or y<Y0 or y>Y1 or z<Z0 or z>Z1:return 0
    return ids[y-Y0,z-Z0,x-X0]

@njit
def texcolor(ids,meta,x,y,z,nx,ny,nz,hx,hy,hz,version):
    bid=block_at(ids,x,y,z)
    if bid==CONCRETE:
        m=meta[y-Y0,z-Z0,x-X0]&15; return float(CONCRETE_RGB[m,0]),float(CONCRETE_RGB[m,1]),float(CONCRETE_RGB[m,2])
    r=float(BASE[bid,0]); g=float(BASE[bid,1]); b=float(BASE[bid,2])
    # 16x16 face coordinate and deterministic block-specific texel hash.
    fx=hx-math.floor(hx); fy=hy-math.floor(hy); fz=hz-math.floor(hz)
    if ny!=0: u=int(fx*16); vv=int(fz*16)
    elif nx!=0: u=int(fz*16); vv=int(fy*16)
    else: u=int(fx*16); vv=int(fy*16)
    h=ihash(x*17+u,y*19+vv,z*23,bid)
    grain=((h&255)/255.0-.5)
    mod=1.0
    if bid in (STONE,DIRT,SAND,GRAVEL,COBBLE): mod=1.0+grain*(.18 if bid==GRAVEL else .11)
    elif bid==GRASS:
        if ny==1: mod=1.0+grain*.13
        else:
            # grass side has a green cap and dirt body
            if fy>.78: r,g,b=91.,145.,58.
            else: r,g,b=127.,91.,57.
            mod=1.0+grain*.11
    elif bid==LOG:
        if ny!=0:
            ring=((u-8)*(u-8)+(vv-8)*(vv-8))%17; mod=.82+ring/70.0
        else: mod=.82+((u//3)%2)*.18+grain*.05
    elif bid==LEAVES: mod=.82+((h>>8)&255)/255*.34
    elif bid==WATER:
        wave=math.sin((u+vv)*.65+x*.17+z*.11)*.10; mod=.92+wave
        if version>=3 and ny==1: r,g,b=62.,125.,205.; mod+=.08
    elif bid==LAVA:
        hot=((h>>9)&255)/255.0
        r=232+20*hot; g=70+90*hot; b=12+16*hot; mod=.92+grain*.14
    elif bid in (COAL_ORE,IRON_ORE,GOLD_ORE,DIAMOND_ORE,EMERALD_ORE):
        # Stone base with colored ore flecks.
        if (h&15)<6: pass
        else: r,g,b=112.,112.,112.
        mod=.92+grain*.13
    return max(0,min(255,r*mod)),max(0,min(255,g*mod)),max(0,min(255,b*mod))

@njit
def cast(ids,meta,ox,oy,oz,dx,dy,dz,maxdist,version):
    x=int(math.floor(ox)); y=int(math.floor(oy)); z=int(math.floor(oz))
    sx=1 if dx>0 else -1; sy=1 if dy>0 else -1; sz=1 if dz>0 else -1
    inf=1e30
    tdx=abs(1.0/dx) if abs(dx)>1e-10 else inf; tdy=abs(1.0/dy) if abs(dy)>1e-10 else inf; tdz=abs(1.0/dz) if abs(dz)>1e-10 else inf
    tx=((x+1-ox)/dx) if dx>0 else ((ox-x)/(-dx) if dx<0 else inf)
    ty=((y+1-oy)/dy) if dy>0 else ((oy-y)/(-dy) if dy<0 else inf)
    tz=((z+1-oz)/dz) if dz>0 else ((oz-z)/(-dz) if dz<0 else inf)
    nx=ny=nz=0; t=0.0
    for _ in range(1600):
        bid=block_at(ids,x,y,z)
        if bid!=AIR:
            hx=ox+dx*t+dx*1e-4; hy=oy+dy*t+dy*1e-4; hz=oz+dz*t+dz*1e-4
            # Pass 2+: cutout-style foliage; holes continue the ray through the leaf block.
            if bid==LEAVES and version>=2:
                fx=hx-math.floor(hx); fy=hy-math.floor(hy); fz=hz-math.floor(hz)
                u=int((fx if ny!=0 or nz!=0 else fz)*16); vv=int((fz if ny!=0 else fy)*16)
                if (ihash(x*17+u,y*19+vv,z*23,18)&15)<2:
                    pass
                else:
                    r,g,b=texcolor(ids,meta,x,y,z,nx,ny,nz,hx,hy,hz,version); return 1,t,r,g,b,nx,ny,nz,x,y,z
            elif bid==WATER and version>=4 and ny==0:
                # Let some rays through vertical water faces for a rough transparency cue.
                if (ihash(x,y,z,int((hx+hz)*8))&7)<2: pass
                else:
                    r,g,b=texcolor(ids,meta,x,y,z,nx,ny,nz,hx,hy,hz,version); return 1,t,r,g,b,nx,ny,nz,x,y,z
            else:
                r,g,b=texcolor(ids,meta,x,y,z,nx,ny,nz,hx,hy,hz,version); return 1,t,r,g,b,nx,ny,nz,x,y,z
        if tx<ty and tx<tz: t=tx; tx+=tdx; x+=sx; nx=-sx; ny=0; nz=0
        elif ty<tz: t=ty; ty+=tdy; y+=sy; nx=0; ny=-sy; nz=0
        else: t=tz; tz+=tdz; z+=sz; nx=0; ny=0; nz=-sz
        if t>maxdist: break
    return 0,maxdist,0.,0.,0.,0,0,0,x,y,z

@njit
def ao_factor(ids,x,y,z,nx,ny,nz,version):
    if version<3:return 1.0
    occ=0
    # cheap local ambient occlusion based on non-air neighbors around hit block
    for dx,dy,dz in ((1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)):
        if block_at(ids,x+dx,y+dy,z+dz)!=AIR: occ+=1
    return max(.72,1.0-occ*.035)

@njit(parallel=True)
def render_core(ids,meta,ox,oy,oz,yaw,pitch,width,height,fov,maxdist,version):
    out=np.empty((height,width,3),np.uint8); aspect=width/height; scale=math.tan(math.radians(fov)*.5)
    cy=math.cos(yaw); sy=math.sin(yaw); cp=math.cos(pitch); sp=math.sin(pitch)
    fx=sy*cp; fy=sp; fz=cy*cp; rx=cy; ry=0.; rz=-sy; ux=-sy*sp; uy=cp; uz=-cy*sp
    for j in prange(height):
        v=(1-2*(j+.5)/height)*scale
        for i in range(width):
            u=(2*(i+.5)/width-1)*scale*aspect
            dx=fx+u*rx+v*ux; dy=fy+u*ry+v*uy; dz=fz+u*rz+v*uz; inv=1/math.sqrt(dx*dx+dy*dy+dz*dz); dx*=inv;dy*=inv;dz*=inv
            hit,d,r,g,b,nx,ny,nz,bx,by,bz=cast(ids,meta,ox,oy,oz,dx,dy,dz,maxdist,version)
            if hit:
                if ny==1: shade=1.05
                elif ny==-1: shade=.57
                elif nx!=0: shade=.83
                else: shade=.74
                shade*=ao_factor(ids,bx,by,bz,nx,ny,nz,version)
                if version>=5:
                    # mild high-altitude brightening + pseudo sun variation
                    shade*=.94+.08*max(0,min(1,(by-60)/48))
                fogstart=.55 if version>=4 else .68
                ff=min(.80 if version>=4 else .67,max(0.,(d-maxdist*fogstart)/(maxdist*(1-fogstart))))
                fogr,fogg,fogb=(183.,207.,231.) if version>=4 else (193.,211.,232.)
                out[j,i,0]=int(min(255,r*shade)*(1-ff)+fogr*ff); out[j,i,1]=int(min(255,g*shade)*(1-ff)+fogg*ff); out[j,i,2]=int(min(255,b*shade)*(1-ff)+fogb*ff)
            else:
                t=j/max(1,height-1)
                # progressively richer sky gradient; pass 5 gets blocky cloud bands.
                sr=(121*(1-t)+181*t); sg=(171*(1-t)+207*t); sb=(235*(1-t)+246*t)
                if version>=5:
                    # simple voxel-like cloud band, only in upper sky
                    wxv=int((i/width*36)+ox*.02); wyv=int((j/height*18)+oz*.015)
                    if j<height*.38 and (ihash(wxv,0,wyv,5)&31)<5: sr=sg=sb=238
                out[j,i,0]=int(sr); out[j,i,1]=int(sg); out[j,i,2]=int(sb)
    return out

def valid_stand(ids,x,z):
    if not(X0<=x<=X1 and Z0<=z<=Z1): return None
    ix0=xi(x); iz0=zi(z)
    # Search top-down for a solid support with two full collision blocks of air above.
    # Feet are on TOP of support block; eye is feet + 1.62, fixing the old 1-block-low bug.
    nonstand={AIR,WATER,LAVA,LEAVES,MUSHROOM_BROWN,MUSHROOM_RED}
    for iy in range(NY-3,-1,-1):
        bid=int(ids[iy,iz0,ix0])
        if bid not in nonstand and ids[iy+1,iz0,ix0]==AIR and ids[iy+2,iz0,ix0]==AIR:
            support_y=iy+Y0; feet_y=support_y+1.0; eye_y=feet_y+1.62
            return support_y,feet_y,eye_y
    return None

def nearest_valid_stand(ids,x,z,radius=18):
    for r in range(radius+1):
        candidates=[]
        for dz in range(-r,r+1):
            for dx in range(-r,r+1):
                if max(abs(dx),abs(dz))!=r:continue
                q=valid_stand(ids,x+dx,z+dz)
                if q:candidates.append((dx*dx+dz*dz,x+dx,z+dz,q))
        if candidates:
            _,xx,zz,q=min(candidates,key=lambda a:a[0]); return xx,zz,q
    return None

def target_y_from_h(h,x,z):
    if X0<=x<=X1 and Z0<=z<=Z1:return float(h[zi(z),xi(x)])+2.0
    return 66.0

def render_view(ids,meta,h,camera,target,outfile,version=1,width=480,height=270,fov=70,maxdist=150):
    st=nearest_valid_stand(ids,int(camera[0]),int(camera[1]),22)
    if not st: raise RuntimeError(f'No standable camera near {camera}')
    x,z,(_,feet,eye)=st; tx,tz=target; ty=target_y_from_h(h,int(tx),int(tz))+1.2
    dx=tx-(x+.5); dz=tz-(z+.5); dy=ty-eye; yaw=math.atan2(dx,dz); pitch=math.atan2(dy,math.hypot(dx,dz))
    arr=render_core(ids,meta,float(x)+.5,float(eye),float(z)+.5,yaw,pitch,width,height,fov,maxdist,version)
    Image.fromarray(arr).save(outfile,'WEBP',quality=82,method=4)
    return {'requested_camera':camera,'actual_camera':(x,z),'support_y':feet-1,'feet_y':feet,'eye_y':eye,'target':target}

# -----------------------------------------------------------------------------
# TOP-DOWN + CAMERA SELECTION
# -----------------------------------------------------------------------------
def topdown(v,outfile):
    h=v['h']; mat=v['material']; img=np.zeros((NZ,NX,3),np.float32)
    colors={M_GRASS:(97,144,67),M_STONE:(116,116,116),M_SAND:(218,203,151),M_GRAVEL:(128,124,119)}
    for m,c in colors.items(): img[mat==m]=c
    img[v['ocean']]=(55,102,175)
    img[v['forest']]=(48,100,46)
    # local height shading from NW light
    gy,gx=np.gradient(h.astype(float)); shade=np.clip(1.02-.045*gx-.034*gy,.62,1.22)
    elev=np.clip((h.astype(float)-58)/48,0,1)
    shade*=.88+.24*elev
    img*=shade[...,None]
    # subtle 8-block contours make mountain topology readable in the review artifact.
    contour=(h%8==0)&(h>66)&v['mountain_mask']
    img[contour]*=.78
    # fluid masks
    for feats,c in [(v['lakes'],(50,105,196)),(v['lava'],(235,86,20))]:
        for mask,level,ctr in feats: img[mask]=c
    img[v['route_mask']]=(240,182,23); img[v['home_n']]=(48,63,164); img[v['home_s']]=(165,48,43)
    # POI circles
    pil=Image.fromarray(np.clip(img,0,255).astype(np.uint8)); d=ImageDraw.Draw(pil)
    for pts,col,r in [(v['villages'],'#f28c28',6),(v['major'],'#a845cf',5),(v['minor'],'#83c944',4)]:
        for x,z in pts:
            ix0,iz0=xi(x),zi(z); d.ellipse((ix0-r,iz0-r,ix0+r,iz0+r),fill=col,outline='white',width=1)
    # resources and landmarks small symbols
    for rr in v['resources']:
        x,z=rr['pos']; ix0,iz0=xi(x),zi(z); d.rectangle((ix0-2,iz0-2,ix0+2,iz0+2),fill='#f4e45e')
    for lm in v['landmarks']:
        x,z=lm['pos']; ix0,iz0=xi(x),zi(z); d.polygon([(ix0,iz0-4),(ix0-4,iz0+3),(ix0+4,iz0+3)],fill='#ffffff')
    # north is top naturally because z increases down image
    pil.thumbnail((620,768),Image.Resampling.LANCZOS)
    pil.save(outfile,'WEBP',quality=84,method=4)

def pick_cameras(v):
    h=v['h']; mountain=v['mountain_mask']; forest=v['forest']; west=v['westness']
    # north junction from known route points
    j=v['north_routes'][0][1]
    cams=[('north route junction',j,(0,-90))]
    # mountain approach: west route midpoint toward interior
    westpts=v['north_routes'][0]
    cam=westpts[-1]; cams.append(('western route / mountain approach',cam,(-245,cam[1]+25)))
    # one generated entrance, from east side looking west
    ex,ez=v['entrances'][len(v['entrances'])//2]; cams.append(('mountain entrance', (ex+22,ez),(-272,ez)))
    # deep/high interior: choose moderate deep cell with valid interior around it
    cand=mountain&(west>.58)&(h>np.percentile(h[mountain],60))&(h<np.percentile(h[mountain],88))
    ys,xs=np.where(cand)
    if len(xs):
        k=len(xs)//2; x,z=wx(xs[k]),wz(ys[k]); cams.append(('mountain interior',(x+15,z+10),(x-45,z-5)))
    else: cams.append(('mountain interior',(-255,20),(-305,30)))
    # forest interior based on max distance transform
    if forest.any():
        dist=distance_transform_edt(forest); iz0,ix0=np.unravel_index(np.argmax(dist),dist.shape); x,z=wx(ix0),wz(iz0)
        cams.append(('forest interior',(x,z),(x+45,z+20)))
    else: cams.append(('ordinary wilderness',(60,-40),(20,30)))
    return cams[:5]

# -----------------------------------------------------------------------------
# ARTIFACT BUILD
# -----------------------------------------------------------------------------
RENDER_NOTES={
  1:'Corrected standing-player camera; procedural face-aware block textures.',
  2:'Adds cutout foliage and improved grass/log texture behavior.',
  3:'Adds local ambient occlusion and improved water surface response.',
  4:'Adds stronger distance fog and rough water transparency cues.',
  5:'Adds altitude-sensitive lighting and simple blocky cloud/atmospheric cues.'
}

def data_uri(path):
    b=Path(path).read_bytes(); mime='image/webp'
    return 'data:'+mime+';base64,'+base64.b64encode(b).decode('ascii')

def build_html(records,outfile,attempts,rejections):
    css='''
:root{color-scheme:dark;--bg:#101215;--card:#181c21;--muted:#9da7b3;--line:#313841;--ok:#72d48b;--accent:#f0c44f}
*{box-sizing:border-box} body{margin:0;background:var(--bg);color:#eef2f5;font:15px/1.45 system-ui,-apple-system,Segoe UI,sans-serif}
main{max-width:1180px;margin:auto;padding:18px}.intro{padding:18px 0 26px}.intro h1{margin:.1em 0}.intro p{max-width:900px;color:#cbd2d9}
.batch{margin:32px 0 14px;border-top:1px solid var(--line);padding-top:24px}.batch h2{margin:0}.batch-note{color:var(--muted);margin-top:6px}
.seed{background:var(--card);border:1px solid var(--line);border-radius:14px;margin:18px 0 30px;overflow:hidden}.seed-head{padding:16px 18px;border-bottom:1px solid var(--line);display:flex;gap:12px;flex-wrap:wrap;align-items:baseline}.pass{color:var(--ok);font-weight:800}.topology{color:var(--accent)}
.seed-body{padding:14px}.overview{display:grid;grid-template-columns:minmax(280px,.72fr) 1.28fr;gap:14px;align-items:start}.overview img,.views img{width:100%;height:auto;display:block;border-radius:8px;background:#0b0d10}.metrics{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:7px}.metric{background:#11151a;border-radius:7px;padding:7px 9px}.metric b{display:block;font-size:13px}.metric span{color:var(--muted);font-size:12px}
.views{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:14px}.view{margin:0}.view figcaption{color:var(--muted);padding:5px 2px 1px;font-size:12px}
details{margin-top:12px;border-top:1px solid var(--line);padding-top:10px} summary{cursor:pointer;color:#cbd2d9}.checks{columns:2;margin-top:8px}.checks div{break-inside:avoid;color:#b8c2cc}.checks .yes{color:var(--ok)}
@media(max-width:720px){main{padding:10px}.overview{grid-template-columns:1fr}.metrics{grid-template-columns:1fr 1fr}.views{grid-template-columns:1fr}.checks{columns:1}.seed{border-radius:9px}}
'''
    parts=[f'<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Minecraft MOBA — 50 Default-map variants</title><style>{css}</style></head><body><main>']
    parts.append(f'''<section class="intro"><h1>Default-map solution-space review</h1><p><strong>50 passing variants</strong>, generated from the documented Default-map contract rather than by moving P2D features around. P2D is not counted among the 50. Generation required {attempts} candidates; {rejections} were rejected by validation or diversity checks.</p><p>Every ground-level camera first finds a legal two-block player standing volume. Camera feet are placed on the top face of the support block and the eye is then placed at <strong>feet + 1.62 blocks</strong>. This corrects the earlier renderer's one-block-low camera error.</p><p>Yellow = authored routes; orange = villages; purple = major POIs; lime = minor POIs; white triangles = mountain landmarks; small yellow squares = differentiated mountain resource regions. Exact resource quantities and POI content remain balance parameters rather than layout invariants.</p></section>''')
    current_batch=0
    metric_keys=['mountain_blocks','mountain_vertical_range','mountain_depth','entrances','resource_regions','mean_resource_route_distance','forest_blocks','forest_depth','villages','min_village_spacing','largest_free_component','route_p95_step']
    labels={'mountain_blocks':'mountain area','mountain_vertical_range':'vertical range','mountain_depth':'mountain depth','entrances':'approaches','resource_regions':'resource regions','mean_resource_route_distance':'resource→route mean','forest_blocks':'forest area','forest_depth':'forest depth','villages':'villages','min_village_spacing':'village spacing','largest_free_component':'connected wilderness','route_p95_step':'route p95 step'}
    for i,r in enumerate(records,1):
        batch=(i-1)//10+1
        if batch!=current_batch:
            current_batch=batch; rv=batch
            parts.append(f'<section class="batch"><h2>Batch {batch} · Variants {(batch-1)*10+1}–{batch*10}</h2><p class="batch-note">Renderer v{rv}: {RENDER_NOTES[rv]}</p></section>')
        m=r['metrics']; parts.append(f'<article class="seed"><header class="seed-head"><strong>Variant {i:02d}</strong><span class="pass">PASS</span><span>generation seed {r["seed"]}</span><span class="topology">{r["topology"].replace("_"," ")}</span><span>nearest prior diversity Δ {r["diversity_distance"]:.2f}</span></header><div class="seed-body">')
        parts.append('<div class="overview"><img loading="lazy" src="'+data_uri(r['topdown'])+'"><div class="metrics">')
        for k in metric_keys:
            val=m[k]
            if k in ('mountain_blocks','forest_blocks','largest_free_component'): val=f'{int(val):,}'
            parts.append(f'<div class="metric"><b>{labels[k]}</b><span>{val}</span></div>')
        parts.append('</div></div><div class="views">')
        for vw in r['views']:
            parts.append(f'<figure class="view"><img loading="lazy" src="{data_uri(vw["path"])}"><figcaption>{vw["name"]} · eye Y {vw["camera"]["eye_y"]:.2f} (support Y {vw["camera"]["support_y"]:.0f})</figcaption></figure>')
        parts.append('</div><details><summary>validator details</summary><div class="checks">')
        for k,val in r['checks'].items(): parts.append(f'<div class="yes">✓ {k.replace("_"," ")}</div>')
        parts.append('</div></details></div></article>')
    parts.append('</main></body></html>')
    Path(outfile).write_text(''.join(parts),encoding='utf-8')

# -----------------------------------------------------------------------------
# MAIN PIPELINE
# -----------------------------------------------------------------------------

def _json_default(o):
    if isinstance(o, np.integer): return int(o)
    if isinstance(o, np.floating): return float(o)
    if isinstance(o, np.ndarray): return o.tolist()
    raise TypeError(f"Not JSON serializable: {type(o)}")

def run(outdir,max_pass=50,start_seed=920260900):
    out=Path(outdir); out.mkdir(parents=True,exist_ok=True)
    state_path=out/'state.json'; t0=time.time()
    if state_path.exists():
        st=json.loads(state_path.read_text())
        recs=st.get('records',[]); attempts=int(st.get('attempts',0)); reject_log=st.get('rejections',[])
        candidate_seed=int(st.get('candidate_seed',start_seed))
        accepted=[{'diversity_vec':np.array(r['diversity_vec'],dtype=float),'topology':r['topology']} for r in recs]
        print(f'RESUME passes={len(recs)} attempts={attempts} next_seed={candidate_seed}',flush=True)
    else:
        recs=[]; accepted=[]; attempts=0; reject_log=[]; candidate_seed=start_seed
    while len(recs)<max_pass:
        attempts+=1; seed=candidate_seed; candidate_seed+=1
        v=generate_candidate(seed); passed,m,checks=validate(v); vec=diversity_vector(v,m)
        diverse,dd=diverse_enough(vec,accepted,v['topology'])
        if not passed or not diverse:
            reject_log.append({'seed':seed,'validation_pass':passed,'diverse':diverse,'distance':round(dd,3),'failed':[k for k,x in checks.items() if not x]})
            continue
        idx=len(recs)+1; version=(idx-1)//10+1
        sd=out/f'variant_{idx:02d}_seed_{seed}'; sd.mkdir(exist_ok=True)
        top=sd/'topdown.webp'; topdown(v,top)
        rng=np.random.default_rng(seed^0x55AA33)
        ids,meta,tree_count=voxelize(v,rng)
        views=[]
        for j,(name,cam,target) in enumerate(pick_cameras(v),1):
            path=sd/f'view_{j:02d}.webp'
            try:
                cinfo=render_view(ids,meta,v['h'],cam,target,path,version=version)
            except Exception:
                # fallback toward map center, still requiring legal stand.
                cinfo=render_view(ids,meta,v['h'],(cam[0]+10,cam[1]+10),target,path,version=version)
            views.append({'name':name,'path':str(path),'camera':cinfo})
        record={'index':idx,'seed':seed,'topology':v['topology'],'metrics':m,'checks':checks,'diversity_distance':float(dd if dd<998 else 9.99),'topdown':str(top),'views':views,'renderer_version':version,'tree_count':tree_count,'diversity_vec':vec.tolist(),
                'villages':v['villages'],'major':v['major'],'minor':v['minor'],'resources':v['resources'],'landmarks':v['landmarks'],'entrances':v['entrances']}
        recs.append(record); accepted.append({'diversity_vec':vec,'topology':v['topology']})
        # Persistent checkpoint: a timeout/restart continues from the next candidate without rerendering accepted seeds.
        state={'records':recs,'attempts':attempts,'rejections':reject_log,'candidate_seed':candidate_seed}
        state_path.write_text(json.dumps(state,default=_json_default))
        (out/'progress.json').write_text(json.dumps({'passes':len(recs),'attempts':attempts,'last_seed':seed,'next_seed':candidate_seed},indent=2))
        print(f'PASS {idx:02d}/50 seed={seed} topo={v["topology"]} renderer=v{version} attempts={attempts} d={dd:.2f}',flush=True)
        del ids,meta,v; gc.collect()
    # JSON/CSV-like summary
    serial=[]
    for r in recs:
        q={k:v for k,v in r.items() if k not in ('topdown','views')}; q['topdown']=os.path.relpath(r['topdown'],out); q['views']=[{'name':x['name'],'path':os.path.relpath(x['path'],out),'camera':x['camera']} for x in r['views']]; serial.append(q)
    (out/'variants.json').write_text(json.dumps(serial,indent=2,default=_json_default))
    (out/'rejections.json').write_text(json.dumps(reject_log,indent=2,default=_json_default))
    html=out/'Minecraft_MOBA_50_Default_Map_Variants.html'; build_html(recs,html,attempts,len(reject_log))
    # Compact summary markdown
    lines=['# Minecraft MOBA — Default-map solution-space run','',f'- Passing variants: **{len(recs)}**',f'- Candidates attempted: **{attempts}**',f'- Rejected: **{len(reject_log)}**',f'- Runtime: **{(time.time()-t0)/60:.1f} minutes**','',
           '## Renderer progression','']+[f'- **v{k}:** {v}' for k,v in RENDER_NOTES.items()]+['','## Variant index','']
    for r in recs: lines.append(f'- Variant {r["index"]:02d}: seed `{r["seed"]}` · {r["topology"]} · mountain {r["metrics"]["mountain_blocks"]:,} blocks · villages {r["metrics"]["villages"]} · approaches {r["metrics"]["entrances"]}')
    (out/'SUMMARY.md').write_text('\n'.join(lines))
    print('DONE',html,'size',html.stat().st_size,'runtime_min',round((time.time()-t0)/60,2),flush=True)
    return html

if __name__=='__main__':
    ap=argparse.ArgumentParser(); ap.add_argument('--out',default='/mnt/data/minecraft_moba_50_variants'); ap.add_argument('--count',type=int,default=50); ap.add_argument('--seed',type=int,default=920260900); args=ap.parse_args()
    run(args.out,args.count,args.seed)
