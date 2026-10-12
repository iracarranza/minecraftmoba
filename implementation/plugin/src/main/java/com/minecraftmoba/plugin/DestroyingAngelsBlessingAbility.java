package com.minecraftmoba.plugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
final class DestroyingAngelsBlessingAbility implements Ability {
 private final FungalState state;private final ConfigurationSection cfg;
 DestroyingAngelsBlessingAbility(FungalState s,ConfigurationSection c){state=s;cfg=c;}
 public String id(){return "destroying_angels_blessing";}public String displayName(){return "Destroying Angel's Blessing";}public long cooldownTicks(){return cfg.getLong("cooldownTicks");}
 public List<AbilityOutput> outputs(){return AbilityOutput.single("destroying_angels_blessing", TargetForm.UNIT_BLOCK, InputForm.INSTANT, Recipients.ENEMIES);}
 public boolean cooldownExemptRecast(Player p,AbilityContext c){return state.hasAngel(p);}
 public boolean execute(Player p,AbilityContext c){if(state.hasAngel(p))return state.angel(p,p.getLocation());var block=c.onBlock()?c.block():p.getTargetBlockExact(cfg.getInt("range"));return block!=null&&state.angel(p,block.getLocation().add(0,1,0));}
}
