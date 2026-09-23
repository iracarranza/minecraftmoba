#!/usr/bin/env python3
"""Regional authored-opportunity optimizer for the realized Minecraft MOBA map.

This tool searches regional authored-opportunity portfolios. It deliberately does
NOT perform block-exact authoring. Exact sites/paths must be rescanned afterward.

Critical rule: a zero-hostile entity snapshot is never interpreted as zero
underground hazard. Hazard remains UNRESOLVED until spawn-pressure evidence or
an explicit sensitivity model exists.

Scenario counts/weights/thresholds in this file are NON-CANON analysis fixtures.

WHAT CHANGED, 23 September 2026 -- the optimization TARGET.

This tool used to optimize `balance_asymmetry`: the mean normalised difference
between the two teams' travel reach to the opportunities a portfolio placed.
The objective was `character - 5.5*b - 25*max(0, b-0.14)`, so balance dominated
every profile, and a portfolio was only kept at all if `b <= 0.12`.

That target is superseded. Doctrine now says functional opportunity is balanced
while Wilderness is not mirrored, and a difference between the two team ends is
not a competitive deficit without evidence of a relevant gameplay consequence.
The near-miss rescue test on 930010639 showed what optimizing it actually buys:
authoring drove the scalar to 0.0161 while leaving the map's 0.3173
accessible-land gap exactly where it was, because the scalar measures
travel-cost equality to the opportunities authoring places, not the terrain.

The MEASUREMENTS are kept -- every balance component is still computed and
still reported -- and only the interpretation changed. The shape is now:

    HARD GATES (viability)  then  OPTIMIZATION AMONG VIABLE CANDIDATES

The gate is reachability, not equality. An opportunity one team cannot attend
to at all is a structural failure; an opportunity that is simply nearer one
team is ordinary competitive geography. Optimization then runs on profile
character and real authoring cost, with balance demoted to a reported number.

See docs/audit/2026-09-23-map-compiler-slice.md.
"""
from __future__ import annotations
import argparse, json, math, statistics, zipfile
from pathlib import Path

SPRINT = 5.612
HOMES = {"north": (-2172.0, -460.0), "south": (-2020.0, 428.0)}
# SUPERSEDED as a filter, retained as the historical value so old frontier
# files stay interpretable. Nothing gates on it any more.
HISTORICAL_BALANCE_FILTER = 0.12

# PROVISIONAL_ALPHA. The worst-placed team's travel reach to an authored
# opportunity, in seconds. An opportunity beyond this cannot be attended to by
# that team inside the ten-minute night that opens it, which is a structural
# argument rather than a preference -- and it is where the measured data sits:
# across the two realized maps (126 cells, Alpha and near-miss 930010639) the
# worst-team reach has p90 400s, p95 491s and max 864s, so 600s excludes 2.4%
# of placements as pathological and leaves ordinary difference alone.
# Tightening to 400s would reject a tenth of real placements; loosening to 900s
# gates nothing at all. Configurable; NOT canon.
PRACTICAL_REACH_BOUND_SEC = 600.0

# PROVISIONAL_ALPHA. A team with no authored Worksite this close cannot take
# part in the Worksite night at all, which is functional opportunity failing --
# not terrain differing. Measured the same way: across both realized maps and
# 800 sampled portfolios, each team's NEAREST Worksite sits at 73-168s (Alpha
# north median 76s / south 96s; near-miss north 74s / south 87s), so 200s
# accepts everything real geography produced with room to spare and fires only
# when a portfolio strands a team entirely. Tightening to 150s starts rejecting
# ordinary southern placements on both maps. Configurable; NOT canon.
OPENING_WORKSITE_REACH_SEC = 200.0
ROUTE_TARGET_STRETCH = 1.25
ROUTE_SPILLOVER_RADIUS = 112.0
ROUTE_SPILLOVER_SCALE = 0.85
PROFILES = {
    "balanced_baseline": (10, 8),
    "exploration_centric": (10, 8),
    "consolidative": (10, 8),
    "resource_dense": (12, 12),
    "resource_light": (8, 6),
    "contested_core": (10, 8),
    "peripheral_specialization": (10, 8),
    "route_centric": (10, 8),
}
ECON_ANIMALS = ("chicken","rabbit","pig","cow","sheep","goat","turtle")

