package com.minecraftmoba.plugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
final class ViscerwallAbility implements Ability {
 private final BloodmasonState state;private final ConfigurationSection cfg;
 ViscerwallAbility(BloodmasonState s,ConfigurationSection c){state=s;cfg=c;}
 public String id(){return "viscerwall";}public String displayName(){return "Viscerwall";}public long cooldownTicks(){return cfg.getLong("cooldownTicks");}
 public List<AbilityOutput> outputs(){return AbilityOutput.of(new AbilityOutput("viscerwall", TargetForm.UNIT_BLOCK, InputForm.INSTANT, Recipients.BOTH, true),new AbilityOutput("teratoma", TargetForm.AREA_BLOCKS, InputForm.CHANNELED, Recipients.BOTH, true));}
 public Map<String,String> branches(){return Map.of("render_flesh","Render Flesh","fracture","Fracture","adipose","Adipose");}
 public List<String> branchIds(){return List.of("render_flesh","fracture","adipose");}
 public boolean execute(Player p,AbilityContext c){if(state.tomb(p)!=null)return state.channel(p,"bone");var b=c.onBlock()?c.block():p.getTargetBlockExact(cfg.getInt("range"));return b!=null&&state.wall(p,b.getLocation().add(0,1,0),c.branchFor(id()));}
 public boolean continuousHold(Player p,AbilityContext c){return state.tomb(p)!=null;}
 public boolean cooldownExemptRecast(Player p,AbilityContext c){return state.tomb(p)!=null;}
 public void cancel(Player p){state.cancelChannel(p);}
 public List<AbilityOutput> outputs(Player p,AbilityContext c){return state.tomb(p)==null?AbilityOutput.single("viscerwall", TargetForm.UNIT_BLOCK, InputForm.INSTANT, Recipients.BOTH, true):AbilityOutput.single("teratoma",TargetForm.AREA_BLOCKS,InputForm.CHANNELED,Recipients.BOTH,true);}
}
