package com.minecraftmoba.plugin;

import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPlaceEvent;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class ProvenanceEventsTest {
    @Test void deniedBuildDoesNotLeaveAPlayerPlacedMark() {
        var provenance = mock(Provenance.class, CALLS_REAL_METHODS);
        doNothing().when(provenance).mark(any(Block.class), anyBoolean());
        var block = mock(Block.class);
        var denied = mock(BlockPlaceEvent.class);
        when(denied.getBlockPlaced()).thenReturn(block);
        when(denied.canBuild()).thenReturn(false);
        provenance.place(denied);
        verify(provenance, never()).mark(any(), anyBoolean());

        var accepted = mock(BlockPlaceEvent.class);
        when(accepted.getBlockPlaced()).thenReturn(block);
        when(accepted.canBuild()).thenReturn(true);
        provenance.place(accepted);
        verify(provenance).mark(block, true);
    }
}
