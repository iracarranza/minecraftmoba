package com.minecraftmoba.plugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import java.util.*;

/** Shared public glow leases for retained ultimate states and authored reveals. */
final class KitGlow {
    private record Lease(Entity entity, boolean original, Set<String> keys) {}
    private final Map<UUID,Lease> leases = new HashMap<>();
    KitGlow(MobaPlugin plugin) { Bukkit.getScheduler().runTaskTimer(plugin,this::tick,1,1); }
    void acquire(Entity entity,String key) {
        Lease lease=leases.computeIfAbsent(entity.getUniqueId(),id->new Lease(entity,entity.isGlowing(),new HashSet<>()));
        lease.keys.add(key);entity.setGlowing(true);
    }
    void release(Entity entity,String key) {
        Lease lease=leases.get(entity.getUniqueId());if(lease==null)return;
        lease.keys.remove(key);
        if(lease.keys.isEmpty()){leases.remove(entity.getUniqueId());if(entity.isValid())entity.setGlowing(lease.original);}
    }
    private void tick() {for(var lease:List.copyOf(leases.values())){if(!lease.entity.isValid())leases.remove(lease.entity.getUniqueId());else lease.entity.setGlowing(true);}}
    void close() {for(var lease:leases.values())if(lease.entity.isValid())lease.entity.setGlowing(lease.original);leases.clear();}
}
