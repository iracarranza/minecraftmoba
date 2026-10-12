package com.minecraftmoba.plugin;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;
final class RebarChainAbility implements Ability {
 private final PaverState state;private final ConfigurationSection cfg;
 RebarChainAbility(PaverState state,ConfigurationSection cfg){this.state=state;this.cfg=cfg;}
 public String id(){return "rebar_chain";}public String displayName(){return "Rebar Chain";}public long cooldownTicks(){return cfg.getLong("cooldownTicks");}
 public List<AbilityOutput> outputs(){return AbilityOutput.single("rebar_chain", TargetForm.UNIT_BLOCK, InputForm.INSTANT, Recipients.NONE, true);}
 public Map<String,String> branches(){return Map.of("galvanized","Galvanized","beveled_edges","Beveled Edges","pitons","Pitons");}
 public List<String> branchIds(){return List.of("galvanized","beveled_edges","pitons");}
 public boolean cooldownExemptRecast(Player p,AbilityContext c){return c.branchFor(id()).equals("pitons")&&state.chaining(p);}
 public boolean execute(Player p,AbilityContext c){return state.chain(p,c.branchFor(id()));}
 public void tick(){state.tick();}public void forget(Player p){state.forget(p);}public void close(){state.close();}
}
