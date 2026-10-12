package com.minecraftmoba.plugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
final class MinersStrikeAbility implements Ability {
 private final QuarrymanState state;private final ConfigurationSection cfg;
 MinersStrikeAbility(QuarrymanState s,ConfigurationSection c){state=s;cfg=c;}
 public String id(){return "miners_strike";}public String displayName(){return "Miner’s Strike";}public long cooldownTicks(){return cfg.getLong("cooldownTicks");}
 public List<AbilityOutput> outputs(){return AbilityOutput.single("miners_strike", TargetForm.SELF, InputForm.INSTANT, Recipients.ENEMIES, true);}
 public boolean execute(Player p,AbilityContext c){return state.strike(p);}
 public boolean cooldownExemptRecast(Player p,AbilityContext c){return state.striking(p);}
}
