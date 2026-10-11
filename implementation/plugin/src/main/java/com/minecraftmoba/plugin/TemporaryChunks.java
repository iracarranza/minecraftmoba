package com.minecraftmoba.plugin;
import org.bukkit.Chunk;
import java.util.*;

/** Plugin chunk tickets are sets, so overlapping temporary casts share a reference-counted lease. */
final class TemporaryChunks {
    private final MobaPlugin plugin;private final Map<Chunk,Integer> leases=new HashMap<>();
    TemporaryChunks(MobaPlugin plugin){this.plugin=plugin;}
    void hold(Chunk chunk){if(leases.merge(chunk,1,Integer::sum)==1)chunk.addPluginChunkTicket(plugin);}
    void release(Chunk chunk){Integer count=leases.get(chunk);if(count==null)return;if(count==1){leases.remove(chunk);chunk.removePluginChunkTicket(plugin);}else leases.put(chunk,count-1);}
}
