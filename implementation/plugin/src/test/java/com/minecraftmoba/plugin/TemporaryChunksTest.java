package com.minecraftmoba.plugin;
import org.bukkit.Chunk;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
class TemporaryChunksTest {
    @Test void overlappingCastsKeepTheChunkUntilTheLastLoanReturns(){
        var plugin=mock(MobaPlugin.class);var chunk=mock(Chunk.class);var leases=new TemporaryChunks(plugin);
        leases.hold(chunk);leases.hold(chunk);verify(chunk,times(1)).addPluginChunkTicket(plugin);
        leases.release(chunk);verify(chunk,never()).removePluginChunkTicket(plugin);
        leases.release(chunk);leases.release(chunk);verify(chunk,times(1)).removePluginChunkTicket(plugin);
    }
}
