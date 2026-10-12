package com.minecraftmoba.plugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
final class CeaselessSwingAbility implements Ability {
 private final QuarrymanState state;private final ConfigurationSection cfg;
 CeaselessSwingAbility(QuarrymanState s,ConfigurationSection c){state=s;cfg=c;}
 public String id(){return "ceaseless_swing";}public String displayName(){return "Ceaseless Swing";}public long cooldownTicks(){return cfg.getLong("cooldownTicks");}
 public List<AbilityOutput> outputs(){return AbilityOutput.single("ceaseless_swing", TargetForm.AREA_BLOCKS, InputForm.INSTANT, Recipients.NONE, true);}
 public boolean execute(Player p,AbilityContext c){return state.scan(p);}
 public void tick(){state.tick();}public void forget(Player p){state.forget(p);}public void close(){state.close();}
}
