package com.minecraftmoba.plugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
final class CapillaryFlowAbility implements Ability {
 private final BloodmasonState state;private final ConfigurationSection cfg;
 CapillaryFlowAbility(BloodmasonState s,ConfigurationSection c){state=s;cfg=c;}
 public String id(){return "capillary_flow";}public String displayName(){return "Capillary Flow";}public long cooldownTicks(){return cfg.getLong("cooldownTicks");}
 public List<AbilityOutput> outputs(){return AbilityOutput.of(new AbilityOutput("capillary_flow", TargetForm.UNIT_BLOCK, InputForm.INSTANT, Recipients.BOTH, true),new AbilityOutput("hemorrhage", TargetForm.AREA_BLOCKS, InputForm.CHANNELED, Recipients.BOTH, true));}
 public Map<String,String> branches(){return Map.of("nausea_wave","Nausea Wave","bloodtrail","Bloodtrail","lifeblood","Lifeblood");}
 public List<String> branchIds(){return List.of("nausea_wave","bloodtrail","lifeblood");}
 public boolean execute(Player p,AbilityContext c){if(state.tomb(p)!=null)return state.channel(p,"blood");var b=c.onBlock()?c.block():p.getTargetBlockExact(cfg.getInt("range"));return b!=null&&state.flow(p,b.getLocation().add(0,1,0),c.branchFor(id()));}
 public boolean continuousHold(Player p,AbilityContext c){return state.tomb(p)!=null;}
 public boolean cooldownExemptRecast(Player p,AbilityContext c){return state.tomb(p)!=null;}
 public void cancel(Player p){state.cancelChannel(p);}
 public List<AbilityOutput> outputs(Player p,AbilityContext c){return state.tomb(p)==null?AbilityOutput.single("capillary_flow", TargetForm.UNIT_BLOCK, InputForm.INSTANT, Recipients.BOTH, true):AbilityOutput.single("hemorrhage",TargetForm.AREA_BLOCKS,InputForm.CHANNELED,Recipients.BOTH,true);}
 public void tick(){state.tick();}public void forget(Player p){state.forget(p);}public void close(){state.close();}
}
