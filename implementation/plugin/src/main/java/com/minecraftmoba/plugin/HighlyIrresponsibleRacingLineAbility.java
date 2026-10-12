package com.minecraftmoba.plugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
final class HighlyIrresponsibleRacingLineAbility implements Ability {
 private final ChauffeurState state;private final ConfigurationSection cfg;
 HighlyIrresponsibleRacingLineAbility(ChauffeurState s,ConfigurationSection c){state=s;cfg=c;}
 public String id(){return "highly_irresponsible_racing_line";}public String displayName(){return "Highly Irresponsible Racing Line";}public long cooldownTicks(){return cfg.getLong("cooldownTicks");}
 public List<AbilityOutput> outputs(){return AbilityOutput.single("highly_irresponsible_racing_line", TargetForm.AREA_ENTITIES, InputForm.INSTANT, Recipients.ENEMIES);}
 public Map<String,String> branches(){return Map.of();}
 public boolean execute(Player p,AbilityContext c){return state.race(p);}
}
