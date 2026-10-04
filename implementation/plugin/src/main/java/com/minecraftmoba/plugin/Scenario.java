package com.minecraftmoba.plugin;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * A scenario: who is present, and what happens on which tick.
 *
 * Data, not code. Adding a scenario is a config edit, which is what keeps them
 * cheap enough to actually write -- and it keeps {@code ConfigKeysDefinedTest}
 * and the rest of the config discipline applying to them.
 *
 * <h2>Ticks are absolute</h2>
 *
 * A relative timeline makes a step's meaning depend on how long the previous
 * one took, so a step that silently runs long turns an assertion into a race.
 * Absolute ticks mean a scenario reads the same every run.
 *
 * <h2>Every step names its actor or subject</h2>
 *
 * There is no implicit "the player", because the entire point of the harness is
 * that there is more than one.
 *
 * <h2>A check carries its reason</h2>
 *
 * {@code because} is part of the assertion rather than a comment, so a failure
 * reports the rule it believed in. The plugin's own refusals do this; a test
 * that says only "expected false, got true" makes the reader re-derive what was
 * being protected.
 */
public record Scenario(String id,
                       String description,
                       List<Member> roster,
                       List<Step> timeline,
                       boolean requiresConnectedBodies) {

    /** A participant the scenario needs before it can start. */
    public record Member(String id, Team team, String classId, int level,
                         double x, double y, double z) {}

    /** One timeline entry. Either something happens, or something is checked. */
    public sealed interface Step permits Act, Check {
        long tick();
    }

    /** An action performed by a named actor. */
    public record Act(long tick, String actor, String verb, Map<String, String> args) implements Step {}

    /**
     * An assertion about a named subject.
     *
     * {@code window} allows a check to hold anywhere in a span of ticks rather
     * than on one exactly, which anything involving a projectile or a scheduled
     * effect needs -- a single-tick check on a travelling arrow is a race, not
     * a test.
     */
    public record Check(long tick, long window, String assertion, String subject,
                        String because, Map<String, String> args) implements Step {
        public long lastTick() { return tick + Math.max(0, window); }
    }

    /**
     * Read one scenario from config.
     *
     * Refuses rather than defaults. A scenario with a missing roster entry or
     * an unnamed actor is a scenario that would run and prove something other
     * than what its author meant, and that is the failure mode this harness
     * exists to avoid.
     */
    public static Scenario load(String id, ConfigurationSection section) {
        Objects.requireNonNull(section, "No scenario section: " + id);
        String description = section.getString("description", "");
        boolean needsConnected = section.getBoolean("requiresConnectedBodies", false);

        List<Member> roster = new ArrayList<>();
        var rosterList = section.getMapList("roster");
        if (rosterList.isEmpty()) throw new IllegalArgumentException("Scenario " + id + " has no roster.");
        for (var raw : rosterList) {
            String memberId = str(raw.get("id"), () -> "A roster entry in " + id + " has no id.");
            Team team = Team.valueOf(str(raw.get("team"), () -> memberId + " has no team.")
                    .toUpperCase(Locale.ROOT));
            String classId = str(raw.get("class"), () -> memberId + " has no class.");
            int level = raw.get("level") == null ? 1 : ((Number) raw.get("level")).intValue();
            List<?> at = (List<?>) raw.get("at");
            if (at == null || at.size() != 3)
                throw new IllegalArgumentException(memberId + " needs an 'at' of three coordinates.");
            roster.add(new Member(memberId, team, classId, level,
                    ((Number) at.get(0)).doubleValue(),
                    ((Number) at.get(1)).doubleValue(),
                    ((Number) at.get(2)).doubleValue()));
        }

        List<Step> timeline = new ArrayList<>();
        for (var raw : section.getMapList("timeline")) {
            long tick = raw.get("tick") == null ? 0 : ((Number) raw.get("tick")).longValue();
            if (tick < 0) throw new IllegalArgumentException("Scenario " + id + " has a negative tick.");
            if (raw.get("assert") != null) {
                long window = raw.get("window") == null ? 0 : ((Number) raw.get("window")).longValue();
                timeline.add(new Check(tick, window,
                        String.valueOf(raw.get("assert")),
                        str(raw.get("of"), () -> "A check at tick " + tick + " in " + id + " has no subject."),
                        raw.get("because") == null ? "" : String.valueOf(raw.get("because")),
                        rest(raw, "tick", "window", "assert", "of", "because")));
            } else {
                timeline.add(new Act(tick,
                        str(raw.get("actor"), () -> "An act at tick " + tick + " in " + id + " has no actor."),
                        str(raw.get("do"), () -> "An act at tick " + tick + " in " + id + " has no verb."),
                        rest(raw, "tick", "actor", "do")));
            }
        }
        if (timeline.isEmpty()) throw new IllegalArgumentException("Scenario " + id + " has no timeline.");

        var scenario = new Scenario(id, description, List.copyOf(roster), List.copyOf(timeline), needsConnected);
        scenario.verify();
        return scenario;
    }

    /**
     * Every actor and subject must be on the roster, and this is checked at
     * load rather than at the tick it is used.
     *
     * A typo in an actor name would otherwise surface halfway through a run as
     * an aborted scenario, after bodies had been spawned and a world prepared --
     * and the abort would look like a harness fault rather than a typo.
     */
    public void verify() {
        var known = roster.stream().map(Member::id).toList();
        for (Step s : timeline) {
            String who = s instanceof Act a ? a.actor() : ((Check) s).subject();
            if (!known.contains(who))
                throw new IllegalArgumentException(
                        "Scenario " + id + " names '" + who + "', who is not on its roster " + known + ".");
        }
    }

    /** The last tick this scenario has anything to say about. */
    public long lastTick() {
        long last = 0;
        for (Step s : timeline)
            last = Math.max(last, s instanceof Check c ? c.lastTick() : s.tick());
        return last;
    }

    /** How many checks a complete run should report. */
    public long checkCount() {
        return timeline.stream().filter(s -> s instanceof Check).count();
    }

    private static String str(Object value, java.util.function.Supplier<String> complaint) {
        if (value == null) throw new IllegalArgumentException(complaint.get());
        return String.valueOf(value);
    }

    private static Map<String, String> rest(Map<?, ?> raw, String... consumed) {
        var out = new LinkedHashMap<String, String>();
        var skip = List.of(consumed);
        for (var e : raw.entrySet()) {
            String key = String.valueOf(e.getKey());
            if (!skip.contains(key) && e.getValue() != null) out.put(key, String.valueOf(e.getValue()));
        }
        return Map.copyOf(out);
    }
}
