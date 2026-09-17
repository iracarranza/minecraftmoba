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

## Step 2 — gate still pending

`:probe:build`: **PASS**. The diagnostic is a separate plugin artifact; it is not
bundled in the main plugin. Its reflective field lookup was checked against the
actual Paper 1.21.11 server jar with `javap`. Attachment still needs live verification.

The official launcher has a Java 1.21.11 profile. The disposable server initialized
successfully, but stopped at Mojang's EULA check. Authorization to set `eula=true`
has been requested. No world or player session has run on this test server yet.

### Static supporting evidence (NOT a live test)

Using Mojang's official 1.21.11 mappings and the locally installed client jar:

- `net.minecraft.client.Minecraft.handleKeybinds` calls `LocalPlayer.drop(boolean)`
  on consumed drop input for a non-spectator; it does not check the held item first.
- `net.minecraft.client.player.LocalPlayer.drop(boolean)` sends the player-action
  packet before checking whether the removed item is empty for its return value.
- Thus the inspected bytecode supports the expectation that empty-hand Q transmits
  a drop packet. This does **not** satisfy the requested listener-backed live gate.

Mapping artifact SHA-1 from the official version manifest:
`031a68bebf55d824f66d6573d8c752f0e1bf232a`.
Do not promote this gate to PASS without actual packet logs from vanilla input.

## Provenance measurements

Not measured. Step 4 has not been started because step 2 is unverified.
No per-chunk memory, tick cost, session growth, or reclamation claim is made.

## Live acceptance tests

All 11 rows in SPEC §10 (including 4b): **NOT RUN**.
Steps 3–6 have deliberately not been built past the verification gate.

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
