package com.minecraftmoba.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Converts what authoring plans into what a chamber judges. Pure. */
final class ChamberBridge {
    private ChamberBridge() {}

    static List<Chamber.At> affected(UUID world, Collection<LabGeometry.Pos> edits) {
        var out = new ArrayList<Chamber.At>(edits.size());
        for (var p : edits) out.add(new Chamber.At(world, p.x(), p.y(), p.z()));
        return out;
    }
}
