package com.minecraftmoba.plugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
final class ChamferingAbility implements Ability {
 private final PaverState state;private final ConfigurationSection cfg;
 ChamferingAbility(PaverState state,ConfigurationSection cfg){this.state=state;this.cfg=cfg;}
 public String id(){return "chamfering";}public String displayName(){return "Chamfering";}public long cooldownTicks(){return cfg.getLong("cooldownTicks");}
 public List<AbilityOutput> outputs(){return AbilityOutput.single("chamfering", TargetForm.SELF, InputForm.INSTANT, Recipients.NONE);}
 public Map<String,String> branches(){return Map.of("steep_grading","Steep Grading","pozzolan_patch","Pozzolan Patch","natural_mix","Natural Mix");}
 public List<String> branchIds(){return List.of("steep_grading","pozzolan_patch","natural_mix");}
 public boolean execute(Player p,AbilityContext c){return state.release(p);}
}
