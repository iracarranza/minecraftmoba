package com.minecraftmoba.plugin;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The act vocabulary: what a scenario step can make a body do, and what each needs.
 *
 * Validated when a scenario is LOADED, not when the act runs: an act with a missing argument would
 * otherwise throw at its tick, after bodies were spawned and a world prepared, and the abort would
 * read as a harness fault rather than a typo. An act this does not know is refused outright.
 *
 * <pre>
 * hit      target, amount          the actor deals authored damage to a roster member
 * explode  at, power               a systemic explosion centred on a roster member, caused by the actor
 * stun     target, ticks           apply a Stun
 * root     target                  apply a Root
 * walk     direction, ticks        the body reports walking, a step a tick (north, south, east, west)
 * cast     slot, at [level]        the body casts a real ability (a1, a2, ult) facing a roster member;
 *                                 level: true aims along the ground, not down into it
 * look     at                      face a roster member
 * heal                             restore the actor to full health
 * </pre>
 *
 * Pure. Tested without a server.
 */
public final class ScenarioActs {
    private ScenarioActs() {}

    private static final Map<String, List<String>> REQUIRES = Map.of(
            "hit", List.of("target", "amount"),
            "explode", List.of("at", "power"),
            "stun", List.of("target", "ticks"),
            "root", List.of("target"),
            "walk", List.of("direction", "ticks"),
            "cast", List.of("slot", "at"),
            "look", List.of("at"),
            "heal", List.of());

    public static final Set<String> KNOWN = REQUIRES.keySet();
    public static final Set<String> DIRECTIONS = Set.of("north", "south", "east", "west");
    public static final Set<String> SLOTS = Set.of("a1", "a2", "ult");

    /** Arguments that name another roster member. */
    private static final Set<String> MEMBER_ARGS = Set.of("target", "at");

    /** Null when the scenario's acts and checks are all well formed; otherwise the first problem. */
    public static String problem(Scenario s) {
        var ids = s.roster().stream().map(Scenario.Member::id).toList();
        String anchor = ScenarioPlacement.refusal(s.anchor());
        if (anchor != null) return s.id() + ": " + anchor;
        for (var step : s.timeline()) {
            if (step instanceof Scenario.Check c) {
                if (!ScenarioJudge.KNOWN.contains(c.assertion().toLowerCase(java.util.Locale.ROOT)))
                    return s.id() + " tick " + c.tick() + ": unknown assertion '" + c.assertion() + "'. Known: " + new java.util.TreeSet<>(ScenarioJudge.KNOWN);
                continue;
            }
            var a = (Scenario.Act) step;
            var needs = REQUIRES.get(a.verb());
            if (needs == null) return s.id() + " tick " + a.tick() + ": unknown act '" + a.verb() + "'. Known: " + new java.util.TreeSet<>(KNOWN);
            for (String n : needs) if (!a.args().containsKey(n)) return s.id() + " tick " + a.tick() + ": '" + a.verb() + "' needs '" + n + "'.";
            for (String n : MEMBER_ARGS) {
                String v = a.args().get(n);
                if (v != null && !ids.contains(v)) return s.id() + " tick " + a.tick() + ": '" + a.verb() + "' names '" + v + "', who is not on the roster " + ids + ".";
            }
            if (a.verb().equals("walk") && !DIRECTIONS.contains(a.args().get("direction")))
                return s.id() + " tick " + a.tick() + ": walk direction '" + a.args().get("direction") + "' is not one of " + new java.util.TreeSet<>(DIRECTIONS) + ".";
            if (a.verb().equals("cast") && !SLOTS.contains(a.args().get("slot")))
                return s.id() + " tick " + a.tick() + ": cast slot '" + a.args().get("slot") + "' is not one of " + new java.util.TreeSet<>(SLOTS) + ".";
            for (String numeric : List.of("amount", "power", "ticks")) {
                String v = a.args().get(numeric);
                if (v != null) try { Double.parseDouble(v); } catch (NumberFormatException ex) {
                    return s.id() + " tick " + a.tick() + ": '" + numeric + "' is '" + v + "', not a number.";
                }
            }
        }
        return null;
    }

    /** The yaw a direction faces, in Minecraft's convention (south is 0, west 90, north 180, east -90). */
    public static float yaw(String direction) {
        return switch (direction) { case "south" -> 0f; case "west" -> 90f; case "north" -> 180f; case "east" -> -90f;
            default -> throw new IllegalArgumentException("direction " + direction); };
    }

    /** The unit step a direction walks along {x, z}. */
    public static double[] step(String direction) {
        return switch (direction) { case "south" -> new double[]{0, 1}; case "west" -> new double[]{-1, 0};
            case "north" -> new double[]{0, -1}; case "east" -> new double[]{1, 0};
            default -> throw new IllegalArgumentException("direction " + direction); };
    }
}
