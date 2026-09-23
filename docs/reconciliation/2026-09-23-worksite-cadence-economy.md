# Worksite cadence + economy reconciliation

**Date:** 2026-09-23
**Source:** `docs/worksiteandlairupdateagain.md`
**Scope:** the Worksite economic ladder, the Factory progression, and Worksite
activation permanence. The general Worksite architecture is preserved unchanged.

---

## 1. What changed

### The ladder

| | Superseded (four tiers) | Current (three tiers) |
| --- | --- | --- |
| Mining | Copper → Iron → Diamond → Ancient Debris | **Iron + Coal → Diamond + Lapis → Ancient Debris + Diamond** |
| Factory | Blast/Smoker → *TBD* → Enchanting → Smithing | **Blast Furnace + Smoker → Enchanting Table → Smithing Table** |

The cadence has three Worksite nights, so the economy has three tiers. The
Copper tier is removed because Copper is abundant in the opening economy and its
identity is *replaceable / field-standard metal*; Copper's broader role in the
world economy is untouched. The TBD intermediate Factory is removed rather than
filled — no workstation was invented to occupy it.

Escalation is **VOLUME → CAPITAL → APEX CONVERSION**, not more blocks each time.
A later Mining Site may contain fewer physical ore and matter far more.

### Activation is permanent

Sunset is an activation event, not an availability window. Sunrise closure is
superseded in doctrine, runtime, config and tests. Worksite geography
**accumulates**: a tier III night does not retire the tier I sites.

---

## 2. Canon / design reconciliation

- `objectives.md` §17C — the tier block rewritten with the three paired
  identities, the escalation shape, the supersession of Copper / TBD factory /
  Diamond+Gold / "Worksite III OPEN", the anti-multiplier specialization
  doctrine, and the provisional quantities labelled NON-CANON.
- `objectives.md` §17C — new sections *Worksite activation is permanent*
  (lifecycles for Mining Site and Factory, accumulation, "night 1/3/5 says when,
  not how long"), *Worksite capitalization and contest* (the +1 Infrastructure
  Slot experimental reward with its full caveat), and *Worksites and the Lair
  remain distinct*.
- `objectives.md` §11 — "at sunrise the active Worksites close" marked
  HISTORICAL and removed from the current-facing sentence.
- `objectives.md` §17A — the day/night loop no longer closes Worksites at dawn.

## 3. Implementation changes

- `OpportunityCadence.WorksiteTier` now carries the economic identity:
  `miningSite()`, `factory()`, `economicRole()`, `identity()`. **The runtime
  understands the three identities.** It does not know any quantity.
- `Worksites.onSunrise()` **deleted** — not emptied. An empty hook is an
  invitation to put closure back into it.
- `Worksites.State.EXPLOITED` → `DEPLETED`, canon's name and the accurate one.
  `PARTIALLY_DEPLETED` is deliberately absent: nothing manifests physical ore,
  so it would be a label with no mechanism.
- `Worksites.exploit` → `deplete`; `/moba worksite exploit` retained as an alias.
- `Match.onSunrise` announces that activation is permanent instead of reporting
  closures.
- `Match` night announcements name the tier's identity and economic role.
- `config.yml` documents the identities as code-side doctrine and states that
  there is deliberately no deactivation or expiry key.

### A defect permanence created

`readiness.MIN_WORKSITES` was **3**, one per night. That was correct only while
sunrise returned an activated Worksite to Dormant and refilled the eligible pool.
Under permanence the pool drains monotonically: at the configured 2/2/3
activations the match consumes **seven** distinct sites, and a three-site map
would open nothing on night 5. Raised to 7, with a test.

## 4. Provisional calibration fixtures (NON-CANON, unchanged in status)

~40–50 Iron at I; ~8–12 Worksite Diamond against ~14–18 ordinary; ~8–12 Ancient
Debris against ~4–8 ordinary (≈3–5 Netherite upgrades, since Fortune does not
multiply Debris); ~6–10 secondary Diamond at III. Retained as hypotheses in
`objectives.md`, labelled, and **not** promoted to any constant in code or config.

## 5. Stale assumptions found and how each was handled

