# Prototype/test: reusable map scoop lab

`/moba lab start` now enters the separate `moba_lab` debug room. It requires
`moba.admin` and an idle match controller. Use the Classes and Map scoops
pedestals (or inventory menu), choose starting level/team, then Launch test.
The room protects blocks and uses Adventure mode.

Command equivalents:

```
/moba lab start
/moba lab class <registered-class-id>
/moba lab map <map-id-or-list-number>
/moba lab level <level>
/moba lab team <north-or-south>
/moba lab play
/moba lab end
/moba lab leave
```

`maps`, `classes`, and `status` expose the catalog/setup. Only the launching
tester can end their session. Ending returns to the room with the selection
retained; leaving returns to the normal lobby. This is a single-session lab:
the shared gameplay controller cannot run a standard match simultaneously.
It refuses to interrupt a draft or match, and standard match commands refuse
to operate while a lab world is active.

Lab templates live in `alpha.lab.mapsDirectory` (default `lab-maps`, relative
to the server directory), separately from the production pool. Launch verifies
the terrain fingerprint and copies the template into `moba_lab_match`. It
binds the scoop's fountains, objectives, Lair, Worksites and renewables through
the existing match systems. It does not claim or retire a production map, and
does not apply the frozen Alpha map configurations. End unloads the disposable
lab world; the next launch copies pristine terrain again.

Install the three certified October 2 scoops after building the plugin:

```
python3 tools/worldgen/install_lab_maps.py /private/tmp/alpha-server --stage-plugin
```

This preserves live configuration and stages the jar in Paper's `plugins/update`
directory for the next restart. It refuses to overwrite different existing
scoops. If configuring a different catalog path, supply `--maps-directory`.
Room/catalog/instance settings are under `alpha.lab` in the default config.
These maps and room are test fixtures, not an established balance decision.
