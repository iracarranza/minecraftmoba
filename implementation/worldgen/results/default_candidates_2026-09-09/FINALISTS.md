# Successor Default candidate finalists

> Status: prototype/test candidates. This comparison does not select the final Default map.

Search evaluated **24** consecutive seeds and retained **4** using contract fitness plus geographic diversity—not top-N aggregate score.

| Seed | Family | Mountain depth | Empty / deep off-Route | Packing hotspot | Mean abs bias | Corrections | Warnings | Survived for |
|---:|---|---:|---:|---:|---:|---:|---|---|
| 920261010 | horseshoe | 305 | 43.6% / 40.8% | 7 | 0.054 | 0 | none | best balanced evidence |
| 920261020 | long_ridge | 300 | 48.5% / 47.7% | 7 | 0.148 | 0 | none | deep off route long ridge stress |
| 920261022 | offset_basin | 300 | 47.2% / 33.6% | 6 | 0.070 | 0 | none | coast variation and route dependence |
| 920261011 | broken_peaks | 295 | 49.9% / 50.0% | 7 | 0.094 | 0 | none | fragmented mountain low route share |

## Candidate tradeoffs

### `920261010` — horseshoe

- Terrain: mountain Y63–114, 305 blocks analytical interior depth, +29 median Y from approach to deep west; 3 forest regions.
- Water/coast: 3 connected drainage features (365 blocks); coast varies 59 blocks.
- Opportunities: 4 villages, 4 major and 6 minor POIs, 30 derived regions.
- Packing: mean nearest opportunity 50.74 blocks; hotspot 7; unrelated close-overlap pairs 23.
- Access: median terrain cost N/S 591.1/597.5; mean detour N/S 1.301/1.292; L3 unsupported round-trip viable N/S 2.7%/0.0%; portfolio mean absolute bias 0.0543; mean Route share 0.308.
- Why retained: best balanced evidence. Tradeoff: no experimental warning in this analytical model. Backend correction count: 0.

### `920261020` — long_ridge

- Terrain: mountain Y62–105, 300 blocks analytical interior depth, +29 median Y from approach to deep west; 3 forest regions.
- Water/coast: 3 connected drainage features (300 blocks); coast varies 66 blocks.
- Opportunities: 4 villages, 4 major and 6 minor POIs, 25 derived regions.
- Packing: mean nearest opportunity 43.3 blocks; hotspot 7; unrelated close-overlap pairs 26.
- Access: median terrain cost N/S 643.9/631.1; mean detour N/S 1.311/1.307; L3 unsupported round-trip viable N/S 0.0%/0.0%; portfolio mean absolute bias 0.1478; mean Route share 0.312.
- Why retained: deep off route long ridge stress. Tradeoff: no experimental warning in this analytical model. Backend correction count: 0.

### `920261022` — offset_basin

- Terrain: mountain Y62–106, 300 blocks analytical interior depth, +28 median Y from approach to deep west; 3 forest regions.
- Water/coast: 3 connected drainage features (265 blocks); coast varies 91 blocks.
- Opportunities: 4 villages, 4 major and 6 minor POIs, 22 derived regions.
- Packing: mean nearest opportunity 41.47 blocks; hotspot 6; unrelated close-overlap pairs 29.
- Access: median terrain cost N/S 607.2/631.1; mean detour N/S 1.291/1.291; L3 unsupported round-trip viable N/S 1.4%/1.4%; portfolio mean absolute bias 0.0695; mean Route share 0.363.
- Why retained: coast variation and route dependence. Tradeoff: no experimental warning in this analytical model. Backend correction count: 0.

### `920261011` — broken_peaks

- Terrain: mountain Y62–100, 295 blocks analytical interior depth, +27 median Y from approach to deep west; 2 forest regions.
- Water/coast: 3 connected drainage features (305 blocks); coast varies 86 blocks.
- Opportunities: 4 villages, 4 major and 6 minor POIs, 18 derived regions.
- Packing: mean nearest opportunity 47.93 blocks; hotspot 7; unrelated close-overlap pairs 26.
- Access: median terrain cost N/S 630.45/646.8; mean detour N/S 1.3/1.292; L3 unsupported round-trip viable N/S 1.4%/0.0%; portfolio mean absolute bias 0.0939; mean Route share 0.26.
- Why retained: fragmented mountain low route share. Tradeoff: no experimental warning in this analytical model. Backend correction count: 0.

## Reading the evidence

Hard contract failures reject a seed. Experimental thresholds create warnings and are not balance decisions. Descriptive metrics never gate a candidate. Terrain-aware costs are analytical surface estimates; visual review and actual Minecraft traversal remain required before selecting or serializing a playable world.
