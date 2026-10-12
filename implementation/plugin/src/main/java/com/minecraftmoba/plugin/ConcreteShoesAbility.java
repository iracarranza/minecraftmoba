package com.minecraftmoba.plugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
final class ConcreteShoesAbility implements Ability {
 private final PaverState state;private final ConfigurationSection cfg;
 ConcreteShoesAbility(PaverState state,ConfigurationSection cfg){this.state=state;this.cfg=cfg;}
 public String id(){return "concrete_shoes";}public String displayName(){return "Concrete Shoes";}public long cooldownTicks(){return cfg.getLong("cooldownTicks");}
 public List<AbilityOutput> outputs(){return AbilityOutput.single("concrete_shoes", TargetForm.AREA_BLOCKS, InputForm.INSTANT, Recipients.ENEMIES);}
 public boolean execute(Player p,AbilityContext c){var hit=c.onBlock()?c.block():p.getTargetBlockExact(cfg.getInt("range"));return hit!=null&&state.shoes(p,hit.getLocation().add(0,1,0));}
}
