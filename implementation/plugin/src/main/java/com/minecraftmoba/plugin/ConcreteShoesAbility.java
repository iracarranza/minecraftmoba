package com.minecraftmoba.plugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
final class ConcreteShoesAbility implements Ability {
 private final PaverState state;private final ConfigurationSection cfg;
 ConcreteShoesAbility(PaverState state,ConfigurationSection cfg){this.state=state;this.cfg=cfg;}
 public String id(){return "concrete_shoes";}public String displayName(){return "Concrete Shoes";}public long cooldownTicks(){return cfg.getLong("cooldownTicks");}
 public List<AbilityOutput> outputs(){return AbilityOutput.single("concrete_shoes", TargetForm.AREA_BLOCKS, InputForm.INSTANT, Recipients.ENEMIES);}
 public List<Need> needs(Player p,AbilityContext c){
  return List.of(
    new Need("Concrete in your inventory to make the shoes from",(pl,ctx)->java.util.Arrays.stream(pl.getInventory().getStorageContents()).anyMatch(i->i!=null&&PaverState.concrete(i.getType())),(pl,ctx)->"you carry none"),
    new Need("a block to land on, within "+cfg.getInt("range")+" blocks",(pl,ctx)->(ctx.onBlock()?ctx.block():pl.getTargetBlockExact(cfg.getInt("range")))!=null,null));
 }
 public boolean execute(Player p,AbilityContext c){var hit=c.onBlock()?c.block():p.getTargetBlockExact(cfg.getInt("range"));return hit!=null&&state.shoes(p,hit.getLocation().add(0,1,0));}
}
