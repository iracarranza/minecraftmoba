package com.minecraftmoba.plugin;
import org.bukkit.entity.*;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
final class BurstingSporeAbility implements Ability {
 private final FungalState state;private final ConfigurationSection cfg;
 BurstingSporeAbility(FungalState s,ConfigurationSection c){state=s;cfg=c;}
 public String id(){return "bursting_spore";}public String displayName(){return "Bursting Spore";}public long cooldownTicks(){return cfg.getLong("cooldownTicks");}
 public List<AbilityOutput> outputs(){return AbilityOutput.single("bursting_spore", TargetForm.UNIT_ENTITY, InputForm.INSTANT, Recipients.ENEMIES);}
 public Map<String,String> branches(){return Map.of("submersive","Submersive","airborne_spread","Airborne Spread","root_network","Root Network");}
 public List<String> branchIds(){return List.of("submersive","airborne_spread","root_network");}
 public List<Need> needs(Player p,AbilityContext c){
  return List.of(new Need("an enemy you have infected, within "+(int)cfg.getDouble("range")+" blocks: hit enemies to infect them, then aim at one",(pl,ctx)->{
    LivingEntity target=ctx.onEntity()&&ctx.entity() instanceof LivingEntity living?living:null;
    if(target==null){var hit=AbilityTargeting.ray(pl,pl.getEyeLocation(),pl.getEyeLocation().getDirection(),cfg.getDouble("range"),.3,AbilityTargeting.Relation.ENEMY);if(hit!=null)target=hit.entity();}
    return state.infectedBy(pl,target);},null));
 }
 public boolean execute(Player p,AbilityContext c){LivingEntity target=c.onEntity()&&c.entity() instanceof LivingEntity living?living:null;if(target==null){var hit=AbilityTargeting.ray(p,p.getEyeLocation(),p.getEyeLocation().getDirection(),cfg.getDouble("range"),.3,AbilityTargeting.Relation.ENEMY);if(hit!=null)target=hit.entity();}return target!=null&&state.burst(p,target,c.branchFor(id()));}
 public void tick(){state.tick();}public void forget(Player p){state.forget(p);}public void close(){state.close();}
}
