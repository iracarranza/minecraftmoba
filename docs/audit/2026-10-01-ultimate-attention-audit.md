# Ultimate attention audit

**1 October 2026.** Audits the developed roster ultimates against the three
footprints and the enemy-versus-ally attention distinction recorded in
`docs/reconciliation/2026-10-01-quarryman-and-ultimate-economy.md`. Every
description below is read from `classes.md` as it stands, not from the handoff
that prompted the audit. Where the two disagree, the repo text is used and the
disagreement is stated.

Footprints: **spatial** (world affected), **temporal** (how long everyone must
care), **attention** (who has to change what they were doing). Ally service is
the scarce one: "enemies must answer this" is legitimate power; "allies must
stop and help this player succeed" is a cost to be justified.

Levels are qualitative. Nothing here is a quota or a score.

## Findings that change the earlier picture

1. **Glistening Greenhouse is superseded.** `classes.md:1940` and `:1966` retire
   it in favor of **Grafter's Handbook**. The handoff's audit item 7 therefore
   audits a dead ultimate. Its open-question bullet at `:2076` is stale and is
   now marked historical.
2. **Grafter's Handbook has no ally service at all.** It grants Gardener the
   other two Cultivar forms for sequential growth. It is self-maximalization,
   not a "protect the progress bar" ultimate. Its unlock level, duration and
   persistence are open.
3. **Wither Golem carries no escort or payload behavior in the repo.** The text
   says only "temporary", "non-destructive to blocks", and that combat behavior
   is open. The handoff's risk (escorting it through a bespoke
   objective-destruction sequence) is **not present** and is a watch item for
   whoever specifies its behavior, not a defect today.
4. **Athanor Anatomb's completion stun is not the whole ultimate.** See below.
5. **Silk Road's recipients are not stated.** The handoff files it as
   empowerment that eases other players' fantasies. The repo text defines a
   procession of Retinue whose Work package is Saturation, Speed, Regeneration,
   Absorption and Instant Damage AoE, tied to Retinue size and Mastery, with
   exact lineup under a faint caution. It does not say who receives the effects.
   Until it does, that classification is unverified.

## Athanor Anatomb in detail

The risk is that optimal play becomes "everyone stop taking risks and protect
Bloodmason until the completion stun wins the fight". The text resists that:

- **Completion is Bloodmason's own channel work.** Hemorrhage and Teratoma
  drive the completion meter and cost Bloodmason Health. Allies are not
  required to contribute to completion.
- **The fight itself is the fuel.** Damage dealt to enemies in any room heals
  Bloodmason, *including allied damage*. The ally-optimal play is to fight, not
  to turtle, which is the opposite of a babysitting obligation.
- **Value is spread across the duration.** Allied protection breaks the
  architecture to heal allies, ascent geometry forces the fight upward, and
  enemy damage heals. The stun is the capstone of an environment that has
  already produced value.
- **The cost is carried by the caster.** Pushing completion lowers Bloodmason's
  Health, which raises the stun, and death ends the ultimate immediately. The
  death spiral is explicit and reactable.

Residual watch items, none a current defect: whether completion is reachable
without any ally contribution and without the stun being the only meaningful
payoff; whether the stun's missing-Health scaling makes allies defend a
near-dead Bloodmason from enemies outside the Anatomb; and the calibration of
"massive" so it does not become an automatic win button.

**Verdict: low ally service.**

## Roster table

| Ultimate | Class | Topology | Enemy attention | Ally service | Bespoke contest |
|---|---|---|---|---|---|
| Sinkhole (revised 27 Sep: any blocks, restored later, No-Build, zero drops) | Mole | Battlefield modification | Medium | Low | No |
| Grafter's Handbook | Gardener | Self-maximalization (capability) | Low | None | No |
| Wither Golem | Golem Master | Summoned force | High | Low (behavior open) | No |
| Covered With Diamonds | Kitfighter | Self-maximalization | Medium to high, local | Low | No |
| Procession / Silk Road | Merchant | Distributed empowerment (recipients unstated) | Low to medium | Low | No |
| A-Head of Schedule | Skeleton Crew | Summoned temporary force, commits by current workforce activity | Medium | Low | No |
| Rabbit's Lucky Foot | Lightfooted | Coordinated multi-actor strike | Medium to high, local | Low | No |
| Deathly Clutches | Daredevil | Self-maximalization | Low | None | No |
| Amber | Waxer | Targeted stasis (target rules open) | High, one target | Low | No |
| Gizmo of Absurdity and Untold Destruction!!! | Toolbox | Amplified prior Construction | Medium | None | No |
| Athanor Anatomb | Bloodmason | Persistent advantageous space | High | Low (see above) | Yes, a combat environment |
| Irresistible Buffet | Chef | Forced enemy participation | High for the captured | Low | Brief, local |
| Destroying Angel's Blessing | Fungal Assassin | Delayed local threat | High, local | Low | Somewhat |
| Talisman of Undying | Looming Talismaniac | Distributed rule change | Medium | Low | No |
| Concrete Shoes | Paver | Immediate event plus terrain | Medium | Low | No |
| Highly Irresponsible Racing Line | Chauffeur | Personal positional empowerment | Medium | Low | No |

Entries for Quarryman (Eternity Mountain) and Werewolf (Monster Howl) come from
the 30 September handoff, not from the repo, and are recorded in the
reconciliation, not here, until their sections exist.

## Cross-cutting observations

- **No developed ultimate asks allies to babysit a progress bar.** The nearest
  candidates (Anatomb, Silk Road, Talisman, Destroying Angel) each either place
  the cost on the caster, or build on prior work, or put the value in the
  duration.
- **Several depend on prior play, not on in-the-moment ally help**: Gizmo needs a
  built circuit, Destroying Angel and Talisman need a developed network, and
  Concrete Shoes uses Concrete already in the world or inventory. That is
  healthy: the ultimate's strength is earned beforehand.
- **Open items that decide the classification.** Talisman's open questions (radius
  versus block-following, which banners participate, whether activation consumes
  the whole network) "radically change the ultimate's economy", so its row is
  provisional. Amber's targeting and what an Ambered target can do are open.
- **Domain-level ultimates are the minority.** Only Anatomb creates its own
  combat environment, and Eternity Mountain would be the second. Both are
  intentional; this is the roster's current ceiling, not a recommendation to add
  more.