| Found | Handling |
| --- | --- |
| Copper Mining Site, "~8–12 Copper + Coal" | Superseded in `objectives.md`. Left intact in the dated 2026-09-14 reconciliation record, which preserves history. |
| Four Worksite tiers | Superseded; three tiers asserted in code and tested. |
| 6m/18m/30m/42m sequence | Historical-only, in the 2026-09-14 record; already superseded by §17C's cadence. Not revived. |
| TBD intermediate Factory | Removed, not filled. |
| Diamond + Gold as the guaranteed tier II backbone | Superseded by Diamond + Lapis. Gold not deleted from the economy. |
| "Worksite III — OPEN" | Resolved: Ancient Debris + Diamond / Smithing Table. |
| Anvil in the tier II identity | Dropped, and recorded as [OPEN] rather than silently deleted — see §7. |
| Sunrise deactivation | Superseded everywhere: doctrine, runtime, config, tests. |
| Worksite IV / Night 4 economic site | None found. |
| Obsolete Apparatus terminology | Conflict recorded, not resolved — see §7. |
| Infrastructure Slot first-capture reward | Checked against current `infrastructure.md`; nothing supersedes it. Retained **with** the handoff's caveat about how little Infrastructure gameplay exists. |

## 6. What the runtime does and does not know

**Knows:** that Worksite I is Iron + Coal with a Blast Furnace and Smoker, II is
Diamond + Lapis with an Enchanting Table, III is Ancient Debris + Diamond with a
Smithing Table; the economic role of each; that activation is permanent; that
three nights consume seven sites.

**Does not know, by design:** how much of anything manifests, where within a site
it manifests, what physically qualifies as a capitalizing Construct, what a
qualifying Factory supply relationship is, or Factory throughput. These remain
seams. No `Material` constant for any tier facility or ore is placed anywhere,
and a test enforces that.

## 7. Unresolved — reported, not resolved

1. **"Apparatus" vs "Construct."** The handoff says "the apparatus model is
   obsolete," yet also requires "a qualifying **Construct** capitalizes the
   Mining Site." `objectives.md` §12's three layers (Core / functional
   multiblock / facility) are a Construction-established topology, not the ore
   dispenser the handoff rules out — so they satisfy the second requirement and
   contradict the first only in vocabulary. §12 is retained unchanged with the
   conflict recorded in place. Whether "apparatus" is merely renamed or the
   layered model is withdrawn is a design decision, not an editorial one.
2. **The Anvil.** The handoff's Factory progression is Blast/Smoker → Enchanting
   → Smithing, with no Anvil. It does not say the Anvil was rejected; it simply
   does not mention it. Dropped from the tier II identity and marked [OPEN].
3. **Per-tier activation counts** remain a NON-CANON fixture (2/2/3), and
   `MIN_WORKSITES = 7` is derived from it — so changing the fixture changes a
   readiness gate.

## 8. Validation

Java **258** tests, 0 failures. Worldgen **402** tests, 0 failures.

**This is not balance validation.** Nothing here was played, no quantity was
measured, and passing tests say only that the runtime states what doctrine says
and refuses to state what doctrine leaves open.


---

# Addendum, same day: opening access is not Routes

Recorded here because it corrects a diagnosis made during the first live play
session, not because it belongs to the Worksite pass.

While playing the generated map, two things were reported missing: the Aether
Fountain and any Routes. The Fountain was a real defect (see the live-play
notes). The Route diagnosis was **wrong**, in both halves:

- *"Exploration Routes are player infrastructure, so zero at match start is
  defensible"* — true only of the formal Exploration Infrastructure object.
- *"generated maps and the frozen map disagree about what a map owes on day
  one"* — wrong. Current Hinterland doctrine already says the generated map
  **does** owe opening access. What was unsettled is whether that authored
  access shares the `Route` identity.

The correct layering is Core -> authored base exits / opening access -> compact
Opening Hinterland -> natural Wilderness -> player-recognized Exploration
Routes. The map says "here are sane ways out"; Exploration says "we have learned
how to move through this wilderness."

**The compiler gap is opening-access certification, not Route generation.** A
generic `routes` stage must not be added if it would author formal Exploration
Infrastructure before play; the Homebase/Hinterland stages are where each Core's
exits into its own Hinterland belong. The gate is **equivalent exit capacity,
not identical exit geometry** — one base may need no intervention and another a
short terrain-conforming connection, and carving matching roads to equalise a
number is the wrong instinct.

See maps.md, *Opening access*. Whether authored opening access should carry the
formal `Route` name remains [OPEN]. Not implemented.
