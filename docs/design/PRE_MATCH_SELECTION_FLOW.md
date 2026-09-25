
---

# Amendment — the stand belongs to the player, not the class

**Status:** Working, 25 September 2026. Amends §2, §3 and §4 above.

## Why the original model does not survive the target roster

§2 gives four reasons for a physical hall. One of them —

> **The roster teaches itself.** Players walk past nine classes to reach the
> one they want.

— is true at nine and **false at fifty**, which is the roster size the
long-term catalogue points at. Walking past fifty stands teaches nobody
anything, and the aesthetic cost of a fifty-station rack is the visible
symptom of an argument that has stopped holding.

That reason is therefore **retired rather than traded away**. It expired with
roster size. The other three survive the change below.

## The inversion

A stand represents a **player**, not a class. The hall holds a fixed **28
stands** in three sections:

| section | stands |
|---|---|
| your team | 7 |
| enemy team | 7 |
| banned | 14 |

Fourteen banned stands is one per player, matching "every player may ban".
The count is **fixed regardless of roster size** — nine classes or five
hundred — which is the property the class-per-station model could not have.

Banned classes keep a physical presence, so §3's global ban survives intact: a
banned class is not merely refused, it is *standing in the banned section
wearing its own gear*, visible to both teams at once. Contention stays visible.

## The verbs

| command | when | effect |
|---|---|---|
| `ban <class>` | your ban turn | bans globally; the class appears in the banned section |
| `hover <class>` | **any time, including off-turn** | your stand wears that class; nothing is committed |
| `pick <class>` | your pick turn | locks in immediately |

`pick` is binding on its own. The right-click-to-lock verb proposed during
design is dropped: a second confirming action adds a failure mode (a player
who typed and did not click) without adding information anyone needed.

## What this changes about the turn — taken deliberately

§4 decides that **the turn is the ability to move**. With a stand of your own
and a chat verb, movement is no longer the selection act, so that sentence no
longer describes enforcement. It is amended to:

> **The turn is the ability to act.** Enforcement is on the verb: `ban` and
> `pick` are refused off-turn. The ghost state remains the *legibility*
> mechanism, which §4 already identified as its strongest property — "turn
> state stops being an invisible permission and becomes the most visible
> property in the room."

Everything §4 asked the ghost state to do still holds. Acting players are
solid and may walk; movement becomes idle and warmup behaviour rather than
selection. Ghosts still cannot obstruct, still broadcast intent — `hover`
works off-turn precisely so they can — and solidifying still marks the turn
boundary without a timer announcing it.

## What `hover` improves

An off-turn player who hovers has **declared a preference in public**. That
makes a better timeout rule than the one first implemented, which assigned the
first available class:

> On timeout, a player receives **the class they are hovering**, if it is
> still available; otherwise the first available. An assignment then follows
> the player's own stated intent wherever one exists, and the spec's
> requirement that a timeout be "a default assignment rather than a stall,
> visible in the hall" is satisfied by the stand already wearing it.

## Bounding and obstruction, resolved by reading rather than deciding

§4 lists ghost bounding as open — physical, soft push-back, or return
teleport. **No mechanism is needed.** Ghosts pass through *"everyone else"* —
players, not blocks — and `LobbyHall` already builds an enclosed room whose
tests assert enclosure, because "a gap you can walk out of" is one of the
three ways a lobby fails. Terrain collision plus the existing walls bound
flight for free.

Obstruction is likewise already answered: ghosts cannot obstruct by
construction, and with a small pick window only one or two players are solid
at once. That removes one of the two reasons §2 offers for separating the
teams, leaving readability — which the facing-gallery layout above serves.

## Still open

Hall layout beyond the three sections; whether `hover` is refused for a banned
or taken class or merely shows it as unavailable; clickable-chat presentation
over the typed minimum; ghost visual treatment; and everything §5 and §6 leave
open.
