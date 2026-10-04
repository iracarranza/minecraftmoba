# Player and debug compendiums

Both written books are issued to all online nonparticipants, including visitors
whose enrollment is paused. Preferred hotbar slots are 7 and 8 (zero-based 6/7).
If occupied, another empty storage slot is used, reserving the configured
cast-mode comparator slot. Existing items are never displaced. A full inventory
defers issuing the missing book until space is available.

Match participants lose the issued books, including during draft preparation;
they return after reset. Tagged duplicates are removed and missing books are
reissued. Ordinary books with the same titles are untouched. Issued books cannot
be dropped, transferred into containers or swapped into the offhand. Death drops
exclude issued books. Distribution is checked once per second and on join,
respawn and player synchronization. No enrollment is required to receive them.

Right-click to read, or use `/moba compendium player` and
`/moba compendium debug` to open a virtual copy without inventory space.
The player guide is basic: enrollment, class catalogue, rewards and casting.
Clickable preference entries call `/moba settings cast quick|hold|double`.
These use the existing persistent setting, with the same outside-match-world,
enrollment and feature-enable checks as the comparator. Quick remains default;
Hold release remains an input-silence heuristic.

The debug book has clickable contents and a categorized command reference:
navigation, draft, lab setup/authoring, match control, player tuning, Worksites,
renewables, contributions, infrastructure, HUD probes and diagnostics. Section
links stay inside the book. Command links show exact syntax on hover. Navigation
can run directly; other entries suggest the command for editing/confirmation
in chat, including arguments, world mutations and status reports. This avoids
an unsolicited chat dump while browsing. Command execution still uses the normal
permission checks; giving the debug book to everyone grants no administrative
access. Returned command results retain the command's existing output behavior.

The command reference lives in `CompendiumContent`; update it when command
syntax changes, and increase the book revision in `Compendiums` when issuing
new item contents. Dynamic IDs and detailed command-specific semantics remain
in their existing menus/handlers. The debug book does not replace them.
