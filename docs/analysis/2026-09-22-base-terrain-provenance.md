# The Alpha base terrain — located, verified pristine, promoted

The runtime-application model needs the base world to be a kept artifact and the
additions to be data. This records where the base actually was, that it is
genuinely pristine, and what the lineage is.

## It was never at risk, and it was not where I said it was

I claimed the base lived at `/tmp/profile-worlds/consolidative` and was one
`/tmp` sweep from gone. **Both halves were wrong.**

`/tmp/profile-worlds/*` are the four *authored* profile worlds — the working
copies `author_portfolio --apply` wrote the placements into. They are outputs,
not the base.

The real base is `tv_ef56852eda10acc88342e5ee`, one of three terrain volumes
inside the harvested gallery:

```
artifacts/worldgen/terrain_harvest_2026-09-18/worlds/TerrainGallery/
  dimensions/harvest/tv_ef56852eda10acc88342e5ee/{region,entities,terrain_volume.json}
```

Eight region files covering the whole Alpha area, in `artifacts/`, safe. It has
the same directory shape as the frozen template, so it is directly loadable.

## Verified pristine, not assumed

Probed at four authored site coordinates against the frozen map:

| site | base terrain | frozen map |
|---|---|---|
| rabbit pen centre (−2448, −48) | `grass_block` @ 89 | `grass_block` @ **91** |
| carrot field centre (−2384, −112) | `grass_block` @ 92 | **`water`** @ 91 |
| worksite ws_0_0 (−2416, −464) | `grass_block` @ 85 | `grass_block` @ 85 |
| poi (−2320, 144) | `grass_block` @ 86 | **`dirt_path`** @ 78 |

The base has natural ground at every site. The frozen map has the pen's levelled
pad two blocks higher, the crop field's central irrigation water, and a route
corridor's decking eight blocks lower. That is the authored layer, and the base
does not have it.

## Promoted

Copied to `artifacts/worldgen/alpha-0.1/base-terrain/`, beside the two authored
maps it is the parent of. Not committed — worlds are gitignored — but it is now
a named artifact rather than something you must know to find inside a gallery's
dimension folder.

## Lineage

```
TerrainGallery volume tv_ef56852eda10acc88342e5ee     seed 930012642
  → materialize                                       (a pristine world)
  → author_portfolio --apply                          32 placements, 178,358 blocks
  → routes --apply                                    6 corridors, 41,136 blocks
  → build_structures                                  8 team structures
  → artifacts/worldgen/alpha-0.1/consolidative-alpha  the frozen template
```

Under the runtime model the chain stops after the first step: the base is
copied, and everything below it becomes a manifest applied at match open.

## Two corrections to earlier claims

**The frozen map does contain the route corridors.** A straight-line sample
between a route's endpoints found decking in one world and not the other, which
looked like the shipped map missing 42,396 blocks of authored corridor. It was a
bad sample — corridors follow terrain and a straight line misses them. A sweep of
the whole authored area at stride 4 finds decking in 1.79% of columns in one and
1.81% in the other. They agree.

**The two worlds still differ**, byte-for-byte, and that is not explained. The
frozen artifact and `/tmp/profile-worlds/consolidative` are both authored and
both have routes. Whatever the difference is, the artifact is the authority: it
is what every match is built from and what the freeze record names.
