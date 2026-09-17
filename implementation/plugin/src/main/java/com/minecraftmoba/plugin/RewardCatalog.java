package com.minecraftmoba.plugin;

import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

/** Opaque choice IDs and configurable numeric effects; no built-in reward content. */
public record RewardCatalog(Map<Integer,List<Option>> levels,Map<String,Capacity.Bonus> bonuses) {
    public record Option(String id,String label,String icon,Capacity.Bonus bonus) {}
    public RewardCatalog {
        var copy=new TreeMap<Integer,List<Option>>(); levels.forEach((k,v)->copy.put(k,List.copyOf(v)));
        levels=Collections.unmodifiableMap(copy); bonuses=Map.copyOf(bonuses);
    }
    public static RewardCatalog load(ConfigurationSection section,int maxLevel) {
        var levels=new TreeMap<Integer,List<Option>>(); var bonuses=new HashMap<String,Capacity.Bonus>();
        if(section==null) throw new IllegalArgumentException("rewards.levels must be a mapping (empty allowed)");
        for(String key:section.getKeys(false)) {
            int level=Integer.parseInt(key);
            if(level<2 || level>maxLevel) throw new IllegalArgumentException("Invalid reward level: "+key);
            if(!section.isList(key)) throw new IllegalArgumentException("Reward level must be a list: "+key);
            var options=new ArrayList<Option>();
            for(Map<?,?> map:section.getMapList(key)) {
                String id=text(map,"id"), label=text(map,"label"), icon=text(map,"icon");
                Object raw=map.get("capacity");
                if(!(raw instanceof Map<?,?> effects)) throw new IllegalArgumentException("Missing reward capacity: "+id);
                double health=number(effects,"health"), hunger=number(effects,"hunger"), slots=number(effects,"slots");
                if(hunger%1!=0 || slots%1!=0 || hunger>Integer.MAX_VALUE || slots>Integer.MAX_VALUE)
                    throw new IllegalArgumentException("Reward hunger/slots must be representable integers");
                var bonus=new Capacity.Bonus(health,(int)hunger,(int)slots);
                if(bonuses.putIfAbsent(id,bonus)!=null) throw new IllegalArgumentException("Duplicate reward ID: "+id);
                options.add(new Option(id,label,icon,bonus));
            }
            if(options.size()>54) throw new IllegalArgumentException("A vanilla chest GUI supports at most 54 choices");
            if(!options.isEmpty()) levels.put(level,List.copyOf(options));
        }
        return new RewardCatalog(levels,bonuses);
    }
    private static String text(Map<?,?> map,String key) {
        Object value=map.get(key);
        if(!(value instanceof String s) || s.isBlank()) throw new IllegalArgumentException("Missing reward "+key);
        return s;
    }
    private static double number(Map<?,?> map,String key) {
        Object value=map.get(key);
        if(!(value instanceof Number n) || !Double.isFinite(n.doubleValue()) || n.doubleValue()<0)
            throw new IllegalArgumentException("Reward "+key+" must be finite and nonnegative");
        return n.doubleValue();
    }
    public List<Integer> pending(PlayerData data) {
        var spent=new HashSet<Integer>(); data.choices.forEach(c->spent.add(c.level()));
        return levels.keySet().stream().filter(l->l<=data.level && !spent.contains(l)).toList();
    }
}
