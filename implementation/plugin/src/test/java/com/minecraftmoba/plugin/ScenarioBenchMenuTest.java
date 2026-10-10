package com.minecraftmoba.plugin;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ScenarioBenchMenuTest {
    @Test void theMenuFitsAndEveryActionIsAVerb() {
        for (var item : ScenarioBenchMenu.menu().view(ChamberMenu.Gate.OPEN).slots())
            if (item != null) assertTrue(ScenarioBenchMenu.VERBS.contains(item.id()), item.id());
    }

    @Test void nothingWorksUntilTheBenchIsStarted() {
        for (String v : ScenarioBenchMenu.VERBS) assertNotNull(ScenarioBenchMenu.refusal(v, new ScenarioBenchMenu.State(false, false)), v);
    }

    @Test void aRunningScenarioCannotBeRestartedOrSwitchedButCanBeStopped() {
        var running = new ScenarioBenchMenu.State(true, true);
        assertTrue(ScenarioBenchMenu.refusal("run", running).contains("already running"));
        assertTrue(ScenarioBenchMenu.refusal("scenario", running).contains("Stop it"));
        assertNull(ScenarioBenchMenu.refusal("stop", running)); assertNull(ScenarioBenchMenu.refusal("report", running));
    }

    @Test void thereIsNothingToStopWhenNothingRuns() {
        var idle = new ScenarioBenchMenu.State(true, false);
        assertTrue(ScenarioBenchMenu.refusal("stop", idle).contains("No scenario"));
        assertNull(ScenarioBenchMenu.refusal("run", idle)); assertNull(ScenarioBenchMenu.refusal("scenario", idle));
    }
}
