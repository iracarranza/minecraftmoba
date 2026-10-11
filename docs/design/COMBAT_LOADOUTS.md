# Combat chamber loadouts and testing mode

**Prototype/test. Built 11 October 2026.** Two things the combat chamber lacked: somewhere to build
and keep sets of gear, and a way to test with nothing on the tester but that gear and a way out.

## Loadouts

A **loadout** is the 36 inventory slots and four armor pieces, nothing else. Each tester has five,
named A to E, with one selected. They persist across restarts (`plugins/MinecraftMoba/lab-loadouts/
<uuid>.yml`, Paper's own item serialisation) and survive the dummy being rebuilt.

**The editor** is the ender chest on the control deck, beside where the tester spawns. Right-click it
and a 54-slot window opens:

```
rows 0-3   the 36 inventory slots (hotbar first); the last hotbar slot is locked
row 4      helmet, chestplate, leggings, boots
row 5      next loadout | copy my inventory in | copy the dummy's gear in | empty this loadout | done
```

Items you move in are the loadout and it saves when the window closes or you switch loadouts.
Moving takes items out of your own inventory, as a chest does. The two "copy" controls COPY: they read
your inventory (seeing through the chamber's menu, never copying the menu itself) or the dummy's, and
leave the originals where they are.

**Applying a loadout** is a separate step from editing it:

| Verb | Command | What it does |
|---|---|---|
| Next loadout | `/moba lab combat loadout next` or `select <1-5>` | choose which one is current |
| Give it to the dummy | `loadout dummy` | dresses the dummy; it keeps this across resets and ghost respawns |
| Empty the dummy | `loadout dummy clear` | empties it, and it stays empty |
| Start testing | `test` | gives it to YOU, in testing mode (below) |
| Give it to me again | `loadout me` | refills while testing, for consumed or dropped gear |

All of these are on the hotbar (a Loadouts page, and Start testing on the root) and on the control deck.

## Testing mode

`Start testing` (hotbar, deck button, or `/moba lab combat test`) does this:

1. Your real inventory is **parked on disk first** (`<uuid>.restore`), then in memory. If it cannot be
   written, testing does not start and nothing is cleared.
2. The chamber menu comes off your hotbar. Your 36 slots and armor are emptied and replaced by the
   selected loadout.
3. **Exit testing** (a barrier, last hotbar slot) is the only other item. Right-click it, click it in
   your inventory, or `/moba lab combat test off`: your real inventory comes back exactly and the menu
   returns. It cannot be dropped, moved or swapped.

Everything is still reachable by command while testing (`log`, `reset`, `cast`, `loadout me`). A tester who
dies keeps the loadout. Leaving the chamber, quitting, or the plugin stopping all restore the real
inventory, and if the server died mid-test the parked file is restored at the next join.

## Why the offhand is not in a loadout

An enrolled player or body carries the plugin's own map in the offhand, and the plugin pauses their
enrolment (class, abilities, progression) as soon as the offhand holds anything else. A loadout that
put an item there would silently disable the class under test. The first version had an offhand slot and
did exactly that, found by the acceptance run: the tester came out un-enrolled. The offhand is never
read, cleared or written by a loadout or by the parked snapshot, and `PlayerInventory#clear()` is not
used because it clears the offhand too. (Kitfighter's Offhander, which exchanges the carried item with
the offhand, was built against that same constraint.)

## A defect found and fixed on the way

`/moba lab combat equipment`, and the **Equipment / controls** hotbar item and deck button that came
with the Chef lab pass, did nothing inside the chamber: the verb was handled only when the tester was NOT
in a session, so pressing it in the chamber fell through to the usage message. It now runs. (Equipment
mode is the lighter cousin of testing mode: it swaps only the hotbar and keeps the menu's controls.)

## Verified, and not

Live on Paper 1.21.11 build 132, with a scripted tester and dummy: the station builds and opens the editor;
items save, reopen and persist to disk; selecting loadouts leaves the others alone; both copy controls
copy the right items and never the menu; the dummy is dressed exactly, keeps it across a reset, is emptied
and stays empty; testing parks the inventory on disk, leaves exactly the loadout and the exit item, refills,
guards the exit item, exits with the real inventory identical and the menu back; leaving or quitting while
testing restores it; a parked file is restored at join. The acceptance run is 192 of 192 and the Java suite
passes.

**Not verified:** that a person can use the editor window with a mouse (shift-click, drag, number keys): the
rules for those clicks are written but the run drove it by setting slots and sending click events. The
quit and join handlers were exercised through their public entry points, not by a real disconnect. Armor in
a loadout was exercised on the dummy and in the editor, not under a real fight. The Exit testing item's
right-click path was exercised with a synthetic interact event.

**Open:** whether loadouts should be shareable between testers, and whether a loadout should be able to
carry the dummy's class kit items (Chef's shovel and food, for instance) as named presets instead of by hand.
