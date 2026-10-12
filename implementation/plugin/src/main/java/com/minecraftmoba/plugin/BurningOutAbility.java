package com.minecraftmoba.plugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import java.util.*;
final class BurningOutAbility implements Ability {
    private final SkeletonCrewState state;private final ConfigurationSection config;
    BurningOutAbility(SkeletonCrewState state,ConfigurationSection config){this.state=state;this.config=config;}
    public String id(){return "burning_out";}
    public String displayName(){return "Burning Out";}
    public long cooldownTicks(){return config.getLong("cooldownTicks");}
    public List<AbilityOutput> outputs(){return AbilityOutput.single("burning_out", TargetForm.AREA_ENTITIES, InputForm.INSTANT, Recipients.NONE);}
    public Map<String,String> branches(){return Map.of("fire_drill","Fire Drill","deadline","Deadline","water_break","Water Break");}
    public List<String> branchIds(){return List.of("fire_drill","deadline","water_break");}
    public List<Need> needs(Player p,AbilityContext ctx){return List.of(new Need("Crew standing in the world to set alight: raise some first (Graveyard Shift)",(pl,c)->state.hasActiveCrew(pl),(pl,c)->"you have none out"));}
    public boolean execute(Player p,AbilityContext ctx){return state.burn(p,ctx.branchFor(id()));}
}
