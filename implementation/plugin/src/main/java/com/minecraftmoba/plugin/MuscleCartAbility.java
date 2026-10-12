package com.minecraftmoba.plugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
final class MuscleCartAbility implements Ability {
 private final ChauffeurState state;private final ConfigurationSection cfg;
 MuscleCartAbility(ChauffeurState s,ConfigurationSection c){state=s;cfg=c;}
 public String id(){return "muscle_cart";}public String displayName(){return "Muscle Cart";}public long cooldownTicks(){return cfg.getLong("cooldownTicks");}
 public List<AbilityOutput> outputs(){return AbilityOutput.single("muscle_cart", TargetForm.SELF, InputForm.INSTANT, Recipients.ENEMIES, true);}
 public Map<String,String> branches(){return Map.of("pop_the_hopper","Pop the Hopper","diesel_engine","Diesel Engine","tactical_dismount","Tactical Dismount");}
 public List<String> branchIds(){return List.of("pop_the_hopper","diesel_engine","tactical_dismount");}
 public boolean execute(Player p,AbilityContext c){return state.summon(p,"cart",c.branchFor(id()));}
}
