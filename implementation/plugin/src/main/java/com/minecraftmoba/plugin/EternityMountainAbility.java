package com.minecraftmoba.plugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
final class EternityMountainAbility implements Ability {
 private final QuarrymanState state;private final ConfigurationSection cfg;
 EternityMountainAbility(QuarrymanState s,ConfigurationSection c){state=s;cfg=c;}
 public String id(){return "eternity_mountain";}public String displayName(){return "Eternity Mountain";}public long cooldownTicks(){return cfg.getLong("cooldownTicks");}
 public List<AbilityOutput> outputs(){return AbilityOutput.single("eternity_mountain", TargetForm.AREA_BLOCKS, InputForm.CHANNELED, Recipients.ENEMIES, true);}
 public boolean execute(Player p,AbilityContext c){return state.channel(p);}
 public boolean active(Player p){return state.channeling(p);}
 public boolean recast(Player p,AbilityContext c){if(state.channeling(p)){state.release(p);return true;}return false;}
 public boolean attackCancels(){return false;}
 public void cancel(Player p){state.cancel(p);}
}
