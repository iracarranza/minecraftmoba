package com.minecraftmoba.plugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
final class CargoBoatAbility implements Ability {
 private final ChauffeurState state;private final ConfigurationSection cfg;
 CargoBoatAbility(ChauffeurState s,ConfigurationSection c){state=s;cfg=c;}
 public String id(){return "cargo_boat";}public String displayName(){return "Cargo Boat";}public long cooldownTicks(){return cfg.getLong("cooldownTicks");}
 public List<AbilityOutput> outputs(){return AbilityOutput.single("cargo_boat", TargetForm.SELF, InputForm.INSTANT, Recipients.NONE, true);}
 public Map<String,String> branches(){return Map.of("jetski","Jetski","freight_engine","Freight Engine","front_propeller","Front Propeller");}
 public List<String> branchIds(){return List.of("jetski","freight_engine","front_propeller");}
 public boolean execute(Player p,AbilityContext c){return state.summon(p,"boat",c.branchFor(id()));}
 public void tick(){state.tick();}public void forget(Player p){state.forget(p);}public void close(){state.close();}
 public List<AbilityOutput> outputs(Player p,AbilityContext c){return c.branchFor(id()).equals("front_propeller")?AbilityOutput.single(id(),TargetForm.SELF,InputForm.INSTANT,Recipients.ENEMIES,true):outputs();}
}
