# Reconciliation — Quarryman, Werewolf, Difficulty-1 doctrine, and ultimate economy

## Authority and scope

Integrates the 30 September 2026 class-design handoff into `classes.md`, which
remains the canonical owner of class design. The roster audit that preceded this
is [docs/audit/2026-10-01-ultimate-attention-audit.md](../audit/2026-10-01-ultimate-attention-audit.md).
No implementation is implied, and nothing here is balance.

The handoff separated canon, strong direction and open calibration. This record
keeps that separation: everything is **Working** unless marked otherwise, formulas
are **Prototype/test**, and unresolved items are **Open**. Nothing in the handoff
was marked **Established**, so nothing is.

## Integrated

- **Difficulty-1 doctrine** (`classes.md` section 21): *simplify access to the
  verb, not the verb itself*; state management is not intrinsically high
  difficulty; the ten tests; and the three ways a class can enrich a verb.
- **Quarryman** (new section 28): greed doctrine, the four-variable Quarry Quota,
  Ceaseless Swing with Scan, Miner's Strike, and Eternity Mountain including the
  bilocation rule.
- **Werewolf** (new section 29): the state model only, with Monster Howl. No kit.
- **Ultimate attention and Domain Assertion** (section 27): enemy attention versus
  allied service, the three-footprint diagnostic, Domain Assertion as a degree,
  the guardrail that it not become the template, and non-mandatory synergy.
- Section 21's exemplar table notes now say Quarryman has a kit and Werewolf a
  state model.

## Superseded or corrected

1. **"Difficulty-1 classes are easy because their choices are always clear."**
   Refined: remove class-specific ambiguity, keep the archetype's native decisions.
2. **"Ultimates should generally allow simultaneous fantasy expression."**
   Refined, not reversed: maximize class fantasy, sometimes by competing to be the
   team's strategic center, provided the competition is for priority and not for
   permission to play. The earlier League, Marvel Rivals and Valorant guardrail
   stays as a warning, and the departure is recorded as deliberate.
3. **The earlier full-Iron armor figure of 20** and the resulting 1632 damage.
   Full Iron is 15 armor; the correct capped result is 2112 displayed.
4. **Glistening Greenhouse** as an audit target. It was superseded on
   11 September 2026 by Grafter's Handbook, and its open-question bullet in
   `classes.md` is now marked historical.

## Corrections found by auditing the handoff against the repo

- **Silk Road's recipients are not stated** in `classes.md`. The handoff's
  classification as empowerment that eases other players' fantasies is unverified.
  The text defines a Retinue procession whose Merchant must keep moving and keep
  paying.
- **Wither Golem has no escort or payload behavior** in the repo, so that risk is
  a watch item for its eventual specification, not a present defect.
- **Athanor Anatomb's completion stun is a capstone, not the whole ultimate.** The
  text puts the completion work on Bloodmason's own channels, makes allied damage
  heal Bloodmason, and spreads value across the duration. Audited as low ally
  service.
- **Sinkhole** was revised on 27 September to target any blocks including player
  construction, restore them afterward and leave a No-Build region. The handoff's
  description is consistent with the revision.

## Preserved

- Quarryman's payoffs are personal, never automatic team stats or objective damage.
- Stage is expectation, not power; there is no "lower quota" control; deliberate
  and involuntary failure are not distinguished.
- Early Miner's Strike is allowed to be worse than a sword, with no damage floor.
- Eternity Mountain's walking-out-of-the-cube does not cancel the terrain threat.

## Explicitly not decided

Quota timer, Stage floor and ceiling and demand formula; Scan width rounding and
anchoring and the hardness implementation; "strongest pickaxe" semantics; the
quota lifecycle on toggle; whether 24 stays the cap; Eternity Mountain's cube
formula, channel, progress mapping, return handling, visuals, health
synchronization and protected-block rules; every Werewolf threshold, duration,
ability and the presentation technique; how many Domain-level ultimates a roster
tolerates (the handoff leans toward a qualitative judgment, not a quota); and the
classification of Talisman of Undying, Amber and Silk Road, which depend on open
mechanics.

## Technical risks recorded, not resolved

- Bedrock is unbreakable in vanilla, so Eternity Mountain's Bedrock target and any
  hardness reduction need custom break progress.
- A player occupies one world, so Quarryman's bilocation needs a stand-in body.
  Which body is the real player is open.
- The remaining roughly sixty classes should distinguish an intentional Domain
  Assertion from an accidental "protect my progress bar" ultimate.
