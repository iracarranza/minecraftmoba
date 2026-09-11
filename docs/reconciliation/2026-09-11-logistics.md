# Reconciliation — Infrastructure Benefits and Logistics redefinition

Date: 2026-09-11

## Source

WAMS Handoff `[WB]`: *Infrastructure Benefits + Logistics / Supply Line
Redefinition*, sections 1–26, supplied complete.

## Superseded

**1. Supply Line establishment model.** The origin-first sequence — Infrastructure
Mode, select source container, select destination container, prove transport —
is no longer the defining model. Replaced by demonstrated destination-oriented
delivery: the destination is strategically intentional, the origin is observed
evidence. Recorded as [HISTORICAL] in `infrastructure.md`, `classes.md` and
manuscript §6.5; not deleted.

**2. Flow Rate.** The single formulation "Flow Rate is proportional to Flow
Weight divided by Transit Time" is superseded by two separate quantities:
Flow Weight (authored by transport method, items per pulse) and Item Rate
(derived from demonstrated Transit Time, pulse frequency). The handoff is
explicit that these must not collapse into one throughput stat.

**3. Construct intrinsic benefit framing.** Occupation / sustainment efficiency
is demoted to [HISTORICAL] framing of the same unresolved slot. Structural
Integrity is now the current preferred intrinsic benefit. This closes a
long-standing open question in a specific direction; it does not yet fix a
numerical effect.

## Retained and reconciled, not replaced

Flow Weight · Transit Time · directionality (reverse still needs its own proof)
· persistent operation without repeated player journeys · physical disruption
of nodes, carriers, rails and waterways · represented flow that need not render
every item · team-relative integration and capture · the four-identity
extent/relation matrix (Supply Lines stay relation-based, but for delivery
reasons rather than endpoint selection).

## Added

| Handoff § | Destination |
| --- | --- |
| 1 Benefit symmetry | `infrastructure.md`; manuscript §6.15 |
| 2 Structural Integrity | `infrastructure.md`; manuscript §6.2.4 |
| 3 Infrastructure protection | `infrastructure.md`; manuscript §6.2.5 |
| 4 Logistics redefined | `classes.md`; manuscript §6.16 |
| 5–7 Destination-oriented Supply Line | `infrastructure.md`; manuscript §6.5 |
| 8–10 Flow Weight / Item Rate | `infrastructure.md`; manuscript §6.5.1 |
| 12–15 Corridor authoring and resolution | manuscript §6.5.5 |
| 16–17 Engineered vs living | manuscript §6.5.6 |
| 11 Persistence | manuscript §6.5.7 |
| 19–23 Classification, capture, geography, feasibility | manuscript §6.5.8 |
| 18 Establishment grammar | folded into §6.5 |
| 26 Open questions | manuscript §12.6 |

## Deliberately not resolved

Structural Integrity strength; whether mining, explosions and abilities are
affected differently; a narrow definition of "infrastructure-relevant block";
Structural Integrity on change of control; corridor-authoring input; qualifying
cargo movement; pulse-interval derivation; Flow Weight values; branching and
rerouting; permitted carrier deviation; obstruction after establishment; any
engineered-vs-living balancing modifier; Known/Designated Location vocabulary;
datapack navigation feasibility.

No material-specific Structural Integrity table was created, per §2.
No Logistics node menu was added to canon, per §22.

## Note on feasibility

Handoff §23 asks that datapack feasibility not drive the design. The design
rule is recorded as written. The separate empirical finding from the input
prototype — that vanilla cannot transplant one mob's navigation onto another,
and that the `input` predicate did not register on 1.21.9 — is implementation
evidence recorded in `implementation/datapacks/README.md`, not a design change.