def load_export(path):
    with zipfile.ZipFile(path) as z:
        return (
            json.loads(z.read("data/opportunity-map.json")),
            json.loads(z.read("data/travel-matrix.json")),
        )

def idw_reach_model(travel):
    cm = travel["cost_matrix"]
    sites = []
    for s in travel["sites"]:
        if s["id"] in ("north_homeland","south_homeland"):
            continue
        sites.append((s["world_xz"][0], s["world_xz"][1],
                      cm["north_homeland"][s["id"]],
                      cm["south_homeland"][s["id"]]))
    homes = {"north":(-2172.,-460.), "south":(-2020.,428.)}
    def predict(team, x, z):
        hi = 2 if team=="north" else 3
        hx,hz = homes[team]
        qd = math.hypot(x-hx,z-hz)
        vals=[]
        for sx,sz,nc,sc in sites:
            eu=math.hypot(sx-hx,sz-hz)
            if eu < 1: continue
            ratio=(nc if team=="north" else sc)/eu
            d=max(math.hypot(x-sx,z-sz),1e-6)
            vals.append((1/(d**3),ratio))
        r=sum(w*v for w,v in vals)/sum(w for w,_ in vals)
        return qd*r/SPRINT
    return predict

def cells_from_export(opp, predict):
    out=[]
    for r in opp["cells"]:
        ox,oz=r["world_origin"]; x,z=ox+64,oz+64
        n,s=predict("north",x,z),predict("south",x,z)
        farm=r["farmable_surface_samples"]/r["sampled_columns"]
        water=r["water_surface_samples"]/r["sampled_columns"]
        ore=sum(r["ore"].values())
        bias="north" if n+45<s else ("south" if s+45<n else "neutral")
        dn=math.hypot(x-HOMES["north"][0],z-HOMES["north"][1])/SPRINT
        ds=math.hypot(x-HOMES["south"][0],z-HOMES["south"][1])/SPRINT
        out.append({
            "cell":r["cell"],"center":[x,z],"n":n,"s":s,"gap":abs(n-s),
            "min":min(n,s),"bias":bias,"farm":farm,"water":water,"ore":ore,
            "fauna":r["fauna"],"vocab":r["regenerative_vocabulary"],
            "direct_n":dn,"direct_s":ds,
            "inflation_n":n/max(dn,1e-9),"inflation_s":s/max(ds,1e-9),
            "development_proxy":farm>=.35,
            "extraction_proxy":ore>=750,
            "buildable_proxy":water<=.39,
        })
    return out

def catalogs(cells):
    founders=[]; renew=[]; works=[]; pois=[]; routes={"north":[],"south":[]}
    for c in cells:
        if c["development_proxy"]:
            for crop in ("carrot","potato"):
                founders.append({"crop":crop,**c})
        for sp in ECON_ANIMALS:
            if c["fauna"].get(sp,0)>0:
                renew.append({"species":sp,"observed_count":c["fauna"][sp],**c})
        if c["buildable_proxy"] and 70<=c["min"]<=235:
            works.append(c)
        if c["buildable_proxy"] and c["min"]>=70:
            pois.append(c)
        for team,key,other in (("north","n","s"),("south","s","n")):
            # Regional Route target: useful opportunity whose current reach is
            # not already trivial. Exact physical path is intentionally absent.
            if 55<=c[key]<=c[other]+90:
                routes[team].append(c)
    return {"founders":founders,"renewables":renew,"worksites":works,
            "pois":pois,"route_targets":routes}

