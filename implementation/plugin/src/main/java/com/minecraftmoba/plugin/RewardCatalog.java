package com.minecraftmoba.plugin;

import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

/**
 * Opaque choice IDs and configurable effects; no built-in reward content.
 *
 * An option's effect is optional and may be of more than one kind. Capacity was
 * once mandatory here, which quietly made this catalogue a capacity-specialization
 * catalogue wearing a generic name: a Task allocation would have had to declare
 * `capacity: {health: 0, hunger: 0, slots: 0}` to say it changes no capacity at
 * all. Both fields are now nullable, and an option carrying neither is a
 * recorded choice with no built-in effect.
 *
 * `task` names one of the Task trees in TaskLedger. The catalogue only records
 * the allocation; tiers are counted from the stored records by TaskLedger and
 * applied by TaskEffects.
 */
public record RewardCatalog(Map<Integer,List<Option>> levels,Map<String,Capacity.Bonus> bonuses) {
    public record Option(String id,String label,String icon,Capacity.Bonus bonus,String task) {
        public Option(String id,String label,String icon,Capacity.Bonus bonus) { this(id,label,icon,bonus,null); }
    }
    public RewardCatalog {
        var copy=new TreeMap<Integer,List<Option>>(); levels.forEach((k,v)->copy.put(k,List.copyOf(v)));
        levels=Collections.unmodifiableMap(copy); bonuses=Map.copyOf(bonuses);
    }
    public static RewardCatalog load(ConfigurationSection section,int maxLevel) {
        var levels=new TreeMap<Integer,List<Option>>(); var bonuses=new HashMap<String,Capacity.Bonus>();
        var seen=new HashSet<String>();
        if(section==null) throw new IllegalArgumentException("rewards.levels must be a mapping (empty allowed)");
        for(String key:section.getKeys(false)) {
            int level=Integer.parseInt(key);
            if(level<2 || level>maxLevel) throw new IllegalArgumentException("Invalid reward level: "+key);
            if(!section.isList(key)) throw new IllegalArgumentException("Reward level must be a list: "+key);
            var options=new ArrayList<Option>();
            for(Map<?,?> map:section.getMapList(key)) {
                String id=text(map,"id"), label=text(map,"label"), icon=text(map,"icon");
                Capacity.Bonus bonus=null;
                Object raw=map.get("capacity");
                if(raw!=null) {
                    if(!(raw instanceof Map<?,?> effects)) throw new IllegalArgumentException("Reward capacity must be a mapping: "+id);
                    double health=number(effects,"health"), hunger=number(effects,"hunger"), slots=number(effects,"slots");
                    if(hunger%1!=0 || slots%1!=0 || hunger>Integer.MAX_VALUE || slots>Integer.MAX_VALUE)
                        throw new IllegalArgumentException("Reward hunger/slots must be representable integers");
                    bonus=new Capacity.Bonus(health,(int)hunger,(int)slots);
                }
                String task=null;
                Object rawTask=map.get("task");
                if(rawTask!=null) {
                    if(!(rawTask instanceof String t) || !TaskLedger.isTree(t))
                        throw new IllegalArgumentException("Reward task must name a Task tree: "+id);
                    task=t;
                }
                if(bonus!=null && task!=null)
                    throw new IllegalArgumentException("A reward option carries one kind of effect: "+id);
                if(seen.contains(id)) throw new IllegalArgumentException("Duplicate reward ID: "+id);
                seen.add(id);
                if(bonus!=null) bonuses.put(id,bonus);
                options.add(new Option(id,label,icon,bonus,task));
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
    /**
     * Levels whose options allocate a Task tree. This is the single source of
     * the Task clock: TaskLedger reads it rather than a second configured list,
     * so the two cannot drift apart.
     */
    public List<Integer> taskLevels() {
        return levels.entrySet().stream()
            .filter(e -> e.getValue().stream().anyMatch(o -> o.task() != null))
            .map(Map.Entry::getKey).toList();
    }
    public List<Integer> pending(PlayerData data) {
        var spent=new HashSet<Integer>(); data.choices.forEach(c->spent.add(c.level()));
        return levels.keySet().stream().filter(l->l<=data.level && !spent.contains(l)).toList();
    }
}
