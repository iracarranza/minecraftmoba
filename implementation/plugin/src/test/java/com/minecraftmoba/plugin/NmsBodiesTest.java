package com.minecraftmoba.plugin;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The parts of NmsBodies that do not need a server: the body registry and the
 * outbound sink. Creating a ServerPlayer cannot be tested here; that is what
 * the live probe is for (docs/audit/2026-10-09-bodies-spawn-prototype.md).
 */
class NmsBodiesTest {

    private enum Colour { RED, GREEN }

    @Test void nothingIsABodyUntilItIsRegistered() {
        UUID id = UUID.randomUUID();
        assertFalse(NmsBodies.isBody(id));
        assertFalse(NmsBodies.isBody(null), "a null id is never a body, so a handler need not check it");
    }

    @Test void registrationIsTheOnlyWayToBecomeABodyAndIsUndone() {
        UUID id = UUID.randomUUID();
        NmsBodies.register(id);
        assertTrue(NmsBodies.isBody(id));
        NmsBodies.unregister(id);
        assertFalse(NmsBodies.isBody(id), "a despawned body must stop being special, or the join handlers skip a real player");
    }

    @Test void aBodyIsRegisteredBeforeItJoinsSoJoinHandlersSeeIt() {
        // place() registers the id before placeNewPlayer, because the join event
        // fires inside that call. This pins the ordering the guards rely on.
        UUID id = UUID.randomUUID();
        NmsBodies.register(id);
        try { assertTrue(NmsBodies.isBody(id)); } finally { NmsBodies.unregister(id); }
    }

    @Test void theSinkDiscardsEverythingTheServerSendsABody() {
        var channel = new EmbeddedChannel(new NmsBodies.Sink());
        var buf = Unpooled.buffer(8).writeInt(1);
        channel.writeOutbound(buf);
        channel.writeOutbound("a packet");
        assertEquals(0, channel.outboundMessages().size(),
                "an embedded channel retains outbound messages; the sink is what stops hundreds per second accumulating");
        assertEquals(0, buf.refCnt(), "a discarded buffer must be released, not leaked");
    }

    @Test void theSinkSucceedsTheWriteSoTheServerDoesNotTreatItAsAFailure() {
        var channel = new EmbeddedChannel(new NmsBodies.Sink());
        var future = channel.writeAndFlush("a packet");
        assertTrue(future.isSuccess(), "a failed write would disconnect the body");
        assertTrue(channel.isActive());
    }

    @Test void enumConstantsAreFoundByNameAndAMissingOneSaysSo() {
        assertEquals(Colour.GREEN, NmsBodies.enumConst(Colour.class, "GREEN"));
        var ex = assertThrows(IllegalStateException.class, () -> NmsBodies.enumConst(Colour.class, "BLUE"));
        assertTrue(ex.getMessage().contains("BLUE"), "the message must name what moved");
    }

    // ---- the settle boundary ------------------------------------------------------

    @Test void aBodyIsNotSettledUntilTheMeasuredDelayHasPassed() {
        long spawned = 1000;
        assertFalse(NmsBodies.settled(spawned, spawned), "the tick it spawns on");
        assertFalse(NmsBodies.settled(spawned, spawned + NmsBodies.SETTLE_TICKS - 1));
        assertTrue(NmsBodies.settled(spawned, spawned + NmsBodies.SETTLE_TICKS));
    }

    @Test void theSettleDelayCoversTheObservedSevenTickThreshold() {
        // Measured on Paper 1.21.11-132: input at 6 ticks was dropped, at 7 it worked.
        assertTrue(NmsBodies.SETTLE_TICKS >= 7,
                "a margin below the observed threshold would let input through that the server then ignores");
    }
}
