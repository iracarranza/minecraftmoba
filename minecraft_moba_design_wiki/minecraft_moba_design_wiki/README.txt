Minecraft MOBA Design Wiki
===========================

START
1. Unzip this folder anywhere on your computer.
2. Open index.html in a browser.
3. Use the navigation bar to move between design areas.

CLASSES
classes.html is now a small local class database:
- Browse/read the canonical Mole, Gardener, and Golem Master drafts.
- Select "Edit selected class" to revise a class directly in the browser.
- Create new classes from the New Class section.
- New classes and browser edits are stored in localStorage.
- Export/import the entire class library as JSON.
- Canonical source drafts are embedded as <template> blocks in classes.html.
- class-library.js provides the library/editor behavior.

OTHER DESIGN PAGES
Most long-form page content is directly editable in the browser and saves through localStorage.
Use the page-level Export JSON / Import JSON controls for backups.

CURRENT SYNCED DOCS
- classes.html    <- CLASSES.md
- objectives.html <- OBJECTIVES.md
- map.html        <- latest MAPS.md

IMPORTANT
Browser edits do not rewrite the physical HTML source file. Export JSON for portable browser
data, or edit the HTML/JS source in a text editor when you want changes committed to the files.

No server, package manager, install, or build step is required.


IMPORT DESIGN
=============
Open import.html or choose "Import Design" in the navigation.

Recommended workflow:
1. Finish a Minecraft MOBA design discussion in ChatGPT.
2. Ask ChatGPT for the "wiki import block".
3. Paste the JSON block into Import Design.
4. Click Parse & Preview.
5. Check the destination and whether the import will create or update an entry.
6. Click Apply Import.
7. Open the destination page.

The importer accepts either raw JSON or JSON surrounded by Markdown ```json code fences.

Supported types:
- class          -> Classes
- objective      -> Objectives
- map_archetype  -> Map / World
- mechanic       -> Unique Mechanics
- item           -> Items / Progression
- balance_note   -> Balance Notes
- idea           -> Inbox / Ideas

Class behavior:
- A new class becomes a new entry in the existing Classes browser/editor.
- An import with id "mole", "gardener", or "golem-master" updates that canonical class locally.
- Imported classes can then be edited normally on classes.html.

Other design types:
- They appear automatically under an "Imported designs" section on the correct page.
- Re-importing the same type + id updates the stored entry instead of creating duplicates.
- Local imported entries can be removed from their destination page.

Safety:
- Pasting alone changes nothing.
- Parse & Preview changes nothing.
- Data is changed only after Apply Import.
- All imported data is localStorage data in your browser; it does not rewrite the HTML source.
