package com.minecraftmoba.plugin;
import org.bukkit.configuration.ConfigurationSection;
import java.util.List;
import org.bukkit.entity.Player;
final class WoodenMawAbility extends HollowAbility {
 WoodenMawAbility(HollowState state,ConfigurationSection config){super(state,"wooden_maw",config);}
 @Override public List<AbilityOutput> outputs(){return AbilityOutput.single("wooden_maw", TargetForm.AREA_ENTITIES, InputForm.INSTANT, Recipients.ENEMIES, true);}
 public List<AbilityOutput> outputs(Player p,AbilityContext c){return c.branchFor(id()).equals("eerie_resonance")?AbilityOutput.single(id(),TargetForm.AREA_ENTITIES,InputForm.INSTANT,Recipients.BOTH,true):outputs();}
}
