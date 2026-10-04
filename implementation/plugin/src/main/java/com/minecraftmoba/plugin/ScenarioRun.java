package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One scenario, running: the roster it spawned, the timeline it is walking,
 * and the single place everything is torn down.
 *
 * <h2>Teardown is the part that will break, so it has one door</h2>
 *
 * Four paths end a run -- it finishes, a step throws, the admin ends the lab
 * session, or the world unloads -- and all four end the same way if they are
 * handled separately: a body alive in a world that no longer exists. So
 * {@link #teardown} is idempotent and every path goes through it, the same
 * discipline {@code DrillRushAbility.cancel} uses for burrowed invulnerability
 * and {@code DisplacementLedger.release} uses for reservations.
 *
 * <h2>An unverifiable assertion is a failure, not a pass</h2>
 *
 * A scenario that needs real client movement cannot be run against
 * connectionless bodies: the handler under test never fires, so every check
 * would pass without the rule being exercised. That is refused up front in
 * {@link #start}, because a green report over an unexercised rule is the one
 * outcome a harness must never produce.
 */
public final class ScenarioRun {

    /** What a finished run has to say. */
    public record Report(String scenarioId, boolean passed, long checksRun, long checksExpected,
                         List<String> failures, String abortedBecause) {

        public boolean aborted() { return abortedBecause != null; }

        /**
         * A run that ended early is reported as a failure even with no failed
         * check, because the checks it never reached are unknowns rather than
         * successes.
         */
        public String summary() {
            if (aborted()) return scenarioId + ": ABORTED after " + checksRun + "/" + checksExpected
                    + " checks -- " + abortedBecause;
            if (!passed) return scenarioId + ": FAILED " + failures.size() + " of " + checksExpected
                    + " checks\n  " + String.join("\n  ", failures);
            return scenarioId + ": passed " + checksRun + "/" + checksExpected + " checks";
        }
    }

    private final Scenario scenario;
    private final Bodies bodies;
    private final Map<String, Bodies.Body> roster = new LinkedHashMap<>();
    private final List<String> failures = new ArrayList<>();
    private final java.util.function.BiFunction<Scenario.Check, Bodies.Body, String> judge;

    private long tick = -1;
    private long checksRun;
    private boolean started, finished;
    private String abortedBecause;

    /**
     * @param judge evaluates one check against one body, returning null for a
     *              pass or the reason for a failure. Injected so the timeline,
     *              the roster and teardown are all testable without a server.
     */
    public ScenarioRun(Scenario scenario, Bodies bodies,
                       java.util.function.BiFunction<Scenario.Check, Bodies.Body, String> judge) {
        this.scenario = scenario;
        this.bodies = bodies;
        this.judge = judge;
    }

    public Scenario scenario() { return scenario; }
    public boolean running() { return started && !finished; }
    public long tick() { return tick; }
    public Bodies.Body body(String id) { return roster.get(id); }
    public int rosterSize() { return roster.size(); }

    /**
     * Spawn the roster.
     *
     * A body that fails to spawn aborts the run and tears down what was already
     * spawned. A half-populated scenario would run and report on a situation
     * nobody designed.
     */
    public void start() {
        if (started) throw new IllegalStateException("Scenario " + scenario.id() + " already started.");
        started = true;
        if (scenario.requiresConnectedBodies() && !bodies.connected()) {
            abort("needs bodies that generate real client input; " + bodies.describe()
                  + " does not, so its checks would pass without the rule running");
            return;
        }
        try {
            for (Scenario.Member m : scenario.roster()) {
                var body = bodies.spawn(m.id(), m.team(), m.classId(), m.level(), null);
                if (body == null) throw new IllegalStateException("spawn returned nothing for " + m.id());
                roster.put(m.id(), body);
            }
        } catch (RuntimeException e) {
            abort("could not spawn the roster: " + e.getMessage());
        }
    }

    /** Advance to the next tick and run everything scheduled for it. */
    public void advance() {
        if (!running()) return;
        tick++;
        for (Scenario.Step step : scenario.timeline()) {
            try {
                if (step instanceof Scenario.Act act && act.tick() == tick) {
                    perform(act);
                } else if (step instanceof Scenario.Check check && check.tick() == tick) {
                    evaluate(check);
                }
            } catch (RuntimeException e) {
                abort("step at tick " + tick + " threw: " + e.getMessage());
                return;
            }
        }
        if (tick >= scenario.lastTick()) finish();
    }

    private void perform(Scenario.Act act) {
        Bodies.Body actor = roster.get(act.actor());
        if (actor == null) throw new IllegalStateException("no body for actor " + act.actor());
        actions.forEach(a -> a.run(actor, act));
    }

    private void evaluate(Scenario.Check check) {
        Bodies.Body subject = roster.get(check.subject());
        if (subject == null) throw new IllegalStateException("no body for subject " + check.subject());
        checksRun++;
        String why = judge.apply(check, subject);
        if (why != null) failures.add(describe(check) + " -- " + why);
    }

    private static String describe(Scenario.Check c) {
        String base = "tick " + c.tick() + " " + c.assertion() + " of " + c.subject();
        return c.because().isEmpty() ? base : base + " (" + c.because() + ")";
    }

    /** Hooks that carry out an act. Registered by the caller that knows how. */
    public interface Action { void run(Bodies.Body actor, Scenario.Act act); }
    private final List<Action> actions = new ArrayList<>();
    public void onAct(Action action) { actions.add(action); }

    /** End normally. */
    public void finish() { teardown(null); }

    /** End early, for a reason the report will carry. */
    public void abort(String because) { teardown(because == null ? "aborted" : because); }

    /**
     * The one door. Idempotent, and tolerant of a roster that is already gone.
     *
     * Despawn failures are swallowed deliberately: the roster must be cleared
     * even if one body refuses to leave, because the alternative is retrying
     * forever against a world that has already unloaded.
     */
    public void teardown(String because) {
        if (finished) return;
        finished = true;
        if (abortedBecause == null) abortedBecause = because;
        for (Bodies.Body body : roster.values()) {
            try { bodies.despawn(body); } catch (RuntimeException ignored) { /* see above */ }
        }
        roster.clear();
    }

    public Report report() {
        boolean passed = failures.isEmpty() && abortedBecause == null
                      && checksRun == scenario.checkCount();
        return new Report(scenario.id(), passed, checksRun, scenario.checkCount(),
                List.copyOf(failures), abortedBecause);
    }
}
