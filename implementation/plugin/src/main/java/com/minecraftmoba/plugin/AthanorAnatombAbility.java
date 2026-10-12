package com.minecraftmoba.plugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
final class AthanorAnatombAbility implements Ability {
 private final BloodmasonState state;private final ConfigurationSection cfg;
 AthanorAnatombAbility(BloodmasonState s,ConfigurationSection c){state=s;cfg=c;}
 public String id(){return "athanor_anatomb";}public String displayName(){return "Athanor Anatomb";}public long cooldownTicks(){return cfg.getLong("cooldownTicks");}
 public List<AbilityOutput> outputs(){return AbilityOutput.single("athanor_anatomb", TargetForm.AREA_BLOCKS, InputForm.INSTANT, Recipients.BOTH, true);}
 public Map<String,String> branches(){return Map.of();}
 public boolean execute(Player p,AbilityContext c){return state.anatomb(p);}
}