class DeterministicRNG:
    """Cross-language LCG so generated artifacts are reproducible outside Python."""
    def __init__(self, seed):
        self.state=int(seed)&0xffffffff
    def random(self):
        self.state=(1664525*self.state+1013904223)&0xffffffff
        return self.state/4294967296.0
    def randbelow(self,n):
        if n<=0:
            raise ValueError("empty range")
        return int(self.random()*n)
    def choice(self,seq):
        return seq[self.randbelow(len(seq))]
    def sample(self,seq,k):
        a=list(seq); k=min(k,len(a))
        for i in range(k):
            j=i+self.randbelow(len(a)-i)
            a[i],a[j]=a[j],a[i]
        return a[:k]

def choose(seq,k,rng):
    return rng.sample(seq,min(k,len(seq)))

def config(cats, profile, rng):
    wc,rc=PROFILES[profile]
    # Preserve side/neutral diversity in Worksite pool.
    by={b:[x for x in cats["worksites"] if x["bias"]==b]
        for b in ("north","south","neutral")}
    qn=5 if wc>=12 else (3 if wc<=8 else 4)
    qs=qn; q0=max(0,wc-qn-qs)
    ws=choose(by["north"],qn,rng)+choose(by["south"],qs,rng)+choose(by["neutral"],q0,rng)

    # Four founder relationships: near/deep for each side. Candidate selection
    # remains stochastic so complete portfolios, not individual nodes, compete.
    founders=[]
    for team,key in (("north","n"),("south","s")):
        pool=[x for x in cats["founders"] if x["bias"]==team]
        near=[x for x in pool if 35<=x[key]<=115] or pool
        deep=[x for x in pool if 100<=x[key]<=205] or pool
        founders += [rng.choice(near),rng.choice(deep)]

    renew=[]
    for team,key in (("north","n"),("south","s")):
        pool=[x for x in cats["renewables"] if x["bias"]==team]
        renew += choose(pool,max(2,(rc-2)//2),rng)
    shared=[x for x in cats["renewables"] if x["gap"]<=75]
    renew += choose(shared,max(0,rc-len(renew)),rng)

    peripheral=[]
    for team,key in (("north","n"),("south","s")):
        p=[x for x in cats["pois"] if x["bias"]==team and 130<=x[key]<=260]
        if p: peripheral.append(rng.choice(p))
    neutral=[x for x in cats["pois"] if x["gap"]<=90 and 80<=x["min"]<=230]
    pois=peripheral+choose(neutral,max(0,4-len(peripheral)),rng)

    route_targets={}
    for team,key in (("north","n"),("south","s")):
        # Prefer targets with meaningful current reach; exact Route geometry is
        # a later world-authoring problem.
        p=sorted(cats["route_targets"][team],key=lambda x:x[key],reverse=True)
        route_targets[team]=choose(p[:max(8,len(p)//2)],3,rng)
    return {"founders":founders,"renewables":renew,"worksites":ws,
            "pois":pois,"route_targets":route_targets}

def ndiff(a,b):
    return abs(a-b)/max((abs(a)+abs(b))/2,1e-9)

def segment_position(point,start,end):
    px,pz=point; ax,az=start; bx,bz=end
    dx,dz=bx-ax,bz-az
    den=dx*dx+dz*dz
    if den<=1e-9:
        return 0.0,math.hypot(px-ax,pz-az)
    t=((px-ax)*dx+(pz-az)*dz)/den
    t=max(0.0,min(1.0,t))
    return t,math.hypot(px-(ax+t*dx),pz-(az+t*dz))

def route_target_saving(target,team):
    key="n" if team=="north" else "s"
    dk="direct_n" if team=="north" else "direct_s"
    return max(0.0,target[key]-target[dk]*ROUTE_TARGET_STRETCH)

def spillover_reach(item,team,cfg):
    """Regional sensitivity for collateral Route effects; exact rescan remains authoritative."""
    key="n" if team=="north" else "s"
    dk="direct_n" if team=="north" else "direct_s"
    base=item[key]
    best=base
    for target in cfg["route_targets"][team]:
        progress,distance=segment_position(item["center"],HOMES[team],target["center"])
        if progress<=0 or distance>=ROUTE_SPILLOVER_RADIUS:
            continue
        falloff=1.0-distance/ROUTE_SPILLOVER_RADIUS
        saving=route_target_saving(target,team)*progress*falloff*ROUTE_SPILLOVER_SCALE
        floor=item[dk]*ROUTE_TARGET_STRETCH
        best=min(best,max(floor,base-saving))
    return best

def balance_components(cfg,spillover=False):
    def reach(item,team):
        if spillover:
            return spillover_reach(item,team,cfg)
        return item["n" if team=="north" else "s"]
    def side(items,team):
        return sum(reach(x,team) for x in items if x["bias"]==team)
    fn,fs=side(cfg["founders"],"north"),side(cfg["founders"],"south")
    rn,rs=side(cfg["renewables"],"north"),side(cfg["renewables"],"south")
    wn=statistics.mean(reach(x,"north") for x in cfg["worksites"])
    ws=statistics.mean(reach(x,"south") for x in cfg["worksites"])
    pn=sum(reach(x,"north") for x in cfg["pois"])
    ps=sum(reach(x,"south") for x in cfg["pois"])
    rtn=statistics.mean(reach(x,"north") for x in cfg["route_targets"]["north"])
    rts=statistics.mean(reach(x,"south") for x in cfg["route_targets"]["south"])
    comps={"founder":ndiff(fn,fs),"renewable":ndiff(rn,rs),
           "worksite":ndiff(wn,ws),"poi":ndiff(pn,ps),"route":ndiff(rtn,rts)}
    return comps,sum(comps.values())/len(comps)

def spillover_summary(cfg):
    selected=cfg["founders"]+cfg["renewables"]+cfg["worksites"]+cfg["pois"]
    out={}
    for team in ("north","south"):
        key="n" if team=="north" else "s"
        positive=[max(0.0,x[key]-spillover_reach(x,team,cfg)) for x in selected]
        positive=[x for x in positive if x>1e-6]
        out[team]={
            "affected_opportunities":len(positive),
            "mean_saving_sec_if_affected":statistics.mean(positive) if positive else 0.0,
            "max_saving_sec":max(positive) if positive else 0.0,
            "total_selected_opportunities":len(selected),
        }
    return out

def metrics(cfg):
    comps,balance=balance_components(cfg,False)
    spill_comps,spill_balance=balance_components(cfg,True)
    allx=cfg["founders"]+cfg["renewables"]+cfg["worksites"]+cfg["pois"]
    contest=sum(x["gap"]<=75 and x["min"]>=60 for x in allx)/len(allx)
    deep=sum(x["min"]>=150 for x in allx)/len(allx)
    unique=len({tuple(x["cell"]) for x in allx})
    repeat=1-unique/len(allx)
    exploration=min(1,.55*deep+.45*statistics.mean(x["min"] for x in allx)/220)
    consolidation=min(1,repeat/.35)
    density=min(1,(len(cfg["renewables"])+len(cfg["worksites"])-14)/10)
    peripheral=min(1,statistics.mean(x["gap"] for x in cfg["renewables"])/220)
    return {
        # Retained MEASUREMENT. Nothing optimizes or gates on these any more;
        # they describe how reach differs between the ends, which doctrine
        # permits. See the module docstring.
        "balance_asymmetry":balance,
        "components":comps,
        "route_spillover_balance_asymmetry":spill_balance,
        "route_spillover_components":spill_comps,
        "route_spillover_delta":spill_balance-balance,
        "effective_balance_asymmetry":max(balance,spill_balance),
        "route_spillover":spillover_summary(cfg),
        "authoring_cost":authoring_cost(cfg),
        "contest_pressure":contest,
        "exploration_pressure":exploration,
        "consolidation_pressure":consolidation,
        "resource_density":density,
        "peripheral_specialization":peripheral,
    }

def viability(cfg,m,reach_bound=PRACTICAL_REACH_BOUND_SEC,
              opening_bound=OPENING_WORKSITE_REACH_SEC):
    """Hard gates, with machine-readable reasons. Empty means viable.

    Deliberately short. These are structural failures, not preferences, and a
    gate that fires on ordinary terrain difference would be the superseded
    balance target wearing a new name.
    """
    reasons=[]
    placed=cfg["founders"]+cfg["renewables"]+cfg["worksites"]+cfg["pois"]
    unreachable=[x for x in placed if max(x["n"],x["s"])>reach_bound]
    if unreachable:
        reasons.append({
            "code":"OPPORTUNITY_UNREACHABLE",
            "count":len(unreachable),
            "worst_reach_sec":max(max(x["n"],x["s"]) for x in unreachable),
            "bound_sec":reach_bound,
            "detail":"a team cannot attend to this opportunity within the night "
                     "that opens it; this is unreachability, not inequality",
        })
    for team,key in (("north","n"),("south","s")):
        if not any(x[key]<=opening_bound for x in cfg["worksites"]):
            reasons.append({
                "code":"NO_OPENING_WORKSITE",
                "team":team,
                "bound_sec":opening_bound,
                "nearest_sec":min(x[key] for x in cfg["worksites"]),
                "detail":"no authored Worksite inside opening reach for this team; "
                         "this team cannot take part in the Worksite night at all",
            })
    return reasons

def authoring_cost(cfg):
    """What this portfolio actually asks to be built and reached.

    A real cost, not a proxy for balance: how many placements, how far the
    average one sits from the nearer team, and how much repeated use is made of
    the same cells.
    """
    placed=cfg["founders"]+cfg["renewables"]+cfg["worksites"]+cfg["pois"]
    unique=len({tuple(x["cell"]) for x in placed})
    return {
        "placements":len(placed),
        "distinct_cells":unique,
        "mean_nearer_team_reach_sec":statistics.mean(x["min"] for x in placed),
        "max_worst_team_reach_sec":max(max(x["n"],x["s"]) for x in placed),
    }

def objective(profile,m):
    """Optimize character and cost among VIABLE candidates.

    Balance is no longer here. It was worth -5.5 per unit plus a -25 cliff,
    which made it the only term that mattered; it is now a reported measurement
    and nothing else. The cost term is small on purpose -- it breaks ties
    toward portfolios that ask for less reaching, without becoming a second
    disguised balance score.
    """
    if profile=="exploration_centric":
        char=m["exploration_pressure"]
    elif profile=="consolidative":
        char=m["consolidation_pressure"]
    elif profile=="resource_dense":
        char=m["resource_density"]
    elif profile=="resource_light":
        char=-m["resource_density"]+.5*m["exploration_pressure"]
    elif profile=="contested_core":
        char=1.4*m["contest_pressure"]
    elif profile=="peripheral_specialization":
        char=m["peripheral_specialization"]
    elif profile=="route_centric":
        char=.5*m["exploration_pressure"]+.3*m["contest_pressure"]
    else:
        char=-(abs(m["contest_pressure"]-.28)+abs(m["exploration_pressure"]-.55))
    reach=m["authoring_cost"]["mean_nearer_team_reach_sec"]
    return char-0.15*(reach/PRACTICAL_REACH_BOUND_SEC)

def normalize(v):
    if isinstance(v,float):
        return round(v,9)
    if isinstance(v,list):
        return [normalize(x) for x in v]
    if isinstance(v,dict):
        return {k:normalize(x) for k,x in v.items()}
    return v

def candidate_counts(cats):
    return {
        "founder_stock":len(cats["founders"]),
        "renewable_manifestations":len(cats["renewables"]),
        "worksites":len(cats["worksites"]),
        "pois":len(cats["pois"]),
        "route_targets_north":len(cats["route_targets"]["north"]),
        "route_targets_south":len(cats["route_targets"]["south"]),
    }

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("realized_map_export",type=Path)
    ap.add_argument("--out",type=Path,default=Path("map-authoring-optimizer-output"))
    ap.add_argument("--samples",type=int,default=1200)
    ap.add_argument("--seed",type=int,default=20260920)
    a=ap.parse_args()
    a.out.mkdir(parents=True,exist_ok=True)
    opp,travel=load_export(a.realized_map_export)
    cells=cells_from_export(opp,idw_reach_model(travel))
    cats=catalogs(cells)
    rng=DeterministicRNG(a.seed)
    frontier={"_meta":{
        "schema":"minecraft_moba_scenario_frontier/2",
        "generator":"tools/analysis/map_authoring_optimizer.py",
        "samples_per_profile":a.samples,
        "random_seed":a.seed,
        "gating":"viability gates, not a balance filter",
        "practical_reach_bound_sec":PRACTICAL_REACH_BOUND_SEC,
        "opening_worksite_reach_sec":OPENING_WORKSITE_REACH_SEC,
        "practical_reach_bound_status":"PROVISIONAL_ALPHA, measured; see module docstring",
        "historical_balance_filter":HISTORICAL_BALANCE_FILTER,
        "historical_balance_filter_status":"SUPERSEDED; retained so older frontier files stay interpretable",
        "route_spillover_model":{
            "radius_blocks":ROUTE_SPILLOVER_RADIUS,
            "target_path_stretch":ROUTE_TARGET_STRETCH,
            "scale":ROUTE_SPILLOVER_SCALE,
            "status":"NON-CANON ANALYTICAL FIXTURE",
        },
    }}
    for profile in PROFILES:
        rows=[]
        for _ in range(a.samples):
            c=config(cats,profile,rng)
            m=metrics(c)
            rejections=viability(c,m)
            rows.append({"objective":objective(profile,m),"metrics":m,
                         "viable":not rejections,"rejections":rejections,
                         "configuration":c})
        rows.sort(key=lambda x:x["objective"],reverse=True)
        good=[x for x in rows if x["viable"]]
        codes={}
        for x in rows:
            for r in x["rejections"]:
                codes[r["code"]]=codes.get(r["code"],0)+1
        frontier[profile]={"samples":a.samples,"viable_found":len(good),
                           "rejection_codes":codes,
                           "finalists":(good or rows)[:10]}
    frontier=normalize(frontier)
    catalog=normalize({"_meta":{
        "schema":"minecraft_moba_candidate_catalog/2",
        "generator":"tools/analysis/map_authoring_optimizer.py",
        "counts":candidate_counts(cats),
        "proxy_warning":"count-calibrated role proxies",
    },**cats})
    hazard={
        "snapshot_hostiles":"DO NOT USE AS SAFETY EVIDENCE",
        "underground_hazard":"UNRESOLVED",
        "forbidden_inference":"hostiles == 0 -> underground is safe",
    }
    (a.out/"candidate-catalog.json").write_text(json.dumps(catalog,indent=2)+"\n")
    (a.out/"scenario-frontier.json").write_text(json.dumps(frontier,indent=2)+"\n")
    (a.out/"unresolved-hazards.json").write_text(json.dumps(hazard,indent=2)+"\n")
    print(json.dumps({
        "candidate_counts":candidate_counts(cats),
        "practical_reach_bound_sec":PRACTICAL_REACH_BOUND_SEC,
        "profiles":{p:{
            "viable_found":frontier[p]["viable_found"],
            "rejection_codes":frontier[p]["rejection_codes"],
            "best_objective":frontier[p]["finalists"][0]["objective"],
            "best_authoring_cost":frontier[p]["finalists"][0]["metrics"]["authoring_cost"],
            "measured_balance_asymmetry":frontier[p]["finalists"][0]["metrics"]["balance_asymmetry"],
        } for p in PROFILES},
    },indent=2))

if __name__=="__main__":
    main()
