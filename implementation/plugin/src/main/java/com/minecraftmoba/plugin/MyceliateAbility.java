package com.minecraftmoba.plugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
final class MyceliateAbility implements Ability {
 private final FungalState state;private final ConfigurationSection cfg;
 MyceliateAbility(FungalState s,ConfigurationSection c){state=s;cfg=c;}
 public String id(){return "myceliate";}public String displayName(){return "Myceliate";}public long cooldownTicks(){return cfg.getLong("cooldownTicks");}
 public List<AbilityOutput> outputs(){return AbilityOutput.single("myceliate", TargetForm.SELF, InputForm.INSTANT, Recipients.NONE, true);}
 public Map<String,String> branches(){return Map.of("hardy","Hardy","nutrient_cycle","Nutrient Cycle","hyphae_lashers","Hyphae Lashers");}
 public List<String> branchIds(){return List.of("hardy","nutrient_cycle","hyphae_lashers");}
 public boolean cooldownExemptRecast(Player p,AbilityContext c){return state.travelling(p);}
 public boolean execute(Player p,AbilityContext c){return state.travel(p,c.branchFor(id()));}
 public List<AbilityOutput> outputs(Player p,AbilityContext c){return c.branchFor(id()).equals("hyphae_lashers")?AbilityOutput.single(id(),TargetForm.SELF,InputForm.INSTANT,Recipients.ENEMIES,true):outputs();}
}
