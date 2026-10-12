package com.minecraftmoba.plugin;
import org.bukkit.configuration.ConfigurationSection;
import java.util.List;
final class PeekabooAbility extends HollowAbility {
 PeekabooAbility(HollowState state,ConfigurationSection config){super(state,"peekaboo",config);}
 @Override public List<AbilityOutput> outputs(){return AbilityOutput.single("peekaboo", TargetForm.AREA_ENTITIES, InputForm.INSTANT, Recipients.ENEMIES, true);}
}
