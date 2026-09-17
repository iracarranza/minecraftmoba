# Phase 1 validation status — 2026-09-17

## Environment

- Target selected before first commit: Paper **1.21.11**, Java **21**.
- Official Paper build **132**, commit `c5eb079`.
- Paper download SHA-256: `5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba` (verified).
- Runtime: Microsoft OpenJDK 21.0.7, macOS arm64.
- Gradle 8.14.3.
- Source isolation: `codex/phase1-plugin`, based on `fe5b534`.

## Step 1

`gradle wrapper test build --no-daemon`: **PASS**.
Two JUnit tests verify binary round-trip, UUID rejection, omission of transient mode,
capacity growth/caps, choice folding, and reset-to-baseline recomputation.
This is not evidence for any live acceptance test.

## Step 2 — PASS (live)

Paper 1.21.11 build 132, vanilla 1.21.11 client, player `inspiralc` in survival.
The listener attached before `packet_handler` at 11:34:30 server-log time.
Two observed runs each produced one swap control, five `DROP_ITEM`, and five
`DROP_ALL_ITEMS`: **20 drop packets total, all with mainhand=AIR and offhand=AIR**.
The first run was 11:34:41–11:34:49; the second was 11:35:18–11:35:22.
No Bukkit drop event was raised. The probe cancels drop/swap packets before vanilla
processing and never writes inventory. See `empty-hand-drop.log` for the excerpt.

**Answer: yes, the client sends both drop actions with an empty mainhand.**
This clears the prerequisite for steps 3–6; it is not acceptance test 4b, which
requires the eventual ultimate ability to execute.

Mojang's official client bytecode independently supports this result: the drop
method sends the action before inspecting the empty item return value.

## Provenance measurements

Not measured. Step 4 has not been started yet.
No per-chunk memory, tick cost, session growth, or reclamation claim is made.

## Live acceptance tests

All 11 rows in SPEC §10 (including 4b): **NOT RUN**.
Steps 3–6 are next after the now-passed verification gate.

## Implementation details / unresolved decisions

- Administrative commands target online players because Player PDC is the storage
  authority. `setlevel` clears progress toward the next level; `setclass ... none`
  clears class; reset restores a fresh data record. These command conventions are
  documented for review, not new game design.
- Choices remain historical when level is lowered; their higher-level effects are
  ignored until eligible. Reset removes choices. No reward content ships.
- Vanilla integer bounds, format-version identifiers and level-one/zero baselines
  are structural constants; balance values and growth tables are config.
- A late-stage decision will be needed for an already occupied offhand on enrollment:
  permanent-map issuance cannot replace, move, or discard an existing item under
  invariant 1. No map issuance policy has been invented or implemented.
- Test ability class assignments and numeric defaults beyond §2 are not supplied
  by the spec. They will need user direction before step 5.
