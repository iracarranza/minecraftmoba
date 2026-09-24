package com.minecraftmoba.plugin;
import java.util.*;
/** Generic tick-based per-player/per-ability cooldown state. */
public final class AbilityCooldowns {
 private final Map<UUID,Map<String,Long>> readyAt=new HashMap<>();
 public boolean ready(UUID player,String ability,long now){return remaining(player,ability,now)==0;}
 public void start(UUID player,String ability,long now,long effective){readyAt.computeIfAbsent(player,k->new HashMap<>()).put(ability,now+Math.max(0,effective));}
 public long remaining(UUID player,String ability,long now){return Math.max(0,readyAt.getOrDefault(player,Map.of()).getOrDefault(ability,0L)-now);}
 public void clear(UUID player){readyAt.remove(player);} public void clear(UUID player,String ability){var m=readyAt.get(player);if(m!=null){m.remove(ability);if(m.isEmpty())readyAt.remove(player);}}
}
