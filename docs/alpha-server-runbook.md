# Alpha play server runbook

Everything here is macOS Terminal (Spotlight → type `Terminal` → Return).

The server at `/private/tmp/alpha-server` runs **detached**: there is no window
to switch to. `start-alpha.sh` pipes a file into the server's stdin, so the
"console" is a file you append to:

    tail -n0 -F cmds | java -jar paper.jar --nogui

Anything appended to `cmds` is executed as a console command, with no leading
slash.

---

## Send a console command

```bash
echo "moba match open" >> /private/tmp/alpha-server/cmds
```

The output does not come back to your terminal — it goes to the log. To watch
the log live in a second Terminal tab (⌘T):

```bash
tail -f /private/tmp/alpha-server/logs/latest.log
```

Stop watching with ⌃C. That only stops the `tail`, never the server.

---

## The full redeploy cycle

A new plugin jar needs a full server restart. Nothing less works: `/reload`,
`moba match reset` and reconnecting all leave the old code loaded.

### 1. Stop the server

```bash
echo "stop" >> /private/tmp/alpha-server/cmds
```

### 2. Confirm it is actually down

```bash
pgrep -f paper.jar
```

No output means it is down. If it still prints a number, wait a few seconds and
run it again — saving the world takes a moment.

### 3. Build the new jar

The JDK is not on your PATH, so `JAVA_HOME` has to be set for this one command:

```bash
cd ~/minecraftmoba/.claude/worktrees/datapack-class-mechanics-3735c1/implementation/plugin && JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew build
```

### 4. Deploy

```bash
cd ~/minecraftmoba/.claude/worktrees/datapack-class-mechanics-3735c1/implementation/plugin && ./deploy.sh
```

This copies the jar **and** `config.yml`. It refuses if a server still has the
jar open — see the warning at the bottom of this file for why that refusal
exists.

### 5. Start it again

```bash
cd /private/tmp/alpha-server && nohup ./start-alpha.sh > /dev/null 2>&1 &
```

`nohup ... &` is what makes it survive closing the Terminal window. Give it
about twenty seconds, then connect to `localhost:25599`.

### 6. Set the match up again

The world instance is rebuilt from the template on `open`, so match state never
survives a restart:

```bash
cd /private/tmp/alpha-server && for c in "moba match open" "moba match add inspiralc north" "moba setclass inspiralc test" "moba match start"; do echo "$c" >> cmds; sleep 1; done
```

---

## Checking the plugin actually loaded

```bash
grep -iE "MinecraftMoba.*(Enabling|Disabling)|ERROR" /private/tmp/alpha-server/logs/latest.log | tail -20
```

---

## Never copy a jar into a running server

If you take one thing from this file, take this.

Paper opens the plugin jar lazily. Copying over it while the server runs leaves
already-loaded classes working and makes every not-yet-loaded class permanently
unloadable. **The server does not crash.** It keeps running and throws
`NoClassDefFoundError` from whichever code path is touched first, which looks
like a game bug and not a deploy mistake:

    java.lang.NoClassDefFoundError: com/minecraftmoba/plugin/Ability$AbilityContext

That happened on 2026-09-21 and surfaced as "locked out of respawning, mobs
still attacking me" — a symptom with no visible connection to a file copy, and
an hour of diagnosing code that was fine.

`deploy.sh` now refuses instead of warning. If you ever see a
`NoClassDefFoundError` naming a `com.minecraftmoba` class in the log, the
diagnosis is this and the fix is a full restart, not a code change.
